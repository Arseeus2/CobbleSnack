package dev.cobblesnack.calc;

import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public final class BiomeMatcher {
   private final Identifier biomeId;
   private final Reference<Biome> biomeEntry;

   private BiomeMatcher(Identifier var1, Reference<Biome> var2) {
      this.biomeId = var1;
      this.biomeEntry = var2;
   }

   public static Optional<BiomeMatcher> create(Registry<Biome> var0, String var1) {
      String var3 = var1 == null ? "" : var1.trim().toLowerCase(Locale.ROOT);
      if (var3.isBlank()) {
         return Optional.empty();
      }

      if (!var3.contains(":")) {
         var3 = "minecraft:" + var3;
      }

      Identifier var2;
      if ((var2 = Identifier.tryParse(var3)) == null) {
         return Optional.empty();
      }

      RegistryKey var5 = RegistryKey.of(RegistryKeys.BIOME, var2);
      Optional var6 = var0.getEntry(var5);
      return var6.map(var1x -> new BiomeMatcher(var2, (Reference<Biome>)var1x));
   }

   public Identifier biomeId() {
      return this.biomeId;
   }

   public boolean matchesSelector(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var3 = var1.trim().toLowerCase(Locale.ROOT);
         if (var3.startsWith("#")) {
            String var5 = var3.substring(1);
            if (!var5.contains(":")) {
               var5 = "cobblemon:" + var5;
            }

            Identifier var4;
            return (var4 = Identifier.tryParse(var5)) != null && this.biomeEntry.isIn(TagKey.of(RegistryKeys.BIOME, var4));
         } else {
            if (!var3.contains(":")) {
               var3 = "minecraft:" + var3;
            }

            Identifier var2;
            return (var2 = Identifier.tryParse(var3)) != null && this.biomeId.equals(var2);
         }
      } else {
         return false;
      }
   }

   public boolean satisfiesEntryBiome(SpawnEntry var1) {
      for (SpawnCondition var3 : var1.conditions) {
         if (!var3.biomes.isEmpty() && var3.biomes.stream().noneMatch(this::matchesSelector)) {
            return false;
         }
      }

      for (SpawnCondition var5 : var1.antiConditions) {
         if (var5.isBiomeOnly() && var5.biomes.stream().anyMatch(this::matchesSelector)) {
            return false;
         }
      }

      return true;
   }

   public boolean satisfiesConditionsBiomeOnly(List<SpawnCondition> var1, List<SpawnCondition> var2) {
      for (SpawnCondition var4 : var1) {
         if (!var4.biomes.isEmpty() && var4.biomes.stream().noneMatch(this::matchesSelector)) {
            return false;
         }
      }

      for (SpawnCondition var6 : var2) {
         if (var6.isBiomeOnly() && var6.biomes.stream().anyMatch(this::matchesSelector)) {
            return false;
         }
      }

      return true;
   }

   public boolean isAquaticDominant() {
      String var1 = this.biomeId.getPath().toLowerCase(Locale.ROOT);
      return !var1.contains("ocean") && !var1.contains("river")
         ? this.matchesSelector("#minecraft:is_ocean")
            || this.matchesSelector("#minecraft:is_river")
            || this.matchesSelector("#cobblemon:is_ocean")
            || this.matchesSelector("#cobblemon:is_river")
         : true;
   }

   public boolean isAquaticFriendly() {
      if (this.isAquaticDominant()) {
         return true;
      }

      String var1 = this.biomeId.getPath().toLowerCase(Locale.ROOT);

      for (String var3 : List.of("beach", "coast", "swamp", "marsh", "wetland", "lake", "mangrove", "bayou", "reef", "lagoon", "freshwater", "waterway")) {
         if (var1.contains(var3)) {
            return true;
         }
      }

      return this.matchesSelector("#cobblemon:is_beach")
         || this.matchesSelector("#cobblemon:is_coast")
         || this.matchesSelector("#cobblemon:is_swamp")
         || this.matchesSelector("#cobblemon:is_freshwater")
         || this.matchesSelector("#cobblemon:is_warm_ocean")
         || this.matchesSelector("#cobblemon:is_lukewarm_ocean")
         || this.matchesSelector("#cobblemon:is_cold_ocean")
         || this.matchesSelector("#cobblemon:is_deep_ocean");
   }

   public boolean isLavaFriendly() {
      String var1 = this.biomeId.getPath().toLowerCase(Locale.ROOT);

      for (String var3 : List.of("volcan", "lava", "magma", "basalt", "infernal")) {
         if (var1.contains(var3)) {
            return true;
         }
      }

      return this.matchesSelector("#minecraft:is_nether") || this.matchesSelector("#cobblemon:is_nether") || this.matchesSelector("#cobblemon:is_volcanic");
   }
}
