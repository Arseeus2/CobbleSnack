package dev.cobblesnack.cache;

import dev.cobblesnack.calc.BiomeReplacementPolicy;
import dev.cobblesnack.data.DataIndex;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class SessionDiagnostics {
   private static final String VERSION = "1.0.0";
   private static final long MAX_BYTES = 4194304L;
   private static final int TICKS_PER_SAMPLE = 1200;
   private static final Object LOCK = new Object();
   private static final AtomicBoolean STARTED = new AtomicBoolean();
   private static final AtomicBoolean FULL_AUDIT_WRITTEN = new AtomicBoolean();
   private static volatile boolean uiOpen;
   private static volatile boolean capped;
   private static int ticks;
   private static int openCount;
   private static int lastIndexIdentity;

   private SessionDiagnostics() {
   }

   public static Path logPath() {
      return DiskCacheStore.gameDirectoryForDiagnostics().resolve("config").resolve("cobblesnack").resolve("diagnostics").resolve("session.log");
   }

   public static void start() {
      if (STARTED.compareAndSet(false, true)) {
         Object var0 = LOCK;
         synchronized (LOCK) {
            try {
               Path var3 = logPath();
               Files.createDirectories(var3.getParent());
               Files.writeString(var3, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
               appendRaw("CobbleSnack temporary session diagnostics\n");
               appendRaw("Version: 1.0.0\n");
               appendRaw("Started UTC: " + Instant.now() + "\n");
               appendRaw("Purpose: cache/load timing, installed data audit, spawn-route audit and JVM memory trend.\n");
               appendRaw("This file is overwritten every Minecraft launch and capped at 4 MiB.\n\n");
               event("diagnostics-start", details());
               memorySnapshot("startup");
            } catch (Throwable var5) {
            }
         }
      }
   }

   public static void tick() {
      if (!STARTED.get()) {
         start();
      }

      BiomeReplacementPolicy.refreshForCurrentConnection();
      if (++ticks >= 1200) {
         ticks = 0;
         memorySnapshot(uiOpen ? "periodic-ui-open" : "periodic-idle");
      }
   }

   public static void uiOpen(DataIndex var0) {
      start();
      if (!uiOpen) {
         uiOpen = true;
         openCount++;
         int var1 = var0 == null ? 0 : System.identityHashCode(var0);
         if (lastIndexIdentity != 0 && var1 != 0 && var1 != lastIndexIdentity) {
            event("WARN-index-instance-changed", "previous=" + lastIndexIdentity + " current=" + var1 + " (expected only after Reload data)");
         }

         if (var1 != 0) {
            lastIndexIdentity = var1;
         }

         event("ui-open", "openCount=" + openCount + " indexIdentity=" + var1 + " " + indexCounts(var0));
         auditIndexAsync(var0);
         memorySnapshot("ui-open");
      }
   }

   public static void uiClose() {
      start();
      if (uiOpen) {
         event("ui-close-before-ephemeral-clear", cacheSizes());
         memorySnapshot("ui-close-before-ephemeral-clear");
         uiOpen = false;
      }
   }

   public static void afterEphemeralClear() {
      event("ui-close-after-ephemeral-clear", cacheSizes());
      memorySnapshot("ui-close-after-ephemeral-clear");
   }

   public static void indexReady(String var0, DataIndex var1, long var2) {
      start();
      int var4 = var1 == null ? 0 : System.identityHashCode(var1);
      if (var4 != 0) {
         lastIndexIdentity = var4;
      }

      event("index-ready", "source=" + var0 + " elapsedMs=" + millis(var2) + " indexIdentity=" + var4 + " " + indexCounts(var1));
      memorySnapshot("index-ready-" + var0);
   }

   public static void indexReused(DataIndex var0) {
      if (var0 != null && lastIndexIdentity == 0) {
         lastIndexIdentity = System.identityHashCode(var0);
      }
   }

   public static void diskCacheSaved(DataIndex var0, long var1) {
      Path var3 = DiskCacheStore.cacheDirectory();
      event(
         "disk-cache-saved",
         "elapsedMs="
            + millis(var1)
            + " "
            + indexCounts(var0)
            + " files="
            + fileSize(var3.resolve("species.json"))
            + "/"
            + fileSize(var3.resolve("spawns.json"))
            + "/"
            + fileSize(var3.resolve("metadata.json"))
            + " bytes(species/spawns/meta)"
      );
   }

   public static void reload(DataIndex var0, long var1) {
      int var3 = var0 == null ? 0 : System.identityHashCode(var0);
      lastIndexIdentity = var3;
      event("explicit-reload", "elapsedMs=" + millis(var1) + " indexIdentity=" + var3 + " " + indexCounts(var0));
      auditIndexAsync(var0);
      memorySnapshot("explicit-reload");
   }

   public static void error(String var0, Throwable var1) {
      start();
      event("ERROR-" + var0, var1 == null ? "unknown" : var1.getClass().getName() + ": " + var1.getMessage());
   }

   public static void event(String var0, String var1) {
      start();
      Object var2 = LOCK;
      synchronized (LOCK) {
         if (!capped) {
            try {
               String var5 = Instant.now() + " [" + var0 + "] " + (var1 == null ? "" : var1) + System.lineSeparator();
               appendRaw(var5);
            } catch (Throwable var7) {
            }
         }
      }
   }

   public static void auditLines(Collection<String> var0) {
      if (var0 != null && !var0.isEmpty()) {
         start();
         synchronized (LOCK) {
            if (!capped) {
               try {
                  StringBuilder var2 = new StringBuilder(131072);

                  for (String var4 : var0) {
                     var2.append("[data-audit] ").append(var4 == null ? "" : var4).append(System.lineSeparator());
                     if (var2.length() >= 131072) {
                        appendRaw(var2.toString());
                        var2.setLength(0);
                        if (capped) {
                           return;
                        }
                     }
                  }

                  if (!var2.isEmpty()) {
                     appendRaw(var2.toString());
                  }
               } catch (Throwable var6) {
                  event("ERROR-data-audit-write", var6.getClass().getName() + ": " + var6.getMessage());
               }
            }
         }
      }
   }

   private static void auditIndexAsync(DataIndex var0) {
      if (var0 != null) {
         if (FULL_AUDIT_WRITTEN.compareAndSet(false, true)) {
            Thread var1 = new Thread(() -> {
               try {
                  DataAuditDiagnostics.emit(var0);
               } catch (Throwable var2) {
                  error("data-audit", var2);
               }
            }, "CobbleSnack Data Audit");
            var1.setDaemon(true);
            var1.setPriority(1);
            var1.start();
         }
      }
   }

   public static void memorySnapshot(String var0) {
      start();
      Runtime var1 = Runtime.getRuntime();
      long var2 = var1.totalMemory() - var1.freeMemory();
      long var4 = var1.totalMemory();
      long var6 = var1.maxMemory();
      long var8 = 0L;
      long var10 = 0L;

      for (GarbageCollectorMXBean var13 : ManagementFactory.getGarbageCollectorMXBeans()) {
         if (var13.getCollectionCount() > 0L) {
            var8 += var13.getCollectionCount();
         }

         if (var13.getCollectionTime() > 0L) {
            var10 += var13.getCollectionTime();
         }
      }

      event(
         "memory",
         "reason="
            + var0
            + " usedMiB="
            + mib(var2)
            + " committedMiB="
            + mib(var4)
            + " maxMiB="
            + mib(var6)
            + " gcCount="
            + var8
            + " gcTimeMs="
            + var10
            + " "
            + cacheSizes()
      );
   }

   private static String indexCounts(DataIndex var0) {
      if (var0 == null) {
         return "index=null";
      }

      try {
         Map var1 = var0.spawns().stream().collect(Collectors.groupingBy(var0x -> var0x.context, LinkedHashMap::new, Collectors.counting()));
         return "species="
            + var0.uniqueSpecies().size()
            + " browserSpecies="
            + var0.browserSpecies().size()
            + " spawns="
            + var0.spawns().size()
            + " warnings="
            + var0.warnings().size()
            + " spawnRuleFiles="
            + var0.spawnRuleFileCount()
            + " inactiveSpawnRules="
            + var0.inactiveSpawnRuleFileCount()
            + " excludedRoutes="
            + var0.excludedSpawnRouteCount()
            + " contexts="
            + var1;
      } catch (Throwable var2) {
         return "indexCounts=unavailable(" + var2.getClass().getSimpleName() + ")";
      }
   }

   private static String cacheSizes() {
      return "caches{biomes="
         + mapSize("dev.cobblesnack.calc.BiomeCatalog", "SPECIES_BIOME_CACHE")
         + ",optimizer="
         + mapSize("dev.cobblesnack.calc.BestPokeSnackOptimizer", "CACHE")
         + ",sprites="
         + mapSize("dev.cobblesnack.client.MinimapSpriteResolver", "RESOLVED")
         + ",formOptions="
         + mapSize("dev.cobblesnack.client.MinimapSpriteResolver", "FORM_OPTIONS")
         + ",formSprites="
         + mapSize("dev.cobblesnack.client.MinimapSpriteResolver", "FORM_SPRITES")
         + ",spriteDims="
         + mapSize("dev.cobblesnack.client.MinimapSpriteResolver", "DIMENSIONS")
         + ",spriteJobs="
         + collectionSize("dev.cobblesnack.client.MinimapSpriteResolver", "PENDING_FORMS")
         + ",icons="
         + mapSize("dev.cobblesnack.client.PokemonIconFactory", "CACHE")
         + "}";
   }

   private static int mapSize(String var0, String var1) {
      try {
         Field var3 = Class.forName(var0).getDeclaredField(var1);
         var3.setAccessible(true);
         int var2;
         if (var3.get(null) instanceof Map var5) {
            var2 = var5.size();
         } else {
            var2 = -1;
         }

         return var2;
      } catch (Throwable var6) {
         return -1;
      }
   }

   private static int collectionSize(String var0, String var1) {
      try {
         Field var3 = Class.forName(var0).getDeclaredField(var1);
         var3.setAccessible(true);
         int var2;
         if (var3.get(null) instanceof Collection var5) {
            var2 = var5.size();
         } else {
            var2 = -1;
         }

         return var2;
      } catch (Throwable var6) {
         return -1;
      }
   }

   private static String details() {
      return "java="
         + System.getProperty("java.version")
         + " os="
         + System.getProperty("os.name")
         + " arch="
         + System.getProperty("os.arch")
         + " processors="
         + Runtime.getRuntime().availableProcessors();
   }

   private static long fileSize(Path var0) {
      try {
         return Files.size(var0);
      } catch (Throwable var2) {
         return -1L;
      }
   }

   private static long millis(long var0) {
      return Math.max(0L, var0 / 1000000L);
   }

   private static long mib(long var0) {
      return Math.max(0L, var0 / 1048576L);
   }

   private static void appendRaw(String var0) throws IOException {
      Path var1 = logPath();
      if (Files.exists(var1) && Files.size(var1) + var0.getBytes(StandardCharsets.UTF_8).length > 4194304L) {
         capped = true;
         Files.writeString(
            var1,
            Instant.now() + " [diagnostics-capped] Log reached 4 MiB; further entries suppressed.\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
            StandardOpenOption.WRITE
         );
      } else {
         Files.writeString(var1, var0, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
      }
   }
}
