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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.registry.Registry;
import net.minecraft.world.biome.Biome;

public final class StructureEstimateOptimizer {
   private static final List<String> BUCKETS = List.of("common", "uncommon", "rare", "ultrarare");
   private final DataIndex data;

   public StructureEstimateOptimizer(DataIndex var1) {
      this.data = var1;
   }

   public StructureEstimateOptimizer.Result optimize(Registry<Biome> var1, String var2, String var3, boolean var4, boolean var5, boolean var6, boolean var7) {
      long var8 = System.nanoTime();
      SpeciesInfo var10 = this.data.findSpecies(var2);
      if (var10 == null) {
         return StructureEstimateOptimizer.Result.empty();
      }

      List<SpawnEntry> var11 = this.data
         .spawnsForSpecies(var10.key(), var3)
         .stream()
         .filter(var0 -> !"fishing".equals(var0.context))
         .filter(SpawnEntry::hasStructureConstraint)
         .filter(StructureSelectorPolicy::routeIsUsable)
         .toList();
      if (var11.isEmpty()) {
         return StructureEstimateOptimizer.Result.empty();
      }

      Set<SpawnEntry> var12 = new HashSet<>(var11);
      List<List<Seasoning>> var13 = candidateRecipes(var10, var11, var4, var6, var7);
      if (var13.isEmpty()) {
         return StructureEstimateOptimizer.Result.empty();
      }

      List<String> var14 = BiomeCatalog.naturallySupportedBiomeIds(var1, this.data);
      List<StructureEstimateOptimizer.Scenario> var15 = new ArrayList<>();
      Map<String, List<SpawnEntry>> var16 = new LinkedHashMap<>();

      for (SpawnEntry var18 : this.data.spawns()) {
         if (!"fishing".equals(var18.context) && StructureSelectorPolicy.routeIsAvailable(var18)) {
            var16.computeIfAbsent(var18.context, var0 -> new ArrayList<>()).add(var18);
         }
      }

      for (String var36 : var14) {
         BiomeMatcher var19 = BiomeMatcher.create(var1, var36).orElse(null);
         if (var19 != null) {
            for (SpawnEntry var21 : var11) {
               if (var19.satisfiesEntryBiome(var21) && HabitatPolicy.allows(var19, var21, var5)) {
                  SpawnEnvironmentProfile var22 = SpawnEnvironmentProfile.fromTarget(var21);
                  List<StructureEstimateOptimizer.PreparedSpawn> var23 = new ArrayList<>();

                  for (SpawnEntry var25 : var16.getOrDefault(var21.context, List.of())) {
                     if (var19.satisfiesEntryBiome(var25)
                        && var22.isCompatible(var25)
                        && (!var25.hasStructureConstraint() || StructureSelectorPolicy.routesMayOverlap(var21, var25))) {
                        int var26 = BUCKETS.indexOf(BucketOdds.normalizeBucket(var25.bucket));
                        if (var26 >= 0) {
                           SpeciesInfo var27 = this.data.species().get(var25.speciesKey);
                           if (var27 == null) {
                              var27 = this.data.findSpecies(var25.speciesKey);
                           }

                           if (var27 != null) {
                              double var28 = var25.weight;

                              for (SpawnEntry.WeightMultiplier var31 : var25.weightMultipliers) {
                                 if (var19.satisfiesConditionsBiomeOnly(var31.conditions(), var31.antiConditions())) {
                                    boolean var32 = var31.conditions().stream().allMatch(var22::definitelySatisfies);
                                    boolean var33 = var31.antiConditions().stream().anyMatch(var22::definitelySatisfies);
                                    if (var32 && !var33) {
                                       var28 *= var31.multiplier();
                                    }
                                 }
                              }

                              if (!(var28 <= 0.0)) {
                                 var23.add(
                                    new StructureEstimateOptimizer.PreparedSpawn(
                                       var26, var28, var12.contains(var25), var27.resolveForSpawnString(var25.pokemonExpression)
                                    )
                                 );
                              }
                           }
                        }
                     }
                  }

                  if (var23.stream().anyMatch(StructureEstimateOptimizer.PreparedSpawn::target)) {
                     var15.add(new StructureEstimateOptimizer.Scenario(var36, List.copyOf(var23)));
                  }
               }
            }
         }
      }

      if (var15.isEmpty()) {
         return StructureEstimateOptimizer.Result.empty();
      }

      StructureEstimateOptimizer.Candidate var35 = null;

      for (List<Seasoning> var39 : var13) {
         Map<String, Double> var40 = new LinkedHashMap<>();

         for (StructureEstimateOptimizer.Scenario var43 : var15) {
            double var45 = chanceFor(var43.spawns(), var39);
            if (!(var45 <= 0.0)) {
               var40.merge(var43.biomeId(), var45, Math::max);
            }
         }

         if (!var40.isEmpty()) {
            List<Double> var42 = new ArrayList<>(var40.values());
            var42.sort(Double::compareTo);
            double var44 = (Double)var42.get(0);
            double var46 = (Double)var42.get(var42.size() - 1);
            double var47 = median(var42);
            StructureEstimateOptimizer.Candidate var48 = new StructureEstimateOptimizer.Candidate(var39, var44, var47, var46, var42.size());
            if (var35 == null || better(var48, var35, var4, var7)) {
               var35 = var48;
            }
         }
      }

      if (var35 == null) {
         return StructureEstimateOptimizer.Result.empty();
      }

      long var38 = (System.nanoTime() - var8) / 1000000L;
      SessionDiagnostics.event(
         "structure-estimate-run",
         String.format(
            Locale.ROOT,
            "pokemon=%s routes=%d scenarios=%d biomes=%d recipes=%d low=%.4f%% typical=%.4f%% high=%.4f%% recipe=%s bite=%s noEnchantedApple=%s shiny=%s elapsedMs=%d",
            var10.key(),
            var11.size(),
            var15.size(),
            var35.biomeCount(),
            var13.size(),
            var35.low() * 100.0,
            var35.typical() * 100.0,
            var35.high() * 100.0,
            var35.recipe().stream().map(Enum::name).collect(Collectors.joining(",")),
            var4,
            var6,
            var7,
            var38
         )
      );
      return new StructureEstimateOptimizer.Result(var35.recipe(), var35.low(), var35.typical(), var35.high(), var35.biomeCount(), var11.size(), var13.size());
   }

   private static double chanceFor(List<StructureEstimateOptimizer.PreparedSpawn> var0, List<Seasoning> var1) {
      int var2 = var1.stream().mapToInt(var0x -> var0x.rarityTierBoost).sum();
      Seasoning var3 = var1.stream().filter(var0x -> var0x.kind == Seasoning.Kind.EV_FILTER).findFirst().orElse(null);
      Seasoning var4 = var1.stream().filter(var0x -> var0x.kind == Seasoning.Kind.TYPE).findFirst().orElse(null);
      List<Seasoning> var5 = var1.stream().filter(var0x -> var0x.kind == Seasoning.Kind.EGG_GROUP).toList();
      double[] var6 = new double[BUCKETS.size()];
      double[] var7 = new double[BUCKETS.size()];

      for (StructureEstimateOptimizer.PreparedSpawn var9 : var0) {
         double var10 = var9.weight();
         if (var3 != null && !var3.matches(var9.traits())) {
            var10 = 0.0;
         }

         if (var10 > 0.0 && var4 != null && var4.matches(var9.traits())) {
            var10 *= 10.0;
         }

         if (var10 > 0.0 && var5.stream().anyMatch(var1x -> var1x.matches(var9.traits()))) {
            var10 *= 10.0;
         }

         if (!(var10 <= 0.0)) {
            var6[var9.bucketIndex()] += var10;
            if (var9.target()) {
               var7[var9.bucketIndex()] += var10;
            }
         }
      }

      double var13 = 0.0;

      for (int var14 = 0; var14 < BUCKETS.size(); var14++) {
         if (var6[var14] > 0.0) {
            var13 += BucketOdds.oddsFor(BUCKETS.get(var14), var2);
         }
      }

      if (var13 <= 0.0) {
         return 0.0;
      }

      double var15 = 0.0;

      for (int var12 = 0; var12 < BUCKETS.size(); var12++) {
         if (!(var6[var12] <= 0.0) && !(var7[var12] <= 0.0)) {
            var15 += BucketOdds.oddsFor(BUCKETS.get(var12), var2) / var13 * var7[var12] / var6[var12];
         }
      }

      return var15;
   }

   private static List<List<Seasoning>> candidateRecipes(SpeciesInfo var0, List<SpawnEntry> var1, boolean var2, boolean var3, boolean var4) {
      List<SpeciesInfo.ResolvedTraits> var5 = var1.stream().map(var1x -> var0.resolveForSpawnString(var1x.pokemonExpression)).distinct().toList();
      List<Seasoning> var6 = new ArrayList<>();

      for (Seasoning var10 : Seasoning.values()) {
         if (var10 != Seasoning.NONE && (!var3 || var10 != Seasoning.ENCHANTED_GOLDEN_APPLE)) {
            boolean var11 = var5.stream().anyMatch(var10::matches);
            boolean var12 = var11 || var10.kind == Seasoning.Kind.RARITY || var10 == Seasoning.APPLE || var4 && var10.shinyBoost() > 0;
            if (var12) {
               var6.add(var10);
            }
         }
      }

      Map<String, List<Seasoning>> var13 = new LinkedHashMap<>();

      for (int var14 = 0; var14 < var6.size(); var14++) {
         for (int var15 = var14; var15 < var6.size(); var15++) {
            for (int var16 = var15; var16 < var6.size(); var16++) {
               List<Seasoning> var17 = List.of(var6.get(var14), var6.get(var15), var6.get(var16));
               if (usefulCategoryCounts(var17)
                  && (!var2 || !var17.stream().noneMatch(Seasoning::reducesBiteTime))
                  && !var17.stream().noneMatch(var1x -> var5.stream().anyMatch(var1x::matches))) {
                  List<Seasoning> var18 = normalizeRecipe(var17);
                  var13.putIfAbsent(recipeKey(var18), var18);
               }
            }
         }
      }

      return List.copyOf(var13.values());
   }

   private static boolean usefulCategoryCounts(List<Seasoning> var0) {
      long var1 = var0.stream().filter(var0x -> var0x.kind == Seasoning.Kind.EV_FILTER).count();
      long var3 = var0.stream().filter(var0x -> var0x.kind == Seasoning.Kind.TYPE).count();
      long var5 = var0.stream().filter(var0x -> var0x.kind == Seasoning.Kind.EGG_GROUP).count();
      long var7 = var0.stream().filter(var0x -> var0x.kind == Seasoning.Kind.BITE_TIME).count();
      return var1 <= 1L && var3 <= 1L && var5 <= 1L && var7 <= 1L;
   }

   private static List<Seasoning> normalizeRecipe(List<Seasoning> var0) {
      List<Seasoning> var1 = new ArrayList<>(var0);
      var1.sort(Comparator.comparingInt(StructureEstimateOptimizer::sortRank).thenComparing(Enum::name));
      return List.copyOf(var1);
   }

   private static int sortRank(Seasoning var0) {
      return switch (var0.kind) {
         case EV_FILTER -> 0;
         case TYPE -> 1;
         case EGG_GROUP -> 2;
         case RARITY -> 3;
         case SHINY -> 4;
         case BITE_TIME -> 5;
         default -> 6;
      };
   }

   private static boolean better(StructureEstimateOptimizer.Candidate var0, StructureEstimateOptimizer.Candidate var1, boolean var2, boolean var3) {
      if (var3) {
         int var4 = Integer.compare(Seasoning.totalShinyMultiplier(var0.recipe()), Seasoning.totalShinyMultiplier(var1.recipe()));
         if (var4 != 0) {
            return var4 > 0;
         }
      }

      int var7 = Double.compare(var0.typical(), var1.typical());
      if (var7 != 0) {
         return var7 > 0;
      }

      int var5 = Double.compare(var0.low(), var1.low());
      if (var5 != 0) {
         return var5 > 0;
      }

      int var6 = Double.compare(Seasoning.expectedBiteTimeReduction(var0.recipe()), Seasoning.expectedBiteTimeReduction(var1.recipe()));
      return var6 != 0 && var2 ? var6 > 0 : recipeKey(var0.recipe()).compareTo(recipeKey(var1.recipe())) < 0;
   }

   private static double median(List<Double> var0) {
      int var1 = var0.size() / 2;
      return var0.size() % 2 == 1 ? (Double)var0.get(var1) : ((Double)var0.get(var1 - 1) + (Double)var0.get(var1)) / 2.0;
   }

   private static String recipeKey(List<Seasoning> var0) {
      return var0.stream().map(Enum::name).sorted().reduce((var0x, var1) -> var0x + "+" + var1).orElse("");
   }

   private record Candidate(List<Seasoning> recipe, double low, double typical, double high, int biomeCount) {
   }

   private record PreparedSpawn(int bucketIndex, double weight, boolean target, SpeciesInfo.ResolvedTraits traits) {
   }

   public record Result(List<Seasoning> recipe, double lowChance, double typicalChance, double highChance, int biomeCount, int routeCount, int recipeCount) {
      public Result {
         recipe = recipe == null ? List.of() : List.copyOf(recipe);
      }

      public boolean available() {
         return this.recipe.size() == 3 && this.typicalChance > 0.0;
      }

      private static StructureEstimateOptimizer.Result empty() {
         return new StructureEstimateOptimizer.Result(List.of(), 0.0, 0.0, 0.0, 0, 0, 0);
      }
   }

   private record Scenario(String biomeId, List<StructureEstimateOptimizer.PreparedSpawn> spawns) {
   }
}
