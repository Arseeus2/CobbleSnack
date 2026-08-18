package dev.cobblesnack.cache;

import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public final class DiskCacheStore {
   private static final int SCHEMA = 4;
   private static final String CACHE_VERSION = "1.0.4";
   private static final Object IO_LOCK = new Object();
   private static final String METADATA = "metadata.json";
   private static final String SPECIES = "species.json";
   private static final String SPAWNS = "spawns.json";
   private static final String WARNINGS = "warnings.txt";
   private static final String README = "README.txt";

   private DiskCacheStore() {
   }

   public static Path cacheDirectory() {
      return gameDir().resolve("config").resolve("cobblesnack").resolve("cache");
   }

   public static DataIndex currentIndex() {
      Class<DataIndex> var1 = DataIndex.class;
      synchronized (DataIndex.class) {
         DataIndex var2 = reflectedInstance();
         if (var2 != null) {
            SessionDiagnostics.indexReused(var2);
            return var2;
         }

         long var3 = System.nanoTime();
         DataIndex var5 = tryLoad();
         String var6 = "disk";
         if (var5 == null) {
            var6 = "source-scan";
            var5 = scanSources();
            save(var5);
         }

         setReflectedInstance(var5);
         SessionDiagnostics.indexReady(var6, var5, System.nanoTime() - var3);
         return var5;
      }
   }

   public static void reloadIndex() {
      Class<DataIndex> var1 = DataIndex.class;
      synchronized (DataIndex.class) {
         long var2 = System.nanoTime();
         DataIndex var4 = scanSources();
         setReflectedInstance(var4);
         save(var4);
         invokeStaticNoArgs("dev.cobblesnack.calc.BiomeCatalog", "clearCache");
         invokeStaticNoArgs("dev.cobblesnack.calc.BestPokeSnackOptimizer", "clearCache");
         invokeStaticNoArgs("dev.cobblesnack.calc.BestPokeSnackOptimizer", "clearPreparedIndex");
         invokeStaticNoArgs("dev.cobblesnack.client.MinimapSpriteResolver", "invalidate");
         clearStaticMap("dev.cobblesnack.client.PokemonIconFactory", "CACHE");
         SessionDiagnostics.reload(var4, System.nanoTime() - var2);
      }
   }

   private static DataIndex scanSources() {
      try {
         Method var0 = DataIndex.class.getDeclaredMethod("load");
         var0.setAccessible(true);
         if (var0.invoke(null) instanceof DataIndex var2) {
            return var2;
         } else {
            throw new IllegalStateException("DataIndex.load() returned no index");
         }
      } catch (ReflectiveOperationException var3) {
         throw new IllegalStateException("Could not scan CobbleSnack source data", var3);
      }
   }

   private static DataIndex reflectedInstance() {
      try {
         Field var1 = DataIndex.class.getDeclaredField("INSTANCE");
         var1.setAccessible(true);
         Object var2 = var1.get(null);
         DataIndex var0;
         return var2 instanceof DataIndex ? (var0 = (DataIndex)var2) : null;
      } catch (Throwable var3) {
         return null;
      }
   }

   private static void setReflectedInstance(DataIndex var0) {
      try {
         Field var1 = DataIndex.class.getDeclaredField("INSTANCE");
         var1.setAccessible(true);
         var1.set(null, var0);
      } catch (Throwable var2) {
         throw new IllegalStateException("Could not publish CobbleSnack DataIndex", var2);
      }
   }

   public static void ensureDiskCache() {
      DataIndex.get();
   }

   public static DataIndex tryLoad() {
      synchronized (IO_LOCK) {
         if (!isCacheUsable()) {
            return null;
         }

         DataIndex var10000;
         try {
            var10000 = readIndex();
         } catch (Throwable var4) {
            SessionDiagnostics.error("disk-cache-load", var4);
            writeDiagnostic("cache-load-error.txt", var4.toString());
            return null;
         }

         return var10000;
      }
   }

   public static void save(DataIndex var0) {
      if (var0 != null) {
         long var1 = System.nanoTime();
         synchronized (IO_LOCK) {
            try {
               Path var5 = cacheDirectory();
               Files.createDirectories(var5);
               Map var6 = speciesDocument(var0);
               Map var7 = spawnsDocument(var0);
               LinkedHashMap var8 = new LinkedHashMap();
               var8.put("schema", SCHEMA);
               var8.put("cobblesnackVersion", CACHE_VERSION);
               var8.put("createdUtc", Instant.now().toString());
               var8.put("sourceFingerprint", sourceFingerprint());
               var8.put("speciesCount", var0.uniqueSpecies().size());
               var8.put("spawnEntryCount", var0.spawns().size());
               var8.put("spawnRuleFileCount", var0.spawnRuleFileCount());
               var8.put("scope", "global client cache shared by all worlds and servers");
               atomicWrite(var5.resolve("species.json"), prettyJson(var6));
               atomicWrite(var5.resolve("spawns.json"), prettyJson(var7));
               atomicWrite(var5.resolve("metadata.json"), prettyJson(var8));
               atomicWrite(var5.resolve("warnings.txt"), String.join(System.lineSeparator(), var0.warnings()) + System.lineSeparator());
               atomicWrite(var5.resolve("README.txt"), readmeText());
               Files.deleteIfExists(var5.resolve("cache-load-error.txt"));
               SessionDiagnostics.diskCacheSaved(var0, System.nanoTime() - var1);
            } catch (Throwable var10) {
               SessionDiagnostics.error("disk-cache-save", var10);
               writeDiagnostic("cache-save-error.txt", var10.toString());
            }
         }
      }
   }

   public static void releaseRuntimeCaches() {
      SessionDiagnostics.uiClose();
      invokeStaticNoArgs("dev.cobblesnack.calc.BiomeCatalog", "clearCache");
      invokeStaticNoArgs("dev.cobblesnack.calc.BestPokeSnackOptimizer", "clearCache");
      SessionDiagnostics.afterEphemeralClear();
   }

   public static void saveCurrentIndex() {
      try {
         save(DataIndex.get());
      } catch (Throwable var1) {
      }
   }

   private static void releaseDataIndexOnly() {
      try {
         Field var0 = DataIndex.class.getDeclaredField("INSTANCE");
         var0.setAccessible(true);
         var0.set(null, null);
      } catch (Throwable var1) {
      }
   }

   private static boolean isCacheUsable() {
      try {
         Path var0 = cacheDirectory();
         Path var1 = var0.resolve("metadata.json");
         if (Files.isRegularFile(var1) && Files.isRegularFile(var0.resolve("species.json")) && Files.isRegularFile(var0.resolve("spawns.json"))) {
            if (!(parseJson(Files.readString(var1, StandardCharsets.UTF_8), Map.class) instanceof Map var3)) {
               return false;
            } else {
               if (intValue(var3.get("schema"), -1) != SCHEMA) {
                  return false;
               }

               String var4 = stringValue(var3.get("sourceFingerprint"));
               return !var4.isBlank() && var4.equals(sourceFingerprint());
            }
         } else {
            return false;
         }
      } catch (Throwable var5) {
         return false;
      }
   }

   private static DataIndex readIndex() throws Exception {
      Path var0 = cacheDirectory();
      Object var1 = parseJson(Files.readString(var0.resolve("species.json"), StandardCharsets.UTF_8), Map.class);
      Object var2 = parseJson(Files.readString(var0.resolve("spawns.json"), StandardCharsets.UTF_8), Map.class);
      Object var3 = parseJson(Files.readString(var0.resolve("metadata.json"), StandardCharsets.UTF_8), Map.class);
      if (var1 instanceof Map var4 && var2 instanceof Map var5) {
         DataIndex var6 = new DataIndex();
         Field var7 = DataIndex.class.getDeclaredField("species");
         Field var8 = DataIndex.class.getDeclaredField("spawns");
         Field var9 = DataIndex.class.getDeclaredField("warnings");
         Field var10 = DataIndex.class.getDeclaredField("spawnRuleFileCount");
         var7.setAccessible(true);
         var8.setAccessible(true);
         var9.setAccessible(true);
         var10.setAccessible(true);
         Map var11 = (Map)var7.get(var6);
         List var12 = (List)var8.get(var6);
         List var13 = (List)var9.get(var6);
         if (!(var4.get("species") instanceof List var15)) {
            throw new IOException("species.json has no species array");
         } else {
            for (Object var17 : var15) {
               if (var17 instanceof Map var18) {
                  String var19 = stringValue(var18.get("key"));
                  if (!var19.isBlank()) {
                     SpeciesInfo var20 = new SpeciesInfo(var19, stringValue(var18.get("displayName")));
                     var20.setNationalPokedexNumber(intValue(var18.get("nationalPokedexNumber"), Integer.MAX_VALUE));
                     var20.setResourceId(nullableString(var18.get("resourceId")));
                     var20.setPrimaryType(nullableString(var18.get("primaryType")));
                     var20.setSecondaryType(nullableString(var18.get("secondaryType")));
                     var20.setEggGroups(stringList(var18.get("eggGroups")));
                     var20.setEvYield(intMap(var18.get("evYield")));
                     var20.setBaseAspects(stringList(var18.get("baseAspects")));
                     Object var21 = var18.get("forms");
                     if (var21 instanceof List) {
                        for (Object var24 : (List)var21) {
                           if (var24 instanceof Map var25) {
                              var20.addForm(
                                 new SpeciesInfo.FormInfo(
                                    stringValue(var25.get("name")),
                                    stringList(var25.get("aspects")),
                                    nullableString(var25.get("primaryType")),
                                    nullableString(var25.get("secondaryType")),
                                    stringList(var25.get("eggGroups")),
                                    intMap(var25.get("evYield"))
                                 )
                              );
                           }
                        }
                     }

                     var11.put(var19, var20);

                     for (String var39 : stringList(var18.get("aliases"))) {
                        if (!var39.isBlank()) {
                           var11.put(var39, var20);
                        }
                     }
                  }
               }
            }

            if (!(var5.get("spawns") instanceof List var28)) {
               throw new IOException("spawns.json has no spawns array");
            } else {
               for (Object var31 : var28) {
                  if (var31 instanceof Map var34) {
                     ArrayList var36 = new ArrayList();
                     Object var38 = var34.get("weightMultipliers");
                     if (var38 instanceof List) {
                        for (Object var42 : (List)var38) {
                           if (var42 instanceof Map var26) {
                              var36.add(
                                 new SpawnEntry.WeightMultiplier(
                                    doubleValue(var26.get("multiplier"), 1.0),
                                    conditionsFrom(var26.get("conditions")),
                                    conditionsFrom(var26.get("antiConditions"))
                                 )
                              );
                           }
                        }
                     }

                     var12.add(
                        new SpawnEntry(
                           stringValue(var34.get("id")),
                           stringValue(var34.get("pokemon")),
                           stringValue(var34.get("speciesKey")),
                           nullableString(var34.get("context")),
                           stringValue(var34.get("bucket")),
                           doubleValue(var34.get("weight"), 1.0),
                           conditionsFrom(var34.get("conditions")),
                           conditionsFrom(var34.get("antiConditions")),
                           var36
                        )
                     );
                  }
               }

               Path var30 = var0.resolve("warnings.txt");
               if (Files.isRegularFile(var30)) {
                  for (String var35 : Files.readAllLines(var30, StandardCharsets.UTF_8)) {
                     if (!var35.isBlank()) {
                        var13.add(var35);
                     }
                  }
               }

               if (var3 instanceof Map var33) {
                  var10.setInt(var6, intValue(var33.get("spawnRuleFileCount"), 0));
               }

               return var6;
            }
         }
      } else {
         throw new IOException("Cache JSON root is not an object");
      }
   }

   private static Map<String, Object> speciesDocument(DataIndex var0) throws Exception {
      IdentityHashMap<SpeciesInfo, List<String>> var1 = new IdentityHashMap<>();

      for (Entry<String, SpeciesInfo> var3 : var0.species().entrySet()) {
         var1.computeIfAbsent(var3.getValue(), var0x -> new ArrayList<>()).add(var3.getKey());
      }

      ArrayList var11 = new ArrayList();

      for (SpeciesInfo var4 : var0.uniqueSpecies()) {
         LinkedHashMap var5 = new LinkedHashMap();
         var5.put("key", var4.key());
         var5.put("displayName", var4.displayName());
         var5.put("nationalPokedexNumber", var4.nationalPokedexNumber());
         var5.put("resourceId", var4.resourceId());
         var5.put("primaryType", var4.primaryType());
         var5.put("secondaryType", var4.secondaryType());
         var5.put("eggGroups", var4.eggGroups());
         var5.put("evYield", var4.evYield());
         var5.put("baseAspects", var4.baseAspects());
         ArrayList<String> var6 = new ArrayList<>(var1.getOrDefault(var4, List.of()));
         var6.remove(var4.key());
         var6.sort(String.CASE_INSENSITIVE_ORDER);
         var5.put("aliases", var6);
         ArrayList var7 = new ArrayList();

         for (SpeciesInfo.FormInfo var9 : var4.forms()) {
            LinkedHashMap var10 = new LinkedHashMap();
            var10.put("name", var9.name());
            var10.put("aspects", var9.aspects());
            var10.put("primaryType", privateField(var9, "primaryType"));
            var10.put("secondaryType", privateField(var9, "secondaryType"));
            var10.put("eggGroups", privateField(var9, "eggGroups"));
            var10.put("evYield", privateField(var9, "evYield"));
            var7.add(var10);
         }

         var5.put("forms", var7);
         var11.add(var5);
      }

      LinkedHashMap var13 = new LinkedHashMap();
      var13.put("description", "Global CobbleSnack species/form cache. Shared by every world/server.");
      var13.put("species", var11);
      return var13;
   }

   private static Map<String, Object> spawnsDocument(DataIndex var0) {
      ArrayList var1 = new ArrayList(var0.spawns().size());

      for (SpawnEntry var3 : var0.spawns()) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("id", var3.id);
         var4.put("pokemon", var3.pokemonExpression);
         var4.put("speciesKey", var3.speciesKey);
         var4.put("context", var3.context);
         var4.put("bucket", var3.bucket);
         var4.put("weight", var3.weight);
         var4.put("conditions", conditionListDocument(var3.conditions));
         var4.put("antiConditions", conditionListDocument(var3.antiConditions));
         ArrayList var5 = new ArrayList();

         for (SpawnEntry.WeightMultiplier var7 : var3.weightMultipliers) {
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("multiplier", var7.multiplier());
            var8.put("conditions", conditionListDocument(var7.conditions()));
            var8.put("antiConditions", conditionListDocument(var7.antiConditions()));
            var5.add(var8);
         }

         var4.put("weightMultipliers", var5);
         var1.add(var4);
      }

      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("description", "Global CobbleSnack world-spawn cache. Shared by every world/server.");
      var9.put("spawns", var1);
      return var9;
   }

   private static List<Object> conditionListDocument(List<SpawnCondition> var0) {
      ArrayList var1 = new ArrayList();

      for (SpawnCondition var3 : var0) {
         var1.add(conditionDocument(var3));
      }

      return var1;
   }

   private static Map<String, Object> conditionDocument(SpawnCondition var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      putIfNotEmpty(var1, "dimensions", var0.dimensions);
      putIfNotEmpty(var1, "biomes", var0.biomes);
      putIfNotEmpty(var1, "structures", var0.structures);
      putIfNotNull(var1, "moonPhase", var0.moonPhase);
      putIfNotNull(var1, "canSeeSky", var0.canSeeSky);
      putIfNotNull(var1, "minX", var0.minX);
      putIfNotNull(var1, "minY", var0.minY);
      putIfNotNull(var1, "minZ", var0.minZ);
      putIfNotNull(var1, "maxX", var0.maxX);
      putIfNotNull(var1, "maxY", var0.maxY);
      putIfNotNull(var1, "maxZ", var0.maxZ);
      putIfNotNull(var1, "minLight", var0.minLight);
      putIfNotNull(var1, "maxLight", var0.maxLight);
      putIfNotNull(var1, "minSkyLight", var0.minSkyLight);
      putIfNotNull(var1, "maxSkyLight", var0.maxSkyLight);
      putIfNotNull(var1, "timeRange", var0.timeRange);
      putIfNotNull(var1, "isRaining", var0.isRaining);
      putIfNotNull(var1, "isThundering", var0.isThundering);
      putIfNotNull(var1, "isSlimeChunk", var0.isSlimeChunk);
      putIfNotEmpty(var1, "labels", var0.labels);
      putIfNotNull(var1, "labelMode", var0.labelMode);
      putIfNotNull(var1, "minWidth", var0.minWidth);
      putIfNotNull(var1, "maxWidth", var0.maxWidth);
      putIfNotNull(var1, "minHeight", var0.minHeight);
      putIfNotNull(var1, "maxHeight", var0.maxHeight);
      putIfNotEmpty(var1, "neededNearbyBlocks", var0.neededNearbyBlocks);
      putIfNotEmpty(var1, "neededBaseBlocks", var0.neededBaseBlocks);
      putIfNotNull(var1, "minDepth", var0.minDepth);
      putIfNotNull(var1, "maxDepth", var0.maxDepth);
      putIfNotNull(var1, "fluidIsSource", var0.fluidIsSource);
      putIfNotEmpty(var1, "fluid", var0.fluid);
      putIfNotNull(var1, "minLureLevel", var0.minLureLevel);
      putIfNotNull(var1, "maxLureLevel", var0.maxLureLevel);
      putIfNotEmpty(var1, "bobber", var0.bobbers);
      putIfNotEmpty(var1, "bait", var0.bait);
      putIfNotEmpty(var1, "rodType", var0.rodTypes);
      if (!var0.unknownKeys.isEmpty()) {
         var1.put("_unknownKeys", var0.unknownKeys);
      }

      return var1;
   }

   private static List<SpawnCondition> conditionsFrom(Object var0) throws Exception {
      if (!(var0 instanceof List var1)) {
         return List.of();
      } else {
         ArrayList var2 = new ArrayList();

         for (Object var4 : var1) {
            if (var4 instanceof Map<?, ?> var5) {
               LinkedHashMap<String, Object> var6 = new LinkedHashMap<>();

               for (Entry<?, ?> var8 : var5.entrySet()) {
                  String var9 = String.valueOf(var8.getKey());
                  if (!var9.startsWith("_")) {
                     var6.put(var9, var8.getValue());
                  }
               }

               Object var12 = var5.get("_unknownKeys");
               if (var12 instanceof Iterable<?> var13) {
                  for (Object var10 : var13) {
                     if (var10 != null) {
                        var6.put(String.valueOf(var10), Boolean.TRUE);
                     }
                  }
               }

               Class var14 = Class.forName("com.google.gson.JsonObject");
               Object var16 = parseJson(compactJson(var6), var14);
               if (SpawnCondition.class.getMethod("from", var14).invoke(null, var16) instanceof SpawnCondition var11) {
                  var2.add(var11);
               }
            }
         }

         return List.copyOf(var2);
      }
   }

   private static Object privateField(Object var0, String var1) throws Exception {
      Field var2 = var0.getClass().getDeclaredField(var1);
      var2.setAccessible(true);
      return var2.get(var0);
   }

   static Path gameDirectoryForDiagnostics() {
      return gameDir();
   }

   private static Path gameDir() {
      try {
         Class var0 = Class.forName("net.fabricmc.loader.api.FabricLoader");
         Object var1 = var0.getMethod("getInstance").invoke(null);
         if (var0.getMethod("getGameDir").invoke(var1) instanceof Path var3) {
            return var3;
         }
      } catch (Throwable var4) {
      }

      return Paths.get(".").toAbsolutePath().normalize();
   }

   private static String sourceFingerprint() {
      try {
         MessageDigest var0 = MessageDigest.getInstance("SHA-256");
         List<String> var1 = new ArrayList<>();
         var1.add("schema=" + SCHEMA);
         var1.addAll(installedModFingerprints());
         Path var2 = gameDir();
         addContentFingerprint(var1, var2, var2.resolve("options.txt"));
         addContentFingerprint(var1, var2, var2.resolve("config").resolve("global_packs.toml"));
         addTreeFingerprint(var1, var2, var2.resolve("resourcepacks"));
         addTreeFingerprint(var1, var2, var2.resolve("datapacks"));
         addTreeFingerprint(var1, var2, var2.resolve("global_packs"));
         Collections.sort(var1);

         for (String var4 : var1) {
            var0.update(var4.getBytes(StandardCharsets.UTF_8));
            var0.update((byte)10);
         }

         return HexFormat.of().formatHex(var0.digest());
      } catch (Throwable var5) {
         return "fallback-" + CACHE_VERSION + "-schema-" + SCHEMA;
      }
   }

   private static List<String> installedModFingerprints() {
      ArrayList var0 = new ArrayList();

      try {
         Class var1 = Class.forName("net.fabricmc.loader.api.FabricLoader");
         Object var2 = var1.getMethod("getInstance").invoke(null);
         Object var3 = var1.getMethod("getAllMods").invoke(var2);
         if (var3 instanceof Iterable) {
            for (Object var6 : (Iterable)var3) {
               Object var7 = var6.getClass().getMethod("getMetadata").invoke(var6);
               String var8 = String.valueOf(var7.getClass().getMethod("getId").invoke(var7));
               Object var9 = var7.getClass().getMethod("getVersion").invoke(var7);
               var0.add("mod:" + var8 + "=" + var9);
            }
         }
      } catch (Throwable var10) {
      }

      return var0;
   }

   private static void addFileFingerprint(List<String> var0, Path var1, Path var2) {
      try {
         if (!Files.exists(var2)) {
            return;
         }

         var0.add("file:" + safeRelative(var1, var2) + ":" + Files.size(var2) + ":" + Files.getLastModifiedTime(var2).toMillis());
      } catch (Throwable var4) {
      }
   }

   private static void addContentFingerprint(List<String> var0, Path var1, Path var2) {
      try {
         if (!Files.isRegularFile(var2)) {
            return;
         }

         MessageDigest var3 = MessageDigest.getInstance("SHA-256");

         try (InputStream var4 = Files.newInputStream(var2)) {
            byte[] var6 = new byte[8192];

            int var5;
            while ((var5 = var4.read(var6)) >= 0) {
               if (var5 > 0) {
                  var3.update(var6, 0, var5);
               }
            }
         }

         var0.add("content:" + safeRelative(var1, var2) + ":" + HexFormat.of().formatHex(var3.digest()));
      } catch (Throwable var9) {
      }
   }

   private static void addTreeFingerprint(List<String> var0, Path var1, Path var2) {
      if (Files.isDirectory(var2)) {
         try (Stream<Path> var3 = Files.walk(var2, 10)) {
            for (Path var5 : var3.filter(var0x -> Files.isRegularFile(var0x)).toList()) {
               String var6 = var5.getFileName().toString().toLowerCase(Locale.ROOT);
               if (var6.endsWith(".json") || var6.endsWith(".zip") || var6.endsWith(".toml") || var6.equals("pack.mcmeta")) {
                  addFileFingerprint(var0, var1, var5);
               }
            }
         } catch (Throwable var9) {
         }
      }
   }

   private static String safeRelative(Path var0, Path var1) {
      try {
         return var0.toAbsolutePath().normalize().relativize(var1.toAbsolutePath().normalize()).toString().replace('\\', '/');
      } catch (Throwable var3) {
         return var1.toString().replace('\\', '/');
      }
   }

   private static String readmeText() {
      return "CobbleSnack global disk cache\n=============================\n\nThis is ONE cache shared by every singleplayer world and every server.\nCobbleSnack does not create per-world or per-server copies.\n\nFiles:\n  metadata.json  - cache version, source fingerprint and counts\n  species.json   - all cached Pokémon, forms, typings, EV yields and aliases\n  spawns.json    - all cached world spawn entries and parsed conditions\n  warnings.txt   - data-scan warnings/notes\n\nCobbleSnack overwrites these same files when the source data changes or\nwhen you press Reload data. It does not keep old generations/history.\n\nHybrid RAM behavior:\n  These files remain the persistent/source-of-truth cache. At Minecraft\n  startup CobbleSnack parses them once into ONE process-wide DataIndex and\n  reuses that same object across every world/server and every GUI open.\n  This is faster than reparsing species.json/spawns.json on every open.\n\n  Temporary optimizer/biome result caches are cleared whenever the main\n  CobbleSnack screen closes. Sprite/icon lookups and the one DataIndex stay\n  resident for fast reopen, are bounded by the installed Pokemon/resources,\n  and are replaced (not duplicated) by Reload data.\n\nTemporary diagnostics:\n  config/cobblesnack/diagnostics/session.log records timings, cache entry\n  counts and JVM memory snapshots. It is overwritten each Minecraft launch\n  and capped at 4 MiB.\n";
   }

   private static String prettyJson(Object var0) throws Exception {
      Class var1 = Class.forName("com.google.gson.GsonBuilder");
      Object var2 = var1.getConstructor().newInstance();
      var1.getMethod("setPrettyPrinting").invoke(var2);
      Object var3 = var1.getMethod("create").invoke(var2);
      return (String)var3.getClass().getMethod("toJson", Object.class).invoke(var3, var0);
   }

   private static String compactJson(Object var0) throws Exception {
      Class var1 = Class.forName("com.google.gson.Gson");
      Object var2 = var1.getConstructor().newInstance();
      return (String)var1.getMethod("toJson", Object.class).invoke(var2, var0);
   }

   private static Object parseJson(String var0, Class<?> var1) throws Exception {
      Class var2 = Class.forName("com.google.gson.Gson");
      Object var3 = var2.getConstructor().newInstance();
      return var2.getMethod("fromJson", String.class, Class.class).invoke(var3, var0, var1);
   }

   private static void atomicWrite(Path var0, String var1) throws IOException {
      Files.createDirectories(var0.getParent());
      Path var2 = var0.resolveSibling(var0.getFileName().toString() + ".tmp");
      Files.writeString(
         var2, var1 == null ? "" : var1, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
      );

      try {
         Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException var4) {
         Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static void writeDiagnostic(String var0, String var1) {
      try {
         atomicWrite(cacheDirectory().resolve(var0), var1 + System.lineSeparator());
      } catch (Throwable var3) {
      }
   }

   private static void invokeStaticNoArgs(String var0, String var1) {
      try {
         Class.forName(var0).getMethod(var1).invoke(null);
      } catch (Throwable var3) {
      }
   }

   private static void clearStaticMap(String var0, String var1) {
      try {
         Field var2 = Class.forName(var0).getDeclaredField(var1);
         var2.setAccessible(true);
         if (var2.get(null) instanceof Map var4) {
            var4.clear();
         }
      } catch (Throwable var5) {
      }
   }

   private static void putIfNotNull(Map<String, Object> var0, String var1, Object var2) {
      if (var2 != null) {
         var0.put(var1, var2);
      }
   }

   private static void putIfNotEmpty(Map<String, Object> var0, String var1, Collection<?> var2) {
      if (var2 != null && !var2.isEmpty()) {
         var0.put(var1, var2);
      }
   }

   private static String stringValue(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }

   private static String nullableString(Object var0) {
      String var1 = stringValue(var0);
      return !var1.isBlank() && !"null".equalsIgnoreCase(var1) ? var1 : null;
   }

   private static int intValue(Object var0, int var1) {
      if (var0 instanceof Number var2) {
         return var2.intValue();
      } else {
         try {
            return Integer.parseInt(stringValue(var0));
         } catch (Exception var3) {
            return var1;
         }
      }
   }

   private static double doubleValue(Object var0, double var1) {
      if (var0 instanceof Number var3) {
         return var3.doubleValue();
      } else {
         try {
            return Double.parseDouble(stringValue(var0));
         } catch (Exception var4) {
            return var1;
         }
      }
   }

   private static List<String> stringList(Object var0) {
      if (var0 instanceof String var5) {
         return var5.isBlank() ? List.of() : List.of(var5);
      } else if (var0 instanceof Iterable<?> var1) {
         List<String> var2 = new ArrayList<>();

         for (Object var4 : var1) {
            if (var4 != null) {
               var2.add(String.valueOf(var4));
            }
         }

         return List.copyOf(var2);
      } else {
         return List.of();
      }
   }

   private static Map<String, Integer> intMap(Object var0) {
      if (!(var0 instanceof Map<?, ?> var1)) {
         return Map.of();
      } else {
         Map<String, Integer> var2 = new LinkedHashMap<>();

         for (Entry<?, ?> var4 : var1.entrySet()) {
            var2.put(String.valueOf(var4.getKey()), intValue(var4.getValue(), 0));
         }

         return var2;
      }
   }
}
