package dev.cobblesnack.calc;

import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class SpawnEnvironmentProfile {
   public final SpawnEntry targetEntry;
   public final String context;
   private final SpawnEnvironmentProfile.Range skyLight;
   private final SpawnEnvironmentProfile.Range light;
   private final SpawnEnvironmentProfile.Range x;
   private final SpawnEnvironmentProfile.Range y;
   private final SpawnEnvironmentProfile.Range z;
   private final SpawnEnvironmentProfile.Range width;
   private final SpawnEnvironmentProfile.Range height;
   private final SpawnEnvironmentProfile.Range depth;
   private final Boolean canSeeSky;
   private final Boolean isRaining;
   private final Boolean isThundering;
   private final Boolean isSlimeChunk;
   private final Boolean fluidIsSource;
   private final Integer moonPhase;
   private final List<List<String>> fluidGroups;
   private final List<String> timeRanges;

   private SpawnEnvironmentProfile(SpawnEntry var1) {
      this.targetEntry = var1;
      this.context = var1.context;
      SpawnEnvironmentProfile.MutableRange var2 = new SpawnEnvironmentProfile.MutableRange(0, 15);
      SpawnEnvironmentProfile.MutableRange var3 = new SpawnEnvironmentProfile.MutableRange(0, 15);
      SpawnEnvironmentProfile.MutableRange var4 = new SpawnEnvironmentProfile.MutableRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
      SpawnEnvironmentProfile.MutableRange var5 = new SpawnEnvironmentProfile.MutableRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
      SpawnEnvironmentProfile.MutableRange var6 = new SpawnEnvironmentProfile.MutableRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
      SpawnEnvironmentProfile.MutableRange var7 = new SpawnEnvironmentProfile.MutableRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
      SpawnEnvironmentProfile.MutableRange var8 = new SpawnEnvironmentProfile.MutableRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
      SpawnEnvironmentProfile.MutableRange var9 = new SpawnEnvironmentProfile.MutableRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
      Boolean var10 = null;
      Boolean var11 = null;
      Boolean var12 = null;
      Boolean var13 = null;
      Boolean var14 = null;
      Integer var15 = null;
      List<List<String>> var16 = new ArrayList<>();
      List<String> var17 = new ArrayList<>();

      for (SpawnCondition var19 : var1.conditions) {
         var2.apply(var19.minSkyLight, var19.maxSkyLight);
         var3.apply(var19.minLight, var19.maxLight);
         var4.apply(var19.minY, var19.maxY);
         var5.apply(var19.minWidth, var19.maxWidth);
         var6.apply(var19.minHeight, var19.maxHeight);
         var7.apply(var19.minDepth, var19.maxDepth);
         var8.apply(var19.minX, var19.maxX);
         var9.apply(var19.minZ, var19.maxZ);
         if (var19.canSeeSky != null) {
            var10 = mergeBoolean(var10, var19.canSeeSky);
         }

         if (var19.isRaining != null) {
            var11 = mergeBoolean(var11, var19.isRaining);
         }

         if (var19.isThundering != null) {
            var12 = mergeBoolean(var12, var19.isThundering);
         }

         if (var19.isSlimeChunk != null) {
            var13 = mergeBoolean(var13, var19.isSlimeChunk);
         }

         if (var19.fluidIsSource != null) {
            var14 = mergeBoolean(var14, var19.fluidIsSource);
         }

         if (var15 == null && var19.moonPhase != null) {
            var15 = var19.moonPhase;
         }

         if (!var19.fluid.isEmpty()) {
            var16.add(var19.fluid);
         }

         if (var19.timeRange != null && !var19.timeRange.isBlank()) {
            var17.add(var19.timeRange);
         }
      }

      this.skyLight = var2.toRange();
      this.light = var3.toRange();
      this.x = var8.toRange();
      this.y = var4.toRange();
      this.z = var9.toRange();
      this.width = var5.toRange();
      this.height = var6.toRange();
      this.depth = var7.toRange();
      this.canSeeSky = var10;
      this.isRaining = var11;
      this.isThundering = var12;
      this.isSlimeChunk = var13;
      this.fluidIsSource = var14;
      this.moonPhase = var15;
      this.fluidGroups = List.copyOf(var16);
      this.timeRanges = List.copyOf(var17);
   }

   public static SpawnEnvironmentProfile fromTarget(SpawnEntry var0) {
      return new SpawnEnvironmentProfile(var0);
   }

   public boolean isCompatible(SpawnEntry var1) {
      if (var1 == null) {
         return false;
      }

      if ("fishing".equals(var1.context)) {
         return false;
      }

      if (!Objects.equals(this.context, var1.context)) {
         return false;
      }

      for (SpawnCondition var3 : var1.conditions) {
         if (!this.compatible(var3)) {
            return false;
         }
      }

      for (SpawnCondition var5 : var1.antiConditions) {
         if (var5.hasNonBiomeConstraint() && this.definitelySatisfies(var5)) {
            return false;
         }
      }

      return true;
   }

   public boolean compatible(SpawnCondition var1) {
      if (var1 == null) {
         return true;
      } else if (this.canSeeSky != null && var1.canSeeSky != null && !this.canSeeSky.equals(var1.canSeeSky)) {
         return false;
      } else if (this.isRaining != null && var1.isRaining != null && !this.isRaining.equals(var1.isRaining)) {
         return false;
      } else if (this.isThundering != null && var1.isThundering != null && !this.isThundering.equals(var1.isThundering)) {
         return false;
      } else if (this.isSlimeChunk != null && var1.isSlimeChunk != null && !this.isSlimeChunk.equals(var1.isSlimeChunk)) {
         return false;
      } else if (this.moonPhase != null && var1.moonPhase != null && !this.moonPhase.equals(var1.moonPhase)) {
         return false;
      } else if (this.fluidIsSource != null && var1.fluidIsSource != null && !this.fluidIsSource.equals(var1.fluidIsSource)) {
         return false;
      } else if (!this.skyLight.overlaps(var1.minSkyLight, var1.maxSkyLight)) {
         return false;
      } else if (!this.light.overlaps(var1.minLight, var1.maxLight)) {
         return false;
      } else if (!this.x.overlaps(var1.minX, var1.maxX)) {
         return false;
      } else if (!this.y.overlaps(var1.minY, var1.maxY)) {
         return false;
      } else if (!this.z.overlaps(var1.minZ, var1.maxZ)) {
         return false;
      } else if (!this.width.overlaps(var1.minWidth, var1.maxWidth)) {
         return false;
      } else if (!this.height.overlaps(var1.minHeight, var1.maxHeight)) {
         return false;
      } else if (!this.depth.overlaps(var1.minDepth, var1.maxDepth)) {
         return false;
      } else {
         return !this.fluidGroups.isEmpty() && !var1.fluid.isEmpty() && !this.fluidMayOverlap(var1.fluid)
            ? false
            : this.timeRanges.isEmpty() || var1.timeRange == null || this.timeRangesMayOverlap(var1.timeRange);
      }
   }

   public boolean definitelySatisfies(SpawnCondition var1) {
      if (var1 == null) {
         return true;
      } else if (var1.hasBiomeConstraint()
         || !var1.dimensions.isEmpty()
         || !var1.structures.isEmpty()
         || !var1.neededBaseBlocks.isEmpty()
         || !var1.neededNearbyBlocks.isEmpty()
         || !var1.labels.isEmpty()
         || var1.labelMode != null
         || var1.hasFishingRequirement()
         || !var1.unknownKeys.isEmpty()) {
         return false;
      } else if (var1.canSeeSky != null && !Objects.equals(this.canSeeSky, var1.canSeeSky)) {
         return false;
      } else if (var1.isRaining != null && !Objects.equals(this.isRaining, var1.isRaining)) {
         return false;
      } else if (var1.isThundering != null && !Objects.equals(this.isThundering, var1.isThundering)) {
         return false;
      } else if (var1.isSlimeChunk != null && !Objects.equals(this.isSlimeChunk, var1.isSlimeChunk)) {
         return false;
      } else if (var1.moonPhase != null && !Objects.equals(this.moonPhase, var1.moonPhase)) {
         return false;
      } else if (var1.fluidIsSource != null && !Objects.equals(this.fluidIsSource, var1.fluidIsSource)) {
         return false;
      } else if (!this.skyLight.isWithin(var1.minSkyLight, var1.maxSkyLight)) {
         return false;
      } else if (!this.light.isWithin(var1.minLight, var1.maxLight)) {
         return false;
      } else if (!this.x.isWithin(var1.minX, var1.maxX)) {
         return false;
      } else if (!this.y.isWithin(var1.minY, var1.maxY)) {
         return false;
      } else if (!this.z.isWithin(var1.minZ, var1.maxZ)) {
         return false;
      } else if (!this.width.isWithin(var1.minWidth, var1.maxWidth)) {
         return false;
      } else if (!this.height.isWithin(var1.minHeight, var1.maxHeight)) {
         return false;
      } else if (!this.depth.isWithin(var1.minDepth, var1.maxDepth)) {
         return false;
      } else {
         return !var1.fluid.isEmpty() && !this.fluidMayOverlap(var1.fluid) ? false : var1.timeRange == null || this.timeRangesMayOverlap(var1.timeRange);
      }
   }

   public String summary() {
      List<String> var1 = this.targetEntry.requirementSummaryParts();
      return var1.isEmpty() ? "No extra conditions" : String.join(" • ", var1);
   }

   private boolean fluidMayOverlap(List<String> var1) {
      for (List<String> var3 : this.fluidGroups) {
         boolean var4 = false;

         for (String var6 : var3) {
            for (String var8 : var1) {
               if (selectorMayOverlap(var6, var8)) {
                  var4 = true;
               }
            }
         }

         if (!var4) {
            return false;
         }
      }

      return true;
   }

   private boolean timeRangesMayOverlap(String var1) {
      for (String var3 : this.timeRanges) {
         if (!simpleTimeOverlap(var3, var1)) {
            return false;
         }
      }

      return true;
   }

   private static boolean simpleTimeOverlap(String var0, String var1) {
      String var2 = var0.toLowerCase(Locale.ROOT).trim();
      String var3 = var1.toLowerCase(Locale.ROOT).trim();
      if (var2.equals(var3)) {
         return true;
      }

      Set var4 = Set.of("day", "morning", "dawn", "noon");
      Set var5 = Set.of("night", "midnight");
      return var4.contains(var2) && var5.contains(var3) ? false : !var5.contains(var2) || !var4.contains(var3);
   }

   private static boolean selectorMayOverlap(String var0, String var1) {
      if (var0 != null && var1 != null) {
         String var2 = var0.toLowerCase(Locale.ROOT);
         String var3 = var1.toLowerCase(Locale.ROOT);
         return var2.equals(var3) ? true : var2.startsWith("#") || var3.startsWith("#");
      } else {
         return true;
      }
   }

   private static Boolean mergeBoolean(Boolean var0, Boolean var1) {
      if (var0 == null) {
         return var1;
      } else {
         return var0.equals(var1) ? var0 : var0;
      }
   }

   private static final class MutableRange {
      int min;
      int max;

      MutableRange(int var1, int var2) {
         this.min = var1;
         this.max = var2;
      }

      void apply(Integer var1, Integer var2) {
         if (var1 != null) {
            this.min = Math.max(this.min, var1);
         }

         if (var2 != null) {
            this.max = Math.min(this.max, var2);
         }
      }

      SpawnEnvironmentProfile.Range toRange() {
         return new SpawnEnvironmentProfile.Range(this.min, this.max);
      }
   }

   private record Range(int min, int max) {
      boolean overlaps(Integer var1, Integer var2) {
         int var3 = var1 == null ? Integer.MIN_VALUE : var1;
         int var4 = var2 == null ? Integer.MAX_VALUE : var2;
         return this.max >= var3 && var4 >= this.min;
      }

      boolean isWithin(Integer var1, Integer var2) {
         int var3 = var1 == null ? Integer.MIN_VALUE : var1;
         int var4 = var2 == null ? Integer.MAX_VALUE : var2;
         return this.min >= var3 && this.max <= var4;
      }
   }
}
