package dev.cobblesnack.calc;

import dev.cobblesnack.cache.SessionDiagnostics;
import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.registry.Registry;
import net.minecraft.world.biome.Biome;

public final class BestPokeSnackOptimizer {
   private static final List<String> BUCKETS = List.of("common", "uncommon", "rare", "ultrarare");
   private static final Map<String, BestPokeSnackOptimizer.OptimizationResult> CACHE = new ConcurrentHashMap<>();
   private static volatile DataIndex preparedData;
   private static volatile BestPokeSnackOptimizer.PreparedSpawnIndex preparedIndex;
   private final DataIndex data;

   public BestPokeSnackOptimizer(DataIndex var1) {
      this.data = var1;
   }

   public static void clearCache() {
      CACHE.clear();
   }

   public static synchronized void clearPreparedIndex() {
      preparedData = null;
      preparedIndex = null;
   }

   public BestPokeSnackOptimizer.OptimizationResult optimizeAllBiomes(Registry<Biome> var1, String var2, int var3) {
      return this.optimizeAllBiomes(var1, var2, null, var3, false, true);
   }

   public BestPokeSnackOptimizer.OptimizationResult optimizeAllBiomes(Registry<Biome> var1, String var2, int var3, boolean var4) {
      return this.optimizeAllBiomes(var1, var2, null, var3, var4, true);
   }

   public BestPokeSnackOptimizer.OptimizationResult optimizeAllBiomes(Registry<Biome> var1, String var2, String var3, int var4, boolean var5) {
      return this.optimizeAllBiomes(var1, var2, var3, var4, var5, true);
   }

   public BestPokeSnackOptimizer.OptimizationResult optimizeAllBiomes(Registry<Biome> var1, String var2, String var3, int var4, boolean var5, boolean var6) {
      return this.optimizeAllBiomes(var1, var2, var3, var4, var5, var6, false, false);
   }

   public BestPokeSnackOptimizer.OptimizationResult optimizeAllBiomes(
      Registry<Biome> var1, String var2, String var3, int var4, boolean var5, boolean var6, boolean var7, boolean var8
   ) {
      long var9 = System.nanoTime();
      SpeciesInfo var11 = this.data.findSpecies(var2);
      if (var11 == null) {
         return new BestPokeSnackOptimizer.OptimizationResult(null, List.of(), 0, 0);
      }

      List<String> var12 = BiomeCatalog.possibleBiomesForSpecies(var1, this.data, var11, var3, var6);
      if (var12.isEmpty()) {
         return new BestPokeSnackOptimizer.OptimizationResult(var11, List.of(), 0, 0);
      }

      String var13 = var3 == null ? "" : SpeciesInfo.normalize(var3);
      String var14 = System.identityHashCode(this.data)
         + ":"
         + BiomeReplacementPolicy.cacheIdentity()
         + ":"
         + var11.key()
         + ":form="
         + var13
         + ":"
         + var12.hashCode()
         + ":"
         + var4
         + ":bite="
         + var5
         + ":habitat="
         + var6
         + ":noEnchantedApple="
         + var7
         + ":shiny="
         + var8;
      BestPokeSnackOptimizer.OptimizationResult var15 = CACHE.get(var14);
      if (var15 != null) {
         SessionDiagnostics.event("optimizer-cache-hit", "pokemon=" + var11.key() + " biomes=" + var12.size());
         return var15;
      }

      List<List<Seasoning>> var16 = this.candidateRecipes(var11, var3, var5, var7, var8);
      Map<String, BestPokeSnackOptimizer.GlobalCombinationResult> var17 = new LinkedHashMap<>();
      int var18 = 0;
      Set<SpawnEntry> var19 = new HashSet<>(this.data.spawnsForSpecies(var11.key(), var3).stream().filter(SpawnEntry::isBiomeRankable).toList());
      List<BestPokeSnackOptimizer.PreparedRouteBase> var20 = this.prepareRouteBases(var19);

      for (String var22 : var12) {
         Optional<BiomeMatcher> var23 = BiomeMatcher.create(var1, var22);
         if (!var23.isEmpty()) {
            BiomeMatcher var24 = (BiomeMatcher)var23.get();
            List<BestPokeSnackOptimizer.PreparedRoute> var25 = new ArrayList<>(var20.size());

            for (BestPokeSnackOptimizer.PreparedRouteBase var27 : var20) {
               BestPokeSnackOptimizer.PreparedRoute var28 = this.prepareRouteForBiome(var24, var27, var11, var19, var6);
               if (var28 != null) {
                  var25.add(var28);
               }
            }

            var18 += var25.size();
            if (!var25.isEmpty()) {
               BestPokeSnackOptimizer.GlobalCombinationResult var42 = null;

               for (List<Seasoning> var44 : var16) {
                  double var29 = 0.0;
                  BestPokeSnackOptimizer.PreparedRoute var31 = null;

                  for (BestPokeSnackOptimizer.PreparedRoute var33 : var25) {
                     double var34 = var33.chanceFor(var44);
                     if (!(var34 <= 0.0)
                        && (var31 == null || (var6 && var33.habitatRank != var31.habitatRank ? var33.habitatRank <= var31.habitatRank : var34 > var29))) {
                        var29 = var34;
                        var31 = var33;
                     }
                  }

                  if (var31 != null && !(var29 <= 0.0)) {
                     BestPokeSnackOptimizer.GlobalCombinationResult var45 = new BestPokeSnackOptimizer.GlobalCombinationResult(
                        var44, var29, var22, var31.profile.summary(), var31.habitatRank
                     );
                     if (var42 == null || betterRecipeInSameBiome(var45, var42, var5, var6, var8)) {
                        var42 = var45;
                     }
                  }
               }

               if (var42 != null) {
                  var17.put(var22, var42);
               }
            }
         }
      }

      Comparator<BestPokeSnackOptimizer.GlobalCombinationResult> var36 = var6
         ? Comparator.comparingInt(BestPokeSnackOptimizer.GlobalCombinationResult::habitatRank)
         : (var0, var1x) -> 0;
      if (var8) {
         var36 = var36.thenComparing(
            Comparator.<BestPokeSnackOptimizer.GlobalCombinationResult>comparingInt(var0 -> Seasoning.totalShinyMultiplier(var0.seasonings())).reversed()
         );
      }

      var36 = var36.thenComparing(Comparator.comparingDouble(BestPokeSnackOptimizer.GlobalCombinationResult::chance).reversed());
      if (var5) {
         var36 = var36.thenComparing(
            Comparator.<BestPokeSnackOptimizer.GlobalCombinationResult>comparingDouble(var0 -> Seasoning.expectedBiteTimeReduction(var0.seasonings()))
               .reversed()
         );
      }

      var36 = var36.thenComparing(BestPokeSnackOptimizer.GlobalCombinationResult::biomeId).thenComparing(var0 -> recipeKey(var0.seasonings()));
      List<BestPokeSnackOptimizer.GlobalCombinationResult> var39 = var17.values().stream().sorted(var36).limit(var4).toList();
      BestPokeSnackOptimizer.OptimizationResult var40 = new BestPokeSnackOptimizer.OptimizationResult(var11, var39, var12.size(), var18);
      CACHE.put(var14, var40);
      long var41 = (System.nanoTime() - var9) / 1000000L;
      SessionDiagnostics.event(
         "optimizer-run",
         "pokemon="
            + var11.key()
            + " biomes="
            + var12.size()
            + " targetRoutes="
            + var20.size()
            + " preparedRoutes="
            + var18
            + " recipes="
            + var16.size()
            + " habitat="
            + (var6 ? "practical" : "raw")
            + " noEnchantedApple="
            + var7
            + " shiny="
            + var8
            + " elapsedMs="
            + var41
      );
      return var40;
   }

   private static boolean betterRecipeInSameBiome(
      BestPokeSnackOptimizer.GlobalCombinationResult var0, BestPokeSnackOptimizer.GlobalCombinationResult var1, boolean var2, boolean var3, boolean var4
   ) {
      if (var3 && var0.habitatRank() != var1.habitatRank()) {
         return var0.habitatRank() < var1.habitatRank();
      }

      if (var4) {
         int var6 = Integer.compare(Seasoning.totalShinyMultiplier(var0.seasonings()), Seasoning.totalShinyMultiplier(var1.seasonings()));
         if (var6 != 0) {
            return var6 > 0;
         }
      }

      int var7 = Double.compare(var0.chance(), var1.chance());
      if (var7 != 0) {
         return var7 > 0;
      }

      int var5;
      return var2
            && (var5 = Double.compare(Seasoning.expectedBiteTimeReduction(var0.seasonings()), Seasoning.expectedBiteTimeReduction(var1.seasonings()))) != 0
         ? var5 > 0
         : recipeKey(var0.seasonings()).compareTo(recipeKey(var1.seasonings())) < 0;
   }

   private List<List<Seasoning>> candidateRecipes(SpeciesInfo var1, String var2, boolean var3, boolean var4, boolean var5) {
      String var6 = this.data.spawnsForSpecies(var1.key(), var2).stream().map(var0 -> var0.pokemonExpression).findFirst().orElse(var1.key());
      SpeciesInfo.ResolvedTraits var7 = var1.resolveForSpawnString(var6);
      List<Seasoning> var8 = new ArrayList<>();
      var8.add(Seasoning.NONE);

      for (Seasoning var12 : Seasoning.values()) {
         if (var12 != Seasoning.NONE && (!var4 || var12 != Seasoning.ENCHANTED_GOLDEN_APPLE)) {
            boolean var13 = var12.matches(var7);
            boolean var14 = var12.kind == Seasoning.Kind.RARITY || var13 || var3 && var12.kind == Seasoning.Kind.BITE_TIME || var5 && var12.shinyBoost() > 0;
            if (var14) {
               var8.add(var12);
            }
         }
      }

      Map<String, List<Seasoning>> var15 = new LinkedHashMap<>();

      for (int var16 = 0; var16 < var8.size(); var16++) {
         for (int var17 = var16; var17 < var8.size(); var17++) {
            for (int var18 = var17; var18 < var8.size(); var18++) {
               List<Seasoning> var19 = List.of(var8.get(var16), var8.get(var17), var8.get(var18));
               if (usefulCategoryCounts(var19)
                  && (!var3 || !var19.stream().noneMatch(Seasoning::reducesBiteTime))
                  && (!var5 || !var19.stream().noneMatch(var1x -> var1x.matches(var7)))) {
                  var15.putIfAbsent(recipeKey(var19), normalizeRecipe(var19));
               }
            }
         }
      }

      return List.copyOf(var15.values());
   }

   private static boolean usefulCategoryCounts(List<Seasoning> var0) {
      int var1 = 0;
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;

      for (Seasoning var6 : var0) {
         if (var6.kind == Seasoning.Kind.TYPE) {
            var1++;
         } else if (var6.kind == Seasoning.Kind.EGG_GROUP) {
            var2++;
         } else if (var6.kind == Seasoning.Kind.EV_FILTER) {
            var3++;
         } else if (var6.kind == Seasoning.Kind.BITE_TIME) {
            var4++;
         }
      }

      return var1 <= 1 && var2 <= 1 && var3 <= 1 && var4 <= 1;
   }

   private static List<Seasoning> normalizeRecipe(List<Seasoning> var0) {
      List<Seasoning> var1 = new ArrayList<>(var0);
      var1.sort(Comparator.comparingInt(BestPokeSnackOptimizer::sortRank).thenComparing(Enum::name));
      return List.copyOf(var1);
   }

   private static int sortRank(Seasoning var0) {
      return switch (var0.kind) {
         case EV_FILTER -> 0;
         case TYPE -> 1;
         case EGG_GROUP -> 2;
         case RARITY -> 3;
         case BITE_TIME -> 4;
         case NONE -> 6;
         default -> 5;
      };
   }

   private BestPokeSnackOptimizer.PreparedSpawnIndex preparedSpawnIndex() {
      BestPokeSnackOptimizer.PreparedSpawnIndex var1 = preparedIndex;
      if (var1 != null && preparedData == this.data) {
         return var1;
      }

      Class<BestPokeSnackOptimizer> var3 = BestPokeSnackOptimizer.class;
      synchronized (BestPokeSnackOptimizer.class) {
         var1 = preparedIndex;
         if (var1 != null && preparedData == this.data) {
            return var1;
         }

         long var4 = System.nanoTime();
         Map<String, List<BestPokeSnackOptimizer.PreparedStaticSpawn>> var6 = new LinkedHashMap<>();
         int var7 = 0;

         for (SpawnEntry var9 : this.data.spawns()) {
            int var11;
            if (!"fishing".equals(var9.context) && !var9.hasStructureConstraint() && (var11 = BUCKETS.indexOf(BucketOdds.normalizeBucket(var9.bucket))) >= 0) {
               SpeciesInfo var12 = this.data.species().get(var9.speciesKey);
               if (var12 == null) {
                  var12 = this.data.findSpecies(var9.speciesKey);
               }

               if (var12 != null) {
                  BestPokeSnackOptimizer.PreparedStaticSpawn var13 = new BestPokeSnackOptimizer.PreparedStaticSpawn(
                     var9, var11, var12.key(), var12.resolveForSpawnString(var9.pokemonExpression)
                  );
                  var6.computeIfAbsent(contextKey(var9.context), var0 -> new ArrayList<>()).add(var13);
                  var7++;
               }
            }
         }

         Map<String, List<BestPokeSnackOptimizer.PreparedStaticSpawn>> var18 = new LinkedHashMap<>();
         var6.forEach((var1x, var2) -> var18.put(var1x, List.copyOf(var2)));
         var1 = new BestPokeSnackOptimizer.PreparedSpawnIndex(Map.copyOf(var18), var7);
         preparedData = this.data;
         preparedIndex = var1;
         long var19 = (System.nanoTime() - var4) / 1000000L;
         SessionDiagnostics.event("optimizer-spawn-index", "rows=" + var7 + " contexts=" + var18.size() + " elapsedMs=" + var19);
         return var1;
      }
   }

   private List<BestPokeSnackOptimizer.PreparedRouteBase> prepareRouteBases(Set<SpawnEntry> var1) {
      if (var1.isEmpty()) {
         return List.of();
      }

      BestPokeSnackOptimizer.PreparedSpawnIndex var2 = this.preparedSpawnIndex();
      List<BestPokeSnackOptimizer.PreparedRouteBase> var3 = new ArrayList<>();

      for (SpawnEntry var5 : var1) {
         if (!"fishing".equals(var5.context)) {
            SpawnEnvironmentProfile var6 = SpawnEnvironmentProfile.fromTarget(var5);
         List<BestPokeSnackOptimizer.PreparedStaticSpawn> var7 = var2.byContext.getOrDefault(contextKey(var6.context), List.of());
         List<BestPokeSnackOptimizer.PreparedStaticSpawn> var8 = new ArrayList<>();

            for (BestPokeSnackOptimizer.PreparedStaticSpawn var10 : var7) {
               if (var6.isCompatible(var10.entry)) {
                  var8.add(var10);
               }
            }

            if (!var8.isEmpty()) {
               var3.add(new BestPokeSnackOptimizer.PreparedRouteBase(var6, List.copyOf(var8)));
            }
         }
      }

      return var3;
   }

   private BestPokeSnackOptimizer.PreparedRoute prepareRouteForBiome(
      BiomeMatcher var1, BestPokeSnackOptimizer.PreparedRouteBase var2, SpeciesInfo var3, Set<SpawnEntry> var4, boolean var5
   ) {
      if (var1.satisfiesEntryBiome(var2.profile.targetEntry) && HabitatPolicy.allows(var1, var2.profile.targetEntry, var5)) {
         List<BestPokeSnackOptimizer.PreparedSpawn> var6 = new ArrayList<>();

         for (BestPokeSnackOptimizer.PreparedStaticSpawn var8 : var2.compatible) {
            SpawnEntry var9 = var8.entry;
            if (var1.satisfiesEntryBiome(var9)) {
               double var10 = var9.weight;

               for (SpawnEntry.WeightMultiplier var13 : var9.weightMultipliers) {
                  if (var1.satisfiesConditionsBiomeOnly(var13.conditions(), var13.antiConditions())) {
                     boolean var14 = var13.conditions().stream().allMatch(var2.profile::definitelySatisfies);
                     boolean var15 = var13.antiConditions().stream().anyMatch(var2.profile::definitelySatisfies);
                     if (var14 && !var15) {
                        var10 *= var13.multiplier();
                     }
                  }
               }

               if (!(var10 <= 0.0)) {
                  var6.add(
                     new BestPokeSnackOptimizer.PreparedSpawn(var8.bucketIndex, var10, var8.speciesKey.equals(var3.key()) && var4.contains(var9), var8.traits)
                  );
               }
            }
         }

         return var6.isEmpty() ? null : new BestPokeSnackOptimizer.PreparedRoute(var2.profile, var6, HabitatPolicy.rank(var1, var2.profile.targetEntry));
      } else {
         return null;
      }
   }

   private static String contextKey(String var0) {
      return var0 == null ? "<null>" : var0;
   }

   private static String recipeKey(List<Seasoning> var0) {
      return var0.stream().map(Enum::name).sorted().reduce((var0x, var1) -> var0x + "+" + var1).orElse("");
   }

   public record GlobalCombinationResult(List<Seasoning> seasonings, double chance, String biomeId, String routeSummary, int habitatRank) {
   }

   public record OptimizationResult(
      SpeciesInfo target, List<BestPokeSnackOptimizer.GlobalCombinationResult> combinations, int biomeCount, int preparedRouteCount
   ) {
   }

   private static final class PreparedRoute {
      final SpawnEnvironmentProfile profile;
      final List<BestPokeSnackOptimizer.PreparedSpawn> spawns;
      final int habitatRank;

      PreparedRoute(SpawnEnvironmentProfile var1, List<BestPokeSnackOptimizer.PreparedSpawn> var2, int var3) {
         this.profile = var1;
         this.spawns = List.copyOf(var2);
         this.habitatRank = var3;
      }

      double chanceFor(List<Seasoning> var1) {
         int var2 = 0;
         Seasoning var3 = null;
         Seasoning var4 = null;
         Seasoning var5 = null;

         for (Seasoning var7 : var1) {
            var2 += var7.rarityTierBoost;
            if (var3 == null && var7.kind == Seasoning.Kind.EV_FILTER) {
               var3 = var7;
            }

            if (var4 == null && var7.kind == Seasoning.Kind.TYPE) {
               var4 = var7;
            }

            if (var5 == null && var7.kind == Seasoning.Kind.EGG_GROUP) {
               var5 = var7;
            }
         }

         double[] var15 = new double[BestPokeSnackOptimizer.BUCKETS.size()];
         double[] var16 = new double[BestPokeSnackOptimizer.BUCKETS.size()];

         for (BestPokeSnackOptimizer.PreparedSpawn var9 : this.spawns) {
            double var10 = var9.baseWeight;
            if (var3 == null || var3.matches(var9.traits)) {
               if (var4 != null && var4.matches(var9.traits)) {
                  var10 *= 10.0;
               }

               if (var5 != null && var5.matches(var9.traits)) {
                  var10 *= 10.0;
               }

               if (!(var10 <= 0.0)) {
                  int var12 = var9.bucketIndex;
                  var15[var12] += var10;
                  if (var9.target) {
                     int var13 = var9.bucketIndex;
                     var16[var13] += var10;
                  }
               }
            }
         }

         double var17 = 0.0;

         for (int var18 = 0; var18 < BestPokeSnackOptimizer.BUCKETS.size(); var18++) {
            if (var15[var18] > 0.0) {
               var17 += BucketOdds.oddsFor(BestPokeSnackOptimizer.BUCKETS.get(var18), var2);
            }
         }

         if (var17 <= 0.0) {
            return 0.0;
         }

         double var19 = 0.0;

         for (int var20 = 0; var20 < BestPokeSnackOptimizer.BUCKETS.size(); var20++) {
            if (!(var15[var20] <= 0.0) && !(var16[var20] <= 0.0)) {
               double var21 = BucketOdds.oddsFor(BestPokeSnackOptimizer.BUCKETS.get(var20), var2) / var17;
               var19 += var21 * (var16[var20] / var15[var20]);
            }
         }

         return var19;
      }
   }

   private record PreparedRouteBase(SpawnEnvironmentProfile profile, List<BestPokeSnackOptimizer.PreparedStaticSpawn> compatible) {
   }

   private record PreparedSpawn(int bucketIndex, double baseWeight, boolean target, SpeciesInfo.ResolvedTraits traits) {
   }

   private record PreparedSpawnIndex(Map<String, List<BestPokeSnackOptimizer.PreparedStaticSpawn>> byContext, int rows) {
   }

   private record PreparedStaticSpawn(SpawnEntry entry, int bucketIndex, String speciesKey, SpeciesInfo.ResolvedTraits traits) {
   }
}
