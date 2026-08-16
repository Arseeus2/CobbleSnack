package dev.cobblesnack.calc;

import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import java.util.Locale;

public final class HabitatPolicy {
   private HabitatPolicy() {
   }

   public static boolean allows(BiomeMatcher var0, SpawnEntry var1, boolean var2) {
      if (!var2 || var0 == null || var1 == null) {
         return true;
      }

      return rank(var0, var1) < 3
         && !hasGroundedOceanConflict(var0.isOceanBiome(), var1.context)
         && !hasCaveSkyConflict(var0.isCaveLike(), var1);
   }

   static boolean hasGroundedOceanConflict(boolean var0, String var1) {
      return var0 && "grounded".equals(normalizeContext(var1));
   }

   static boolean hasCaveSkyConflict(boolean var0, SpawnEntry var1) {
      if (!var0 || var1 == null) {
         return false;
      }

      for (SpawnCondition var3 : var1.conditions) {
         if (Boolean.TRUE.equals(var3.canSeeSky) || var3.minSkyLight != null && var3.minSkyLight > 7) {
            return true;
         }
      }

      return false;
   }

   public static int rank(BiomeMatcher var0, SpawnEntry var1) {
      return var0 != null && var1 != null
         ? rankForSignals(
            var0.isAquaticDominant(),
            var0.isAquaticFriendly(),
            var0.isLavaFriendly(),
            hasMatchedSpecificBiomeSelector(var0, var1),
            hasMatchedAquaticSelector(var0, var1),
            var1.context,
            requiresFluid(var1, "water"),
            requiresFluid(var1, "lava")
         )
         : 1;
   }

   static boolean allows(boolean var0, String var1, boolean var2) {
      return !var0 || !"grounded".equals(normalizeContext(var1)) || var2;
   }

   static int rankForSignals(boolean var0, boolean var1, boolean var2, boolean var3, boolean var4, String var5, boolean var6, boolean var7) {
      String var8 = normalizeContext(var5);
      if ("grounded".equals(var8)) {
         if (var0) {
            return var4 ? 0 : 3;
         } else {
            return var4 ? 0 : 1;
         }
      } else if (!isAquaticContext(var8)) {
         return 1;
      } else if (var7) {
         return !var2 && !var3 ? 2 : 0;
      } else {
         return !var1 && !var3 ? 2 : 0;
      }
   }

   static boolean isAquaticContext(String var0) {
      return switch (normalizeContext(var0)) {
         case "submerged", "surface", "seafloor" -> true;
         default -> false;
      };
   }

   private static boolean hasMatchedSpecificBiomeSelector(BiomeMatcher var0, SpawnEntry var1) {
      for (SpawnCondition var3 : var1.conditions) {
         for (String var5 : var3.biomes) {
            if (!isBroadBiomeSelector(var5) && var0.matchesSelector(var5)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean hasMatchedAquaticSelector(BiomeMatcher var0, SpawnEntry var1) {
      for (SpawnCondition var3 : var1.conditions) {
         for (String var5 : var3.biomes) {
            String var6 = var5 == null ? "" : var5.trim().toLowerCase(Locale.ROOT);
            if (var0.matchesSelector(var5)
               && containsAny(
                  var6,
                  "ocean",
                  "river",
                  "water",
                  "freshwater",
                  "coast",
                  "beach",
                  "swamp",
                  "marsh",
                  "wetland",
                  "lake",
                  "mangrove",
                  "bayou",
                  "reef",
                  "lagoon",
                  "aquatic"
               )) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean isBroadBiomeSelector(String var0) {
      if (var0 == null) {
         return true;
      }

      String var1 = var0.trim().toLowerCase(Locale.ROOT);
      return var1.isBlank() || var1.equals("#is_overworld") || var1.endsWith(":is_overworld");
   }

   private static boolean requiresFluid(SpawnEntry var0, String var1) {
      for (SpawnCondition var3 : var0.conditions) {
         for (String var5 : var3.fluid) {
            if (var5 != null && var5.toLowerCase(Locale.ROOT).contains(var1)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean containsAny(String var0, String... var1) {
      for (String var5 : var1) {
         if (var0.contains(var5)) {
            return true;
         }
      }

      return false;
   }

   private static String normalizeContext(String var0) {
      return var0 == null ? "grounded" : var0.trim().toLowerCase(Locale.ROOT);
   }
}
