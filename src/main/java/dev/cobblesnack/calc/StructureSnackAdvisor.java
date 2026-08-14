package dev.cobblesnack.calc;

import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StructureSnackAdvisor {
   private StructureSnackAdvisor() {
   }

   public static List<Seasoning> recommend(SpeciesInfo var0, List<SpawnEntry> var1, boolean var2, boolean var3, boolean var4) {
      if (var0 != null && var1 != null && !var1.isEmpty()) {
         SpeciesInfo.ResolvedTraits var5 = var0.resolveForSpawnString(((SpawnEntry)var1.get(0)).pokemonExpression);
         List<Seasoning> var6 = new ArrayList<>(3);
         Seasoning var7 = primaryTypeMatch(var5);
         if (var7 == null) {
            var7 = firstMatching(var5, Seasoning.Kind.EGG_GROUP);
         }

         if (var7 == null) {
            var7 = bestEvMatch(var5);
         }

         if (var4) {
            add(var6, var7);
            if (!var3) {
               add(var6, Seasoning.ENCHANTED_GOLDEN_APPLE);
            }

            add(var6, Seasoning.STARF);
            if (var6.size() < 3) {
               add(var6, Seasoning.GOLDEN_APPLE);
            }
         } else {
            if (var2) {
               add(var6, Seasoning.APPLE);
            }

            add(var6, bestEvMatch(var5));
            add(var6, var7);
            add(var6, firstMatching(var5, Seasoning.Kind.EGG_GROUP));
         }

         if (var2 && var6.stream().noneMatch(Seasoning::reducesBiteTime)) {
            if (var6.size() == 3) {
               var6.remove(var6.size() - 1);
            }

            add(var6, Seasoning.APPLE);
         }

         if (var6.stream().noneMatch(var1x -> var1x.matches(var5))) {
            if (var6.size() == 3) {
               var6.remove(var6.size() - 1);
            }

            add(var6, var7);
         }

         if (var6.size() < 3) {
            boolean var8 = var1.stream().map(var0x -> BucketOdds.normalizeBucket(var0x.bucket)).anyMatch(var0x -> !"common".equals(var0x));
            add(var6, var8 ? (var3 ? Seasoning.GOLDEN_APPLE : Seasoning.ENCHANTED_GOLDEN_APPLE) : Seasoning.APPLE);
         }

         add(var6, Seasoning.STARF);
         add(var6, Seasoning.HOPO);
         var6.sort(Comparator.comparingInt(StructureSnackAdvisor::sortRank).thenComparing(Enum::name));
         return List.copyOf(var6);
      } else {
         return List.of();
      }
   }

   private static void add(List<Seasoning> var0, Seasoning var1) {
      if (var1 != null && var1 != Seasoning.NONE && var0.size() < 3 && !var0.contains(var1)) {
         var0.add(var1);
      }
   }

   private static Seasoning primaryTypeMatch(SpeciesInfo.ResolvedTraits var0) {
      if (var0 == null) {
         return null;
      }

      String var1 = var0.primaryType();

      for (Seasoning var5 : Seasoning.values()) {
         if (var5.kind == Seasoning.Kind.TYPE && var5.targets.contains(var1)) {
            return var5;
         }
      }

      return firstMatching(var0, Seasoning.Kind.TYPE);
   }

   private static Seasoning firstMatching(SpeciesInfo.ResolvedTraits var0, Seasoning.Kind var1) {
      for (Seasoning var5 : Seasoning.values()) {
         if (var5.kind == var1 && var5.matches(var0)) {
            return var5;
         }
      }

      return null;
   }

   private static Seasoning bestEvMatch(SpeciesInfo.ResolvedTraits var0) {
      Seasoning var1 = null;
      int var2 = 0;

      for (Seasoning var6 : Seasoning.values()) {
         if (var6.kind == Seasoning.Kind.EV_FILTER && var6.matches(var0)) {
            int var7 = var0.evYieldFor(var6.evStat);
            if (var7 > var2) {
               var1 = var6;
               var2 = var7;
            }
         }
      }

      return var1;
   }

   private static int sortRank(Seasoning var0) {
      return switch (var0.kind) {
         case EV_FILTER -> 0;
         case TYPE -> 1;
         case EGG_GROUP -> 2;
         case RARITY -> 3;
         case SHINY -> 4;
         case BITE_TIME -> 5;
         case NONE -> 7;
         default -> 6;
      };
   }
}
