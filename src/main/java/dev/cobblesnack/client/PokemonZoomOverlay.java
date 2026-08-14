package dev.cobblesnack.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PokemonZoomOverlay {
   private static final double MIN_ICON_SCALE = 1.0;
   private static final double MAX_ICON_SCALE = 5.0;
   private static final double ICON_STEP = 1.0;
   private static final double DEFAULT_ICON_SCALE = 1.0;
   private static final double MIN_BOX_SCALE = 0.1;
   private static final double MAX_BOX_SCALE = 5.0;
   private static final double BOX_STEP = 0.1;
   private static final double DEFAULT_BOX_SCALE = 1.0;
   private static final int BASE_ICON = 32;
   private static final int BASE_BOX = 48;
   private static final int CONTROL_WIDTH = 126;
   private static final int CONTROL_HEIGHT = 18;
   private static final int CONTROL_GAP = 6;
   private static final int TRACK_LEFT = 54;
   private static final int TRACK_RIGHT = 118;
   private static final Path ICON_SCALE_FILE = Path.of("config", "cobblesnack-pokemon-sprite-scale.txt");
   private static final Path BOX_SCALE_FILE = Path.of("config", "cobblesnack-pokemon-box-scale.txt");
   private static final Map<String, Field> SCREEN_FIELDS = new HashMap<>();
   private static Method fillMethod;
   private static Method drawStringMethod;
   private static Field textRendererField;
   private static double iconScale = loadScale(ICON_SCALE_FILE, 1.0, 1.0, 5.0, 1.0);
   private static double boxScale = loadScale(BOX_SCALE_FILE, 1.0, 0.1, 5.0, 0.1);
   private static PokemonZoomOverlay.DragTarget dragging = PokemonZoomOverlay.DragTarget.NONE;

   private PokemonZoomOverlay() {
   }

   public static int iconSize() {
      return Math.max(1, (int)Math.round(32.0 * iconScale));
   }

   public static int boxSize() {
      return Math.max(5, (int)Math.round(48.0 * boxScale));
   }

   public static boolean mouseClicked(Object var0, double var1, double var3, int var5) {
      if (var0 == null) {
         return false;
      }

      int[][] var6 = controlBoxes(var0);
      if (var6 == null) {
         return false;
      }

      if (var5 == 1) {
         if (inside(var6[0], var1, var3)) {
            boxScale = 1.0;
            saveScale(BOX_SCALE_FILE, boxScale);
            invokeVoid(var0, "onPokemonBoxScaleChanged");
            return true;
         } else if (inside(var6[1], var1, var3)) {
            iconScale = 1.0;
            saveScale(ICON_SCALE_FILE, iconScale);
            return true;
         } else {
            return false;
         }
      } else {
         if (var5 != 0) {
            return false;
         }

         if (inside(var6[0], var1, var3)) {
            dragging = PokemonZoomOverlay.DragTarget.BOX;
         } else {
            if (!inside(var6[1], var1, var3)) {
               return false;
            }

            dragging = PokemonZoomOverlay.DragTarget.ICON;
         }

         updateFromMouse(var0, var1);
         return true;
      }
   }

   public static boolean mouseScrolled(Object var0, double var1, double var3, double var5, double var7) {
      if (var0 != null && var7 != 0.0) {
         int[][] var11 = controlBoxes(var0);
         if (var11 == null) {
            return false;
         }

         double var9 = var7 > 0.0 ? 1.0 : -1.0;
         if (inside(var11[0], var1, var3)) {
            double var14 = snap(boxScale + var9 * 0.1, 0.1, 5.0, 0.1);
            if (Double.compare(var14, boxScale) != 0) {
               boxScale = var14;
               saveScale(BOX_SCALE_FILE, boxScale);
               invokeVoid(var0, "onPokemonBoxScaleChanged");
            }

            return true;
         } else if (inside(var11[1], var1, var3)) {
            iconScale = snap(iconScale + var9 * 1.0, 1.0, 5.0, 1.0);
            saveScale(ICON_SCALE_FILE, iconScale);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean mouseDragged(Object var0, double var1, double var3, int var5, double var6, double var8) {
      if (var5 == 0 && dragging != PokemonZoomOverlay.DragTarget.NONE && var0 != null) {
         updateFromMouse(var0, var1);
         return true;
      } else {
         return false;
      }
   }

   public static boolean mouseReleased(Object var0, double var1, double var3, int var5) {
      if (var5 == 0 && dragging != PokemonZoomOverlay.DragTarget.NONE) {
         saveScale(ICON_SCALE_FILE, iconScale);
         saveScale(BOX_SCALE_FILE, boxScale);
         dragging = PokemonZoomOverlay.DragTarget.NONE;
         return true;
      } else {
         return false;
      }
   }

   public static void render(Object var0, Object var1, int var2, int var3) {
      if (var0 != null && var1 != null) {
         try {
            int[][] var4 = controlBoxes(var0);
            if (var4 == null) {
               return;
            }

            drawControl(
               var0, var1, var4[0], "Box: " + formatScale(boxScale) + "x", boxScale, 0.1, 5.0, 6, dragging == PokemonZoomOverlay.DragTarget.BOX, var2, var3
            );
            drawControl(
               var0, var1, var4[1], "Icon: " + formatScale(iconScale) + "x", iconScale, 1.0, 5.0, 5, dragging == PokemonZoomOverlay.DragTarget.ICON, var2, var3
            );
         } catch (Throwable var5) {
         }
      }
   }

   private static void drawControl(
      Object var0, Object var1, int[] var2, String var3, double var4, double var6, double var8, int var10, boolean var11, int var12, int var13
   ) throws ReflectiveOperationException {
      int var15 = var2[0];
      int var16 = var2[1];
      boolean var17 = inside(var2, var12, var13);
      fill(var1, var15, var16, var15 + 126, var16 + 18, !var17 && !var11 ? -401141223 : -265674192);
      fill(var1, var15, var16, var15 + 126, var16 + 1, -4737097);
      fill(var1, var15, var16 + 18 - 1, var15 + 126, var16 + 18, -4737097);
      fill(var1, var15, var16, var15 + 1, var16 + 18, -4737097);
      fill(var1, var15 + 126 - 1, var16, var15 + 126, var16 + 18, -4737097);
      drawString(var0, var1, var3, var15 + 5, var16 + 5, 16777215);
      int var18 = var15 + 54;
      int var19 = var15 + 118;
      int var20 = var16 + 9;
      fill(var1, var18, var20 - 1, var19 + 1, var20 + 1, -10066330);

      for (int var21 = 0; var21 < var10; var21++) {
         double var22 = var10 <= 1 ? 0.0 : (double)var21 / (var10 - 1);
         int var14 = var18 + (int)Math.round((var19 - var18) * var22);
         fill(var1, var14, var20 - 3, var14 + 1, var20 + 4, -7368817);
      }

      double var25 = (var4 - var6) / (var8 - var6);
      int var23 = var18 + (int)Math.round((var19 - var18) * var25);
      int var24 = var11 ? -137 : -10163;
      fill(var1, var18, var20 - 1, var23 + 1, var20 + 1, var24);
      fill(var1, var23 - 2, var20 - 5, var23 + 3, var20 + 6, var24);
   }

   private static void updateFromMouse(Object var0, double var1) {
      int[][] var3 = controlBoxes(var0);
      if (var3 != null && dragging != PokemonZoomOverlay.DragTarget.NONE) {
         int[] var4 = dragging == PokemonZoomOverlay.DragTarget.BOX ? var3[0] : var3[1];
         int var5 = var4[0] + 54;
         int var6 = var4[0] + 118;
         double var7 = Math.max(0.0, Math.min(1.0, (var1 - var5) / Math.max(1.0, var6 - var5)));
         if (dragging == PokemonZoomOverlay.DragTarget.BOX) {
            double var9 = snap(0.1 + var7 * 4.9, 0.1, 5.0, 0.1);
            if (Double.compare(var9, boxScale) != 0) {
               boxScale = var9;
               invokeVoid(var0, "onPokemonBoxScaleChanged");
            }
         } else {
            iconScale = snap(1.0 + var7 * 4.0, 1.0, 5.0, 1.0);
         }
      }
   }

   private static int[][] controlBoxes(Object var0) {
      try {
         int var1 = screenField(var0, "pokemonGridX").getInt(var0);
         int var2 = screenField(var0, "pokemonGridWidth").getInt(var0);
         int var3 = screenField(var0, "pokemonGridBottom").getInt(var0);
         int var4 = invokeInt(var0, "currentVisiblePokemonContentBottom");
         int var5 = Math.min(var3, var4) + 5;
         int var6 = var1;
         int var7 = var1 + Math.max(0, var2 - 126);
         if (var2 < 258) {
            var7 = var6 + 126 + 6;
         }

         return new int[][]{{var6, var5, 126, 18}, {var7, var5, 126, 18}};
      } catch (Throwable var8) {
         return null;
      }
   }

   private static void invokeVoid(Object var0, String var1) {
      try {
         findDeclaredMethod(var0.getClass(), var1).invoke(var0);
      } catch (Throwable var3) {
      }
   }

   private static int invokeInt(Object var0, String var1) throws ReflectiveOperationException {
      return ((Number)findDeclaredMethod(var0.getClass(), var1).invoke(var0)).intValue();
   }

   private static Method findDeclaredMethod(Class<?> var0, String var1) throws NoSuchMethodException {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         try {
            Method var3 = var2.getDeclaredMethod(var1);
            var3.setAccessible(true);
            return var3;
         } catch (NoSuchMethodException var4) {
         }
      }

      throw new NoSuchMethodException(var1);
   }

   private static boolean inside(int[] var0, double var1, double var3) {
      return var1 >= var0[0] && var1 < var0[0] + var0[2] && var3 >= var0[1] && var3 < var0[1] + var0[3];
   }

   private static Field screenField(Object var0, String var1) throws ReflectiveOperationException {
      Field var2 = SCREEN_FIELDS.get(var1);
      if (var2 != null) {
         return var2;
      }

      for (Class var3 = var0.getClass(); var3 != null; var3 = var3.getSuperclass()) {
         try {
            Field var4 = var3.getDeclaredField(var1);
            var4.setAccessible(true);
            SCREEN_FIELDS.put(var1, var4);
            return var4;
         } catch (NoSuchFieldException var5) {
         }
      }

      throw new NoSuchFieldException(var1);
   }

   private static void fill(Object var0, int var1, int var2, int var3, int var4, int var5) throws ReflectiveOperationException {
      if (fillMethod == null) {
         fillMethod = findMethod(var0.getClass(), new String[]{"method_25294", "fill"}, 5);
      }

      fillMethod.invoke(var0, var1, var2, var3, var4, var5);
   }

   private static void drawString(Object var0, Object var1, String var2, int var3, int var4, int var5) throws ReflectiveOperationException {
      if (textRendererField == null) {
         textRendererField = findField(var0.getClass(), new String[]{"field_22793", "textRenderer"});
      }

      Object var6 = textRendererField.get(var0);
      if (drawStringMethod == null) {
         drawStringMethod = findStringDrawMethod(var1.getClass(), var6.getClass());
      }

      drawStringMethod.invoke(var1, var6, var2, var3, var4, var5);
   }

   private static Method findMethod(Class<?> var0, String[] var1, int var2) throws NoSuchMethodException {
      for (String var6 : var1) {
         for (Method var10 : var0.getMethods()) {
            if (var10.getName().equals(var6) && var10.getParameterCount() == var2) {
               boolean var11 = true;

               for (Class var15 : var10.getParameterTypes()) {
                  if (var15 != int.class) {
                     var11 = false;
                     break;
                  }
               }

               if (var11) {
                  var10.setAccessible(true);
                  return var10;
               }
            }
         }
      }

      throw new NoSuchMethodException(String.join("/", var1));
   }

   private static Method findStringDrawMethod(Class<?> var0, Class<?> var1) throws NoSuchMethodException {
      for (Method var5 : var0.getMethods()) {
         String var6 = var5.getName();
         Class[] var7 = var5.getParameterTypes();
         if ((var6.equals("method_25303") || var6.equals("drawTextWithShadow") || var6.equals("drawText"))
            && var7.length == 5
            && var7[0].isAssignableFrom(var1)
            && var7[1] == String.class
            && var7[2] == int.class
            && var7[3] == int.class
            && var7[4] == int.class) {
            var5.setAccessible(true);
            return var5;
         }
      }

      throw new NoSuchMethodException("string draw");
   }

   private static Field findField(Class<?> var0, String[] var1) throws NoSuchFieldException {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         for (String var6 : var1) {
            try {
               Field var7 = var2.getDeclaredField(var6);
               var7.setAccessible(true);
               return var7;
            } catch (NoSuchFieldException var8) {
            }
         }
      }

      throw new NoSuchFieldException(String.join("/", var1));
   }

   private static double loadScale(Path var0, double var1, double var3, double var5, double var7) {
      try {
         return !Files.exists(var0) ? var1 : snap(Double.parseDouble(Files.readString(var0, StandardCharsets.UTF_8).trim()), var3, var5, var7);
      } catch (Throwable var10) {
         return var1;
      }
   }

   private static void saveScale(Path var0, double var1) {
      try {
         Path var3 = var0.getParent();
         if (var3 != null) {
            Files.createDirectories(var3);
         }

         Files.writeString(var0, formatScale(var1), StandardCharsets.UTF_8);
      } catch (Throwable var4) {
      }
   }

   private static double snap(double var0, double var2, double var4, double var6) {
      double var8 = Math.max(var2, Math.min(var4, var0));
      double var10 = var2 + Math.round((var8 - var2) / var6) * var6;
      return Math.max(var2, Math.min(var4, Math.round(var10 * 10.0) / 10.0));
   }

   private static String formatScale(double var0) {
      return String.format(Locale.ROOT, "%.1f", var0);
   }

   private enum DragTarget {
      NONE,
      BOX,
      ICON;
   }
}
