package dev.cobblesnack.calc;

import dev.cobblesnack.cache.SessionDiagnostics;
import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList.Named;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.gen.structure.Structure;

public final class StructureSelectorPolicy {
   private static final Object LOCK = new Object();
   private static volatile StructureSelectorPolicy.RuntimeState runtimeState = StructureSelectorPolicy.RuntimeState.disconnected();
   private static volatile String lastAuditKey = "";
   private static volatile long nextFallbackRefreshNanos;

   private StructureSelectorPolicy() {
   }

   public static boolean isUsable(String var0) {
      return isUsable(var0, currentState());
   }

   static boolean isUsable(String var0, boolean var1) {
      return fallbackIsUsable(normalize(var0), var1);
   }

   public static boolean routeIsAvailable(SpawnEntry var0) {
      return var0 != null && (!var0.hasStructureConstraint() || routeIsUsable(var0));
   }

   public static boolean routeIsUsable(SpawnEntry var0) {
      return routeIsUsable(var0, currentState());
   }

   public static boolean routesMayOverlap(SpawnEntry var0, SpawnEntry var1) {
      if (var0 != null && var1 != null && var0.hasStructureConstraint() && var1.hasStructureConstraint()) {
         List<String> var2 = rawSelectors(var0);
         List<String> var3 = rawSelectors(var1);
         if (!var2.isEmpty() && !var3.isEmpty()) {
            for (String var5 : var2) {
               for (String var7 : var3) {
                  if (selectorsMayOverlap(var5, var7)) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean routeIsUsable(SpawnEntry var0, StructureSelectorPolicy.RuntimeState var1) {
      if (var0 != null && var0.hasStructureConstraint()) {
         for (SpawnCondition var3 : var0.conditions) {
            if (!var3.structures.isEmpty() && var3.structures.stream().noneMatch(var1x -> isUsable(var1x, var1))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static List<String> displaySelectors(Collection<SpawnEntry> var0) {
      LinkedHashSet var1 = new LinkedHashSet();
      if (var0 == null) {
         return List.of();
      }

      StructureSelectorPolicy.RuntimeState var2 = currentState();

      for (SpawnEntry var4 : var0) {
         if (var4 != null) {
            for (SpawnCondition var6 : var4.conditions) {
               for (String var8 : var6.structures) {
                  if (isUsable(var8, var2)) {
                     var1.add(displaySelector(var8));
                  }
               }
            }
         }
      }

      return List.copyOf(var1);
   }

   public static String displaySummary(List<String> var0) {
      if (var0 != null && !var0.isEmpty()) {
         List var1 = var0.stream().map(StructureSelectorPolicy::displayName).distinct().toList();
         return String.join(", ", var1);
      } else {
         return "";
      }
   }

   public static void logAudit(DataIndex var0) {
      if (var0 != null) {
         StructureSelectorPolicy.RuntimeState var1 = currentState();
         String var2 = System.identityHashCode(var0) + ":" + var1.identity() + ":" + var1.source() + ":" + var1.registeredIds().hashCode();
         if (!var2.equals(lastAuditKey)) {
            lastAuditKey = var2;
            LinkedHashSet var3 = new LinkedHashSet();
            LinkedHashSet var4 = new LinkedHashSet();
            LinkedHashSet var5 = new LinkedHashSet();
            ArrayList var6 = new ArrayList();
            int var7 = 0;

            for (SpawnEntry var9 : var0.spawns()) {
               if (var9.hasStructureConstraint()) {
                  var7++;

                  for (SpawnCondition var11 : var9.conditions) {
                     for (String var13 : var11.structures) {
                        String var14 = normalize(var13);
                        var3.add(var14);
                        (isUsable(var14, var1) ? var4 : var5).add(var14);
                     }
                  }

                  if (!routeIsUsable(var9, var1)) {
                     var6.add(var9.id);
                  }
               }
            }

            SessionDiagnostics.event(
               "structure-registry",
               "connection="
                  + var1.identity()
                  + " source="
                  + var1.source()
                  + " definitions="
                  + var1.registeredIds().size()
                  + " placed="
                  + var1.placedIds().size()
                  + " structureSets="
                  + var1.structureSetCount()
            );
            SessionDiagnostics.event(
               "structure-coverage",
               "selectors="
                  + var3.size()
                  + " structureRoutes="
                  + var7
                  + " resolvedSelectors="
                  + var4.size()
                  + " unavailableSelectors="
                  + var5.size()
                  + " disabledRoutes="
                  + var6.size()
            );
            SessionDiagnostics.event("structure-resolved", "ids=" + String.join(",", var4));
            SessionDiagnostics.event("structure-unavailable", "ids=" + String.join(",", var5));
            SessionDiagnostics.event("structure-disabled-routes", "ids=" + String.join(",", var6));
         }
      }
   }

   private static boolean isUsable(String var0, StructureSelectorPolicy.RuntimeState var1) {
      String var2 = normalize(var0);
      if (var2.isBlank()) {
         return false;
      }

      if (!var1.connected()) {
         return fallbackIsUsable(var2, aetherLoaded());
      }

      Set<String> var3 = var1.placementAware() ? var1.placedIds() : var1.registeredIds();
      if (!var2.startsWith("#")) {
         return var3.contains(var2);
      }

      if (var1.structureRegistry() == null) {
         return tagMatchesCandidates(var2, var3);
      }

      try {
         Identifier var4 = Identifier.of(var2.substring(1));
         TagKey var5 = TagKey.of(RegistryKeys.STRUCTURE, var4);
         Optional<Named<Structure>> var6 = var1.structureRegistry().getEntryList(var5);
         if (var6.isEmpty()) {
            return false;
         }

         for (RegistryEntry<Structure> var8 : var6.get()) {
            Identifier var9 = var1.structureRegistry().getId(var8.value());
            if (var9 != null && var3.contains(var9.toString())) {
               return true;
            }
         }
      } catch (Throwable var10) {
      }

      return false;
   }

   static boolean tagMatchesCandidates(String var0, Set<String> var1) {
      String var2 = normalize(var0);
      if (var2.startsWith("#") && var1 != null && !var1.isEmpty()) {
         String var3 = var2.substring(1);
         int var4 = var3.indexOf(58);
         String var5 = var4 >= 0 ? var3.substring(0, var4) : "minecraft";
         String var6 = var4 >= 0 ? var3.substring(var4 + 1) : var3;
         if (var6.endsWith("s") && var6.length() > 1) {
            var6 = var6.substring(0, var6.length() - 1);
         }

         String var7 = var6;

         for (String var9 : var1) {
            String var10 = normalize(var9);
            int var11 = var10.indexOf(58);
            if (var11 >= 0 && var10.substring(0, var11).equals(var5)) {
               String var12 = var10.substring(var11 + 1);
               if (var12.equals(var7) || var12.startsWith(var7 + "_") || var12.startsWith(var7 + "/") || var12.contains(var7)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   static boolean selectorsMayOverlap(String var0, String var1) {
      String var2 = normalize(var0);
      String var3 = normalize(var1);
      if (!var2.isBlank() && !var3.isBlank() && !var2.equals(var3)) {
         if (var2.contains("village") && var3.contains("village")) {
            return true;
         }

         if (var2.startsWith("#") && !var3.startsWith("#") && tagMatchesCandidates(var2, Set.of(var3))) {
            return true;
         }

         if (var3.startsWith("#") && !var2.startsWith("#") && tagMatchesCandidates(var3, Set.of(var2))) {
            return true;
         }

         String var4 = selectorPath(var2);
         String var5 = selectorPath(var3);

         for (String var9 : var4.split("[/_-]+")) {
            if (var9.length() >= 4 && var5.contains(var9)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static List<String> rawSelectors(SpawnEntry var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (SpawnCondition var3 : var0.conditions) {
         for (String var5 : var3.structures) {
            String var6 = normalize(var5);
            if (!var6.isBlank()) {
               var1.add(var6);
            }
         }
      }

      return List.copyOf(var1);
   }

   private static String selectorPath(String var0) {
      String var1 = var0.startsWith("#") ? var0.substring(1) : var0;
      int var2 = var1.indexOf(58);
      return var2 >= 0 ? var1.substring(var2 + 1) : var1;
   }

   private static boolean fallbackIsUsable(String var0, boolean var1) {
      if (var0.isBlank() || var0.equals("minecraft:desert_well") || var0.equals("minecraft:woodland_mansion")) {
         return false;
      } else {
         return !var0.startsWith("#aether:") && !var0.startsWith("aether:") ? true : var1;
      }
   }

   private static StructureSelectorPolicy.RuntimeState currentState() {
      MinecraftClient var0;
      try {
         var0 = MinecraftClient.getInstance();
      } catch (Throwable var8) {
         return StructureSelectorPolicy.RuntimeState.disconnected();
      }

      if (var0 != null && var0.world != null) {
         String var10 = "world-" + Integer.toHexString(System.identityHashCode(var0.world));
         StructureSelectorPolicy.RuntimeState var2 = runtimeState;
         long var3 = System.nanoTime();
         if (!var10.equals(var2.identity()) || !var2.connected() && var3 >= nextFallbackRefreshNanos) {
            synchronized (LOCK) {
               var2 = runtimeState;
               var3 = System.nanoTime();
               if (!var10.equals(var2.identity()) || !var2.connected() && var3 >= nextFallbackRefreshNanos) {
                  runtimeState = readRuntimeState(var0, var10);
                  nextFallbackRefreshNanos = runtimeState.connected() ? 0L : var3 + 5000000000L;
                  lastAuditKey = "";
                  return runtimeState;
               } else {
                  return var2;
               }
            }
         } else {
            return var2;
         }
      } else {
         if (runtimeState.connected()) {
            synchronized (LOCK) {
               runtimeState = StructureSelectorPolicy.RuntimeState.disconnected();
               lastAuditKey = "";
            }
         }

         return runtimeState;
      }
   }

   private static StructureSelectorPolicy.RuntimeState readRuntimeState(MinecraftClient var0, String var1) {
      try {
         Registry<Structure> var2 = var0.world.getRegistryManager().get(RegistryKeys.STRUCTURE);
         Registry<StructureSet> var13 = var0.world.getRegistryManager().get(RegistryKeys.STRUCTURE_SET);
         LinkedHashSet<String> var4 = new LinkedHashSet<>();
         var2.getIds().stream().map(Object::toString).sorted().forEach(var4::add);
         LinkedHashSet<String> var5 = new LinkedHashSet<>();
         int var6 = 0;

         for (StructureSet var8 : var13) {
            var6++;

            for (WeightedEntry var10 : var8.structures()) {
               Identifier var11 = var2.getId(var10.structure().value());
               if (var11 != null) {
                  var5.add(var11.toString());
               }
            }
         }

         return new StructureSelectorPolicy.RuntimeState(
            var1, true, var6 > 0 ? "synced-registry+placements" : "synced-registry", var2, Set.copyOf(var4), Set.copyOf(var5), var6
         );
      } catch (Throwable var12) {
         StructureSelectorPolicy.RuntimeState var3 = readExplorerCompassState(var1);
         if (var3 != null) {
            SessionDiagnostics.event(
               "structure-registry-provider", "clientRegistry=unavailable provider=explorers-compass reason=" + var12.getClass().getSimpleName()
            );
            return var3;
         } else {
            SessionDiagnostics.event("structure-registry-provider", "clientRegistry=unavailable provider=fallback reason=" + var12.getClass().getSimpleName());
            return new StructureSelectorPolicy.RuntimeState(var1, false, "fallback", null, Set.of(), Set.of(), 0);
         }
      }
   }

   private static StructureSelectorPolicy.RuntimeState readExplorerCompassState(String var0) {
      try {
         if (!FabricLoader.getInstance().isModLoaded("explorerscompass")) {
            return null;
         } else {
            Class var1 = Class.forName("com.chaosthedude.explorerscompass.ExplorersCompass", false, StructureSelectorPolicy.class.getClassLoader());
            Field var2 = var1.getField("synced");
            if (!var2.getBoolean(null)) {
               return null;
            } else {
               Field var3 = var1.getField("allowedStructureIDs");
               if (var3.get(null) instanceof Collection<?> var5) {
                  LinkedHashSet<String> var6 = new LinkedHashSet<>();
                  var5.stream().map(Object::toString).map(StructureSelectorPolicy::normalize).sorted().forEach(var6::add);
                  Set<String> var7 = Set.copyOf(var6);
                  return new StructureSelectorPolicy.RuntimeState(var0, true, "explorers-compass-sync", null, var7, var7, 0);
               } else {
                  return null;
               }
            }
         }
      } catch (Throwable var8) {
         return null;
      }
   }

   private static String displaySelector(String var0) {
      String var1 = normalize(var0);
      return var1.contains("village") ? "#minecraft:village" : var1;
   }

   private static String displayName(String var0) {
      String var1 = normalize(var0);
      if (var1.contains("village")) {
         return "Village";
      }

      String var2 = var1.startsWith("#") ? var1.substring(1) : var1;

      try {
         Identifier var3 = Identifier.of(var2);
         String var4 = Util.createTranslationKey("structure", var3);
         if (I18n.hasTranslation(var4)) {
            return I18n.translate(var4);
         }
      } catch (Throwable var12) {
      }

      int var13 = var2.indexOf(58);
      String var14 = var13 >= 0 ? var2.substring(var13 + 1) : var2;
      int var5 = var14.lastIndexOf(47);
      if (var5 >= 0) {
         var14 = var14.substring(var5 + 1);
      }

      StringBuilder var6 = new StringBuilder();
      boolean var7 = true;

      for (char var11 : var14.toCharArray()) {
         if (var11 == '_' || var11 == '-') {
            var11 = ' ';
         }

         if (var7 && Character.isLetter(var11)) {
            var6.append(Character.toUpperCase(var11));
            var7 = false;
         } else {
            var6.append(var11);
         }

         if (var11 == ' ') {
            var7 = true;
         }
      }

      return var6.isEmpty() ? "Required structure" : var6.toString();
   }

   private static String normalize(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
      if (var1.isBlank()) {
         return "";
      }

      boolean var2 = var1.startsWith("#");
      String var3 = var2 ? var1.substring(1) : var1;
      if (!var3.contains(":")) {
         var3 = "minecraft:" + var3;
      }

      return var2 ? "#" + var3 : var3;
   }

   private static boolean aetherLoaded() {
      try {
         return FabricLoader.getInstance().isModLoaded("aether");
      } catch (Throwable var1) {
         return false;
      }
   }

   private record RuntimeState(
      String identity,
      boolean connected,
      String source,
      Registry<Structure> structureRegistry,
      Set<String> registeredIds,
      Set<String> placedIds,
      int structureSetCount
   ) {
      private RuntimeState {
         registeredIds = registeredIds == null ? Set.of() : Set.copyOf(registeredIds);
         placedIds = placedIds == null ? Set.of() : Set.copyOf(placedIds);
      }

      private boolean placementAware() {
         return this.structureSetCount > 0;
      }

      private static StructureSelectorPolicy.RuntimeState disconnected() {
         return new StructureSelectorPolicy.RuntimeState("disconnected", false, "fallback", null, Set.of(), Set.of(), 0);
      }
   }
}
