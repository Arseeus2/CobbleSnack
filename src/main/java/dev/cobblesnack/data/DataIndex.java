package dev.cobblesnack.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cobblesnack.cache.DataAuditSourceStore;
import dev.cobblesnack.cache.DiskCacheStore;
import dev.cobblesnack.cache.SessionDiagnostics;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class DataIndex {
   private static volatile DataIndex INSTANCE;
   private static volatile boolean CONTEXT_CACHE_CHECKED;
   private static volatile int AUDIT_SOURCES_RESTORED_ID;
   private final Map<String, SpeciesInfo> species = new LinkedHashMap<>();
   private final List<SpawnEntry> spawns = new ArrayList<>();
   private final List<String> warnings = new ArrayList<>();
   private int spawnRuleFileCount;
   private int inactiveSpawnRuleFileCount;
   private int excludedSpawnRouteCount;
   private final List<String> excludedSpawnRoutes = new ArrayList<>();
   private volatile List<SpeciesInfo> browserSpecies;
   private volatile Map<String, List<DataIndex.FormSpawnOption>> spawnFormOptionsBySpecies;
   private static final String BASE_FORM_KEY = "__base__";
   private static final Set<String> NON_FORM_POKEMON_PROPERTIES = Set.of(
      "level",
      "shiny",
      "nature",
      "ability",
      "friendship",
      "pokeball",
      "ball",
      "ivs",
      "minperfectivs",
      "evs",
      "moves",
      "moveset",
      "status",
      "uncatchable",
      "nickname",
      "helditem"
   );

   public static DataIndex get() {
      DataIndex var0 = DiskCacheStore.currentIndex();
      int var1 = System.identityHashCode(var0);
      if (var1 != AUDIT_SOURCES_RESTORED_ID) {
         DataAuditSourceStore.restore(var0);
         AUDIT_SOURCES_RESTORED_ID = var1;
      }

      if (CONTEXT_CACHE_CHECKED) {
         return var0;
      }

      Class<DataIndex> var3 = DataIndex.class;
      synchronized (DataIndex.class) {
         if (CONTEXT_CACHE_CHECKED) {
            return var0;
         }

         CONTEXT_CACHE_CHECKED = true;
         boolean var4 = !var0.spawns.isEmpty() && var0.spawns.stream().allMatch(var0x -> "grounded".equals(var0x.context));
         boolean var5 = var0.spawns.stream().anyMatch(var0x -> !Double.isFinite(var0x.weight) || var0x.weight <= 0.0);
         if (!var4 && !var5) {
            return var0;
         }

         SessionDiagnostics.event(
            "legacy-context-cache",
            var4
               ? "All cached routes were grounded; rebuilding for Cobblemon spawnablePositionType."
               : "Cached inactive/zero-weight routes were found; rebuilding the active spawn index."
         );
         DiskCacheStore.reloadIndex();
         DataIndex var6 = DiskCacheStore.currentIndex();
         AUDIT_SOURCES_RESTORED_ID = System.identityHashCode(var6);
         return var6;
      }
   }

   public static void reload() {
      DiskCacheStore.reloadIndex();
   }

   public Map<String, SpeciesInfo> species() {
      return Collections.unmodifiableMap(this.species);
   }

   public List<SpawnEntry> spawns() {
      return Collections.unmodifiableList(this.spawns);
   }

   public List<String> warnings() {
      return Collections.unmodifiableList(this.warnings);
   }

   public int spawnRuleFileCount() {
      return this.spawnRuleFileCount;
   }

   public int inactiveSpawnRuleFileCount() {
      return this.inactiveSpawnRuleFileCount;
   }

   public int excludedSpawnRouteCount() {
      return this.excludedSpawnRouteCount;
   }

   public List<String> excludedSpawnRoutes() {
      return Collections.unmodifiableList(this.excludedSpawnRoutes);
   }

   public void restoreAuditCounts(int var1, int var2, Collection<String> var3) {
      this.inactiveSpawnRuleFileCount = Math.max(0, var1);
      this.excludedSpawnRouteCount = Math.max(0, var2);
      this.excludedSpawnRoutes.clear();
      if (var3 != null) {
         this.excludedSpawnRoutes.addAll(var3);
      }
   }

   public List<SpeciesInfo> uniqueSpecies() {
      Map<String, SpeciesInfo> var1 = new LinkedHashMap<>();

      for (SpeciesInfo var3 : this.species.values()) {
         var1.putIfAbsent(var3.key(), var3);
      }

      return var1.values().stream().sorted(Comparator.comparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER)).toList();
   }

   public List<SpeciesInfo> browserSpecies() {
      List<SpeciesInfo> var1 = this.browserSpecies;
      if (var1 != null) {
         return var1;
      }

      synchronized (this) {
         var1 = this.browserSpecies;
         if (var1 == null) {
            Map<String, SpeciesInfo> var3 = new LinkedHashMap<>();

            for (SpawnEntry var5 : this.spawns) {
               SpeciesInfo var6 = this.findSpecies(var5.speciesKey);
               if (var6 != null) {
                  var3.putIfAbsent(var6.key(), var6);
               }
            }

            var1 = var3.values().stream().sorted(Comparator.comparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER)).toList();
            this.browserSpecies = var1;
         }

         return var1;
      }
   }

   public SpeciesInfo findSpecies(String var1) {
      String var2 = SpeciesInfo.normalize(var1);
      SpeciesInfo var3 = this.species.get(var2);
      if (var3 != null) {
         return var3;
      }

      for (SpeciesInfo var5 : this.species.values()) {
         if (SpeciesInfo.normalize(var5.displayName()).equals(var2)) {
            return var5;
         }
      }

      return null;
   }

   public List<SpawnEntry> spawnsForSpecies(String var1) {
      SpeciesInfo var2 = this.findSpecies(var1);
      return var2 == null ? List.of() : this.spawns.stream().filter(var2x -> {
         if (var2x.speciesKey.equals(var2.key())) {
            return true;
         }

         SpeciesInfo var3 = this.findSpecies(var2x.speciesKey);
         return var3 != null && var3.key().equals(var2.key());
      }).toList();
   }

   public List<SpawnEntry> spawnsForSpecies(String var1, String var2) {
      if (FormPolicy.collapseSpawnForms(this, var1)) {
         return this.spawnsForSpecies(var1);
      } else {
         List<SpawnEntry> var3 = this.spawnsForSpecies(var1);
         if (var2 != null && !var2.isBlank()) {
            SpeciesInfo var4 = this.findSpecies(var1);
            String var5 = var2.trim().toLowerCase(Locale.ROOT);
            return var3.stream().filter(var2x -> var5.equals(spawnVariant(var4, var2x.pokemonExpression).key())).toList();
         } else {
            return var3;
         }
      }
   }

   public List<DataIndex.FormSpawnOption> spawnFormOptions(String var1) {
      SpeciesInfo var2 = this.findSpecies(var1);
      return var2 != null && !FormPolicy.collapseSpawnForms(this, var2.key()) ? this.cachedSpawnFormOptions().getOrDefault(var2.key(), List.of()) : List.of();
   }

   public Set<String> selectableSpawnFormSpeciesKeys() {
      return this.cachedSpawnFormOptions().keySet();
   }

   private Map<String, List<DataIndex.FormSpawnOption>> cachedSpawnFormOptions() {
      Map<String, List<DataIndex.FormSpawnOption>> var1 = this.spawnFormOptionsBySpecies;
      if (var1 != null) {
         return var1;
      }

      synchronized (this) {
         var1 = this.spawnFormOptionsBySpecies;
         if (var1 != null) {
            return var1;
         }

         Map<String, LinkedHashMap<String, String>> var5 = new LinkedHashMap<>();

         for (SpawnEntry var7 : this.spawns) {
            SpeciesInfo var8 = this.findSpecies(var7.speciesKey);
            if (var8 != null && !FormPolicy.collapseSpawnForms(this, var8.key())) {
               DataIndex.SpawnVariant var4 = spawnVariant(var8, var7.pokemonExpression);
               var5.computeIfAbsent(var8.key(), var0 -> new LinkedHashMap<>()).putIfAbsent(var4.key(), var4.label());
            }
         }

         Map<String, List<DataIndex.FormSpawnOption>> var16 = new LinkedHashMap<>();

         for (Entry<String, LinkedHashMap<String, String>> var18 : var5.entrySet()) {
            LinkedHashMap<String, String> var15 = var18.getValue();
            if (var15.size() != 1 || !var15.containsKey("__base__")) {
               Map<String, String> var9 = var15;
               List<DataIndex.FormSpawnOption> var10 = var9.entrySet()
                  .stream()
                  .map(var0 -> new DataIndex.FormSpawnOption(var0.getKey(), var0.getValue()))
                  .sorted(
                     Comparator.<DataIndex.FormSpawnOption, Boolean>comparing(var0 -> !"__base__".equals(var0.key()))
                        .thenComparing(DataIndex.FormSpawnOption::label, String.CASE_INSENSITIVE_ORDER)
                  )
                  .toList();
               var16.put(var18.getKey(), var10);
            }
         }

         this.spawnFormOptionsBySpecies = var1 = Collections.unmodifiableMap(var16);
         return var1;
      }
   }

   private static DataIndex.SpawnVariant spawnVariant(SpeciesInfo var0, String var1) {
      if (var1 != null && !var1.isBlank()) {
         String[] var6 = var1.trim().split("\\s+");
         if (var6.length <= 1) {
            return new DataIndex.SpawnVariant("__base__", "Base form");
         }

         List<String> var7 = new ArrayList<>();
         List<String> var8 = new ArrayList<>();

         for (int var9 = 1; var9 < var6.length; var9++) {
            String var5 = cleanPropertyToken(var6[var9]);
            if (!var5.isBlank()) {
               int var10 = var5.indexOf(61);
               if (var10 < 0) {
                  if (!NON_FORM_POKEMON_PROPERTIES.contains(SpeciesInfo.normalize(var5))) {
                     String var19 = simplifyVariantAtom(var5);
                     if (!var19.isBlank() && !var7.contains(var19)) {
                        var7.add(var19);
                     }

                     String var17;
                     if (!(var17 = prettyVariantWord(var5)).isBlank() && !var8.contains(var17)) {
                        var8.add(var17);
                     }
                  }
               } else {
                  String var4 = var5.substring(0, var10);
                  String var3 = var5.substring(var10 + 1);
                  String var11 = SpeciesInfo.normalize(var4);
                  if (!var11.isBlank() && !NON_FORM_POKEMON_PROPERTIES.contains(var11)) {
                     for (String var15 : var3.split("[,;|]+")) {
                        addPropertyAtoms(var7, var4, var15);
                        String var16 = friendlyPropertyLabel(var4, var15);
                        if (!var16.isBlank() && !var8.contains(var16)) {
                           var8.add(var16);
                        }
                     }
                  }
               }
            }
         }

         if (var7.isEmpty()) {
            return new DataIndex.SpawnVariant("__base__", "Base form");
         }

         List<String> var22 = var7.stream().filter(var0x -> !var0x.isBlank()).distinct().sorted().toList();
         String var21 = "route:" + String.join("+", var22);
         if (var0 != null && "minior".equals(var0.key())) {
            if (var8.size() >= 2 && "Core".equalsIgnoreCase((String)var8.get(0))) {
               return new DataIndex.SpawnVariant(var21, (String)var8.get(1) + " Core");
            }

            if (!var8.isEmpty()) {
               return new DataIndex.SpawnVariant(var21, String.join(" • ", var8));
            }
         }

         SpeciesInfo.FormInfo var2;
         String var20;
         if ((var2 = bestMatchingForm(var0, var22, var1)) != null) {
            String var18 = var2.name() == null ? "" : var2.name().trim();
            var20 = var18.isBlank() ? friendlyAtomsLabel(var2.aspects()) : var18;
         } else {
            var20 = var8.isEmpty() ? friendlyAtomsLabel(var22) : String.join(" • ", var8);
         }

         return new DataIndex.SpawnVariant(var21, var20);
      } else {
         return new DataIndex.SpawnVariant("__base__", "Base form");
      }
   }

   private static SpeciesInfo.FormInfo bestMatchingForm(SpeciesInfo var0, List<String> var1, String var2) {
      if (var0 != null && !var0.forms().isEmpty()) {
         SpeciesInfo.FormInfo var3 = null;
         int var4 = -1;
         String var5 = simplifyVariantAtom(var2);

         for (SpeciesInfo.FormInfo var7 : var0.forms()) {
            List<String> var10 = var7.aspects();
            int var8;
            boolean var9;
            if (!var10.isEmpty()) {
               var9 = true;
               var8 = 0;

               for (String var12 : var10) {
                  if (!variantAspectMatches(var12, var1, var5)) {
                     var9 = false;
                     break;
                  }

                  var8++;
               }
            } else {
               String var11 = simplifyVariantAtom(var7.name());
               var9 = !var11.isBlank() && (var5.contains(var11) || var1.stream().anyMatch(var1x -> variantsEquivalent(var1x, var11)));
               var8 = var9 ? 1 : 0;
            }

            if (var9 && var8 > var4) {
               var3 = var7;
               var4 = var8;
            }
         }

         return var3;
      } else {
         return null;
      }
   }

   private static boolean variantAspectMatches(String var0, List<String> var1, String var2) {
      String var3 = simplifyVariantAtom(var0);
      if (var3.isBlank()) {
         return false;
      }

      if (var2.contains(var3)) {
         return true;
      }

      for (String var5 : var1) {
         if (variantsEquivalent(var5, var3)) {
            return true;
         }
      }

      return false;
   }

   private static boolean variantsEquivalent(String var0, String var1) {
      String var2 = simplifyVariantAtom(var0);
      String var3 = simplifyVariantAtom(var1);
      return !var2.isBlank() && !var3.isBlank() && (var2.equals(var3) || var2.contains(var3) || var3.contains(var2));
   }

   private static void addPropertyAtoms(List<String> var0, String var1, String var2) {
      String var3 = var1 == null ? "" : var1.toLowerCase(Locale.ROOT).replace('-', '_');
      String var4 = var2 == null ? "" : var2.toLowerCase(Locale.ROOT).replaceAll("[\\[\\]{}()\"']", "");
      String var5 = simplifyVariantAtom(var4);
      if (!var5.isBlank()) {
         addAtom(var0, var5);
         String[] var6 = var3.split("[_-]+");

         for (String var10 : var6) {
            String var11 = singularize(SpeciesInfo.normalize(var10));
            if (!var11.isBlank()) {
               addAtom(var0, simplifyVariantAtom(var4 + var11));
               addAtom(var0, simplifyVariantAtom(var11 + var4));
            }
         }

         String var12 = singularize(SpeciesInfo.normalize(var3));
         if (!var12.isBlank()) {
            addAtom(var0, simplifyVariantAtom(var12 + var4));
         }

         if (var3.contains("breed")) {
            addAtom(var0, simplifyVariantAtom(var4 + "breed"));
         }

         if (var3.contains("flower")) {
            addAtom(var0, simplifyVariantAtom(var4 + "flower"));
         }

         if (var3.contains("sea")) {
            addAtom(var0, simplifyVariantAtom(var4 + "sea"));
         }

         if (var3.contains("season")) {
            addAtom(var0, simplifyVariantAtom(var4 + "season"));
         }

         if (var3.contains("segment")) {
            addAtom(var0, simplifyVariantAtom(var4 + "segment"));
         }

         if (var3.contains("family")) {
            addAtom(var0, simplifyVariantAtom("family" + var4));
         }
      }
   }

   private static void addAtom(List<String> var0, String var1) {
      if (var1 != null && !var1.isBlank() && !var0.contains(var1)) {
         var0.add(var1);
      }
   }

   private static String cleanPropertyToken(String var0) {
      return var0 == null ? "" : var0.trim().replaceAll("^[,;]+|[,;]+$", "");
   }

   private static String singularize(String var0) {
      if (var0 == null) {
         return "";
      } else if (var0.endsWith("ies") && var0.length() > 3) {
         return var0.substring(0, var0.length() - 3) + "y";
      } else {
         return var0.endsWith("s") && var0.length() > 3 ? var0.substring(0, var0.length() - 1) : var0;
      }
   }

   private static String simplifyVariantAtom(String var0) {
      String var1 = SpeciesInfo.normalize(var0);
      var1 = var1.replace("hisuian", "hisui").replace("alolan", "alola").replace("galarian", "galar").replace("paldean", "paldea");
      var1 = var1.replace("flowercolour", "flowercolor");

      for (String var3 : List.of("form", "variant", "aspect", "mode", "style", "pattern", "colour", "color", "of")) {
         var1 = var1.replace(var3, "");
      }

      return var1;
   }

   private static String friendlyPropertyLabel(String var0, String var1) {
      String var2 = var0 == null ? "" : var0.toLowerCase(Locale.ROOT).replace('-', '_');
      String var3 = var1 == null ? "" : var1.replaceAll("[\\[\\]{}()\"']", "");
      if (var3.isBlank()) {
         return "";
      }

      String var5 = SpeciesInfo.normalize(var2);
      if ("true".equalsIgnoreCase(var3)) {
         if (var5.contains("whiscashnero")) {
            return "Nero";
         }

         if (var5.contains("wooperheart")) {
            return "Heart";
         }
      }

      if (var2.contains("breed")) {
         return prettyForm(var3) + " Breed";
      } else if (var2.contains("flower")) {
         return prettyForm(var3) + " Flower";
      } else if (var2.contains("sea")) {
         return prettyForm(var3) + " Sea";
      } else if (var2.contains("season")) {
         return prettyForm(var3);
      } else if (var2.contains("gender")) {
         return prettyForm(var3);
      } else if (var2.contains("segment")) {
         return prettyForm(var3) + " Segment";
      } else {
         return var2.contains("family") ? "Family " + prettyForm(var3) : prettyForm(var3);
      }
   }

   private static String prettyVariantWord(String var0) {
      String var1 = var0 == null ? "" : var0.trim();
      if (var1.equalsIgnoreCase("hisuian") || var1.equalsIgnoreCase("hisui")) {
         return "Hisuian";
      } else if (var1.equalsIgnoreCase("alolan") || var1.equalsIgnoreCase("alola")) {
         return "Alolan";
      } else if (var1.equalsIgnoreCase("galarian") || var1.equalsIgnoreCase("galar")) {
         return "Galarian";
      } else {
         return !var1.equalsIgnoreCase("paldean") && !var1.equalsIgnoreCase("paldea") ? prettyForm(var1) : "Paldean";
      }
   }

   private static String friendlyAtomsLabel(Collection<String> var0) {
      return var0 != null && !var0.isEmpty() ? var0.stream().map(DataIndex::prettyVariantWord).distinct().collect(Collectors.joining(" • ")) : "Form";
   }

   private static String prettyForm(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.replace('_', ' ').replace('-', ' ');
         StringBuilder var2 = new StringBuilder();
         boolean var3 = true;

         for (char var7 : var1.toCharArray()) {
            if (var3 && Character.isLetter(var7)) {
               var2.append(Character.toUpperCase(var7));
               var3 = false;
            } else {
               var2.append(var7);
            }

            if (var7 == ' ') {
               var3 = true;
            }
         }

         return var2.toString();
      } else {
         return "Form";
      }
   }

   private static DataIndex load() {
      DataIndex var0 = new DataIndex();
      LinkedHashMap<String, DataIndex.ResourceJson> var1 = new LinkedHashMap<>();

      for (ModContainer var3 : FabricLoader.getInstance().getAllMods()) {
         Optional<Path> var4 = var3.findPath("data");
         if (!var4.isEmpty()) {
            scanDataRoot((Path)var4.get(), var1, var0.warnings, "mod " + var3.getMetadata().getId());
         }
      }

      Path var13 = FabricLoader.getInstance().getGameDir();
      Set<Path> var14 = new LinkedHashSet<>();
      int var15 = var1.size();
      int var5 = scanEnabledResourcePacks(var13, var1, var0.warnings, var14);
      int var6 = Math.max(0, var1.size() - var15);
      var0.warnings
         .add(
            var5 > 0
               ? "Loaded active combined pack data from " + var5 + " enabled resource pack(s) (" + var6 + " effective resource(s))."
               : "No enabled combined resource-pack data source was found; using mod/datapack data only."
         );
      scanConfiguredDatapackLocation(var13.resolve("datapacks"), var1, var0.warnings, var14);
      scanConfiguredDatapackLocation(var13.resolve("global_packs").resolve("required_data"), var1, var0.warnings, var14);
      scanConfiguredDatapackLocation(var13.resolve("global_packs").resolve("optional_data"), var1, var0.warnings, var14);
      scanGlobalPacksConfig(var13, var1, var0.warnings, var14);
      Map<String, DataIndex.SpawnPreset> var7 = new HashMap<>();

      for (Entry<String, DataIndex.ResourceJson> var9 : var1.entrySet()) {
         DataIndex.ResourceJson var10 = var9.getValue();
         switch (var10.category) {
            case "species":
               var0.parseSpecies(var10);
               break;
            case "spawn_detail_presets":
               var0.parsePreset(var9.getKey(), var10, var7);
               break;
            case "spawn_rules":
               if (getBoolean(var10.json, "enabled", true) && var0.modsSatisfied(var10.json)) {
                  var0.spawnRuleFileCount++;
               } else {
                  var0.inactiveSpawnRuleFileCount++;
               }
         }
      }

      for (DataIndex.ResourceJson var18 : var1.values()) {
         if (var18.category.equals("species_additions")) {
            var0.parseSpeciesAddition(var18);
         }
      }

      for (DataIndex.ResourceJson var19 : var1.values()) {
         if (var19.category.equals("spawn_pool_world")) {
            var0.parseSpawnPool(var19, var7);
         }
      }

      var0.warnings.add("Loaded " + var0.uniqueSpecies().size() + " Pokémon and " + var0.spawns.size() + " world spawn entries.");
      if (var0.spawnRuleFileCount > 0) {
         var0.warnings.add(var0.spawnRuleFileCount + " spawn-rule file(s) detected; arbitrary MoLang spawn-rule effects are still not evaluated.");
      }

      if (var0.excludedSpawnRouteCount > 0) {
         var0.warnings.add("Ignored " + var0.excludedSpawnRouteCount + " inactive spawn route(s), including disabled pools and non-positive weights.");
      }

      DataAuditSourceStore.save(var0);
      return var0;
   }

   private static int scanEnabledResourcePacks(Path var0, LinkedHashMap<String, DataIndex.ResourceJson> var1, List<String> var2, Set<Path> var3) {
      Path var4 = var0.resolve("resourcepacks");
      Path var5 = var0.resolve("options.txt");
      if (!Files.isDirectory(var4)) {
         return 0;
      }

      if (!Files.isRegularFile(var5)) {
         var2.add("Could not find options.txt; local resource-pack data was not scanned to avoid using disabled/stale packs.");
         return 0;
      }

      String var6 = null;

      try {
         for (String var8 : Files.readAllLines(var5, StandardCharsets.UTF_8)) {
            if (var8.startsWith("resourcePacks:")) {
               var6 = var8.substring("resourcePacks:".length()).trim();
               break;
            }
         }
      } catch (IOException var19) {
         var2.add("Could not read enabled resource packs from options.txt: " + var19.getMessage());
         return 0;
      }

      if (var6 != null && !var6.isBlank()) {
         Matcher var20 = Pattern.compile("\"([^\"]*)\"").matcher(var6);
         List<String> var21 = new ArrayList<>();

         while (var20.find()) {
            String var9 = unescapeOptionsString(var20.group(1));
            if (var9 != null && !var9.isBlank()) {
               var21.add(var9);
            }
         }

         int var22 = 0;
         Set<String> var10 = new LinkedHashSet<>();

         for (String var12 : var21) {
            String var14 = null;
            if (var12.startsWith("file/")) {
               var14 = var12.substring("file/".length());
            } else if (var12.startsWith("file:")) {
               var14 = var12.substring("file:".length());
            }

            if (var14 != null && !var14.isBlank()) {
               Path var15 = var4.resolve(var14).normalize();
               Path var16 = var4.toAbsolutePath().normalize();

               Path var13;
               try {
                  var13 = var15.toAbsolutePath().normalize();
               } catch (Exception var18) {
                  var13 = var15;
               }

               if (!var13.startsWith(var16)) {
                  var2.add("Ignored resource pack path outside resourcepacks/: " + var14);
               } else {
                  var10.add(var13.getFileName().toString());
                  if (!Files.exists(var13)) {
                     var2.add("Enabled resource pack was not found on disk: " + var14);
                  } else {
                     int var17 = var1.size();
                     scanConfiguredDatapackLocation(var13, var1, var2, var3);
                     if (var1.size() != var17 || packContainsDataTree(var13)) {
                        var22++;
                     }
                  }
               }
            }
         }

         int var23 = countDisabledCombinedPacks(var4, var10);
         if (var23 > 0) {
            var2.add("Ignored " + var23 + " disabled resource pack(s) containing data/ so they cannot override active spawn routes.");
         }

         return var22;
      } else {
         return 0;
      }
   }

   private static String unescapeOptionsString(String var0) {
      if (var0 == null) {
         return null;
      }

      StringBuilder var1 = new StringBuilder(var0.length());
      boolean var2 = false;

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var2) {
            var1.append(switch (var4) {
               case '"' -> '"';
               case '\\' -> '\\';
               case 'n' -> '\n';
               case 'r' -> '\r';
               case 't' -> '\t';
               default -> var4;
            });
            var2 = false;
         } else if (var4 == '\\') {
            var2 = true;
         } else {
            var1.append(var4);
         }
      }

      if (var2) {
         var1.append('\\');
      }

      return var1.toString();
   }

   private static boolean packContainsDataTree(Path var0) {
      try {
         if (Files.isDirectory(var0)) {
            return Files.isDirectory(var0.resolve("data"));
         }

         if (!var0.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return false;
         }

         try (FileSystem var1 = FileSystems.newFileSystem(var0)) {
            if (Files.isDirectory(var1.getPath("/data"))) {
               boolean var14 = true;
               return true;
            }

            Stream<Path> var3 = Files.walk(var1.getPath("/"), 3);

            boolean var2;
            try {
               var2 = var3.filter(var0x -> Files.isRegularFile(var0x))
                  .filter(var0x -> var0x.getFileName().toString().equals("pack.mcmeta"))
                  .anyMatch(var0x -> Files.isDirectory(var0x.getParent().resolve("data")));
               if (var3 == null) {
                  return var2;
               }
            } catch (Throwable var9) {
               Throwable var4 = var9;
               if (var3 == null) {
                  throw var9;
               }

               try {
                  var3.close();
                  throw var4;
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
                  throw var9;
               }
            }

            var3.close();
            return var2;
         }
      } catch (Exception var11) {
         return false;
      }
   }

   private static int countDisabledCombinedPacks(Path var0, Set<String> var1) {
      int var2 = 0;

      try (Stream<Path> var3 = Files.list(var0)) {
         for (Path var5 : var3.toList()) {
            if (!var1.contains(var5.getFileName().toString()) && packContainsDataTree(var5)) {
               var2++;
            }
         }
      } catch (IOException var8) {
      }

      return var2;
   }

   private static void scanConfiguredDatapackLocation(Path var0, LinkedHashMap<String, DataIndex.ResourceJson> var1, List<String> var2, Set<Path> var3) {
      if (var0 != null) {
         Path var4;
         try {
            var4 = var0.toAbsolutePath().normalize();
         } catch (Exception var6) {
            var4 = var0.normalize();
         }

         if (var3.add(var4) && Files.exists(var4)) {
            if (Files.isRegularFile(var4)) {
               if (var4.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                  scanDatapackZip(var4, var1, var2);
               }
            } else {
               Path var5 = var4.resolve("data");
               if (Files.isDirectory(var5) && Files.isRegularFile(var4.resolve("pack.mcmeta"))) {
                  scanDataRoot(var5, var1, var2, "datapack " + var4.getFileName());
               } else {
                  scanInstanceDatapacks(var4, var1, var2);
               }
            }
         }
      }
   }

   private static void scanGlobalPacksConfig(Path var0, LinkedHashMap<String, DataIndex.ResourceJson> var1, List<String> var2, Set<Path> var3) {
      Path var4 = var0.resolve("config").resolve("global_packs.toml");
      if (Files.isRegularFile(var4)) {
         try {
            List<String> var5 = Files.readAllLines(var4, StandardCharsets.UTF_8);
            boolean var6 = false;
            StringBuilder var7 = new StringBuilder();

            for (String var9 : var5) {
               String var11 = var9.replaceFirst("#.*$", "").trim();
               if (!var11.isBlank()) {
                  if (var11.startsWith("[") && var11.endsWith("]")) {
                     var6 = var11.equalsIgnoreCase("[datapacks]");
                     var7.setLength(0);
                  } else if (var6) {
                     if (var7.length() > 0) {
                        var7.append(' ');
                     }

                     var7.append(var11);
                     if (var7.indexOf("[") < 0 || var7.indexOf("]") >= 0) {
                        String var12 = var7.toString();
                        var7.setLength(0);
                        int var13 = var12.indexOf(61);
                        String var10;
                        if (var13 >= 0 && ((var10 = var12.substring(0, var13).trim().toLowerCase(Locale.ROOT)).equals("required") || var10.equals("optional"))) {
                           String var14 = var12.substring(var13 + 1);
                           Matcher var15 = Pattern.compile("[\\\"']([^\\\"']+)[\\\"']").matcher(var14);

                           while (var15.find()) {
                              String var16 = var15.group(1).trim();
                              if (!var16.isBlank()) {
                                 Path var17 = Paths.get(var16);
                                 if (!var17.isAbsolute()) {
                                    var17 = var0.resolve(var17);
                                 }

                                 scanConfiguredDatapackLocation(var17, var1, var2, var3);
                              }
                           }
                        }
                     }
                  }
               }
            }
         } catch (Exception var18) {
            var2.add("Could not read Global Packs datapack paths: " + var18.getMessage());
         }
      }
   }

   private static void scanDatapackZip(Path var0, LinkedHashMap<String, DataIndex.ResourceJson> var1, List<String> var2) {
      try (FileSystem var3 = FileSystems.newFileSystem(var0)) {
         Set<Path> var4 = new LinkedHashSet<>();
         Path var5 = var3.getPath("/data");
         if (Files.isDirectory(var5)) {
            var4.add(var5);
         }

         if (var4.isEmpty()) {
            try (Stream<Path> var6 = Files.walk(var3.getPath("/"), 3)) {
               for (Path var8 : var6.filter(var0x -> Files.isRegularFile(var0x)).filter(var0x -> var0x.getFileName().toString().equals("pack.mcmeta")).toList()) {
                  Path var9 = var8.getParent().resolve("data");
                  if (Files.isDirectory(var9)) {
                     var4.add(var9);
                  }
               }
            }
         }

         for (Path var16 : var4) {
            scanDataRoot(var16, var1, var2, "pack " + var0.getFileName());
         }
      } catch (Exception var14) {
         var2.add("Could not scan pack " + var0.getFileName() + ": " + var14.getMessage());
      }
   }

   private static void scanInstanceDatapacks(Path var0, LinkedHashMap<String, DataIndex.ResourceJson> var1, List<String> var2) {
      if (Files.isDirectory(var0)) {
         try {
            Stream<Path> var3 = Files.walk(var0, 4);

            try {
               for (Path var6 : var3.filter(var0x -> Files.isRegularFile(var0x))
                  .filter(var0x -> var0x.getFileName().toString().equals("pack.mcmeta"))
                  .map(Path::getParent)
                  .distinct()
                  .toList()) {
                  Path var7 = var6.resolve("data");
                  if (Files.isDirectory(var7)) {
                     scanDataRoot(var7, var1, var2, "datapack " + var6.getFileName());
                  }
               }
            } finally {
               if (var3 != null) {
                  var3.close();
               }
            }
         } catch (IOException var21) {
            var2.add("Could not discover folder datapacks: " + var21.getMessage());
         }

         try {
            Stream<Path> var22 = Files.walk(var0, 3);

            try {
               for (Path var24 : var22.filter(var0x -> Files.isRegularFile(var0x))
                  .filter(var0x -> var0x.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                  .toList()) {
                  scanDatapackZip(var24, var1, var2);
               }
            } finally {
               if (var22 != null) {
                  var22.close();
               }
            }
         } catch (IOException var19) {
            var2.add("Could not discover ZIP datapacks: " + var19.getMessage());
         }
      }
   }

   private static void scanDataRoot(Path var0, LinkedHashMap<String, DataIndex.ResourceJson> var1, List<String> var2, String var3) {
      try (Stream<Path> var4 = Files.walk(var0)) {
         for (Path var6 : var4.filter(var0x -> Files.isRegularFile(var0x)).filter(var0x -> var0x.getFileName().toString().endsWith(".json")).toList()) {
            Path var7 = var0.relativize(var6);
            if (var7.getNameCount() >= 3) {
               String var8 = var7.getName(0).toString();
               String var9 = var7.getName(1).toString();
               if (isInterestingCategory(var9)) {
                  String var10 = var8 + ":" + var7.subpath(1, var7.getNameCount()).toString().replace('\\', '/');

                  try {
                     BufferedReader var11 = Files.newBufferedReader(var6, StandardCharsets.UTF_8);

                     try {
                        JsonElement var12 = JsonParser.parseReader(var11);
                        if (var12.isJsonObject()) {
                           DataIndex.ResourceJson var13 = new DataIndex.ResourceJson(var8, var9, var6, var12.getAsJsonObject(), var3);
                           DataIndex.ResourceJson var14 = var1.put(var10, var13);
                           if (var14 != null && !var14.source.equals(var13.source)) {
                              var2.add("Resource override " + var10 + ": " + var14.source + " -> " + var13.source);
                           }
                        }
                     } finally {
                        if (var11 != null) {
                           var11.close();
                        }
                        continue;
                     }
                  } catch (Exception var23) {
                     var2.add("Could not parse " + var10 + " from " + var3 + ": " + var23.getMessage());
                  }
               }
            }
         }
      } catch (IOException var25) {
         var2.add("Could not scan " + var3 + ": " + var25.getMessage());
      }
   }

   private static boolean isInterestingCategory(String var0) {
      return var0.equals("species")
         || var0.equals("species_additions")
         || var0.equals("spawn_pool_world")
         || var0.equals("spawn_detail_presets")
         || var0.equals("spawn_rules");
   }

   private void parseSpecies(DataIndex.ResourceJson var1) {
      JsonObject var2 = var1.json;
      String var3 = getString(var2, "name", null);
      String var4 = var1.path.getFileName().toString().replaceFirst("\\.json$", "");
      String var5 = var4.replaceFirst("^\\d+[_-]*", "");
      String var6 = var3 != null ? SpeciesInfo.normalize(var3) : SpeciesInfo.normalize(var5);
      if (!var6.isBlank()) {
         SpeciesInfo var8 = this.species.computeIfAbsent(var6, var2x -> new SpeciesInfo(var2x, var3 != null ? var3 : var5));
         var8.addSource(var1.source);
         var8.setDisplayName(var3);
         var8.setResourceId(var1.namespace + ":" + var5.toLowerCase(Locale.ROOT));
         if (var2.has("nationalPokedexNumber")) {
            try {
               var8.setNationalPokedexNumber(var2.get("nationalPokedexNumber").getAsInt());
            } catch (Exception var10) {
            }
         }

         if (var2.has("primaryType")) {
            var8.setPrimaryType(getString(var2, "primaryType", null));
         }

         if (var2.has("secondaryType")) {
            var8.setSecondaryType(getString(var2, "secondaryType", null));
         }

         if (var2.has("eggGroups")) {
            var8.setEggGroups(stringList(var2.get("eggGroups")));
         }

         if (var2.has("evYield")) {
            var8.setEvYield(intMap(var2.get("evYield")));
         }

         if (var2.has("aspects")) {
            var8.setBaseAspects(stringList(var2.get("aspects")));
         }

         parseFormsInto(var8, var2.get("forms"));
         String var9 = SpeciesInfo.normalize(var5);
         if (!var9.isBlank() && !var9.equals(var6)) {
            this.species.putIfAbsent(var9, var8);
         }
      }
   }

   private void parseSpeciesAddition(DataIndex.ResourceJson var1) {
      JsonObject var2 = var1.json;
      String var3 = getString(var2, "target", null);
      if (var3 != null) {
         SpeciesInfo var4 = this.findSpecies(var3);
         if (var4 != null) {
            var4.addSource(var1.source);
            if (var2.has("nationalPokedexNumber")) {
               try {
                  var4.setNationalPokedexNumber(var2.get("nationalPokedexNumber").getAsInt());
               } catch (Exception var6) {
               }
            }

            if (var2.has("primaryType")) {
               var4.setPrimaryType(getString(var2, "primaryType", null));
            }

            if (var2.has("secondaryType")) {
               var4.setSecondaryType(getString(var2, "secondaryType", null));
            }

            if (var2.has("eggGroups")) {
               var4.setEggGroups(stringList(var2.get("eggGroups")));
            }

            if (var2.has("evYield")) {
               var4.setEvYield(intMap(var2.get("evYield")));
            }

            if (var2.has("aspects")) {
               var4.setBaseAspects(stringList(var2.get("aspects")));
            }

            parseFormsInto(var4, var2.get("forms"));
         }
      }
   }

   private static void parseFormsInto(SpeciesInfo var0, JsonElement var1) {
      if (var1 != null && var1.isJsonArray()) {
         for (JsonElement var3 : var1.getAsJsonArray()) {
            if (var3.isJsonObject()) {
               JsonObject var4 = var3.getAsJsonObject();
               var0.addForm(
                  new SpeciesInfo.FormInfo(
                     getString(var4, "name", ""),
                     stringList(var4.get("aspects")),
                     getString(var4, "primaryType", null),
                     getString(var4, "secondaryType", null),
                     stringList(var4.get("eggGroups")),
                     intMap(var4.get("evYield"))
                  )
               );
            }
         }
      }
   }

   private void parsePreset(String var1, DataIndex.ResourceJson var2, Map<String, DataIndex.SpawnPreset> var3) {
      JsonObject var4 = var2.json;
      String var5 = var1.substring(var1.indexOf(58) + 1);
      var5 = var5.substring("spawn_detail_presets/".length()).replaceFirst("\\.json$", "");
      String var6 = var2.namespace + ":" + var5;
      DataIndex.SpawnPreset var7 = new DataIndex.SpawnPreset(
         getString(var4, "context", null), conditionList(var4.get("condition")), conditionList(var4.get("anticondition"))
      );
      var3.put(var6, var7);
      if (var2.namespace.equals("cobblemon")) {
         var3.putIfAbsent(var5, var7);
      }
   }

   private void parseSpawnPool(DataIndex.ResourceJson var1, Map<String, DataIndex.SpawnPreset> var2) {
      JsonObject var3 = var1.json;
      if (getBoolean(var3, "enabled", true) && this.modsSatisfied(var3)) {
         JsonElement var22 = var3.get("spawns");
         if (var22 != null && var22.isJsonArray()) {
            for (JsonElement var6 : var22.getAsJsonArray()) {
               String var7;
               JsonObject var8;
               if (var6.isJsonObject()
                  && "pokemon".equalsIgnoreCase(getString(var8 = var6.getAsJsonObject(), "type", "pokemon"))
                  && (var7 = getString(var8, "pokemon", null)) != null
                  && !var7.isBlank()) {
                  String var9 = var7.trim().split("\\s+", 2)[0];
                  String var10 = SpeciesInfo.normalize(var9);
                  String var11 = getString(var8, "bucket", "common");
                  double var12 = getDouble(var8, "weight", 1.0);
                  String var14 = getString(var8, "id", var1.path.getFileName().toString());
                  if (Double.isFinite(var12) && !(var12 <= 0.0)) {
                     String var15 = getString(var8, "spawnablePositionType", getString(var8, "context", null));
                     ArrayList var16 = new ArrayList();
                     ArrayList var17 = new ArrayList();

                     for (String var19 : stringList(var8.get("presets"))) {
                        String var20 = var19.contains(":") ? var19 : "cobblemon:" + var19;
                        DataIndex.SpawnPreset var21 = (DataIndex.SpawnPreset)var2.getOrDefault(var20, (DataIndex.SpawnPreset)var2.get(var19));
                        if (var21 != null) {
                           if ((var15 == null || var15.isBlank()) && var21.context != null) {
                              var15 = var21.context;
                           }

                           var16.addAll(var21.conditions);
                           var17.addAll(var21.antiConditions);
                        }
                     }

                     var16.addAll(conditionList(var8.get("condition")));
                     var17.addAll(conditionList(var8.get("anticondition")));
                     List var24 = parseWeightMultipliers(var8.get("weightMultiplier"));
                     this.spawns.add(new SpawnEntry(var14, var7, var10, var15, var11, var12, var16, var17, var24, var1.resourceName()));
                  } else {
                     this.excludedSpawnRouteCount++;
                     this.recordExcludedRoute(
                        "pokemon=" + var10 + " id=" + var14 + " reason=non-positive-weight weight=" + var12 + " source=" + var1.resourceName()
                     );
                  }
               }
            }
         }
      } else {
         JsonElement var4 = var3.get("spawns");
         int var5 = var4 != null && var4.isJsonArray() ? var4.getAsJsonArray().size() : 0;
         this.excludedSpawnRouteCount += var5;
         this.recordExcludedRoute("pool=" + var1.resourceName() + " reason=disabled-or-mod-requirements routes=" + var5);
      }
   }

   private void recordExcludedRoute(String var1) {
      if (var1 != null && !var1.isBlank() && this.excludedSpawnRoutes.size() < 1024) {
         this.excludedSpawnRoutes.add(var1);
      }
   }

   private boolean modsSatisfied(JsonObject var1) {
      FabricLoader var2 = FabricLoader.getInstance();

      for (String var4 : stringList(var1.get("neededInstalledMods"))) {
         if (!var2.isModLoaded(var4)) {
            return false;
         }
      }

      for (String var6 : stringList(var1.get("neededUninstalledMods"))) {
         if (var2.isModLoaded(var6)) {
            return false;
         }
      }

      return true;
   }

   private static List<SpawnEntry.WeightMultiplier> parseWeightMultipliers(JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         ArrayList var1 = new ArrayList();
         if (var0.isJsonArray()) {
            for (JsonElement var3 : var0.getAsJsonArray()) {
               parseOneMultiplier(var3, var1);
            }
         } else {
            parseOneMultiplier(var0, var1);
         }

         return var1;
      } else {
         return List.of();
      }
   }

   private static void parseOneMultiplier(JsonElement var0, List<SpawnEntry.WeightMultiplier> var1) {
      if (var0.isJsonObject()) {
         JsonObject var2 = var0.getAsJsonObject();
         var1.add(
            new SpawnEntry.WeightMultiplier(getDouble(var2, "multiplier", 1.0), conditionList(var2.get("condition")), conditionList(var2.get("anticondition")))
         );
      }
   }

   private static List<SpawnCondition> conditionList(JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         ArrayList var1 = new ArrayList();
         if (var0.isJsonArray()) {
            for (JsonElement var3 : var0.getAsJsonArray()) {
               if (var3.isJsonObject()) {
                  var1.add(SpawnCondition.from(var3.getAsJsonObject()));
               }
            }
         } else if (var0.isJsonObject()) {
            var1.add(SpawnCondition.from(var0.getAsJsonObject()));
         }

         var1.removeIf(Objects::isNull);
         return List.copyOf(var1);
      } else {
         return List.of();
      }
   }

   private static List<String> stringList(JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         ArrayList var1 = new ArrayList();
         if (!var0.isJsonArray()) {
            if (var0.isJsonPrimitive()) {
               try {
                  var1.add(var0.getAsString());
               } catch (Exception var5) {
               }
            }
         } else {
            for (JsonElement var3 : var0.getAsJsonArray()) {
               if (var3.isJsonPrimitive()) {
                  try {
                     var1.add(var3.getAsString());
                  } catch (Exception var6) {
                  }
               }
            }
         }

         return var1;
      } else {
         return List.of();
      }
   }

   private static Map<String, Integer> intMap(JsonElement var0) {
      if (var0 != null && var0.isJsonObject()) {
         LinkedHashMap var1 = new LinkedHashMap();

         for (Entry var3 : var0.getAsJsonObject().entrySet()) {
            try {
               var1.put((String)var3.getKey(), ((JsonElement)var3.getValue()).getAsInt());
            } catch (Exception var5) {
            }
         }

         return var1;
      } else {
         return Map.of();
      }
   }

   private static String getString(JsonObject var0, String var1, String var2) {
      if (var0 != null && var0.has(var1) && !var0.get(var1).isJsonNull()) {
         try {
            return var0.get(var1).getAsString();
         } catch (Exception var4) {
            return var2;
         }
      } else {
         return var2;
      }
   }

   private static boolean getBoolean(JsonObject var0, String var1, boolean var2) {
      if (var0 != null && var0.has(var1) && !var0.get(var1).isJsonNull()) {
         try {
            return var0.get(var1).getAsBoolean();
         } catch (Exception var4) {
            return var2;
         }
      } else {
         return var2;
      }
   }

   private static double getDouble(JsonObject var0, String var1, double var2) {
      if (var0 != null && var0.has(var1) && !var0.get(var1).isJsonNull()) {
         try {
            return var0.get(var1).getAsDouble();
         } catch (Exception var5) {
            return var2;
         }
      } else {
         return var2;
      }
   }

   public record FormSpawnOption(String key, String label) {
      public String label() {
         if (this.label != null && this.key != null && "Meteor".equalsIgnoreCase(this.label)) {
            String var1 = this.key.toLowerCase(Locale.ROOT);
            if (!var1.contains("core")) {
               return this.label;
            }

            for (String var3 : List.of("red", "orange", "yellow", "green", "blue", "indigo", "violet")) {
               if (var1.contains(var3)) {
                  return Character.toUpperCase(var3.charAt(0)) + var3.substring(1) + " Core";
               }
            }

            return "Core";
         } else {
            return this.label;
         }
      }
   }

   private record ResourceJson(String namespace, String category, Path path, JsonObject json, String source) {
      private String resourceName() {
         return this.source + " :: " + this.namespace + ":" + this.category + "/" + this.path.getFileName();
      }
   }

   private record SpawnPreset(String context, List<SpawnCondition> conditions, List<SpawnCondition> antiConditions) {
   }

   private record SpawnVariant(String key, String label) {
   }
}
