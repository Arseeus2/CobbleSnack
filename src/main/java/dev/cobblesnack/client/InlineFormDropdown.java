package dev.cobblesnack.client;

import dev.cobblesnack.data.SpeciesInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class InlineFormDropdown {
   private static final int ROW_H = 24;
   private static final int ICON_SIZE = 32;
   private static final int MAX_ROWS = 8;
   private static final Map<Object, InlineFormDropdown.State> STATES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

   private InlineFormDropdown() {
   }

   private static InlineFormDropdown.State state(Object var0) {
      return STATES.computeIfAbsent(var0, var0x -> new InlineFormDropdown.State());
   }

   public static void toggle(Object var0) {
      try {
         ensureSelectedForm(var0);
         List var1 = options(var0);
         InlineFormDropdown.State var2 = state(var0);
         if (var1.size() <= 1) {
            var2.open = false;
            return;
         }

         setField(var0, "pokemonSortMenuOpen", false);
         boolean var3 = var2.open = !var2.open;
         if (var2.open) {
            scrollSelectedIntoView(var0, var2, var1);
         }
      } catch (Throwable var4) {
      }
   }

   public static void ensureSelectedForm(Object var0) {
      try {
         Object var1 = getField(var0, "selectedPokemon");
         if (var1 == null) {
            setField(var0, "selectedSpawnForm", null);
            return;
         }

         List var2 = options(var0);
         if (var2.isEmpty()) {
            setField(var0, "selectedSpawnForm", null);
            return;
         }

         String var3 = (String)getField(var0, "selectedSpawnForm");
         boolean var4 = false;
         if (var3 != null && !var3.isBlank()) {
            for (Object var6 : var2) {
               if (formKeysMatch(var0, optionKey(var6), var3)) {
                  var4 = true;
                  break;
               }
            }
         }

         if (!var4) {
            setField(var0, "selectedSpawnForm", defaultKey(var2));
         }
      } catch (Throwable var7) {
      }
   }

   public static void updateButtonState(Object var0) {
      try {
         Object var1 = getField(var0, "pokemonButton");
         if (var1 == null) {
            return;
         }

         Object var2 = getField(var0, "selectedPokemon");
         boolean var3 = var2 != null && options(var0).size() > 1;
         setField(var1, "field_22763", var3);
      } catch (Throwable var4) {
      }
   }

   public static boolean mouseScrolled(Object var0, double var1, double var3, double var5, double var7) {
      InlineFormDropdown.State var9 = state(var0);
      if (!var9.open) {
         return false;
      }

      try {
         InlineFormDropdown.Geometry var10 = geometry(var0);
         if (var10 != null && inside(var10, var1, var3) && var7 != 0.0) {
            int var11 = maxScroll(var0);
            var9.scroll = clamp(var9.scroll + (var7 < 0.0 ? 1 : -1), 0, var11);
         }
      } catch (Throwable var12) {
      }

      return true;
   }

   public static boolean mouseClicked(Object var0, double var1, double var3, int var5) {
      InlineFormDropdown.State var6 = state(var0);
      if (var6.open && var5 == 0) {
         try {
            InlineFormDropdown.Geometry var7 = geometry(var0);
            Object var8 = getField(var0, "pokemonButton");
            if (var8 != null && insideButton(var8, var1, var3)) {
               var6.open = false;
               return true;
            }

            if (var7 != null && inside(var7, var1, var3)) {
               int var9 = (int)((var3 - var7.y) / 24.0);
               int var10 = var6.scroll + var9;
               List var11 = options(var0);
               if (var10 >= 0 && var10 < var11.size()) {
                  select(var0, optionKey(var11.get(var10)));
                  var6.open = false;
                  return true;
               }
            }

            var6.open = false;
            return true;
         } catch (Throwable var12) {
            var6.open = false;
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean keyPressed(Object var0, int var1) {
      InlineFormDropdown.State var2 = state(var0);
      if (var2.open && var1 == 256) {
         var2.open = false;
         return true;
      } else {
         return false;
      }
   }

   public static void render(Object var0, Object var1, int var2, int var3) {
      OverlayZ.push(var1);
      InlineFormDropdown.State var4 = state(var0);

      try {
         if (!var4.open) {
            return;
         }

         ensureSelectedForm(var0);
         List var5 = options(var0);
         InlineFormDropdown.Geometry var6 = geometry(var0);
         if (var6 != null && var5.size() > 1) {
            int var9 = Math.max(0, var5.size() - 8);
            var4.scroll = clamp(var4.scroll, 0, var9);
            fill(var1, var6.x - 2, var6.y - 2, var6.x + var6.w + 2, var6.y + var6.h() + 2, -15265255);
            String var10 = (String)getField(var0, "selectedSpawnForm");
            Object var11 = getField(var0, "field_22793");
            Object var12 = getField(var0, "selectedPokemon");

            int var7;
            for (int var8 = 0; var8 < var6.rows && (var7 = var4.scroll + var8) < var5.size(); var8++) {
               Object var13 = var5.get(var7);
               String var14 = optionKey(var13);
               String var15 = optionLabel(var13);
               int var16 = var6.y + var8 * 24;
               boolean var17 = var2 >= var6.x && var2 < var6.x + var6.w && var3 >= var16 && var3 < var16 + 24;
               boolean var18 = formKeysMatch(var0, var14, var10);
               int var19 = var17 ? -8947849 : (var18 ? -9936825 : -11250604);
               fill(var1, var6.x, var16, var6.x + var6.w, var16 + 24, var19);
               fill(var1, var6.x, var16 + 24 - 1, var6.x + var6.w, var16 + 24, 1883258944);
               if (var18) {
                  drawText(var1, var11, "✓", var6.x + 4, var16 + 8, 16777045);
               }

               if (var12 instanceof SpeciesInfo var20) {
                  MinimapSpriteResolver.SpriteRef var21 = MinimapSpriteResolver.spriteForSpawnForm(var20, var14, var15);
                  if (var21 != null) {
                     MinimapSpriteResolver.useNearest(var21);
                     drawTexture(var1, var21.texture(), var6.x + 14, var16 - 4, var21.textureWidth(), var21.textureHeight());
                  }
               }

               String var31 = trimToWidth(var11, var15, Math.max(20, var6.w - 60));
               drawText(var1, var11, var31, var6.x + 48, var16 + 8, var18 ? 16777045 : 16777215);
            }

            if (var9 > 0) {
               var7 = var6.x + var6.w - 4;
               int var28 = Math.max(1, var5.size());
               int var29 = Math.max(24, (int)Math.round(var6.h() * ((double)var6.rows / var28)));
               int var30 = var6.y + (int)Math.round((var6.h() - var29) * ((double)var4.scroll / var9));
               fill(var1, var7, var6.y, var7 + 3, var6.y + var6.h(), -13619152);
               fill(var1, var7, var30, var7 + 3, var30 + var29, -3618616);
            }

            return;
         }

         var4.open = false;
      } catch (Throwable var25) {
         var4.open = false;
         return;
      } finally {
         OverlayZ.pop(var1);
      }
   }

   private static void select(Object var0, String var1) throws Exception {
      setField(var0, "selectedSpawnForm", var1);
      invoke(var0, "refreshAvailableBiomes");
      String var3 = (String)getField(var0, "selectedBiomeId");
      List var4 = (List)getField(var0, "availableBiomes");
      if (var3 != null && (var4 == null || !var4.contains(var3))) {
         setField(var0, "selectedBiomeId", null);
      }

      Object var2;
      setField(var0, "statusLine", (var2 = invoke(var0, "selectedTargetDisplayName")) == null ? "" : String.valueOf(var2));
      setField(var0, "outputLines", List.of("This form has a distinct installed spawn route and will be targeted separately."));
      setField(var0, "resultsScrollOffset", 0);
      invoke(var0, "refreshButtonLabels");
      invoke(var0, "saveRememberedState");
   }

   private static List<?> options(Object var0) throws Exception {
      Object var1 = getField(var0, "selectedPokemon");
      InlineFormDropdown.State var2 = state(var0);
      if (var1 == null) {
         var2.optionOwner = null;
         var2.options = List.of();
         var2.widestLabelWidth = -1;
         return List.of();
      }

      if (var1 == var2.optionOwner) {
         return var2.options;
      }

      Object var3 = invoke(var0, "allFormChoices", var1);
      List var4 = var3 instanceof List ? (List)var3 : List.of();
      var2.optionOwner = var1;
      var2.options = var4;
      var2.widestLabelWidth = -1;
      return var4;
   }

   private static String defaultKey(List<?> var0) throws Exception {
      for (Object var2 : var0) {
         if ("__base__".equalsIgnoreCase(optionKey(var2))) {
            return optionKey(var2);
         }
      }

      return var0.isEmpty() ? null : optionKey(var0.get(0));
   }

   private static String optionKey(Object var0) throws Exception {
      return String.valueOf(publicMethod(var0.getClass(), "key").invoke(var0));
   }

   private static String optionLabel(Object var0) throws Exception {
      return String.valueOf(publicMethod(var0.getClass(), "label").invoke(var0));
   }

   private static boolean formKeysMatch(Object var0, String var1, String var2) {
      if (var1 != null && var2 != null) {
         try {
            Method var3 = findMethod(var0.getClass(), "formKeysMatch", 2);
            return Boolean.TRUE.equals(var3.invoke(null, var1, var2));
         } catch (Throwable var4) {
            return var1.equalsIgnoreCase(var2);
         }
      } else {
         return false;
      }
   }

   private static void scrollSelectedIntoView(Object var0, InlineFormDropdown.State var1, List<?> var2) throws Exception {
      String var3 = (String)getField(var0, "selectedSpawnForm");
      int var4 = -1;

      for (int var5 = 0; var5 < var2.size(); var5++) {
         if (formKeysMatch(var0, optionKey(var2.get(var5)), var3)) {
            var4 = var5;
            break;
         }
      }

      if (var4 < 0) {
         var1.scroll = 0;
      } else {
         if (var4 < var1.scroll) {
            var1.scroll = var4;
         }

         if (var4 >= var1.scroll + 8) {
            var1.scroll = var4 - 8 + 1;
         }

         var1.scroll = clamp(var1.scroll, 0, Math.max(0, var2.size() - 8));
      }
   }

   private static int maxScroll(Object var0) throws Exception {
      return Math.max(0, options(var0).size() - 8);
   }

   private static InlineFormDropdown.Geometry geometry(Object var0) throws Exception {
      Object var1 = getField(var0, "pokemonButton");
      if (var1 != null && options(var0).size() > 1) {
         int var2 = intMethod(var1, "method_46426");
         int var3 = intMethod(var1, "method_46427");
         int var4 = intMethod(var1, "method_25368");
         int var5 = intMethod(var1, "method_25364");
         int var6 = (Integer)getField(var0, "leftPanelWidth");
         int var7 = (Integer)getField(var0, "uiLeft");
         int var8 = (Integer)getField(var0, "field_22790");
         Object var9 = getField(var0, "field_22793");
         List var10 = options(var0);
         InlineFormDropdown.State var11 = state(var0);
         int var12 = Math.min(8, var10.size());
         if (var11.widestLabelWidth < 0) {
            int var13 = 0;

            for (Object var15 : var10) {
               var13 = Math.max(var13, textWidth(var9, optionLabel(var15)) + 64);
            }

            var11.widestLabelWidth = Math.max(128, var13);
         }

         int var18 = Math.min(var6, var11.widestLabelWidth);
         int var19 = Math.max(var7, Math.min(var2, var7 + var6 - var18));
         int var20 = var3 + var5 + 1;
         int var16 = var12 * 24;
         int var17 = var20 + var16 + 4 <= var8 ? var20 : var3 - var16 - 1;
         var17 = Math.max(4, Math.min(var17, var8 - var16 - 4));
         return new InlineFormDropdown.Geometry(var19, var17, var18, var12);
      } else {
         return null;
      }
   }

   private static boolean inside(InlineFormDropdown.Geometry var0, double var1, double var3) {
      return var1 >= var0.x && var1 < var0.x + var0.w && var3 >= var0.y && var3 < var0.y + var0.h();
   }

   private static boolean insideButton(Object var0, double var1, double var3) throws Exception {
      int var5 = intMethod(var0, "method_46426");
      int var6 = intMethod(var0, "method_46427");
      int var7 = intMethod(var0, "method_25368");
      int var8 = intMethod(var0, "method_25364");
      return var1 >= var5 && var1 < var5 + var7 && var3 >= var6 && var3 < var6 + var8;
   }

   private static int intMethod(Object var0, String var1) throws Exception {
      Method var2 = publicMethod(var0.getClass(), var1);
      return ((Number)var2.invoke(var0)).intValue();
   }

   private static int textWidth(Object var0, String var1) {
      try {
         Method var2 = publicMethod(var0.getClass(), "method_1727", String.class);
         return ((Number)var2.invoke(var0, var1)).intValue();
      } catch (Throwable var3) {
         return var1 == null ? 0 : var1.length() * 6;
      }
   }

   private static String trimToWidth(Object var0, String var1, int var2) {
      if (textWidth(var0, var1) <= var2) {
         return var1;
      }

      String var3 = "...";
      String var4 = var1;

      while (!var4.isEmpty() && textWidth(var0, var4 + var3) > var2) {
         var4 = var4.substring(0, var4.length() - 1);
      }

      return var4 + var3;
   }

   private static void fill(Object var0, int var1, int var2, int var3, int var4, int var5) throws Exception {
      Method var6 = publicMethod(var0.getClass(), "method_25294", int.class, int.class, int.class, int.class, int.class);
      var6.invoke(var0, var1, var2, var3, var4, var5);
   }

   private static void drawText(Object var0, Object var1, String var2, int var3, int var4, int var5) throws Exception {
      Class var6 = Class.forName("net.minecraft.class_2561");
      Object var7 = var6.getMethod("method_43470", String.class).invoke(null, var2);
      String var8 = "draw:" + var0.getClass().getName() + ":" + var1.getClass().getName() + ":" + var7.getClass().getName();
      Method var9 = METHOD_CACHE.get(var8);
      if (var9 == null) {
         for (Method var13 : var0.getClass().getMethods()) {
            Class[] var14;
            if (var13.getName().equals("method_27535")
               && var13.getParameterCount() == 5
               && (var14 = var13.getParameterTypes())[0].isAssignableFrom(var1.getClass())
               && var14[1].isAssignableFrom(var7.getClass())
               && var14[2] == int.class
               && var14[3] == int.class
               && var14[4] == int.class) {
               var9 = var13;
               METHOD_CACHE.put(var8, var9);
               break;
            }
         }
      }

      if (var9 == null) {
         throw new NoSuchMethodException("DrawContext.method_27535");
      }

      var9.invoke(var0, var1, var7, var3, var4, var5);
   }

   private static void drawTexture(Object var0, Object var1, int var2, int var3, int var4, int var5) throws Exception {
      String var6 = "texture:" + var0.getClass().getName() + ":" + var1.getClass().getName();
      Method var7 = METHOD_CACHE.get(var6);
      if (var7 == null) {
         for (Method var11 : var0.getClass().getMethods()) {
            Class[] var12 = var11.getParameterTypes();
            if (var11.getName().equals("method_25293") && var11.getParameterCount() == 11 && var12[0].isAssignableFrom(var1.getClass())) {
               var7 = var11;
               METHOD_CACHE.put(var6, var7);
               break;
            }
         }
      }

      if (var7 == null) {
         throw new NoSuchMethodException("DrawContext.method_25293");
      }

      var7.invoke(var0, var1, var2, var3, 32, 32, 0.0F, 0.0F, var4, var5, var4, var5);
   }

   private static Object getField(Object var0, String var1) throws Exception {
      Field var2 = findField(var0.getClass(), var1);
      return var2.get(var0);
   }

   private static void setField(Object var0, String var1, Object var2) throws Exception {
      Field var3 = findField(var0.getClass(), var1);
      var3.set(var0, var2);
   }

   private static Field findField(Class<?> var0, String var1) throws NoSuchFieldException {
      String var2 = var0.getName() + "#" + var1;
      Field var3 = FIELD_CACHE.get(var2);
      if (var3 != null) {
         return var3;
      }

      for (Class var4 = var0; var4 != null; var4 = var4.getSuperclass()) {
         try {
            Field var5 = var4.getDeclaredField(var1);
            var5.setAccessible(true);
            FIELD_CACHE.put(var2, var5);
            return var5;
         } catch (NoSuchFieldException var6) {
         }
      }

      throw new NoSuchFieldException(var1);
   }

   private static Object invoke(Object var0, String var1, Object... var2) throws Exception {
      Method var3 = findMethod(var0.getClass(), var1, var2.length);
      return var3.invoke(var0, var2);
   }

   private static Method findMethod(Class<?> var0, String var1, int var2) throws NoSuchMethodException {
      String var3 = "declared:" + var0.getName() + "#" + var1 + "/" + var2;
      Method var4 = METHOD_CACHE.get(var3);
      if (var4 != null) {
         return var4;
      }

      for (Class var5 = var0; var5 != null; var5 = var5.getSuperclass()) {
         for (Method var9 : var5.getDeclaredMethods()) {
            if (var9.getName().equals(var1) && var9.getParameterCount() == var2) {
               var9.setAccessible(true);
               METHOD_CACHE.put(var3, var9);
               return var9;
            }
         }
      }

      throw new NoSuchMethodException(var1 + "/" + var2);
   }

   private static Method publicMethod(Class<?> var0, String var1, Class<?>... var2) throws NoSuchMethodException {
      StringBuilder var3 = new StringBuilder("public:").append(var0.getName()).append('#').append(var1);

      for (Class var7 : var2) {
         var3.append('/').append(var7.getName());
      }

      String var8 = var3.toString();
      Method var9 = METHOD_CACHE.get(var8);
      if (var9 != null) {
         return var9;
      }

      Method var10 = var0.getMethod(var1, var2);
      METHOD_CACHE.put(var8, var10);
      return var10;
   }

   private static int clamp(int var0, int var1, int var2) {
      return Math.max(var1, Math.min(var2, var0));
   }

   private record Geometry(int x, int y, int w, int rows) {
      int h() {
         return this.rows * 24;
      }
   }

   private static final class State {
      boolean open;
      int scroll;
      Object optionOwner;
      List<?> options = List.of();
      int widestLabelWidth = -1;
   }
}
