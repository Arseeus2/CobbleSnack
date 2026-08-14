package dev.cobblesnack.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class SpawnCondition {
   private static final Set<String> KNOWN_KEYS = Set.of(
      "dimensions",
      "biomes",
      "structures",
      "moonPhase",
      "canSeeSky",
      "minX",
      "minY",
      "minZ",
      "maxX",
      "maxY",
      "maxZ",
      "minLight",
      "maxLight",
      "minSkyLight",
      "maxSkyLight",
      "timeRange",
      "isRaining",
      "isThundering",
      "isSlimeChunk",
      "labels",
      "labelMode",
      "minWidth",
      "maxWidth",
      "minHeight",
      "maxHeight",
      "neededNearbyBlocks",
      "neededBaseBlocks",
      "minDepth",
      "maxDepth",
      "fluidIsSource",
      "fluid",
      "minLureLevel",
      "maxLureLevel",
      "bobber",
      "bait",
      "rodType"
   );
   public final List<String> dimensions;
   public final List<String> biomes;
   public final List<String> structures;
   public final Integer moonPhase;
   public final Boolean canSeeSky;
   public final Integer minX;
   public final Integer minY;
   public final Integer minZ;
   public final Integer maxX;
   public final Integer maxY;
   public final Integer maxZ;
   public final Integer minLight;
   public final Integer maxLight;
   public final Integer minSkyLight;
   public final Integer maxSkyLight;
   public final String timeRange;
   public final Boolean isRaining;
   public final Boolean isThundering;
   public final Boolean isSlimeChunk;
   public final List<String> labels;
   public final String labelMode;
   public final Integer minWidth;
   public final Integer maxWidth;
   public final Integer minHeight;
   public final Integer maxHeight;
   public final List<String> neededNearbyBlocks;
   public final List<String> neededBaseBlocks;
   public final Integer minDepth;
   public final Integer maxDepth;
   public final Boolean fluidIsSource;
   public final List<String> fluid;
   public final List<String> rodTypes;
   public final Set<String> unknownKeys;

   private SpawnCondition(JsonObject var1) {
      this.dimensions = stringList(var1.get("dimensions"));
      this.biomes = stringList(var1.get("biomes"));
      this.structures = stringList(var1.get("structures"));
      this.moonPhase = integer(var1, "moonPhase");
      this.canSeeSky = bool(var1, "canSeeSky");
      this.minX = integer(var1, "minX");
      this.minY = integer(var1, "minY");
      this.minZ = integer(var1, "minZ");
      this.maxX = integer(var1, "maxX");
      this.maxY = integer(var1, "maxY");
      this.maxZ = integer(var1, "maxZ");
      this.minLight = integer(var1, "minLight");
      this.maxLight = integer(var1, "maxLight");
      this.minSkyLight = integer(var1, "minSkyLight");
      this.maxSkyLight = integer(var1, "maxSkyLight");
      this.timeRange = string(var1, "timeRange");
      this.isRaining = bool(var1, "isRaining");
      this.isThundering = bool(var1, "isThundering");
      this.isSlimeChunk = bool(var1, "isSlimeChunk");
      this.labels = stringList(var1.get("labels"));
      this.labelMode = string(var1, "labelMode");
      this.minWidth = integer(var1, "minWidth");
      this.maxWidth = integer(var1, "maxWidth");
      this.minHeight = integer(var1, "minHeight");
      this.maxHeight = integer(var1, "maxHeight");
      this.neededNearbyBlocks = stringList(var1.get("neededNearbyBlocks"));
      this.neededBaseBlocks = stringList(var1.get("neededBaseBlocks"));
      this.minDepth = integer(var1, "minDepth");
      this.maxDepth = integer(var1, "maxDepth");
      this.fluidIsSource = bool(var1, "fluidIsSource");
      this.fluid = stringList(var1.get("fluid"));
      this.rodTypes = stringList(var1.get("rodType"));
      LinkedHashSet var2 = new LinkedHashSet();

      for (String var4 : var1.keySet()) {
         if (!KNOWN_KEYS.contains(var4)) {
            var2.add(var4);
         }
      }

      this.unknownKeys = Collections.unmodifiableSet(var2);
   }

   public static SpawnCondition from(JsonObject var0) {
      return var0 == null ? null : new SpawnCondition(var0);
   }

   public boolean hasBiomeConstraint() {
      return !this.biomes.isEmpty();
   }

   public boolean hasNonBiomeConstraint() {
      return !this.dimensions.isEmpty()
         || !this.structures.isEmpty()
         || this.moonPhase != null
         || this.canSeeSky != null
         || this.minX != null
         || this.minY != null
         || this.minZ != null
         || this.maxX != null
         || this.maxY != null
         || this.maxZ != null
         || this.minLight != null
         || this.maxLight != null
         || this.minSkyLight != null
         || this.maxSkyLight != null
         || this.timeRange != null
         || this.isRaining != null
         || this.isThundering != null
         || this.isSlimeChunk != null
         || !this.labels.isEmpty()
         || this.labelMode != null
         || this.minWidth != null
         || this.maxWidth != null
         || this.minHeight != null
         || this.maxHeight != null
         || !this.neededNearbyBlocks.isEmpty()
         || !this.neededBaseBlocks.isEmpty()
         || this.minDepth != null
         || this.maxDepth != null
         || this.fluidIsSource != null
         || !this.fluid.isEmpty()
         || !this.rodTypes.isEmpty()
         || !this.unknownKeys.isEmpty();
   }

   public boolean isBiomeOnly() {
      return this.hasBiomeConstraint() && !this.hasNonBiomeConstraint();
   }

   public boolean hasSupportedEnvironmentConstraint() {
      return this.canSeeSky != null
         || this.minY != null
         || this.maxY != null
         || this.minLight != null
         || this.maxLight != null
         || this.minSkyLight != null
         || this.maxSkyLight != null
         || this.timeRange != null
         || this.isRaining != null
         || this.isThundering != null
         || this.minWidth != null
         || this.maxWidth != null
         || this.minHeight != null
         || this.maxHeight != null
         || this.minDepth != null
         || this.maxDepth != null
         || this.fluidIsSource != null
         || !this.fluid.isEmpty()
         || !this.rodTypes.isEmpty();
   }

   public List<String> conciseSummaryParts() {
      ArrayList var1 = new ArrayList();
      if (this.canSeeSky != null) {
         var1.add(this.canSeeSky ? "Sky visible" : "No sky visibility");
      }

      addRange(var1, "Sky light", this.minSkyLight, this.maxSkyLight);
      addRange(var1, "Light", this.minLight, this.maxLight);
      addRange(var1, "Y", this.minY, this.maxY);
      if (this.timeRange != null && !this.timeRange.isBlank()) {
         var1.add("Time: " + this.timeRange);
      }

      if (Boolean.TRUE.equals(this.isThundering)) {
         var1.add("Thunder");
      } else if (Boolean.TRUE.equals(this.isRaining)) {
         var1.add("Rain");
      } else if (Boolean.FALSE.equals(this.isRaining)) {
         var1.add("No rain");
      }

      if (!this.neededBaseBlocks.isEmpty()) {
         var1.add("On: " + shortList(this.neededBaseBlocks));
      }

      if (!this.neededNearbyBlocks.isEmpty()) {
         var1.add("Near: " + shortList(this.neededNearbyBlocks));
      }

      if (!this.structures.isEmpty()) {
         var1.add("Structure: " + shortList(this.structures));
      }

      if (!this.fluid.isEmpty()) {
         var1.add("Fluid: " + shortList(this.fluid));
      }

      if (!this.rodTypes.isEmpty()) {
         var1.add("Needs a " + this.rodTypes.stream().map(SpawnCondition::friendlyItemName).collect(Collectors.joining(" or ")));
      }

      addRange(var1, "Depth", this.minDepth, this.maxDepth);
      if (!this.dimensions.isEmpty()) {
         var1.add("Dimension: " + shortList(this.dimensions));
      }

      if (!this.unknownKeys.isEmpty()) {
         var1.add("Other: " + String.join(", ", this.unknownKeys));
      }

      return var1;
   }

   private static void addRange(List<String> var0, String var1, Integer var2, Integer var3) {
      if (var2 != null || var3 != null) {
         if (var2 != null && var3 != null) {
            var0.add(var1 + " " + var2 + "-" + var3);
         } else if (var2 != null) {
            var0.add(var1 + " >=" + var2);
         } else {
            var0.add(var1 + " <=" + var3);
         }
      }
   }

   public static String shortList(List<String> var0) {
      if (var0 != null && !var0.isEmpty()) {
         int var1 = Math.min(2, var0.size());
         String var2 = String.join(", ", var0.subList(0, var1));
         return var0.size() > var1 ? var2 + " +" + (var0.size() - var1) : var2;
      } else {
         return "";
      }
   }

   private static String friendlyItemName(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim();
         int var2 = var1.indexOf(58);
         if (var2 >= 0 && var2 + 1 < var1.length()) {
            var1 = var1.substring(var2 + 1);
         }

         StringBuilder var3 = new StringBuilder();

         for (String var7 : var1.replace('-', '_').split("_+")) {
            if (!var7.isBlank()) {
               if (!var3.isEmpty()) {
                  var3.append(' ');
               }

               var3.append(Character.toUpperCase(var7.charAt(0))).append(var7.substring(1).toLowerCase(Locale.ROOT));
            }
         }

         return var3.isEmpty() ? "special rod" : var3.toString();
      } else {
         return "special rod";
      }
   }

   private static List<String> stringList(JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         ArrayList var1 = new ArrayList();
         if (!var0.isJsonArray()) {
            if (var0.isJsonPrimitive()) {
               try {
                  var1.add(var0.getAsString());
               } catch (Exception var5) {
               }
            }
         } else {
            for (JsonElement var3 : var0.getAsJsonArray()) {
               if (var3.isJsonPrimitive()) {
                  try {
                     var1.add(var3.getAsString());
                  } catch (Exception var6) {
                  }
               }
            }
         }

         return Collections.unmodifiableList(var1);
      } else {
         return List.of();
      }
   }

   private static Integer integer(JsonObject var0, String var1) {
      if (var0 != null && var0.has(var1) && !var0.get(var1).isJsonNull()) {
         try {
            return var0.get(var1).getAsInt();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Boolean bool(JsonObject var0, String var1) {
      if (var0 != null && var0.has(var1) && !var0.get(var1).isJsonNull()) {
         try {
            return var0.get(var1).getAsBoolean();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String string(JsonObject var0, String var1) {
      if (var0 != null && var0.has(var1) && !var0.get(var1).isJsonNull()) {
         try {
            return var0.get(var1).getAsString();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }
}
