package dev.cobblesnack.client;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OverlayZ {
   private static final float Z = 900.0F;
   private static final ThreadLocal<Integer> PUSH_DEPTH = ThreadLocal.withInitial(() -> 0);
   private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

   private OverlayZ() {
   }

   public static void push(Object var0) {
      if (var0 != null) {
         try {
            Object var1 = find(var0.getClass(), "method_51448", "getMatrices", 0).invoke(var0);
            find(var1.getClass(), "method_22903", "push", 0).invoke(var1);
            find(var1.getClass(), "method_46416", "translate", 3).invoke(var1, 0.0F, 0.0F, 900.0F);
            PUSH_DEPTH.set(PUSH_DEPTH.get() + 1);
         } catch (Throwable var2) {
         }
      }
   }

   public static void pop(Object var0) {
      int var1 = PUSH_DEPTH.get();
      if (var0 != null && var1 > 0) {
         try {
            Object var2 = find(var0.getClass(), "method_51448", "getMatrices", 0).invoke(var0);
            find(var2.getClass(), "method_22909", "pop", 0).invoke(var2);
         } catch (Throwable var6) {
         } finally {
            PUSH_DEPTH.set(Math.max(0, var1 - 1));
         }
      }
   }

   private static Method find(Class<?> var0, String var1, String var2, int var3) throws NoSuchMethodException {
      String var4 = var0.getName() + "#" + var1 + "/" + var2 + "/" + var3;
      Method var5 = METHOD_CACHE.get(var4);
      if (var5 != null) {
         return var5;
      }

      for (Class var6 = var0; var6 != null; var6 = var6.getSuperclass()) {
         for (Method var10 : var6.getDeclaredMethods()) {
            if (var10.getParameterCount() == var3 && (var10.getName().equals(var1) || var10.getName().equals(var2))) {
               var10.setAccessible(true);
               METHOD_CACHE.put(var4, var10);
               return var10;
            }
         }
      }

      for (Method var14 : var0.getMethods()) {
         if (var14.getParameterCount() == var3 && (var14.getName().equals(var1) || var14.getName().equals(var2))) {
            METHOD_CACHE.put(var4, var14);
            return var14;
         }
      }

      throw new NoSuchMethodException(var0.getName() + "." + var1 + "/" + var2 + " (" + var3 + ")");
   }
}
