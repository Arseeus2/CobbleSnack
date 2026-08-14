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
   private final SpawnEnvironmentProfile.Range y;
   private final SpawnEnvironmentProfile.Range width;
   private final SpawnEnvironmentProfile.Range height;
   private final SpawnEnvironmentProfile.Range depth;
   private final Boolean canSeeSky;
   private final Boolean isRaining;
   private final Boolean isThundering;
   private final Boolean fluidIsSource;
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
      Boolean var8 = null;
      Boolean var9 = null;
      Boolean var10 = null;
      Boolean var11 = null;
      List<List<String>> var12 = new ArrayList<>();
      List<String> var13 = new ArrayList<>();

      for (SpawnCondition var15 : var1.conditions) {
         var2.apply(var15.minSkyLight, var15.maxSkyLight);
         var3.apply(var15.minLight, var15.maxLight);
         var4.apply(var15.minY, var15.maxY);
         var5.apply(var15.minWidth, var15.maxWidth);
         var6.apply(var15.minHeight, var15.maxHeight);
         var7.apply(var15.minDepth, var15.maxDepth);
         if (var15.canSeeSky != null) {
            var8 = mergeBoolean(var8, var15.canSeeSky);
         }

         if (var15.isRaining != null) {
            var9 = mergeBoolean(var9, var15.isRaining);
         }

         if (var15.isThundering != null) {
            var10 = mergeBoolean(var10, var15.isThundering);
         }

         if (var15.fluidIsSource != null) {
            var11 = mergeBoolean(var11, var15.fluidIsSource);
         }

         if (!var15.fluid.isEmpty()) {
            var12.add(var15.fluid);
         }

         if (var15.timeRange != null && !var15.timeRange.isBlank()) {
            var13.add(var15.timeRange);
         }
      }

      this.skyLight = var2.toRange();
      this.light = var3.toRange();
      this.y = var4.toRange();
      this.width = var5.toRange();
      this.height = var6.toRange();
      this.depth = var7.toRange();
      this.canSeeSky = var8;
      this.isRaining = var9;
      this.isThundering = var10;
      this.fluidIsSource = var11;
      this.fluidGroups = List.copyOf(var12);
      this.timeRanges = List.copyOf(var13);
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
      } else if (this.fluidIsSource != null && var1.fluidIsSource != null && !this.fluidIsSource.equals(var1.fluidIsSource)) {
         return false;
      } else if (!this.skyLight.overlaps(var1.minSkyLight, var1.maxSkyLight)) {
         return false;
      } else if (!this.light.overlaps(var1.minLight, var1.maxLight)) {
         return false;
      } else if (!this.y.overlaps(var1.minY, var1.maxY)) {
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
         || !var1.unknownKeys.isEmpty()) {
         return false;
      } else if (var1.canSeeSky != null && !Objects.equals(this.canSeeSky, var1.canSeeSky)) {
         return false;
      } else if (var1.isRaining != null && !Objects.equals(this.isRaining, var1.isRaining)) {
         return false;
      } else if (var1.isThundering != null && !Objects.equals(this.isThundering, var1.isThundering)) {
         return false;
      } else if (var1.fluidIsSource != null && !Objects.equals(this.fluidIsSource, var1.fluidIsSource)) {
         return false;
      } else if (!this.skyLight.isWithin(var1.minSkyLight, var1.maxSkyLight)) {
         return false;
      } else if (!this.light.isWithin(var1.minLight, var1.maxLight)) {
         return false;
      } else if (!this.y.isWithin(var1.minY, var1.maxY)) {
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
