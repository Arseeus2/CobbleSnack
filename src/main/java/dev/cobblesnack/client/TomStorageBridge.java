package dev.cobblesnack.client;

import dev.cobblesnack.cache.DiskCacheStore;
import dev.cobblesnack.cache.SessionDiagnostics;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TomStorageBridge {
   private static final int PACKETS_PER_TICK = 8;
   private static final String MILK_TAG = "c:drinks/milk";
   private static final List<String> MILK_FALLBACK_IDS = List.of("cobblemon:moomoo_milk", "minecraft:milk_bucket");
   private static final Pattern RESOURCE_ID = Pattern.compile("\"(#[a-z0-9_.-]+:[a-z0-9_./-]+|[a-z0-9_.-]+:[a-z0-9_./-]+)\"");
   private static final Queue<TomStorageBridge.TransferOp> PENDING = new ConcurrentLinkedQueue<>();
   private static Object transferOwner;
   private static int transferItems;
   private static int transferPackets;
   private static int sentPackets;
   private static String transferSummary = "";
   private static boolean transferHadMissing;
   private static Object transferMenu;
   private static final Map<String, Integer> transferExpectedInventory = new LinkedHashMap<>();
   private static int transferDeliveryWaitTicks;
   private static boolean pullFullRecipeAmount;

   private TomStorageBridge() {
   }

   public static boolean isTomTerminalScreen(Object var0) {
      if (var0 == null) {
         return false;
      }

      for (Class var1 = var0.getClass(); var1 != null; var1 = var1.getSuperclass()) {
         String var2 = var1.getName();
         if (var2.equals("com.tom.storagemod.screen.AbstractStorageTerminalScreen")) {
            return true;
         }

         if (var2.equals("com.tom.storagemod.screen.StorageTerminalScreen")) {
            return true;
         }

         if (var2.equals("com.tom.storagemod.screen.CraftingTerminalScreen")) {
            return true;
         }
      }

      return false;
   }

   public static void installTerminalOpenButton(Object var0, Object var1, int var2, int var3) {
      if (isTomTerminalScreen(var1)) {
         try {
            int var4 = intFieldAny(var1, var2 / 2 - 97, "field_2776", "x");
            int var5 = intFieldAny(var1, var3 / 2 - 101, "field_2800", "y");
            int var6 = intFieldAny(var1, 194, "field_2792", "backgroundWidth");
            int var7 = Math.max(4, Math.min(var2 - 24, var4 + var6 + 4));
            int var8 = Math.max(4, Math.min(var3 - 24, var5 + 4));
            Class var9 = Class.forName("dev.cobblesnack.client.PokeSnackIconButton");
            Constructor var10 = Arrays.stream(var9.getConstructors()).filter(var0x -> var0x.getParameterCount() == 3).findFirst().orElseThrow();
            Class var11 = var10.getParameterTypes()[2];
            Object var12 = actionProxy(var11, () -> openCalculator(var0, var1));
            Object var13 = var10.newInstance(var7, var8, var12);
            addDrawableChild(var1, var13);
         } catch (Throwable var14) {
         }
      }
   }

   public static void installCalculatorButtons(Object var0) {
      if (var0 != null) {
         try {
            int var1 = intFieldAny(var0, 0, "rightPanelX");
            int var2 = intFieldAny(var0, 250, "rightPanelWidth");
            int var3 = intFieldAny(var0, 24, "uiTop");
            int var4 = var3 + 36;
            int var5 = Math.max(125, Math.min(190, var2 - 138));
            int var6 = var1 + 20 + var5 + 20;
            int var7 = Math.max(105, var2 - var5 - 20 - 26);
            Object var8 = makeButton("Clear inputs", var6, var4 + 28, var7, 20, () -> clearInputs(var0));
            setTooltip(var8, "Clear all seasonings and the selected biome");
            addDrawableChild(var0, var8);
            Object var9 = objectFieldAny(var0, "parent");
            if (!isTomTerminalScreen(var9)) {
               return;
            }

            int var10 = intFieldAny(var0, 0, "resultsY");
            int var11 = intFieldAny(var0, 100, "resultsHeight");
            int var12 = Math.max(72, var11 - 26);
            setFieldAny(var0, var12, "resultsHeight");
            int var13 = var10 + var12 + 8;
            int var14 = Math.max(200, var2 - 24);
            int var15 = Math.max(104, Math.min(130, var14 / 2));
            Object var16 = makeButton("Pull ingredients", var1 + 12, var13, var15, 20, () -> beginPull(var0));
            setTooltip(var16, "Pull PokéSnack ingredients from this Tom's storage network");
            addDrawableChild(var0, var16);
            int var17 = var1 + 12 + var15 + 8;
            int var18 = Math.max(92, var1 + var2 - 12 - var17);
            Object var19 = makeCheckbox(var0, "Pull full recipe", var17, var13, var18, pullFullRecipeAmount, var0x -> pullFullRecipeAmount = var0x);
            setTooltip(var19, "When checked, pull a complete additional recipe batch instead of only topping up what your inventory is missing");
            addDrawableChild(var0, var19);
         } catch (Throwable var20) {
            setStatus(var0, "Could not initialize Tom's storage integration: " + shortError(var20));
         }
      }
   }

   public static void tick(Object var0) {
      if (!PENDING.isEmpty()) {
         try {
            int var1 = 0;

            while (var1 < 8) {
               TomStorageBridge.TransferOp var2 = PENDING.peek();
               if (var2 == null) {
                  break;
               }

               if (var2 instanceof TomStorageBridge.PullPacket var3) {
                  sendPull(var3);
                  PENDING.poll();
                  var1++;
                  sentPackets++;
               } else {
                  if (!(var2 instanceof TomStorageBridge.PlaceCursorOp var6)) {
                     throw new IllegalStateException("Unknown Tom's transfer operation");
                  }

                  TomStorageBridge.PlaceResult var4 = placeCursorInMainInventory(var6);
                  if (var4 == TomStorageBridge.PlaceResult.WAITING) {
                     break;
                  }

                  if (var4 != TomStorageBridge.PlaceResult.DONE) {
                     throw new IllegalStateException("Could not place pulled ingredients into the main inventory");
                  }

                  PENDING.poll();
                  var1++;
                  sentPackets++;
               }
            }

            if (transferOwner != null && transferPackets > 0 && !PENDING.isEmpty()) {
               setStatus(transferOwner, "Pulling PokéSnack ingredients from Tom's storage… " + sentPackets + "/" + transferPackets + " transfer step(s)");
            }

            if (PENDING.isEmpty() && transferOwner != null) {
               if (!expectedDeliveriesArrived()) {
                  transferDeliveryWaitTicks++;
                  if (transferDeliveryWaitTicks % 20 == 0) {
                     setStatus(transferOwner, "Waiting for Tom's storage to finish delivering ingredients…");
                  }

                  return;
               }

               if (transferHadMissing) {
                  setStatus(transferOwner, "Pulled everything available. Missing items are listed in chat.");
                  closeCalculatorToWorld();
               } else {
                  setStatus(
                     transferOwner,
                     transferSummary != null && !transferSummary.isBlank()
                        ? "Pulled from Tom's storage: " + transferSummary
                        : "Pulled " + transferItems + " ingredient item" + (transferItems == 1 ? "" : "s") + " from Tom's storage."
                  );
               }

               resetTransferState();
            }
         } catch (Throwable var5) {
            PENDING.clear();
            if (transferOwner != null) {
               setStatus(transferOwner, "Tom's storage transfer stopped: " + shortError(var5));
            }

            clearCursorSafely();
            resetTransferState();
         }
      }
   }

   public static void clearInputs(Object var0) {
      try {
         Field var1 = findField(var0.getClass(), "selected");
         var1.setAccessible(true);
         if (var1.get(var0) instanceof Object[] var3) {
            Class var4 = Class.forName("dev.cobblesnack.calc.Seasoning");
            Object var5 = var4.getField("NONE").get(null);
            Arrays.fill(var3, var5);
         }

         setFieldAny(var0, null, "selectedBiomeId");
         setStatus(var0, "Cleared seasonings and biome.");
         invokeNoArg(var0, "refreshButtonLabels");
         invokeNoArg(var0, "saveRememberedState");
      } catch (Throwable var6) {
         setStatus(var0, "Could not clear inputs: " + shortError(var6));
      }
   }

   public static void beginPull(Object var0) {
      if (var0 != null) {
         if (PENDING.isEmpty() && transferOwner == null) {
            try {
               Object var1 = objectFieldAny(var0, "parent");
               if (!isTomTerminalScreen(var1)) {
                  setStatus(var0, "Open CobbleSnack from a Tom's Storage Terminal to pull ingredients.");
                  return;
               }

               Object var2 = terminalMenu(var1);
               if (var2 == null) {
                  throw new IllegalStateException("Tom's terminal menu is unavailable");
               }

               Boolean var3 = booleanFieldAny(var2, "itemsLoaded");
               if (var3 != null && !var3) {
                  setStatus(var0, "Tom's storage is still syncing. Try again in a moment.");
                  return;
               }

               Object var4 = minecraftClient();
               Object var5 = clientPlayer(var4);
               if (var4 == null || var5 == null || clientInteractionManager(var4) == null) {
                  setStatus(var0, "Join a world/server first.");
                  return;
               }

               Object var6 = handlerCursorStack(var2);
               if (var6 == null || !isEmptyStack(var6)) {
                  setStatus(var0, "Put down the item on your cursor before pulling ingredients.");
                  return;
               }

               Object var7 = publicOrDeclaredField(var2, "sync").get(var2);
               if (var7 == null) {
                  throw new IllegalStateException("Tom's terminal sync is unavailable");
               }

               List var8 = (List)var7.getClass().getMethod("getAsList").invoke(var7);
               List var9 = readStorageSources(var8);
               List var10 = playerMainInventory(var2);
               int var11 = Math.max(1, intFieldAny(var0, 1, "giveAmount"));
               boolean var12 = pullFullRecipeAmount;
               Map<String, TomStorageBridge.Requirement> var13 = new LinkedHashMap<>();
               addExact(var13, "minecraft:honey_bottle", "Honey Bottle", 2 * var11);
               addExact(var13, "cobblemon:vivichoke", "Vivichoke", var11);
               addExact(var13, "cobblemon:hearty_grains", "Hearty Grains", 3 * var11);
               if (objectFieldAny(var0, "selected") instanceof Object[] var15) {
                  for (Object var19 : var15) {
                     if (var19 != null && !var19.toString().equals("NONE")) {
                        Method var20 = var19.getClass().getMethod("itemId");
                        String var21 = (String)var20.invoke(var19);
                        if (var21 != null && !var21.isBlank()) {
                           String var22 = friendlyItemLabel(var21);
                           addExact(var13, var21, var22, var11);
                        }
                     }
                  }
               }

               int var28 = 3 * var11;
               List<String> var29 = discoverItemTagValues("c:drinks/milk", MILK_FALLBACK_IDS);
               List<TomStorageBridge.Allocation> var30 = new ArrayList<>();
               List<TomStorageBridge.MissingItem> var31 = new ArrayList<>();
               int var32 = var12 ? 0 : countInventoryAnyIds(var10, var29);
               int var33 = var12 ? var28 : Math.max(0, var28 - var32);
               if (var33 > 0) {
                  int var34 = allocateAlternativeIds("Milk", var29, var33, var9, var30);
                  if (var34 > 0) {
                     String var37 = var29.stream()
                        .map(TomStorageBridge::friendlyItemLabel)
                        .distinct()
                        .reduce((var0x, var1x) -> var0x + " or " + var1x)
                        .orElse("Milk");
                     var31.add(new TomStorageBridge.MissingItem(var34, "Milk", var37));
                  }
               }

               for (TomStorageBridge.Requirement var38 : var13.values()) {
                  int var23 = var12 ? 0 : countInventoryId(var10, var38.itemId);
                  int var24 = var12 ? var38.count : Math.max(0, var38.count - var23);
                  if (var24 > 0) {
                     int var25 = allocateExact(var38, var24, var9, var30);
                     if (var25 > 0) {
                        var31.add(new TomStorageBridge.MissingItem(var25, var38.label, null));
                     }
                  }
               }

               if (!var31.isEmpty()) {
                  sendMissingChat(var11, var31, var12);
               }

               int var36 = var30.stream().mapToInt(var0x -> var0x.count).sum();
               if (var36 == 0) {
                  if (var31.isEmpty()) {
                     setStatus(
                        var0,
                        var12
                           ? "No additional recipe ingredients were available to pull from Tom's storage."
                           : "You already have all ingredients for " + var11 + " PokéSnack" + (var11 == 1 ? "" : "s") + " in your inventory."
                     );
                  } else {
                     setStatus(var0, "Nothing available to pull. Missing items are listed in chat.");
                     closeCalculatorToWorld();
                  }

                  return;
               }

               if (!inventoryCanFit(var10, var30)) {
                  setStatus(var0, "Not enough main-inventory space for the available PokéSnack ingredients. Nothing was pulled.");
                  sendInventoryFullChat();
                  return;
               }

               List<TomStorageBridge.TransferOp> var39 = buildPackets(var7, var2, var30);
               if (var39.isEmpty()) {
                  setStatus(var0, "No Tom's storage transfers were required.");
                  return;
               }

               transferExpectedInventory.clear();
               Map<String, Integer> var40 = new LinkedHashMap<>();

               for (TomStorageBridge.Allocation var43 : var30) {
                  var40.merge(var43.source.itemId, var43.count, Integer::sum);
               }

               for (Entry<String, Integer> var44 : var40.entrySet()) {
                  int var26 = countInventoryId(var10, var44.getKey());
                  transferExpectedInventory.put(var44.getKey(), var26 + var44.getValue());
               }

               PENDING.addAll(var39);
               transferOwner = var0;
               transferMenu = var2;
               transferItems = var36;
               transferPackets = var39.size();
               sentPackets = 0;
               transferDeliveryWaitTicks = 0;
               transferSummary = summarizeAllocations(var30);
               transferHadMissing = !var31.isEmpty();
               setStatus(
                  var0,
                  transferHadMissing
                     ? (
                        var12
                           ? "Pulling the available part of a full additional recipe batch… Missing items are listed in chat."
                           : "Pulling everything available from Tom's storage… Missing items are listed in chat."
                     )
                     : (
                        var12
                           ? "Pulling a full additional " + var11 + "-PokéSnack recipe batch: " + transferSummary
                           : "Pulling " + var36 + " ingredient item" + (var36 == 1 ? "" : "s") + " from Tom's storage: " + transferSummary
                     )
               );
            } catch (Throwable var27) {
               PENDING.clear();
               resetTransferState();
               setStatus(var0, "Could not pull from Tom's storage: " + shortError(var27));
            }
         } else {
            setStatus(var0, "A Tom's storage ingredient transfer is already in progress.");
         }
      }
   }

   private static void openCalculator(Object var0, Object var1) {
      try {
         Class var2 = Class.forName("dev.cobblesnack.client.SnackCalculatorScreen");
         Constructor var3 = Arrays.stream(var2.getConstructors())
            .filter(var1x -> var1x.getParameterCount() == 1 && var1x.getParameterTypes()[0].isAssignableFrom(var1.getClass()))
            .findFirst()
            .orElseGet(() -> Arrays.stream(var2.getConstructors()).filter(var0xx -> var0xx.getParameterCount() == 1).findFirst().orElse(null));
         if (var3 == null) {
            throw new NoSuchMethodException("SnackCalculatorScreen(Screen)");
         }

         Object var4 = var3.newInstance(var1);
         Method var5 = findMethodByNames(var0.getClass(), new String[]{"method_1507", "setScreen"}, 1);
         var5.invoke(var0, var4);
      } catch (Throwable var6) {
      }
   }

   private static List<TomStorageBridge.StorageSource> readStorageSources(List<?> var0) throws Exception {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      }

      for (Object var3 : var0) {
         if (var3 != null) {
            Object var4 = var3.getClass().getMethod("getStack").invoke(var3);
            long var5 = ((Number)var3.getClass().getMethod("getQuantity").invoke(var3)).longValue();
            if (var4 != null && var5 > 0L) {
               Object var7 = itemOf(var4);
               String var8 = registryItemId(var7);
               int var9 = ((Number)var3.getClass().getMethod("getMaxStackSize").invoke(var3)).intValue();
               var1.add(new TomStorageBridge.StorageSource(var3, var4, var7, var8, var5, Math.max(1, var9)));
            }
         }
      }

      return var1;
   }

   private static List<Object> playerMainInventory(Object var0) throws Exception {
      Field var1 = findField(var0.getClass(), "pinv");
      var1.setAccessible(true);
      Object var2 = var1.get(var0);
      if (var2 == null) {
         throw new IllegalStateException("Player inventory is unavailable");
      }

      for (Class var3 = var2.getClass(); var3 != null; var3 = var3.getSuperclass()) {
         for (Field var7 : var3.getDeclaredFields()) {
            var7.setAccessible(true);

            Object var8;
            try {
               var8 = var7.get(var2);
            } catch (Throwable var10) {
               continue;
            }

            if (var8 instanceof List var9 && var9.size() == 36) {
               return new ArrayList<>(var9);
            }
         }
      }

      throw new IllegalStateException("Could not inspect the 36-slot player inventory");
   }

   private static int allocateAlternativeIds(
      String var0, List<String> var1, int var2, List<TomStorageBridge.StorageSource> var3, List<TomStorageBridge.Allocation> var4
   ) {
      Set<String> var5 = new LinkedHashSet<>(var1);
      List<TomStorageBridge.StorageSource> var6 = var3.stream()
         .filter(var1x -> var1x.itemId != null && var5.contains(var1x.itemId))
         .sorted(
            Comparator.<TomStorageBridge.StorageSource>comparingInt(var0x -> "cobblemon:moomoo_milk".equals(var0x.itemId) ? 0 : 1)
               .thenComparing(Comparator.<TomStorageBridge.StorageSource>comparingInt(var0x -> var0x.maxStack).reversed())
               .thenComparing(Comparator.<TomStorageBridge.StorageSource>comparingLong(var0x -> var0x.quantity).reversed())
         )
         .toList();
      int var7 = var2;

      for (TomStorageBridge.StorageSource var9 : var6) {
         if (var7 <= 0) {
            break;
         }

         int var10 = (int)Math.min(var7, var9.remaining());
         if (var10 > 0) {
            var4.add(new TomStorageBridge.Allocation(var0, var9, var10));
            var9.allocated += var10;
            var7 -= var10;
         }
      }

      return var7;
   }

   private static int allocateExact(
      TomStorageBridge.Requirement var0, int var1, List<TomStorageBridge.StorageSource> var2, List<TomStorageBridge.Allocation> var3
   ) {
      int var4 = var1;

      for (TomStorageBridge.StorageSource var6 : var2) {
         if (var4 <= 0) {
            break;
         }

         if (var6.itemId != null && var6.itemId.equals(var0.itemId)) {
            int var7 = (int)Math.min(var4, var6.remaining());
            if (var7 > 0) {
               var3.add(new TomStorageBridge.Allocation(var0.label, var6, var7));
               var6.allocated += var7;
               var4 -= var7;
            }
         }
      }

      return var4;
   }

   private static boolean inventoryCanFit(List<Object> var0, List<TomStorageBridge.Allocation> var1) throws Exception {
      List<TomStorageBridge.VirtualSlot> var2 = new ArrayList<>(36);

      for (Object var4 : var0) {
         if (var4 != null && !isEmptyStack(var4)) {
            var2.add(new TomStorageBridge.VirtualSlot(var4, stackCount(var4), stackMax(var4)));
         } else {
            var2.add(new TomStorageBridge.VirtualSlot(null, 0, 64));
         }
      }

      for (TomStorageBridge.Allocation var11 : var1) {
         int var5 = var11.count;
         Object var6 = var11.source.stack;

         for (TomStorageBridge.VirtualSlot var8 : var2) {
            if (var5 <= 0) {
               break;
            }

            if (var8.stack != null && var8.count < var8.max && stacksCompatible(var8.stack, var6)) {
               int var9 = Math.min(var5, var8.max - var8.count);
               var8.count += var9;
               var5 -= var9;
            }
         }

         while (var5 > 0) {
            TomStorageBridge.VirtualSlot var12 = var2.stream().filter(var0x -> var0x.stack == null).findFirst().orElse(null);
            if (var12 == null) {
               return false;
            }

            int var13 = var11.source.maxStack;
            int var14 = Math.min(var5, var13);
            var12.stack = var6;
            var12.max = var13;
            var12.count = var14;
            var5 -= var14;
         }
      }

      return true;
   }

   private static List<TomStorageBridge.TransferOp> buildPackets(Object var0, Object var1, List<TomStorageBridge.Allocation> var2) {
      ArrayList var3 = new ArrayList();

      for (TomStorageBridge.Allocation var5 : var2) {
         long var6 = var5.source.quantity;
         int var8 = var5.count;

         while (var8 > 0) {
            int var9 = (int)Math.min(var5.source.maxStack, var6);
            if (var9 > 0 && var8 >= var9) {
               var3.add(new TomStorageBridge.PullPacket(var0, var5.source.stored, "SHIFT_PULL", true));
               var8 -= var9;
               var6 -= var9;
            } else {
               int var10 = Math.max(1, var8);

               for (int var11 = 0; var11 < var10; var11++) {
                  var3.add(new TomStorageBridge.PullPacket(var0, var5.source.stored, "PULL_ONE", false));
               }

               var3.add(new TomStorageBridge.PlaceCursorOp(var1, var5.source.itemId, var10, false));
               var8 = 0;
               var6 = Math.max(0L, var6 - var10);
            }
         }
      }

      return var3;
   }

   private static void sendPull(TomStorageBridge.PullPacket var0) throws Exception {
      Class var1 = Class.forName("com.tom.storagemod.util.TerminalSyncManager$SlotAction");
      Enum var2 = Enum.valueOf(var1.asSubclass(Enum.class), var0.action);
      Method var3 = Arrays.stream(var0.sync.getClass().getMethods())
         .filter(var0x -> var0x.getName().equals("sendInteract") && var0x.getParameterCount() == 3)
         .findFirst()
         .orElseThrow();
      var3.invoke(var0.sync, var0.stored, var2, var0.shiftToInventory);
   }

   private static TomStorageBridge.PlaceResult placeCursorInMainInventory(TomStorageBridge.PlaceCursorOp var0) throws Exception {
      Object var1 = handlerCursorStack(var0.menu);
      if (var1 == null) {
         return TomStorageBridge.PlaceResult.ERROR;
      }

      if (!var0.started) {
         if (isEmptyStack(var1)) {
            return var0.bumpWait() ? TomStorageBridge.PlaceResult.WAITING : TomStorageBridge.PlaceResult.ERROR;
         }

         String var2 = registryItemId(itemOf(var1));
         if (!var0.itemId.equals(var2)) {
            return TomStorageBridge.PlaceResult.ERROR;
         }

         if (stackCount(var1) < var0.expectedCount) {
            return var0.bumpWait() ? TomStorageBridge.PlaceResult.WAITING : TomStorageBridge.PlaceResult.ERROR;
         }

         var0.started = true;
         var0.waitTicks = 0;
      }

      if (isEmptyStack(var1)) {
         return TomStorageBridge.PlaceResult.DONE;
      }

      String var5 = registryItemId(itemOf(var1));
      if (!var0.itemId.equals(var5)) {
         return TomStorageBridge.PlaceResult.ERROR;
      }

      int var3 = stackCount(var1);
      if (var0.clickPending) {
         if (var3 >= var0.lastCursorCount) {
            return var0.bumpWait() ? TomStorageBridge.PlaceResult.WAITING : TomStorageBridge.PlaceResult.ERROR;
         }

         var0.clickPending = false;
         var0.waitTicks = 0;
         if (var3 <= 0 || isEmptyStack(var1)) {
            return TomStorageBridge.PlaceResult.DONE;
         }
      }

      int var4 = findMainInventoryDestination(var0.menu, var1);
      if (var4 < 0) {
         return TomStorageBridge.PlaceResult.ERROR;
      }

      var0.lastCursorCount = var3;
      clickMenuSlot(var0.menu, var4);
      var0.clickPending = true;
      return TomStorageBridge.PlaceResult.WAITING;
   }

   private static int findMainInventoryDestination(Object var0, Object var1) throws Exception {
      int var2 = intFieldAny(var0, -1, "playerSlotsStart");
      if (var2 < 0) {
         return -1;
      } else {
         Field var3 = findField(var0.getClass(), "field_7761", "slots");
         var3.setAccessible(true);
         if (!(var3.get(var0) instanceof List var5)) {
            return -1;
         } else {
            int var6 = var2 + 1;
            int var7 = Math.min(var5.size(), var6 + 36);

            for (int var8 = var6; var8 < var7; var8++) {
               Object var9 = slotStack(var5.get(var8));
               if (var9 != null && !isEmptyStack(var9) && stacksCompatible(var9, var1) && stackCount(var9) < stackMax(var9)) {
                  return var8;
               }
            }

            for (int var10 = var6; var10 < var7; var10++) {
               Object var11 = slotStack(var5.get(var10));
               if (var11 == null || isEmptyStack(var11)) {
                  return var10;
               }
            }

            return -1;
         }
      }
   }

   private static Object slotStack(Object var0) throws Exception {
      if (var0 == null) {
         return null;
      }

      Method var1 = findMethodByNames(var0.getClass(), new String[]{"method_7677", "getStack"}, 0);
      return var1.invoke(var0);
   }

   private static void clickMenuSlot(Object var0, int var1) throws Exception {
      Object var2 = minecraftClient();
      Object var3 = clientPlayer(var2);
      Object var4 = clientInteractionManager(var2);
      if (var2 != null && var3 != null && var4 != null) {
         int var5 = intFieldAny(var0, -1, "field_7763", "syncId");
         if (var5 < 0) {
            throw new IllegalStateException("Tom's terminal sync id unavailable");
         }

         Class var6 = loadClass("net.minecraft.class_1713", "net.minecraft.screen.slot.SlotActionType");
         Enum var7 = Enum.valueOf(var6.asSubclass(Enum.class), "PICKUP");
         Method var8 = null;

         for (Class var9 = var4.getClass(); var9 != null && var8 == null; var9 = var9.getSuperclass()) {
            for (Method var13 : var9.getDeclaredMethods()) {
               Class[] var14 = var13.getParameterTypes();
               if (var14.length == 5
                  && var14[0] == int.class
                  && var14[1] == int.class
                  && var14[2] == int.class
                  && var14[3].isInstance(var7)
                  && var14[4].isInstance(var3)) {
                  var13.setAccessible(true);
                  var8 = var13;
                  break;
               }
            }
         }

         if (var8 == null) {
            throw new NoSuchMethodException("ClientPlayerInteractionManager.clickSlot");
         }

         var8.invoke(var4, var5, var1, 0, var7, var3);
      } else {
         throw new IllegalStateException("Client interaction manager unavailable");
      }
   }

   private static Object handlerCursorStack(Object var0) throws Exception {
      Method var1 = findMethodByNames(var0.getClass(), new String[]{"method_34255", "getCursorStack"}, 0);
      return var1.invoke(var0);
   }

   private static Object minecraftClient() {
      try {
         Class var0 = loadClass("net.minecraft.class_310", "net.minecraft.client.MinecraftClient");
         Method var1 = findStaticMethodByNames(var0, new String[]{"method_1551", "getInstance"}, 0);
         return var1.invoke(null);
      } catch (Throwable var2) {
         return null;
      }
   }

   private static Object clientPlayer(Object var0) {
      if (var0 == null) {
         return null;
      }

      try {
         Field var1 = findField(var0.getClass(), "field_1724", "player");
         var1.setAccessible(true);
         return var1.get(var0);
      } catch (Throwable var2) {
         return null;
      }
   }

   private static Object clientInteractionManager(Object var0) {
      if (var0 == null) {
         return null;
      }

      try {
         Field var1 = findField(var0.getClass(), "field_1761", "interactionManager");
         var1.setAccessible(true);
         return var1.get(var0);
      } catch (Throwable var2) {
         return null;
      }
   }

   private static int countInventoryAnyIds(List<Object> var0, List<String> var1) throws Exception {
      HashSet var2 = new HashSet(var1);
      int var3 = 0;

      for (Object var5 : var0) {
         if (var5 != null && !isEmptyStack(var5)) {
            String var6 = registryItemId(itemOf(var5));
            if (var6 != null && var2.contains(var6)) {
               var3 += stackCount(var5);
            }
         }
      }

      return var3;
   }

   private static int countInventoryId(List<Object> var0, String var1) throws Exception {
      int var2 = 0;

      for (Object var4 : var0) {
         if (var4 != null && !isEmptyStack(var4) && var1.equals(registryItemId(itemOf(var4)))) {
            var2 += stackCount(var4);
         }
      }

      return var2;
   }

   private static void addExact(Map<String, TomStorageBridge.Requirement> var0, String var1, String var2, int var3) throws Exception {
      if (var3 > 0 && var1 != null && !var1.isBlank()) {
         Object var4 = resolveItem(var1);
         TomStorageBridge.Requirement var5 = (TomStorageBridge.Requirement)var0.get(var1);
         if (var5 == null) {
            var0.put(var1, new TomStorageBridge.Requirement(var1, var4, var2, var3));
         } else {
            var0.put(var1, new TomStorageBridge.Requirement(var1, var4, var5.label, var5.count + var3));
         }
      }
   }

   private static List<String> discoverItemTagValues(String var0, List<String> var1) {
      LinkedHashSet<String> var2 = new LinkedHashSet<>();
      expandItemTag(var0, var2, new HashSet<>());
      if (var1 != null) {
         var2.addAll(var1);
      }

      return List.copyOf(var2);
   }

   private static void expandItemTag(String var0, LinkedHashSet<String> var1, Set<String> var2) {
      if (var0 != null && var2.add(var0)) {
         String[] var3 = var0.split(":", 2);
         if (var3.length == 2) {
            List<String> var4 = List.of("data/" + var3[0] + "/tags/item/" + var3[1] + ".json", "data/" + var3[0] + "/tags/items/" + var3[1] + ".json");
            ClassLoader var5 = TomStorageBridge.class.getClassLoader();

            for (String var7 : var4) {
               try {
                  Enumeration<URL> var8 = var5.getResources(var7);

                  while (var8.hasMoreElements()) {
                     URL var9 = var8.nextElement();

                     try (InputStream var10 = var9.openStream()) {
                        String var11 = new String(var10.readAllBytes(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                        Matcher var12 = RESOURCE_ID.matcher(var11);

                        while (var12.find()) {
                           String var13 = var12.group(1);
                           if (var13.startsWith("#")) {
                              expandItemTag(var13.substring(1), var1, var2);
                           } else {
                              var1.add(var13);
                           }
                        }
                     }
                  }
               } catch (Throwable var16) {
               }
            }
         }
      }
   }

   private static String registryItemId(Object var0) {
      if (var0 == null) {
         return null;
      }

      try {
         Class var1 = loadClass("net.minecraft.class_7923", "net.minecraft.registry.Registries");
         Field var2 = findField(var1, "field_41178", "ITEM");
         var2.setAccessible(true);
         Object var3 = var2.get(null);
         Method var4 = findMethodByNames(var3.getClass(), new String[]{"method_10221", "getId"}, 1);
         Object var5 = var4.invoke(var3, var0);
         return var5 == null ? null : var5.toString();
      } catch (Throwable var6) {
         return null;
      }
   }

   private static String summarizeAllocations(List<TomStorageBridge.Allocation> var0) {
      Map<String, Integer> var1 = new LinkedHashMap<>();

      for (TomStorageBridge.Allocation var3 : var0) {
         String var4 = var3.source.itemId == null ? var3.label : var3.source.itemId;
         var1.merge(var4, var3.count, Integer::sum);
      }

      List<String> var5 = new ArrayList<>();

      for (Entry<String, Integer> var7 : var1.entrySet()) {
         var5.add(var7.getValue() + "× " + (var7.getKey().contains(":") ? friendlyItemLabel(var7.getKey()) : var7.getKey()));
      }

      return String.join(", ", var5);
   }

   private static Object resolveItem(String var0) throws Exception {
      String[] var1 = var0.split(":", 2);
      if (var1.length != 2) {
         return null;
      }

      Class var2 = loadClass("net.minecraft.class_2960", "net.minecraft.util.Identifier");
      Method var3 = findStaticMethodByNames(var2, new String[]{"method_60655", "of"}, 2);
      Object var4 = var3.invoke(null, var1[0], var1[1]);
      Class var5 = loadClass("net.minecraft.class_7923", "net.minecraft.registry.Registries");
      Field var6 = findField(var5, "field_41178", "ITEM");
      var6.setAccessible(true);
      Object var7 = var6.get(null);
      Method var8 = findMethodByNames(var7.getClass(), new String[]{"method_10223", "get"}, 1);
      Object var9 = var8.invoke(var7, var4);
      if (var9 == null) {
         return null;
      }

      String var10 = var9.toString();
      return !var10.equals("air") && !var10.endsWith(":air") ? var9 : null;
   }

   private static Object itemOf(Object var0) throws Exception {
      return findMethodByNames(var0.getClass(), new String[]{"method_7909", "getItem"}, 0).invoke(var0);
   }

   private static boolean isEmptyStack(Object var0) throws Exception {
      return (Boolean)findMethodByNames(var0.getClass(), new String[]{"method_7960", "isEmpty"}, 0).invoke(var0);
   }

   private static int stackCount(Object var0) throws Exception {
      return ((Number)findMethodByNames(var0.getClass(), new String[]{"method_7947", "getCount"}, 0).invoke(var0)).intValue();
   }

   private static int stackMax(Object var0) throws Exception {
      return ((Number)findMethodByNames(var0.getClass(), new String[]{"method_7914", "getMaxCount"}, 0).invoke(var0)).intValue();
   }

   private static boolean stacksCompatible(Object var0, Object var1) throws Exception {
      Class var2 = var0.getClass();
      Method var3 = findStaticMethodByNames(var2, new String[]{"method_31577", "areItemsAndComponentsEqual"}, 2);
      return (Boolean)var3.invoke(null, var0, var1);
   }

   private static Object terminalMenu(Object var0) throws Exception {
      Field var1;
      try {
         var1 = findField(var0.getClass(), "field_2797", "handler");
      } catch (NoSuchFieldException var4) {
         return null;
      }

      var1.setAccessible(true);
      Object var2 = var1.get(var0);
      if (var2 == null) {
         return null;
      }

      for (Class var3 = var2.getClass(); var3 != null; var3 = var3.getSuperclass()) {
         if (var3.getName().equals("com.tom.storagemod.menu.StorageTerminalMenu")) {
            return var2;
         }
      }

      return null;
   }

   private static void sendMissingChat(int var0, List<TomStorageBridge.MissingItem> var1, boolean var2) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            Object var3 = coloredText("[CobbleSnack] ", "AQUA");
            String var4 = var2
               ? "Storage is missing for the extra " + var0 + " PokéSnack" + (var0 == 1 ? "" : "s") + " batch:"
               : "Still needed for " + var0 + " PokéSnack" + (var0 == 1 ? "" : "s") + ":";
            appendText(var3, coloredText(var4, "RED", "BOLD"));
            sendClientChat(var3);

            for (TomStorageBridge.MissingItem var6 : var1) {
               Object var7 = coloredText("  • ", "DARK_GRAY");
               appendText(var7, coloredText(var6.count + "× ", "YELLOW", "BOLD"));
               appendText(var7, coloredText(var6.label, "GOLD"));
               if (var6.alternatives != null && !var6.alternatives.isBlank() && !var6.alternatives.equals(var6.label)) {
                  appendText(var7, coloredText(" (" + var6.alternatives + ")", "GRAY"));
               }

               sendClientChat(var7);
            }

            sendClientChat(coloredText("  Everything available will still be pulled from Tom's storage.", "GRAY"));
         } catch (Throwable var8) {
         }
      }
   }

   private static void sendInventoryFullChat() {
      try {
         Object var0 = coloredText("[CobbleSnack] ", "AQUA");
         appendText(var0, coloredText("Not enough main-inventory space. ", "RED", "BOLD"));
         appendText(var0, coloredText("Nothing was pulled; the offhand is never used as storage space.", "GRAY"));
         sendClientChat(var0);
      } catch (Throwable var1) {
      }
   }

   private static Object coloredText(String var0, String... var1) throws Exception {
      Class var2 = loadClass("net.minecraft.class_2561", "net.minecraft.text.Text");
      Object var3 = findStaticMethodByNames(var2, new String[]{"method_43470", "literal"}, 1).invoke(null, var0);
      if (var1 != null && var1.length != 0) {
         Class var4 = loadClass("net.minecraft.class_124", "net.minecraft.util.Formatting");
         Method var5 = null;
         Method var6 = null;

         for (Class var7 = var3.getClass(); var7 != null && var5 == null && var6 == null; var7 = var7.getSuperclass()) {
            for (Method var11 : var7.getDeclaredMethods()) {
               if ((var11.getName().equals("method_27692") || var11.getName().equals("formatted")) && var11.getParameterCount() == 1) {
                  Class var12 = var11.getParameterTypes()[0];
                  if (var12 == var4) {
                     var11.setAccessible(true);
                     var5 = var11;
                     break;
                  }

                  if (var12.isArray() && var12.getComponentType() == var4) {
                     var11.setAccessible(true);
                     var6 = var11;
                  }
               }
            }
         }

         if (var5 != null) {
            for (String var18 : var1) {
               Enum var19 = Enum.valueOf(var4.asSubclass(Enum.class), var18);
               var5.invoke(var3, var19);
            }
         } else if (var6 != null) {
            Object var14 = Array.newInstance(var4, var1.length);

            for (int var16 = 0; var16 < var1.length; var16++) {
               Array.set(var14, var16, Enum.valueOf(var4.asSubclass(Enum.class), var1[var16]));
            }

            var6.invoke(var3, var14);
         }

         return var3;
      } else {
         return var3;
      }
   }

   private static void appendText(Object var0, Object var1) throws Exception {
      if (var0 != null && var1 != null) {
         Method var2 = findMethodByNames(var0.getClass(), new String[]{"method_10852", "append"}, 1);
         var2.invoke(var0, var1);
      }
   }

   private static void sendClientChat(Object var0) throws Exception {
      Object var1 = minecraftClient();
      Object var2 = clientPlayer(var1);
      if (var2 != null && var0 != null) {
         Class var3 = loadClass("net.minecraft.class_2561", "net.minecraft.text.Text");
         Method var4 = null;

         for (Class var5 = var2.getClass(); var5 != null && var4 == null; var5 = var5.getSuperclass()) {
            for (Method var9 : var5.getDeclaredMethods()) {
               Class[] var10 = var9.getParameterTypes();
               if (var10.length == 2
                  && var10[1] == boolean.class
                  && var10[0].isAssignableFrom(var3)
                  && (var9.getName().equals("method_7353") || var9.getName().equals("sendMessage"))) {
                  var9.setAccessible(true);
                  var4 = var9;
                  break;
               }
            }
         }

         if (var4 == null) {
            for (Method var14 : var2.getClass().getMethods()) {
               Class[] var15 = var14.getParameterTypes();
               if (var15.length == 2 && var15[1] == boolean.class && var15[0].isInstance(var0)) {
                  var4 = var14;
                  break;
               }
            }
         }

         if (var4 != null) {
            var4.invoke(var2, var0, false);
         }
      }
   }

   private static void clearCursorSafely() {
   }

   private static void closeCalculatorToWorld() {
      try {
         Object var0 = minecraftClient();
         if (var0 == null) {
            return;
         }

         Method var1 = findMethodByNames(var0.getClass(), new String[]{"method_1507", "setScreen"}, 1);
         var1.invoke(var0, (Object)null);
         DiskCacheStore.releaseRuntimeCaches();
         SessionDiagnostics.event("tom-storage-close", "result=success reason=missing-ingredients");
      } catch (Throwable var2) {
         SessionDiagnostics.event(
            "tom-storage-close",
            "result=failed type=" + var2.getClass().getSimpleName() + " message=" + String.valueOf(var2.getMessage())
         );
      }
   }

   private static boolean expectedDeliveriesArrived() {
      try {
         if (transferExpectedInventory.isEmpty()) {
            return true;
         }

         if (transferMenu == null) {
            return false;
         }

         List var0 = playerMainInventory(transferMenu);

         for (Entry var2 : transferExpectedInventory.entrySet()) {
            if (countInventoryId(var0, (String)var2.getKey()) < (Integer)var2.getValue()) {
               return false;
            }
         }

         return true;
      } catch (Throwable var3) {
         return false;
      }
   }

   private static void resetTransferState() {
      transferOwner = null;
      transferMenu = null;
      transferExpectedInventory.clear();
      transferDeliveryWaitTicks = 0;
      sentPackets = 0;
      transferPackets = 0;
      transferItems = 0;
      transferSummary = "";
      transferHadMissing = false;
   }

   private static void setStatus(Object var0, String var1) {
      try {
         setFieldAny(var0, var1, "statusLine");
      } catch (Throwable var3) {
      }
   }

   private static Object makeButton(String var0, int var1, int var2, int var3, int var4, Runnable var5) throws Exception {
      Class var6 = loadClass("net.minecraft.class_2561", "net.minecraft.text.Text");
      Object var7 = findStaticMethodByNames(var6, new String[]{"method_43470", "literal"}, 1).invoke(null, var0);
      Class var8 = loadClass("net.minecraft.class_4185", "net.minecraft.client.gui.widget.ButtonWidget");
      Method var9 = findStaticMethodByNames(var8, new String[]{"method_46430", "builder"}, 2);
      Class var10 = var9.getParameterTypes()[1];
      Object var11 = actionProxy(var10, var5);
      Object var12 = var9.invoke(null, var7, var11);
      Method var13 = findMethodByNames(var12.getClass(), new String[]{"method_46434", "dimensions"}, 4);
      Object var14 = var13.invoke(var12, var1, var2, var3, var4);
      return findMethodByNames(var14.getClass(), new String[]{"method_46431", "build"}, 0).invoke(var14);
   }

   private static Object makeCheckbox(Object var0, String var1, int var2, int var3, int var4, boolean var5, Consumer<Boolean> var6) throws Exception {
      Class var7 = loadClass("net.minecraft.class_2561", "net.minecraft.text.Text");
      Object var8 = findStaticMethodByNames(var7, new String[]{"method_43470", "literal"}, 1).invoke(null, var1);
      Class var9 = loadClass("net.minecraft.class_4286", "net.minecraft.client.gui.widget.CheckboxWidget");
      Class var10 = loadClass("net.minecraft.class_4286$class_8930", "net.minecraft.client.gui.widget.CheckboxWidget$Callback");
      Object var11 = Proxy.newProxyInstance(
         var10.getClassLoader(),
         new Class[]{var10},
         (var1x, var2x, var3x) -> {
            if (var2x.getDeclaringClass() == Object.class) {
               return switch (var2x.getName()) {
                  case "toString" -> "CobbleSnackCheckboxCallback";
                  case "hashCode" -> System.identityHashCode(var1x);
                  case "equals" -> var1x == (var3x == null ? null : var3x[0]);
                  default -> null;
               };
            }

            if (var3x != null && var3x.length >= 2 && var3x[1] instanceof Boolean var4x) {
               var6.accept(var4x);
            }

            return null;
         }
      );
      Object var12 = objectFieldAny(var0, "field_22793", "textRenderer");
      Constructor var13 = null;

      for (Constructor var17 : var9.getDeclaredConstructors()) {
         Class[] var18 = var17.getParameterTypes();
         if (var18.length == 7
            && var18[0] == int.class
            && var18[1] == int.class
            && var18[2] == int.class
            && var18[5] == boolean.class
            && var18[6].isAssignableFrom(var10)) {
            var13 = var17;
            break;
         }
      }

      if (var13 == null) {
         throw new NoSuchMethodException("CheckboxWidget constructor");
      }

      var13.setAccessible(true);
      return var13.newInstance(var2, var3, var4, var8, var12, var5, var11);
   }

   private static void setTooltip(Object var0, String var1) {
      try {
         Class var2 = loadClass("net.minecraft.class_2561", "net.minecraft.text.Text");
         Object var3 = findStaticMethodByNames(var2, new String[]{"method_43470", "literal"}, 1).invoke(null, var1);
         Class var4 = loadClass("net.minecraft.class_7919", "net.minecraft.client.gui.tooltip.Tooltip");
         Object var5 = findStaticMethodByNames(var4, new String[]{"method_47407", "of"}, 1).invoke(null, var3);
         findMethodByNames(var0.getClass(), new String[]{"method_47400", "setTooltip"}, 1).invoke(var0, var5);
      } catch (Throwable var6) {
      }
   }

   private static Object actionProxy(Class<?> var0, Runnable var1) {
      return Proxy.newProxyInstance(
         var0.getClassLoader(),
         new Class[]{var0},
         (var1x, var2, var3) -> {
            if (var2.getDeclaringClass() == Object.class) {
               return switch (var2.getName()) {
                  case "toString" -> "CobbleSnackButtonAction";
                  case "hashCode" -> System.identityHashCode(var1x);
                  case "equals" -> var1x == (var3 == null ? null : var3[0]);
                  default -> null;
               };
            }

            var1.run();
            return null;
         }
      );
   }

   private static void addDrawableChild(Object var0, Object var1) throws Exception {
      Method var2 = findMethodByNames(var0.getClass(), new String[]{"method_37063", "addDrawableChild"}, 1);
      var2.setAccessible(true);
      var2.invoke(var0, var1);
   }

   private static void invokeNoArg(Object var0, String var1) throws Exception {
      Method var2 = findMethodByNames(var0.getClass(), new String[]{var1}, 0);
      var2.setAccessible(true);
      var2.invoke(var0);
   }

   private static Object objectFieldAny(Object var0, String... var1) throws Exception {
      Field var2 = findField(var0.getClass(), var1);
      var2.setAccessible(true);
      return var2.get(var0);
   }

   private static int intFieldAny(Object var0, int var1, String... var2) {
      try {
         Field var3 = findField(var0.getClass(), var2);
         var3.setAccessible(true);
         return ((Number)var3.get(var0)).intValue();
      } catch (Throwable var4) {
         return var1;
      }
   }

   private static Boolean booleanFieldAny(Object var0, String... var1) {
      try {
         Field var2 = findField(var0.getClass(), var1);
         var2.setAccessible(true);
         return (Boolean)var2.get(var0);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static Field publicOrDeclaredField(Object var0, String... var1) throws Exception {
      Field var2 = findField(var0.getClass(), var1);
      var2.setAccessible(true);
      return var2;
   }

   private static void setFieldAny(Object var0, Object var1, String... var2) throws Exception {
      Field var3 = findField(var0.getClass(), var2);
      var3.setAccessible(true);
      var3.set(var0, var1);
   }

   private static Field findField(Class<?> var0, String... var1) throws NoSuchFieldException {
      for (Class var2 = var0; var2 != null; var2 = var2.getSuperclass()) {
         for (String var6 : var1) {
            try {
               return var2.getDeclaredField(var6);
            } catch (NoSuchFieldException var8) {
            }
         }
      }

      throw new NoSuchFieldException(Arrays.toString(var1));
   }

   private static Method findMethodByNames(Class<?> var0, String[] var1, int var2) throws NoSuchMethodException {
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

      for (Method var15 : var0.getMethods()) {
         if (var15.getParameterCount() == var2) {
            for (String var19 : var1) {
               if (var15.getName().equals(var19)) {
                  return var15;
               }
            }
         }
      }

      throw new NoSuchMethodException(var0.getName() + " " + Arrays.toString(var1) + "/" + var2);
   }

   private static Method findStaticMethodByNames(Class<?> var0, String[] var1, int var2) throws NoSuchMethodException {
      for (Method var6 : var0.getDeclaredMethods()) {
         if (Modifier.isStatic(var6.getModifiers()) && var6.getParameterCount() == var2) {
            for (String var10 : var1) {
               if (var6.getName().equals(var10)) {
                  var6.setAccessible(true);
                  return var6;
               }
            }
         }
      }

      throw new NoSuchMethodException(var0.getName() + " static " + Arrays.toString(var1) + "/" + var2);
   }

   private static Class<?> loadClass(String var0, String var1) throws ClassNotFoundException {
      try {
         return Class.forName(var0);
      } catch (ClassNotFoundException var3) {
         return Class.forName(var1);
      }
   }

   private static boolean containsIdentity(List<Object> var0, Object var1) {
      for (Object var3 : var0) {
         if (var3 == var1) {
            return true;
         }
      }

      return false;
   }

   private static String friendlyItemLabel(String var0) {
      String var1 = var0.contains(":") ? var0.substring(var0.indexOf(58) + 1) : var0;
      String[] var2 = var1.split("_");
      StringBuilder var3 = new StringBuilder();

      for (String var7 : var2) {
         if (!var7.isEmpty()) {
            if (!var3.isEmpty()) {
               var3.append(' ');
            }

            var3.append(Character.toUpperCase(var7.charAt(0))).append(var7.substring(1));
         }
      }

      return var3.toString();
   }

   private static String shortError(Throwable var0) {
      Throwable var1 = var0;

      while (var1 instanceof InvocationTargetException) {
         InvocationTargetException var2 = (InvocationTargetException)var1;
         if (var2.getCause() == null) {
            break;
         }

         var1 = var2.getCause();
      }

      String var3 = var1.getMessage();
      return var3 != null && !var3.isBlank() ? var3 : var1.getClass().getSimpleName();
   }

   private record Allocation(String label, TomStorageBridge.StorageSource source, int count) {
   }

   private record MissingItem(int count, String label, String alternatives) {
   }

   private static final class PlaceCursorOp implements TomStorageBridge.TransferOp {
      final Object menu;
      final String itemId;
      final int expectedCount;
      boolean started;
      boolean clickPending;
      int lastCursorCount;
      int waitTicks;

      PlaceCursorOp(Object var1, String var2, int var3, boolean var4) {
         this.menu = var1;
         this.itemId = var2;
         this.expectedCount = var3;
         this.started = var4;
      }

      boolean bumpWait() {
         return ++this.waitTicks <= 40;
      }
   }

   private enum PlaceResult {
      WAITING,
      DONE,
      ERROR;
   }

   private record PullPacket(Object sync, Object stored, String action, boolean shiftToInventory) implements TomStorageBridge.TransferOp {
   }

   private record Requirement(String itemId, Object item, String label, int count) {
   }

   private static final class StorageSource {
      final Object stored;
      final Object stack;
      final Object item;
      final String itemId;
      final long quantity;
      final int maxStack;
      long allocated;

      StorageSource(Object var1, Object var2, Object var3, String var4, long var5, int var7) {
         this.stored = var1;
         this.stack = var2;
         this.item = var3;
         this.itemId = var4;
         this.quantity = var5;
         this.maxStack = var7;
      }

      long remaining() {
         return Math.max(0L, this.quantity - this.allocated);
      }
   }

   private interface TransferOp {
   }

   private static final class VirtualSlot {
      Object stack;
      int count;
      int max;

      VirtualSlot(Object var1, int var2, int var3) {
         this.stack = var1;
         this.count = var2;
         this.max = Math.max(1, var3);
      }
   }
}
