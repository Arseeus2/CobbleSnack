package dev.cobblesnack.client;

import dev.cobblesnack.cache.DiskCacheStore;
import dev.cobblesnack.cache.SessionDiagnostics;
import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpeciesInfo;
import java.util.List;

public final class PerformanceWarmup {
   private static volatile List<SpeciesInfo> species = List.of();
   private static volatile boolean dataReady;
   private static boolean loading;
   private static long generation;
   private static int spriteCursor;

   private PerformanceWarmup() {
   }

   public static synchronized void start() {
      SessionDiagnostics.start();
      if (!dataReady && !loading) {
         loading = true;
         long var0 = generation;
         Thread var2 = new Thread(() -> {
            List var2x = null;

            try {
               DiskCacheStore.ensureDiskCache();
               var2x = DataIndex.get().browserSpecies();
            } catch (Throwable var7) {
               SessionDiagnostics.error("startup-cache", var7);
            }

            Class<PerformanceWarmup> var4 = PerformanceWarmup.class;
            synchronized (PerformanceWarmup.class) {
               if (var0 == generation && var2x != null) {
                  species = var2x;
                  dataReady = true;
                  spriteCursor = 0;
               }

               if (var0 == generation) {
                  loading = false;
               }

               PerformanceWarmup.class.notifyAll();
            }
         }, "CobbleSnack Hybrid Cache Warmup");
         var2.setDaemon(true);
         var2.setPriority(1);
         var2.start();
      }
   }

   public static List<SpeciesInfo> species() {
      if (!dataReady) {
         start();
         Class<PerformanceWarmup> var0 = PerformanceWarmup.class;

         while (loading && !dataReady) {
            try {
               PerformanceWarmup.class.wait();
            } catch (InterruptedException var2) {
               Thread.currentThread().interrupt();
               break;
            }
         }

         if (!dataReady) {
            species = DataIndex.get().browserSpecies();
            dataReady = true;
            loading = false;
            spriteCursor = 0;
         }
      }

      DataIndex var3 = DataIndex.get();
      SessionDiagnostics.uiOpen(var3);
      return species;
   }

   public static void tick() {
      SessionDiagnostics.tick();
      if (dataReady) {
         List var0 = species;
         int var1 = spriteCursor;
         int var2 = Math.min(var0.size(), 64);
         if (var1 >= 0 && var1 < var2) {
            try {
               MinimapSpriteResolver.spriteFor((SpeciesInfo)var0.get(var1));
            } catch (Throwable var7) {
               SessionDiagnostics.error("sprite-prewarm", var7);
            } finally {
               spriteCursor = var1 + 1;
            }
         }
      }
   }

   public static synchronized void refreshDataSnapshot() {
      generation++;
      species = DataIndex.get().browserSpecies();
      dataReady = true;
      loading = false;
      spriteCursor = 0;
      PerformanceWarmup.class.notifyAll();
   }

   public static void resetSpriteWarmup() {
      spriteCursor = 0;
   }
}
