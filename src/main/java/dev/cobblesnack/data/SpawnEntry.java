package dev.cobblesnack.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpawnEntry {
   public final String id;
   public final String pokemonExpression;
   public final String speciesKey;
   public final String context;
   public final String bucket;
   public final double weight;
   public final List<SpawnCondition> conditions;
   public final List<SpawnCondition> antiConditions;
   public final List<SpawnEntry.WeightMultiplier> weightMultipliers;
   public final boolean hasUnknownConditions;
   public String source;

   public SpawnEntry(
      String var1,
      String var2,
      String var3,
      String var4,
      String var5,
      double var6,
      List<SpawnCondition> var8,
      List<SpawnCondition> var9,
      List<SpawnEntry.WeightMultiplier> var10
   ) {
      this(var1, var2, var3, var4, var5, var6, var8, var9, var10, "unknown");
   }

   public SpawnEntry(
      String var1,
      String var2,
      String var3,
      String var4,
      String var5,
      double var6,
      List<SpawnCondition> var8,
      List<SpawnCondition> var9,
      List<SpawnEntry.WeightMultiplier> var10,
      String var11
   ) {
      this.id = var1;
      this.pokemonExpression = var2;
      this.speciesKey = var3;
      this.bucket = var5;
      this.weight = var6;
      this.conditions = List.copyOf(var8);
      this.antiConditions = List.copyOf(var9);
      this.weightMultipliers = List.copyOf(var10);
      String var12 = var4 != null && !var4.isBlank() ? var4.toLowerCase(Locale.ROOT) : "grounded";
      this.context = this.conditions.stream().anyMatch(var0 -> !var0.rodTypes.isEmpty()) ? "fishing" : var12;
      this.source = var11 != null && !var11.isBlank() ? var11 : "unknown";
      this.hasUnknownConditions = var8.stream().anyMatch(var0 -> !var0.unknownKeys.isEmpty())
         || var9.stream().anyMatch(var0 -> !var0.unknownKeys.isEmpty())
         || var10.stream()
            .anyMatch(
               var0 -> var0.conditions.stream().anyMatch(var0x -> !var0x.unknownKeys.isEmpty())
                  || var0.antiConditions.stream().anyMatch(var0x -> !var0x.unknownKeys.isEmpty())
            );
   }

   public void setSource(String var1) {
      if (var1 != null && !var1.isBlank()) {
         this.source = var1;
      }
   }

   public boolean hasNonBiomeConditions() {
      if (!"grounded".equals(this.context)) {
         return true;
      } else if (this.conditions.stream().anyMatch(SpawnCondition::hasNonBiomeConstraint)) {
         return true;
      } else {
         return this.antiConditions.stream().anyMatch(SpawnCondition::hasNonBiomeConstraint)
            ? true
            : this.weightMultipliers.stream().anyMatch(var0 -> !var0.conditions.isEmpty() || !var0.antiConditions.isEmpty());
      }
   }

   public boolean hasBaseBlockRequirement() {
      return this.conditions.stream().anyMatch(var0 -> !var0.neededBaseBlocks.isEmpty());
   }

   public boolean hasPositiveBiomeConstraint() {
      return this.conditions.stream().anyMatch(SpawnCondition::hasBiomeConstraint);
   }

   public boolean hasStructureConstraint() {
      return this.conditions.stream().anyMatch(var0 -> !var0.structures.isEmpty());
   }

   public boolean isFishingRoute() {
      return "fishing".equals(this.context) || this.conditions.stream().anyMatch(var0 -> !var0.rodTypes.isEmpty());
   }

   public boolean isBiomeRankable() {
      return !this.isFishingRoute() && this.hasPositiveBiomeConstraint() && !this.hasStructureConstraint();
   }

   public boolean explicitlyTargetsBiome(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var2 = var1.toLowerCase(Locale.ROOT);

         for (SpawnCondition var4 : this.conditions) {
            for (String var6 : var4.biomes) {
               String var7 = var6.toLowerCase(Locale.ROOT);
               if (var7.equals(var2) || var7.contains("ultra") || var7.contains("void")) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean explicitlyTargetsAquaticBiome() {
      for (SpawnCondition var2 : this.conditions) {
         for (String var4 : var2.biomes) {
            String var5 = var4.toLowerCase(Locale.ROOT);
            if (var5.contains("ocean") || var5.contains("river") || var5.contains("coast") || var5.contains("beach") || var5.contains("swamp")) {
               return true;
            }
         }
      }

      return false;
   }

   public List<String> requirementSummaryParts() {
      ArrayList var1 = new ArrayList();

      var1.add(switch (this.context) {
         case "submerged" -> "Underwater/submerged";
         case "surface" -> "Water/lava surface";
         case "seafloor" -> "Seafloor";
         case "air" -> "In the air";
         case "fishing" -> "Fishing";
         case "grounded" -> "Grounded";
         default -> "Context: " + this.context;
      });

      for (SpawnCondition var5 : this.conditions) {
         var1.addAll(var5.conciseSummaryParts());
      }

      for (SpawnCondition var7 : this.antiConditions) {
         var1.addAll(var7.conciseAvoidSummaryParts());
      }

      return var1;
   }

   public record WeightMultiplier(double multiplier, List<SpawnCondition> conditions, List<SpawnCondition> antiConditions) {
      public WeightMultiplier {
         conditions = List.copyOf(conditions);
         antiConditions = List.copyOf(antiConditions);
      }
   }
}
