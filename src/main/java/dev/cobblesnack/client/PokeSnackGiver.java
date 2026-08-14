package dev.cobblesnack.client;

import dev.cobblesnack.cache.SessionDiagnostics;
import dev.cobblesnack.calc.Seasoning;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class PokeSnackGiver {
   private static final Identifier POKE_SNACK_ID = Identifier.of("cobblemon", "poke_snack");
   private static final Identifier BAIT_EFFECTS_COMPONENT_ID = Identifier.of("cobblemon", "bait_effects");
   private static final Identifier INGREDIENT_COMPONENT_ID = Identifier.of("cobblemon", "ingredient");
   private static final Identifier FOOD_COLOUR_COMPONENT_ID = Identifier.of("cobblemon", "food_colour");
   private static final String BAIT_EFFECTS_CLASS = "com.cobblemon.mod.common.item.components.BaitEffectsComponent";
   private static final String INGREDIENT_CLASS = "com.cobblemon.mod.common.item.components.IngredientComponent";
   private static final String SPAWN_BAIT_EFFECTS_CLASS = "com.cobblemon.mod.common.api.fishing.SpawnBaitEffects";
   private static final String SEASONINGS_CLASS = "com.cobblemon.mod.common.api.cooking.Seasonings";
   private static final String FOOD_COLOUR_CLASS = "com.cobblemon.mod.common.item.components.FoodColourComponent";

   private PokeSnackGiver() {
   }

   public static boolean canGive(MinecraftClient var0) {
      if (var0 == null || var0.player == null || var0.world == null || var0.interactionManager == null) {
         return false;
      } else {
         return var0.interactionManager.hasCreativeInventory() ? true : canUseGiveCommand(var0);
      }
   }

   public static boolean canUseGiveCommand(MinecraftClient var0) {
      if (var0 != null && var0.player != null && var0.player.networkHandler != null) {
         try {
            return var0.player.networkHandler.getCommandDispatcher().getRoot().getChild("give") != null;
         } catch (RuntimeException var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static PokeSnackGiver.GiveResult giveSelected(MinecraftClient var0, Seasoning[] var1, int var2) {
      if (var0 == null || var0.player == null || var0.world == null || var0.interactionManager == null) {
         return PokeSnackGiver.GiveResult.failure("Join a world/server before giving a PokéSnack.");
      }

      if (var2 >= 1 && var2 <= 64) {
         try {
            PokeSnackGiver.BuiltSnack var3 = buildSelected(var1);
            if (var0.interactionManager.hasCreativeInventory()) {
               return giveThroughCreativeInventory(var0, var3, var2);
            }

            if (!canUseGiveCommand(var0)) {
               return PokeSnackGiver.GiveResult.failure("Creative mode or access to /give is required to give a PokéSnack.");
            }

            int var5 = var2;

            while (var5 > 0) {
               int var8 = Math.min(16, var5);
               var0.player.networkHandler.sendChatCommand(buildGiveCommand(var3, var8));
               var5 -= var8;
            }

            return PokeSnackGiver.GiveResult.success("Requested " + var2 + " PokéSnack" + (var2 == 1 ? "" : "s") + " via /give: " + var3.description());
         } catch (ClassNotFoundException var6) {
            return PokeSnackGiver.GiveResult.failure("Cobblemon's PokéSnack component classes were not found. Is Cobblemon 1.7.x loaded?");
         } catch (Exception var7) {
            String var4 = var7.getMessage();
            if (var4 == null || var4.isBlank()) {
               var4 = var7.getClass().getSimpleName();
            }

            return PokeSnackGiver.GiveResult.failure("Could not build PokéSnack: " + var4);
         }
      } else {
         return PokeSnackGiver.GiveResult.failure("PokéSnack amount must be between 1 and 64.");
      }
   }

   public static PokeSnackGiver.GiveResult copySelectedItemArgument(MinecraftClient var0, Seasoning[] var1) {
      if (var0 == null) {
         return PokeSnackGiver.GiveResult.failure("Minecraft client is not available.");
      }

      try {
         PokeSnackGiver.BuiltSnack var2 = buildSelected(var1);
         String var7 = buildChatSafeItemArgument(var2);
         String var4 = "/give @s " + var7 + " 1";
         if (var4.length() > 256) {
            return PokeSnackGiver.GiveResult.failure("This PokéSnack's effects are too long for Minecraft chat. Use Give instead.");
         }

         GLFW.glfwSetClipboardString(var0.getWindow().getHandle(), var4);
         return PokeSnackGiver.GiveResult.success("Copied a ready-to-paste /give command. Effects are preserved; colour and ingredient display are omitted.");
      } catch (ClassNotFoundException var5) {
         return PokeSnackGiver.GiveResult.failure("Cobblemon's PokéSnack component classes were not found. Is Cobblemon 1.7.x loaded?");
      } catch (Exception var6) {
         String var3 = var6.getMessage();
         if (var3 == null || var3.isBlank()) {
            var3 = var6.getClass().getSimpleName();
         }

         return PokeSnackGiver.GiveResult.failure("Could not copy PokéSnack item: " + var3);
      }
   }

   private static PokeSnackGiver.BuiltSnack buildSelected(Seasoning[] var0) throws Exception {
      Item var2 = Registries.ITEM.get(POKE_SNACK_ID);
      if (var2 != null && var2 != Items.AIR) {
         ItemStack var3 = new ItemStack(var2, 1);
         ArrayList var4 = new ArrayList();
         ArrayList var5 = new ArrayList();
         ArrayList var6 = new ArrayList();
         ArrayList var7 = new ArrayList();
         ArrayList var8 = new ArrayList();
         if (var0 != null) {
            for (Seasoning var12 : var0) {
               if (var12 != null && var12 != Seasoning.NONE) {
                  Identifier var13 = Identifier.tryParse(var12.itemId());
                  if (var13 == null) {
                     throw new IllegalStateException("Invalid ingredient id for " + var12.displayName);
                  }

                  Item var14 = Registries.ITEM.get(var13);
                  if (var14 == null || var14 == Items.AIR) {
                     throw new IllegalStateException("Ingredient is not registered: " + var13);
                  }

                  ItemStack var15 = new ItemStack(var14);
                  var4.add(var13);
                  var8.add(shortName(var12));
                  var5.addAll(getBaitIdentifiers(var15));
                  Object var16 = getSeasoningColour(var15);
                  if (var16 != null) {
                     var6.add(var16);
                     var7.add(var16.toString().toLowerCase(Locale.ROOT));
                  }
               }
            }
         }

         boolean var20 = applyCobblemonRecipeProcessors(var3, var4);
         if (!var20) {
            if (!var5.isEmpty()) {
               Object var1 = newListComponent("com.cobblemon.mod.common.item.components.BaitEffectsComponent", var5);
               setRawComponent(var3, BAIT_EFFECTS_COMPONENT_ID, var1);
            }

            if (!var4.isEmpty()) {
               Object var17 = newListComponent("com.cobblemon.mod.common.item.components.IngredientComponent", var4);
               setRawComponent(var3, INGREDIENT_COMPONENT_ID, var17);
            }

            if (!var6.isEmpty()) {
               Object var18 = newListComponent("com.cobblemon.mod.common.item.components.FoodColourComponent", var6);
               setRawComponent(var3, FOOD_COLOUR_COMPONENT_ID, var18);
            }
         }

         SessionDiagnostics.event(
            "snack-component-order",
            "source=" + (var20 ? "cobblemon-processors" : "compatibility-fallback") + " ingredients=" + var4 + " colours=" + var7
         );
         String var19 = var8.isEmpty() ? "Base PokéSnack (no modifiers)" : String.join(" + ", var8);
         return new PokeSnackGiver.BuiltSnack(var3, List.copyOf(var4), List.copyOf(var5), List.copyOf(var7), var19);
      } else {
         throw new IllegalStateException("cobblemon:poke_snack is not registered");
      }
   }

   private static boolean applyCobblemonRecipeProcessors(ItemStack var0, List<Identifier> var1) {
      ArrayList<ItemStack> var2 = new ArrayList<>(var1.size());

      for (Identifier var4 : var1) {
         Item var5 = Registries.ITEM.get(var4);
         if (var5 == null || var5 == Items.AIR) {
            return false;
         }

         var2.add(new ItemStack(var5));
      }

      try {
         for (String var7 : List.of(
            "com.cobblemon.mod.common.item.crafting.FoodColourSeasoningProcessor",
            "com.cobblemon.mod.common.item.crafting.IngredientSeasoningProcessor",
            "com.cobblemon.mod.common.item.crafting.BaitSeasoningProcessor"
         )) {
            Class<?> var8 = Class.forName(var7);
            Object var9 = var8.getField("INSTANCE").get(null);
            Method var10 = Arrays.stream(var8.getMethods())
               .filter(var0x -> var0x.getName().equals("apply"))
               .filter(var0x -> var0x.getParameterCount() == 2)
               .findFirst()
               .orElseThrow(() -> new NoSuchMethodException(var7 + ".apply"));
            var10.invoke(var9, var0, var2);
         }

         return true;
      } catch (ReflectiveOperationException | RuntimeException var6) {
         SessionDiagnostics.event(
            "snack-component-processor",
            "result=fallback type=" + var6.getClass().getSimpleName() + " message=" + String.valueOf(var6.getMessage())
         );
         return false;
      }
   }

   private static Object getSeasoningColour(ItemStack var0) {
      try {
         Class var1 = Class.forName("com.cobblemon.mod.common.api.cooking.Seasonings");
         Object var2 = var1.getField("INSTANCE").get(null);
         Method var3 = Arrays.stream(var1.getMethods())
            .filter(var0x -> var0x.getName().equals("getFromItemStack"))
            .filter(var0x -> var0x.getParameterCount() == 1)
            .findFirst()
            .orElse(null);
         if (var3 == null) {
            return null;
         }

         Object var4 = var3.invoke(var2, var0);
         if (var4 == null) {
            return null;
         }

         Method var5 = Arrays.stream(var4.getClass().getMethods())
            .filter(var0x -> var0x.getName().equals("getColour"))
            .filter(var0x -> var0x.getParameterCount() == 0)
            .findFirst()
            .orElse(null);
         return var5 == null ? null : var5.invoke(var4);
      } catch (ReflectiveOperationException | RuntimeException var6) {
         return null;
      }
   }

   private static List<Identifier> getBaitIdentifiers(ItemStack var0) throws Exception {
      Class var2 = Class.forName("com.cobblemon.mod.common.api.fishing.SpawnBaitEffects");
      Method var3 = Arrays.stream(var2.getMethods())
         .filter(var0x -> var0x.getName().equals("getBaitIdentifiersFromItem"))
         .filter(var0x -> Modifier.isStatic(var0x.getModifiers()))
         .filter(var0x -> var0x.getParameterCount() == 1)
         .findFirst()
         .orElseThrow(() -> new NoSuchMethodException("SpawnBaitEffects.getBaitIdentifiersFromItem"));
      if (!(var3.invoke(null, var0.getRegistryEntry()) instanceof List var5)) {
         return List.of();
      } else {
         ArrayList var6 = new ArrayList();

         for (Object var8 : var5) {
            if (var8 instanceof Identifier var10) {
               var6.add(var10);
            } else {
               Identifier var9;
               if (var8 != null && (var9 = Identifier.tryParse(var8.toString())) != null) {
                  var6.add(var9);
               }
            }
         }

         Identifier var1;
         if (var6.isEmpty() && (var1 = getSeasoningFallbackIdentifier(var0)) != null) {
            var6.add(var1);
         }

         return var6;
      }
   }

   private static Identifier getSeasoningFallbackIdentifier(ItemStack var0) {
      try {
         Class var2 = Class.forName("com.cobblemon.mod.common.api.cooking.Seasonings");
         Object var3 = var2.getField("INSTANCE").get(null);
         Method var4 = Arrays.stream(var2.getMethods())
            .filter(var0x -> var0x.getName().equals("getFromItemStack"))
            .filter(var0x -> var0x.getParameterCount() == 1)
            .findFirst()
            .orElse(null);
         if (var4 == null) {
            return null;
         }

         Object var5 = var4.invoke(var3, var0);
         if (var5 == null) {
            return null;
         }

         Method var6 = Arrays.stream(var5.getClass().getMethods())
            .filter(var0x -> var0x.getName().equals("getBaitEffects"))
            .filter(var0x -> var0x.getParameterCount() == 0)
            .findFirst()
            .orElse(null);
         if (var6 == null) {
            return null;
         }

         Object var7 = var6.invoke(var5);
         List var1;
         if (var7 instanceof List && !(var1 = (List)var7).isEmpty()) {
            Method var8 = Arrays.stream(var5.getClass().getMethods())
               .filter(var0x -> var0x.getName().equals("getIngredient"))
               .filter(var0x -> var0x.getParameterCount() == 0)
               .findFirst()
               .orElse(null);
            if (var8 == null) {
               return null;
            }

            Object var9 = var8.invoke(var5);
            if (var9 == null) {
               return null;
            }

            Method var10 = Arrays.stream(var9.getClass().getMethods())
               .filter(var0x -> var0x.getName().equals("getIdentifier"))
               .filter(var0x -> var0x.getParameterCount() == 0)
               .findFirst()
               .orElse(null);
            if (var10 == null) {
               return null;
            }

            Object var11 = var10.invoke(var9);
            if (var11 == null) {
               return null;
            }

            Identifier var12 = Identifier.tryParse(var11.toString());
            return var12 == null ? null : Identifier.of("seasonings", var12.getPath());
         } else {
            return null;
         }
      } catch (ReflectiveOperationException | RuntimeException var13) {
         return null;
      }
   }

   private static Object newListComponent(String var0, List<?> var1) throws Exception {
      Class var2 = Class.forName(var0);
      Constructor var3 = var2.getConstructor(List.class);
      return var3.newInstance(new ArrayList(var1));
   }

   private static void setRawComponent(ItemStack var0, Identifier var1, Object var2) {
      ComponentType var3 = Registries.DATA_COMPONENT_TYPE.get(var1);
      if (var3 == null) {
         throw new IllegalStateException("Missing data component: " + var1);
      }

      var0.set(var3, var2);
   }

   private static PokeSnackGiver.GiveResult giveThroughCreativeInventory(MinecraftClient var0, PokeSnackGiver.BuiltSnack var1, int var2) {
      List<Integer> var7 = new ArrayList<>();
      List<Integer> var8 = new ArrayList<>();
      int var9 = 0;

      for (int var6 = 0; var6 < 36; var6++) {
         ItemStack var5 = var0.player.getInventory().getStack(var6);
         if (var5.isEmpty()) {
            var8.add(var6);
            var9 += 16;
         } else if (ItemStack.areItemsAndComponentsEqual(var5, var1.stack()) && var5.getCount() < 16) {
            var7.add(var6);
            var9 += 16 - var5.getCount();
         }
      }

      if (var9 < var2) {
         return PokeSnackGiver.GiveResult.failure("Not enough inventory space for " + var2 + " PokéSnacks (matching stacks hold at most 16).");
      }

      int var16 = var2;

      for (Integer var11 : var7) {
         int var4 = var11;
         ItemStack var12 = var0.player.getInventory().getStack(var4).copy();
         int var3 = Math.min(16 - var12.getCount(), var16);
         if (var3 > 0) {
            var12.setCount(var12.getCount() + var3);
            int var13 = var4 < 9 ? 36 + var4 : var4;
            var0.player.getInventory().setStack(var4, var12.copy());
            var0.interactionManager.clickCreativeStack(var12, var13);
            if ((var16 -= var3) <= 0) {
               break;
            }
         }
      }

      for (Integer var18 : var8) {
         int var15 = var18;
         if (var16 <= 0) {
            break;
         }

         int var19 = Math.min(16, var16);
         int var14 = var15 < 9 ? 36 + var15 : var15;
         ItemStack var20 = var1.stack().copy();
         var20.setCount(var19);
         var0.player.getInventory().setStack(var15, var20.copy());
         var0.interactionManager.clickCreativeStack(var20, var14);
         var16 -= var19;
      }

      return PokeSnackGiver.GiveResult.success("Gave " + var2 + " PokéSnack" + (var2 == 1 ? "" : "s") + ": " + var1.description());
   }

   private static String buildItemArgument(PokeSnackGiver.BuiltSnack var0) {
      StringBuilder var1 = new StringBuilder("cobblemon:poke_snack");
      ArrayList var2 = new ArrayList();
      if (!var0.baitEffectIds().isEmpty()) {
         var2.add("cobblemon:bait_effects=" + identifierListForCommand(var0.baitEffectIds()));
      }

      if (!var0.ingredientIds().isEmpty()) {
         var2.add("cobblemon:ingredient=" + identifierListForCommand(var0.ingredientIds()));
      }

      if (!var0.foodColourNames().isEmpty()) {
         var2.add("cobblemon:food_colour=" + stringListForCommand(var0.foodColourNames()));
      }

      if (!var2.isEmpty()) {
         var1.append('[').append(String.join(",", var2)).append(']');
      }

      return var1.toString();
   }

   private static String buildChatSafeItemArgument(PokeSnackGiver.BuiltSnack var0) {
      StringBuilder var1 = new StringBuilder("cobblemon:poke_snack");
      if (!var0.baitEffectIds().isEmpty()) {
         var1.append("[cobblemon:bait_effects=").append(identifierListForCommand(var0.baitEffectIds())).append(']');
      }

      return var1.toString();
   }

   private static String buildGiveCommand(PokeSnackGiver.BuiltSnack var0, int var1) {
      return "give @s " + buildItemArgument(var0) + " " + var1;
   }

   private static String identifierListForCommand(List<Identifier> var0) {
      ArrayList var1 = new ArrayList();

      for (Identifier var3 : var0) {
         var1.add("\"" + var3.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
      }

      return "[" + String.join(",", var1) + "]";
   }

   private static String stringListForCommand(List<String> var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : var0) {
         var1.add("\"" + var3.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
      }

      return "[" + String.join(",", var1) + "]";
   }

   private static String shortName(Seasoning var0) {
      String var1 = var0.displayName;
      int var2 = var1.indexOf(" (");
      return var2 > 0 ? var1.substring(0, var2) : var1;
   }

   private record BuiltSnack(ItemStack stack, List<Identifier> ingredientIds, List<Identifier> baitEffectIds, List<String> foodColourNames, String description) {
   }

   public record GiveResult(boolean success, String message) {
      static PokeSnackGiver.GiveResult success(String var0) {
         return new PokeSnackGiver.GiveResult(true, var0);
      }

      static PokeSnackGiver.GiveResult failure(String var0) {
         return new PokeSnackGiver.GiveResult(false, var0);
      }
   }
}
