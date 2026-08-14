package dev.cobblesnack.calc;

import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpawnCalculator {
   private static final List<String> BUCKETS = List.of("common", "uncommon", "rare", "ultrarare");
   private final DataIndex data;

   public SpawnCalculator(DataIndex var1) {
      this.data = var1;
   }

   public SpawnCalculator.SimulationResult simulate(BiomeMatcher var1, List<Seasoning> var2) {
      return this.simulate(var1, var2, null);
   }

   public SpawnCalculator.SimulationResult simulate(BiomeMatcher var1, List<Seasoning> var2, SpawnEnvironmentProfile var3) {
      int var4 = var2.stream().mapToInt(var0 -> var0.rarityTierBoost).sum();
      Map<String, List<SpawnCalculator.WeightedSpawn>> var5 = new LinkedHashMap<>();

      for (String var7 : BUCKETS) {
         var5.put(var7, new ArrayList<>());
      }

      int var28 = 0;
      int var29 = 0;
      int var8 = 0;
      int var9 = 0;
      Seasoning var10 = var2.stream().filter(var0 -> var0.kind == Seasoning.Kind.EV_FILTER).findFirst().orElse(null);
      Seasoning var11 = var2.stream().filter(var0 -> var0.kind == Seasoning.Kind.TYPE).findFirst().orElse(null);
      List<Seasoning> var12 = var2.stream().filter(var0 -> var0.kind == Seasoning.Kind.EGG_GROUP).toList();

      for (SpawnEntry var14 : this.data.spawns()) {
         if (!"fishing".equals(var14.context) && StructureSelectorPolicy.routeIsAvailable(var14) && var1.satisfiesEntryBiome(var14)) {
            var28++;
            if (var14.hasNonBiomeConditions()) {
               var8++;
            }

            if (var3 != null && !var3.isCompatible(var14)) {
               var29++;
            } else {
               SpeciesInfo var15 = this.data.species().get(var14.speciesKey);
               if (var15 == null) {
                  var15 = this.data.findSpecies(var14.speciesKey);
               }

               if (var15 == null) {
                  var9++;
               } else {
                  double var16 = var14.weight;

                  for (SpawnEntry.WeightMultiplier var19 : var14.weightMultipliers) {
                     if (var1.satisfiesConditionsBiomeOnly(var19.conditions(), var19.antiConditions())) {
                        if (var3 == null) {
                           boolean var20 = var19.conditions().stream().anyMatch(SpawnCondition::hasNonBiomeConstraint)
                              || var19.antiConditions().stream().anyMatch(SpawnCondition::hasNonBiomeConstraint);
                           if (!var20) {
                              var16 *= var19.multiplier();
                           }
                        } else {
                           boolean var42 = var19.conditions().stream().allMatch(var3::definitelySatisfies);
                           boolean var21 = var19.antiConditions().stream().anyMatch(var3::definitelySatisfies);
                           if (var42 && !var21) {
                              var16 *= var19.multiplier();
                           }
                        }
                     }
                  }

                  SpeciesInfo.ResolvedTraits var37 = var15.resolveForSpawnString(var14.pokemonExpression);
                  if (var10 != null && !var10.matches(var37)) {
                     var16 = 0.0;
                  }

                  if (var16 > 0.0 && var11 != null && var11.matches(var37)) {
                     var16 *= 10.0;
                  }

                  if (var16 > 0.0) {
                     for (Seasoning var43 : var12) {
                        if (var43.matches(var37)) {
                           var16 *= 10.0;
                           break;
                        }
                     }
                  }

                  var16 = Math.max(0.0, var16);
                  String var40 = BucketOdds.normalizeBucket(var14.bucket);
                  if (var5.containsKey(var40)) {
                     var5.get(var40).add(new SpawnCalculator.WeightedSpawn(var14, var15, var16));
                  }
               }
            }
         }
      }

      double var30 = 0.0;

      for (String var34 : BUCKETS) {
         if (var5.get(var34).stream().anyMatch(var0 -> var0.weight > 0.0)) {
            var30 += BucketOdds.oddsFor(var34, var4);
         }
      }

      Map<String, SpawnCalculator.MutableSpeciesChance> var32 = new HashMap<>();
      if (var30 > 0.0) {
         for (String var17 : BUCKETS) {
            List<SpawnCalculator.WeightedSpawn> var38 = var5.get(var17);
            double var41 = var38.stream().mapToDouble(SpawnCalculator.WeightedSpawn::weight).sum();
            if (!(var41 <= 0.0)) {
               double var44 = BucketOdds.oddsFor(var17, var4) / var30;

               for (SpawnCalculator.WeightedSpawn var24 : var38) {
                  if (!(var24.weight <= 0.0)) {
                     double var25 = var44 * var24.weight / var41;
                     SpawnCalculator.MutableSpeciesChance var27 = var32.computeIfAbsent(
                        var24.info.key(), var1x -> new SpawnCalculator.MutableSpeciesChance(var24.info.displayName(), var24.info.key())
                     );
                     var27.chance += var25;
                     var27.hasExtraConditions = var27.hasExtraConditions | var24.entry.hasNonBiomeConditions();
                     var27.spawnEntryCount++;
                  }
               }
            }
         }
      }

      List<SpawnCalculator.SpeciesChance> var36 = var32.values()
         .stream()
         .map(var0 -> new SpawnCalculator.SpeciesChance(var0.displayName, var0.speciesKey, var0.chance, var0.hasExtraConditions, var0.spawnEntryCount))
         .sorted(Comparator.comparingDouble(SpawnCalculator.SpeciesChance::chance).reversed().thenComparing(SpawnCalculator.SpeciesChance::displayName))
         .toList();
      return new SpawnCalculator.SimulationResult(var36, var4, var28, var29, var8, var9);
   }

   public SpawnCalculator.TargetedSimulationResult simulateForTarget(BiomeMatcher var1, List<Seasoning> var2, String var3) {
      return this.simulateForTarget(var1, var2, var3, null, true);
   }

   public SpawnCalculator.TargetedSimulationResult simulateForTarget(BiomeMatcher var1, List<Seasoning> var2, String var3, String var4) {
      return this.simulateForTarget(var1, var2, var3, var4, true);
   }

   public SpawnCalculator.TargetedSimulationResult simulateForTarget(BiomeMatcher var1, List<Seasoning> var2, String var3, String var4, boolean var5) {
      SpeciesInfo var6 = this.data.findSpecies(var3);
      if (var6 == null) {
         return new SpawnCalculator.TargetedSimulationResult(null, null, null, 0.0);
      }

      SpawnCalculator.TargetedSimulationResult var7 = null;
      int var8 = Integer.MAX_VALUE;

      for (SpawnEntry var10 : this.data.spawnsForSpecies(var6.key(), var4)) {
         if (!"fishing".equals(var10.context)
            && StructureSelectorPolicy.routeIsAvailable(var10)
            && var1.satisfiesEntryBiome(var10)
            && HabitatPolicy.allows(var1, var10, var5)) {
            SpawnEnvironmentProfile var11 = SpawnEnvironmentProfile.fromTarget(var10);
            SpawnCalculator.SimulationResult var12 = this.simulate(var1, var2, var11);
            double var13 = chanceFrom(var12, var6.key());
            int var15 = HabitatPolicy.rank(var1, var10);
            if (var7 == null || (var5 && var15 != var8 ? var15 <= var8 : var13 > var7.targetChance())) {
               var7 = new SpawnCalculator.TargetedSimulationResult(var6, var12, var11, var13);
               var8 = var15;
            }
         }
      }

      return var7 == null ? new SpawnCalculator.TargetedSimulationResult(var6, null, null, 0.0) : var7;
   }

   public double chanceForTarget(BiomeMatcher var1, List<Seasoning> var2, String var3) {
      return this.simulateForTarget(var1, var2, var3, null, true).targetChance();
   }

   private static double chanceFrom(SpawnCalculator.SimulationResult var0, String var1) {
      for (SpawnCalculator.SpeciesChance var3 : var0.results()) {
         if (var3.speciesKey.equals(var1)) {
            return var3.chance;
         }
      }

      return 0.0;
   }

   private static final class MutableSpeciesChance {
      final String displayName;
      final String speciesKey;
      double chance;
      boolean hasExtraConditions;
      int spawnEntryCount;

      MutableSpeciesChance(String var1, String var2) {
         this.displayName = var1;
         this.speciesKey = var2;
      }
   }

   public record SimulationResult(
      List<SpawnCalculator.SpeciesChance> results,
      int rarityTier,
      int biomeEligibleSpawnEntries,
      int conditionFilteredSpawnEntries,
      int extraConditionSpawnEntries,
      int unknownSpeciesSpawnEntries
   ) {
   }

   public record SpeciesChance(String displayName, String speciesKey, double chance, boolean hasExtraConditions, int spawnEntryCount) {
   }

   public record TargetedSimulationResult(SpeciesInfo target, SpawnCalculator.SimulationResult simulation, SpawnEnvironmentProfile profile, double targetChance) {
   }

   private record WeightedSpawn(SpawnEntry entry, SpeciesInfo info, double weight) {
   }
}
