package dev.cobblesnack.calc;

import dev.cobblesnack.cache.SessionDiagnostics;
import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.biome.Biome;

public final class BiomeCatalog {
   private static final Map<String, List<String>> SPECIES_BIOME_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, List<String>> NATURAL_BIOME_CACHE = new ConcurrentHashMap<>();

   private BiomeCatalog() {
   }

   public static void clearCache() {
      SPECIES_BIOME_CACHE.clear();
      NATURAL_BIOME_CACHE.clear();
   }

   public static List<String> allBiomeIds(Registry<Biome> var0) {
      BiomeReplacementPolicy.refreshForCurrentConnection();
      return var0.getKeys()
         .stream()
         .map(RegistryKey::getValue)
         .map(Object::toString)
         .filter(var1 -> !BiomeReplacementPolicy.isReplacedSource(var0, var1))
         .sorted(String.CASE_INSENSITIVE_ORDER)
         .toList();
   }

   public static List<String> naturallySupportedBiomeIds(Registry<Biome> var0, DataIndex var1) {
      List<String> var2 = allBiomeIds(var0);
      int var3 = var0.getKeys().size();
      int var4 = Math.max(0, var3 - var2.size());
      String var5 = BiomeReplacementPolicy.cacheIdentity();
      String var6 = System.identityHashCode(var1) + ":" + var5 + ":" + var2.hashCode();
      List<String> var7 = NATURAL_BIOME_CACHE.get(var6);
      if (var7 != null) {
         return var7;
      }

      List<SpawnEntry> var8 = var1.spawns().stream().filter(SpawnEntry::isBiomeRankable).toList();
      List<String> var9 = new ArrayList<>();
      List<String> var10 = new ArrayList<>();

      for (String var12 : var2) {
         boolean var13 = isSpecialSpawnBiome(var12);
         boolean var14 = BiomeMatcher.create(var0, var12)
            .map(var3x -> var8.stream().anyMatch(var3xx -> var3x.satisfiesEntryBiome(var3xx) && (!var13 || var3xx.explicitlyTargetsBiome(var12))))
            .orElse(false);
         (var14 ? var9 : var10).add(var12);
      }

      List<String> var15 = List.copyOf(var9);
      NATURAL_BIOME_CACHE.put(var6, var15);
      SessionDiagnostics.event(
         "biome-coverage",
         "registry="
            + var3
            + " accessible="
            + var2.size()
            + " replaced="
            + var4
            + " supported="
            + var15.size()
            + " noNaturalRoutes="
            + var10.size()
            + " ids="
            + String.join(",", var10)
      );
      SessionDiagnostics.event(
         "biome-labels", "count=" + var15.size() + " ids=" + var15.stream().map(var0x -> var0x + "=" + friendlyName(var0x)).collect(Collectors.joining("|"))
      );
      return var15;
   }

   private static boolean isSpecialSpawnBiome(String var0) {
      String var1 = var0 == null ? "" : var0.toLowerCase(Locale.ROOT);
      return var1.contains("ultrabeast") || var1.contains("ultra_void");
   }

   public static List<String> possibleBiomesForSpecies(Registry<Biome> var0, DataIndex var1, SpeciesInfo var2) {
      return possibleBiomesForSpecies(var0, var1, var2, null, true);
   }

   public static List<String> possibleBiomesForSpecies(Registry<Biome> var0, DataIndex var1, SpeciesInfo var2, String var3) {
      return possibleBiomesForSpecies(var0, var1, var2, var3, true);
   }

   public static List<String> possibleBiomesForSpecies(Registry<Biome> var0, DataIndex var1, SpeciesInfo var2, String var3, boolean var4) {
      if (var2 == null) {
         return allBiomeIds(var0);
      }

      List<String> var5 = naturallySupportedBiomeIds(var0, var1);
      String var6 = var3 == null ? "" : SpeciesInfo.normalize(var3);
      String var7 = System.identityHashCode(var1)
         + ":"
         + BiomeReplacementPolicy.cacheIdentity()
         + ":"
         + var2.key()
         + ":form="
         + var6
         + ":habitat="
         + var4
         + ":"
         + var5.hashCode();
      List<String> var8 = SPECIES_BIOME_CACHE.get(var7);
      if (var8 != null) {
         return var8;
      }

      List<SpawnEntry> var9 = var1.spawnsForSpecies(var2.key(), var3);
      if (var9.isEmpty()) {
         return List.of();
      }

      LinkedHashSet<String> var10 = new LinkedHashSet<>();

      for (String var12 : var5) {
         BiomeMatcher.create(var0, var12).ifPresent(var4x -> {
            for (SpawnEntry var6x : var9) {
               if (var6x.isBiomeRankable() && var4x.satisfiesEntryBiome(var6x) && HabitatPolicy.allows(var4x, var6x, var4)) {
                  var10.add(var12);
                  break;
               }
            }
         });
      }

      List<String> var13 = new ArrayList<>(var10);
      var13.sort(Comparator.comparing(BiomeCatalog::friendlyName, String.CASE_INSENSITIVE_ORDER));
      List<String> var14 = List.copyOf(var13);
      SPECIES_BIOME_CACHE.put(var7, var14);
      return var14;
   }

   public static String friendlyName(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            Identifier var1 = Identifier.of(var0);
            String var2 = Util.createTranslationKey("biome", var1);
            if (I18n.hasTranslation(var2)) {
               return I18n.translate(var2);
            }
         } catch (Throwable var9) {
         }

         String var10 = var0;
         int var11 = var10.indexOf(58);
         if (var11 >= 0 && var11 + 1 < var10.length()) {
            var10 = var10.substring(var11 + 1);
         }

         String[] var3 = var10.replace('-', '_').split("_");
         StringBuilder var4 = new StringBuilder();

         for (String var8 : var3) {
            if (!var8.isBlank()) {
               if (!var4.isEmpty()) {
                  var4.append(' ');
               }

               var4.append(var8.substring(0, 1).toUpperCase(Locale.ROOT));
               if (var8.length() > 1) {
                  var4.append(var8.substring(1));
               }
            }
         }

         return (String)(var4.isEmpty() ? var0 : var4.toString());
      } else {
         return "Unknown biome";
      }
   }
}
