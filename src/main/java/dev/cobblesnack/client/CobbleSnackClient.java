package dev.cobblesnack.client;

import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;

public final class CobbleSnackClient implements ClientModInitializer {
   private static KeyBinding openCalculatorKey;

   @Override
   public void onInitializeClient() {
      registerPotButton();
      registerOpenKey();
      PerformanceWarmup.start();
   }

   private static void registerOpenKey() {
      openCalculatorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.cobblesnack.open", Type.KEYSYM, 75, "category.cobblesnack"));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         PerformanceWarmup.tick();
         TomStorageBridge.tick(client);

         while (openCalculatorKey.wasPressed()) {
            if (client.world != null) {
               if (client.currentScreen instanceof SnackCalculatorScreen snackScreen) {
                  if (!snackScreen.isTypingInSearch()) {
                     snackScreen.close();
                  }
               } else if (client.currentScreen == null) {
                  client.setScreen(new SnackCalculatorScreen(null));
               }
            }
         }
      });
   }

   private static void registerPotButton() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, scaledWidth, scaledHeight) -> {
         TomStorageBridge.installTerminalOpenButton(client, screen, scaledWidth, scaledHeight);
         if (isCobblemonPotScreen(screen)) {
            int x = Math.min(scaledWidth - 106, scaledWidth / 2 + 92);
            int y = Math.max(6, scaledHeight / 2 - 84);
            PokeSnackIconButton calculatorButton = new PokeSnackIconButton(x, y, button -> client.setScreen(new SnackCalculatorScreen(screen)));
            Screens.getButtons(screen).add(calculatorButton);
         }
      });
   }

   private static boolean isCobblemonPotScreen(Screen screen) {
      String className = screen.getClass().getName().toLowerCase(Locale.ROOT);
      String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
      return !className.contains("cobblemon")
         ? false
         : className.contains("campfirepot")
            || className.contains("cookingpot")
            || className.contains("campfire") && className.contains("screen")
            || className.contains("cooking") && className.contains("screen")
            || title.contains("campfire pot")
            || title.contains("cooking pot");
   }

   public static boolean matchesOpenCalculatorKey(int keyCode, int scanCode) {
      return openCalculatorKey != null && openCalculatorKey.matchesKey(keyCode, scanCode);
   }
}
