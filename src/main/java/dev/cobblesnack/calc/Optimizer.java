package dev.cobblesnack.calc;

import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpeciesInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Optimizer {
   private final DataIndex data;
   private final SpawnCalculator calculator;

   public Optimizer(DataIndex data) {
      this.data = data;
      this.calculator = new SpawnCalculator(data);
   }

   public Optimizer.OptimizationResult optimize(BiomeMatcher biome, String pokemonQuery, int limit) {
      SpeciesInfo target = this.data.findSpecies(pokemonQuery);
      if (target == null) {
         return new Optimizer.OptimizationResult(null, List.of());
      }

      SpeciesInfo.ResolvedTraits traits = target.resolveForSpawnString(target.key());
      List<Seasoning> candidates = new ArrayList<>();
      candidates.add(Seasoning.NONE);

      for (Seasoning seasoning : Seasoning.values()) {
         if (seasoning != Seasoning.NONE && (seasoning.kind == Seasoning.Kind.RARITY || seasoning.matches(traits))) {
            candidates.add(seasoning);
         }
      }

      Map<String, Optimizer.CombinationResult> bestByIngredients = new HashMap<>();

      for (Seasoning a : candidates) {
         for (Seasoning b : candidates) {
            for (Seasoning c : candidates) {
               List<Seasoning> combo = List.of(a, b, c);
               double chance = this.calculator.chanceForTarget(biome, combo, target.key());
               String key = multisetKey(combo);
               Optimizer.CombinationResult old = bestByIngredients.get(key);
               if (old == null || chance > old.chance) {
                  bestByIngredients.put(key, new Optimizer.CombinationResult(combo, chance));
               }
            }
         }
      }

      List<Optimizer.CombinationResult> sorted = bestByIngredients.values()
         .stream()
         .sorted(Comparator.comparingDouble(Optimizer.CombinationResult::chance).reversed().thenComparing(r -> multisetKey(r.seasonings())))
         .limit(limit)
         .toList();
      return new Optimizer.OptimizationResult(target, sorted);
   }

   private static String multisetKey(List<Seasoning> combo) {
      return combo.stream().map(Enum::name).sorted().reduce((a, b) -> a + "+" + b).orElse("");
   }

   public record CombinationResult(List<Seasoning> seasonings, double chance) {
   }

   public record OptimizationResult(SpeciesInfo target, List<Optimizer.CombinationResult> combinations) {
   }
}
