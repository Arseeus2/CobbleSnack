package dev.cobblesnack.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SeasoningUiHelper {
   private static final int GOLD = -10934;
   private static final int GOLD_SOFT = -1326005437;

   private SeasoningUiHelper() {
   }

   public static void afterPickerRender(Object var0, Object var1, int var2, int var3) {
      try {
         List var4 = listField(var0, "rowButtons");
         List var5 = listField(var0, "filtered");
         int var6 = intField(var0, "scrollOffset");
         int var7 = intField(var0, "rowButtonWidth");
         Object var8 = field(var0, "selected");
         Object var9 = field(var0, "parent");
         Set var10 = selectedSeasonings(var9);
         Object var11 = null;

         for (int var12 = 0; var12 < var4.size(); var12++) {
            int var13 = var6 + var12;
            if (var13 >= 0 && var13 < var5.size()) {
               Object var14 = var5.get(var13);
               Object var15 = var4.get(var12);
               int var16 = widgetX(var15);
               int var17 = widgetY(var15);
               int var18 = var16 - 20;
               if (var10.contains(var14) && !isNone(var14)) {
                  boolean var19 = Objects.equals(var14, var8);
                  int var20 = var19 ? pulsingGoldAlpha() : -10934;
                  drawBorder(var1, var18 - 2, var17 - 2, var7 + 23, 20, var20);
                  fill(var1, var18 - 2, var17, var18, var17 + 16, var19 ? var20 : -1326005437);
                  if (var19) {
                     fill(var1, var18 - 10, var17 + 7, var18 - 4, var17 + 9, -10934);
                     fill(var1, var18 - 5, var17 + 5, var18 - 4, var17 + 11, -10934);
                     fill(var1, var18 - 4, var17 + 6, var18 - 3, var17 + 10, -10934);
                     fill(var1, var18 - 3, var17 + 7, var18 - 2, var17 + 9, -10934);
                  }
               }

               if (var2 >= var18 && var2 < var18 + 16 && var3 >= var17 && var3 < var17 + 16) {
                  var11 = seasoningStack(var0, var14);
               }
            }
         }

         if (var11 != null && !isEmptyStack(var11)) {
            OverlayZ.push(var1);

            try {
               drawItemTooltip(var0, var1, var11, var2, var3);
            } finally {
               OverlayZ.pop(var1);
            }
         }
      } catch (Throwable var25) {
      }
   }

   public static void afterCalculatorRender(Object var0, Object var1, int var2, int var3) {
      try {
         if (booleanFieldOrFalse(var0, "pokemonSortMenuOpen") || booleanFieldOrFalse(var0, "pokemonFormDropdownOpen")) {
            return;
         }

         if (!(field(var0, "selected") instanceof Object[] var5)) {
            return;
         }

         int var6 = intField(var0, "rightPanelX");
         int var7 = intField(var0, "uiTop");
         int var8 = var6 + 6;
         int var9 = var7 + 36;

         for (int var10 = 0; var10 < Math.min(3, var5.length); var10++) {
            int var12 = var9 + var10 * 28;
            boolean var13 = var2 >= var8 && var2 < var8 + 20 && var3 >= var12 && var3 < var12 + 20;
            if (var13) {
               Object var11 = seasoningStack(var0, var5[var10]);
               if (var11 != null && !isEmptyStack(var11)) {
                  OverlayZ.push(var1);

                  try {
                     drawItemTooltip(var0, var1, var11, var2, var3);
                  } finally {
                     OverlayZ.pop(var1);
                  }
               }

               return;
            }
         }
      } catch (Throwable var18) {
      }
   }

   public static boolean afterMouseClicked(boolean var0, Object var1, int var2) {
      if (var0 && var2 == 0 && var1 != null) {
         try {
            clearButtonFocus(var1);
         } catch (Throwable var4) {
         }

         return var0;
      } else {
         return var0;
      }
   }

   private static void clearButtonFocus(Object var0) throws Exception {
      Set var1 = Collections.newSetFromMap(new IdentityHashMap());

      for (Class var2 = var0.getClass(); var2 != null; var2 = var2.getSuperclass()) {
         for (Field var6 : var2.getDeclaredFields()) {
            var6.setAccessible(true);

            Object var7;
            try {
               var7 = var6.get(var0);
            } catch (Throwable var9) {
               continue;
            }

            clearButtonValue(var7, var1);
         }
      }
   }

   private static void clearButtonValue(Object var0, Set<Object> var1) {
      if (var0 != null && !var1.contains(var0)) {
         var1.add(var0);
         if (isButtonWidget(var0)) {
            try {
               findMethod(var0.getClass(), new String[]{"method_25365", "setFocused"}, 1).invoke(var0, false);
            } catch (Throwable var7) {
            }
         } else {
            if (var0 instanceof Iterable) {
               Object[] var2 = (Object[])var0;

               for (Object var6 : var2) {
                  clearButtonValue(var6, var1);
               }
            }

            if (var0 instanceof Object[]) {
               Object[] var8;
               for (Object var12 : var8 = (Object[])var0) {
                  clearButtonValue(var12, var1);
               }
            }
         }
      }
   }

   private static boolean isButtonWidget(Object var0) {
      if (var0 == null) {
         return false;
      }

      for (Class var1 = var0.getClass(); var1 != null; var1 = var1.getSuperclass()) {
         String var2 = var1.getName();
         if (var2.equals("net.minecraft.class_4185") || var2.equals("net.minecraft.client.gui.widget.ButtonWidget")) {
            return true;
         }
      }

      return false;
   }

   public static boolean handlePickerKey(Object var0, int var1, int var2, int var3) {
      try {
         if (var1 == 256 || matchesInventoryKey(var1, var2) || matchesCobbleSnackKey(var1, var2)) {
            goParent(var0);
            return true;
         }
      } catch (Throwable var5) {
      }

      return false;
   }

   private static Set<Object> selectedSeasonings(Object var0) {
      Set var1 = Collections.newSetFromMap(new IdentityHashMap());

      try {
         if (field(var0, "selected") instanceof Object[] var3) {
            Collections.addAll(var1, var3);
         }
      } catch (Throwable var4) {
      }

      return var1;
   }

   private static boolean matchesCobbleSnackKey(int var0, int var1) {
      try {
         Class var2 = Class.forName("dev.cobblesnack.client.CobbleSnackClient");
         Method var3 = var2.getMethod("matchesOpenCalculatorKey", int.class, int.class);
         return Boolean.TRUE.equals(var3.invoke(null, var0, var1));
      } catch (Throwable var4) {
         return false;
      }
   }

   private static boolean matchesInventoryKey(int var0, int var1) {
      try {
         Object var5 = minecraftClient();
         if (var5 == null) {
            return false;
         }

         Object var6 = null;

         for (Class var4 = var5.getClass(); var4 != null && var6 == null; var4 = var4.getSuperclass()) {
            for (Field var10 : var4.getDeclaredFields()) {
               var10.setAccessible(true);

               Object var3;
               try {
                  var3 = var10.get(var5);
               } catch (Throwable var15) {
                  continue;
               }

               String var2;
               if (var3 != null
                  && ((var2 = var3.getClass().getName()).equals("net.minecraft.class_315") || var2.equals("net.minecraft.client.option.GameOptions"))) {
                  var6 = var3;
                  break;
               }
            }
         }

         if (var6 == null) {
            return false;
         }

         for (Class var19 = var6.getClass(); var19 != null; var19 = var19.getSuperclass()) {
            for (Field var23 : var19.getDeclaredFields()) {
               var23.setAccessible(true);

               Object var18;
               try {
                  var18 = var23.get(var6);
               } catch (Throwable var14) {
                  continue;
               }

               String var17;
               if (var18 != null
                  && (
                     (var17 = var18.getClass().getName()).equals("net.minecraft.class_304")
                        || var17.equals("net.minecraft.client.option.KeyBinding")
                        || isSubclassNamed(var18.getClass(), "net.minecraft.class_304", "net.minecraft.client.option.KeyBinding")
                  )
                  && "key.inventory".equals(String.valueOf(findMethod(var18.getClass(), new String[]{"method_1431", "getTranslationKey"}, 0).invoke(var18)))) {
                  Method var13 = findMethod(var18.getClass(), new String[]{"method_1417", "matchesKey"}, 2);
                  return Boolean.TRUE.equals(var13.invoke(var18, var0, var1));
               }
            }
         }
      } catch (Throwable var16) {
      }

      return false;
   }

   private static boolean isSubclassNamed(Class<?> var0, String... var1) {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         for (String var6 : var1) {
            if (var2.getName().equals(var6)) {
               return true;
            }
         }
      }

      return false;
   }

   private static void goParent(Object var0) throws Exception {
      Object var1 = field(var0, "parent");
      Object var2 = minecraftClient();
      if (var2 != null) {
         Method var3 = findMethod(var2.getClass(), new String[]{"method_1507", "setScreen"}, 1);
         var3.invoke(var2, var1);
      }
   }

   private static Object minecraftClient() {
      try {
         Class var0 = Class.forName("net.minecraft.class_310");
         Method var4 = findMethod(var0, new String[]{"method_1551", "getInstance"}, 0);
         return var4.invoke(null);
      } catch (Throwable var3) {
         try {
            Class var1 = Class.forName("net.minecraft.client.MinecraftClient");
            return var1.getMethod("getInstance").invoke(null);
         } catch (Throwable var2) {
            return null;
         }
      }
   }

   private static Object seasoningStack(Object var0, Object var1) throws Exception {
      for (Class var2 = var0.getClass(); var2 != null; var2 = var2.getSuperclass()) {
         for (Method var6 : var2.getDeclaredMethods()) {
            if (var6.getName().equals("seasoningStack") && var6.getParameterCount() == 1) {
               var6.setAccessible(true);
               return var6.invoke(var0, var1);
            }
         }
      }

      return null;
   }

   private static boolean isNone(Object var0) {
      return var0 == null || "NONE".equals(String.valueOf(var0));
   }

   private static boolean isEmptyStack(Object var0) {
      if (var0 == null) {
         return true;
      }

      try {
         Method var1 = findMethod(var0.getClass(), new String[]{"method_7960", "isEmpty"}, 0);
         return Boolean.TRUE.equals(var1.invoke(var0));
      } catch (Throwable var2) {
         return true;
      }
   }

   private static void drawItemTooltip(Object var0, Object var1, Object var2, int var3, int var4) throws Exception {
      Object var5 = fieldAny(var0, "field_22793", "textRenderer");
      Method var6 = null;

      for (Method var10 : var1.getClass().getMethods()) {
         if ((var10.getName().equals("method_51446") || var10.getName().equals("drawItemTooltip")) && var10.getParameterCount() == 4) {
            var6 = var10;
            break;
         }
      }

      if (var6 == null) {
         throw new NoSuchMethodException("DrawContext.drawItemTooltip");
      }

      var6.invoke(var1, var5, var2, var3, var4);
   }

   private static int pulsingGoldAlpha() {
      double var0 = System.currentTimeMillis() % 1650L / 1650.0 * Math.PI * 2.0;
      double var2 = (Math.sin(var0) + 1.0) * 0.5;
      int var4 = lerp(0, 255, var2);
      return withAlpha(-15315, var4);
   }

   private static int lerp(int var0, int var1, double var2) {
      return (int)Math.round(var0 + (var1 - var0) * var2);
   }

   private static int withAlpha(int var0, int var1) {
      return (var1 & 0xFF) << 24 | var0 & 16777215;
   }

   private static void drawBorder(Object var0, int var1, int var2, int var3, int var4, int var5) throws Exception {
      fill(var0, var1, var2, var1 + var3, var2 + 1, var5);
      fill(var0, var1, var2 + var4 - 1, var1 + var3, var2 + var4, var5);
      fill(var0, var1, var2, var1 + 1, var2 + var4, var5);
      fill(var0, var1 + var3 - 1, var2, var1 + var3, var2 + var4, var5);
   }

   private static void fill(Object var0, int var1, int var2, int var3, int var4, int var5) throws Exception {
      Method var6 = findMethod(var0.getClass(), new String[]{"method_25294", "fill"}, 5);
      var6.invoke(var0, var1, var2, var3, var4, var5);
   }

   private static int widgetX(Object var0) throws Exception {
      return ((Number)findMethod(var0.getClass(), new String[]{"method_46426", "getX"}, 0).invoke(var0)).intValue();
   }

   private static int widgetY(Object var0) throws Exception {
      return ((Number)findMethod(var0.getClass(), new String[]{"method_46427", "getY"}, 0).invoke(var0)).intValue();
   }

   private static int widgetWidth(Object var0) throws Exception {
      return ((Number)findMethod(var0.getClass(), new String[]{"method_25368", "getWidth"}, 0).invoke(var0)).intValue();
   }

   private static int widgetHeight(Object var0) throws Exception {
      return ((Number)findMethod(var0.getClass(), new String[]{"method_25364", "getHeight"}, 0).invoke(var0)).intValue();
   }

   private static List<?> listField(Object var0, String var1) throws Exception {
      return (List<?>)field(var0, var1);
   }

   private static int intField(Object var0, String var1) throws Exception {
      return ((Number)field(var0, var1)).intValue();
   }

   private static boolean booleanField(Object var0, String var1) throws Exception {
      return Boolean.TRUE.equals(field(var0, var1));
   }

   private static boolean booleanFieldOrFalse(Object var0, String var1) {
      try {
         return booleanField(var0, var1);
      } catch (Throwable var3) {
         return false;
      }
   }

   private static Object field(Object var0, String var1) throws Exception {
      return fieldAny(var0, var1);
   }

   private static Object fieldAny(Object var0, String... var1) throws Exception {
      if (var0 == null) {
         throw new NullPointerException("object");
      }

      for (Class var2 = var0.getClass(); var2 != null; var2 = var2.getSuperclass()) {
         for (String var6 : var1) {
            try {
               Field var7 = var2.getDeclaredField(var6);
               var7.setAccessible(true);
               return var7.get(var0);
            } catch (NoSuchFieldException var8) {
            }
         }
      }

      throw new NoSuchFieldException(Arrays.toString(var1));
   }

   private static Method findMethod(Class<?> var0, String[] var1, int var2) throws Exception {
      for (Class var3 = var0; var3 != null; var3 = var3.getSuperclass()) {
         for (Method var7 : var3.getDeclaredMethods()) {
            if (var7.getParameterCount() == var2) {
               for (String var11 : var1) {
                  if (var7.getName().equals(var11)) {
                     var7.setAccessible(true);
                     return var7;
                  }
               }
            }
         }
      }

      throw new NoSuchMethodException(Arrays.toString(var1));
   }
}
