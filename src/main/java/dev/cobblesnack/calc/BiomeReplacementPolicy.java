package dev.cobblesnack.calc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cobblesnack.cache.SessionDiagnostics;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.Registry;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.biome.Biome;

public final class BiomeReplacementPolicy {
   private static final Object LOCK = new Object();
   private static final String REPLACER_CONFIG = "biome_replacer.properties";
   private static final String AUTOMODPACK_CONFIG = "automodpack/automodpack-client.json";
   private static final String AUTOMODPACK_CONTENT = "automodpack-content.json";
   private static volatile BiomeReplacementPolicy.State state = BiomeReplacementPolicy.State.initial();

   private BiomeReplacementPolicy() {
   }

   public static void refreshForCurrentConnection() {
      refresh(MinecraftClient.getInstance());
   }

   public static void refresh(MinecraftClient var0) {
      BiomeReplacementPolicy.ConnectionContext var1 = connectionContext(var0);
      BiomeReplacementPolicy.State var2 = state;
      if (!var2.connectionId().equals(var1.connectionId())) {
         synchronized (LOCK) {
            var2 = state;
            if (!var2.connectionId().equals(var1.connectionId())) {
               BiomeReplacementPolicy.State var4 = loadState(var1);
               state = var4;
               BiomeCatalog.clearCache();
               BestPokeSnackOptimizer.clearCache();
               SessionDiagnostics.event(
                  "biome-replacements",
                  "connection="
                     + var4.connectionId()
                     + " source="
                     + var4.source()
                     + " selectors="
                     + var4.sourceSelectors().size()
                     + " cache="
                     + var4.cacheIdentity()
                     + " ids="
                     + String.join(",", var4.sourceSelectors())
               );
            }
         }
      }
   }

   public static String cacheIdentity() {
      return state.cacheIdentity();
   }

   public static List<String> sourceSelectors() {
      return state.sourceSelectors();
   }

   public static boolean isReplacedSource(Registry<Biome> var0, String var1) {
      List<String> var2 = state.sourceSelectors();
      if (var2.isEmpty()) {
         return false;
      }

      Optional<BiomeMatcher> var3 = BiomeMatcher.create(var0, var1);
      if (var3.isEmpty()) {
         return false;
      }

      for (String var5 : var2) {
         if (var3.get().matchesSelector(var5)) {
            return true;
         }
      }

      return false;
   }

   public static List<String> parseSourceSelectors(List<String> var0) {
      LinkedHashSet<String> var1 = new LinkedHashSet<>();
      if (var0 == null) {
         return List.of();
      }

      for (String var3 : var0) {
         String var4 = var3 == null ? "" : var3.trim();
         if (!var4.isEmpty() && !var4.startsWith("!") && !var4.startsWith("#") && !var4.contains("=")) {
            String[] var5 = var4.split(">", 2);
            if (var5.length == 2) {
               String var6 = normalizeBiomeSelector(var5[0]);
               if (!var6.isBlank()) {
                  var1.add(var6);
               }
            }
         }
      }

      ArrayList var7 = new ArrayList(var1);
      var7.sort(String.CASE_INSENSITIVE_ORDER);
      return List.copyOf(var7);
   }

   public static String normalizeServerAddress(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);

      while (var1.endsWith(".")) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      if (var1.isBlank()) {
         return "";
      } else if (var1.startsWith("[")) {
         int var4 = var1.indexOf(93);
         return var4 >= 0 && var4 + 1 == var1.length() ? var1 + ":25565" : var1;
      } else {
         long var2 = var1.chars().filter(var0x -> var0x == 58).count();
         return var2 == 0L ? var1 + ":25565" : var1;
      }
   }

   private static BiomeReplacementPolicy.State loadState(BiomeReplacementPolicy.ConnectionContext var0) {
      if (var0.disconnected()) {
         return new BiomeReplacementPolicy.State(var0.connectionId(), "disconnected", List.of(), var0.connectionId() + "|none");
      }

      String var1 = "none";
      boolean var2 = false;
      if (var0.singleplayer()) {
         var2 = FabricLoader.getInstance().isModLoaded("biome_replacer");
         var1 = var2 ? "integrated-server" : "none";
      } else if (!var0.serverAddress().isBlank()) {
         String var3 = autoModpackWithBiomeReplacer(var0.serverAddress());
         if (var3 != null) {
            var2 = true;
            var1 = "automodpack:" + var3;
         }
      }

      List var5 = var2 ? readConfiguredSelectors() : List.of();
      String var4 = shortHash(String.join("|", var5));
      return new BiomeReplacementPolicy.State(var0.connectionId(), var1, var5, var0.connectionId() + "|" + var4);
   }

   private static BiomeReplacementPolicy.ConnectionContext connectionContext(MinecraftClient var0) {
      if (var0 == null || var0.world == null) {
         return new BiomeReplacementPolicy.ConnectionContext("disconnected", false, true, "");
      }

      if (var0.isIntegratedServerRunning()) {
         String var6 = "singleplayer";

         try {
            IntegratedServer var7 = var0.getServer();
            if (var7 != null) {
               String var8 = String.valueOf(var7.getRunDirectory().toAbsolutePath().normalize());
               String var4 = var7.getSaveProperties() == null ? "" : var7.getSaveProperties().getLevelName();
               var6 = var8 + "|" + var4;
            }
         } catch (Throwable var5) {
         }

         return new BiomeReplacementPolicy.ConnectionContext("singleplayer:" + shortHash(var6), true, false, "");
      } else {
         ServerInfo var1 = var0.getCurrentServerEntry();
         String var2 = normalizeServerAddress(var1 == null ? "" : var1.address);
         String var3 = var2.isBlank() ? "multiplayer:unknown" : "multiplayer:" + shortHash(var2);
         return new BiomeReplacementPolicy.ConnectionContext(var3, false, false, var2);
      }
   }

   private static List<String> readConfiguredSelectors() {
      Path var0 = FabricLoader.getInstance().getConfigDir().resolve("biome_replacer.properties");

      try {
         return !Files.isRegularFile(var0) ? List.of() : parseSourceSelectors(Files.readAllLines(var0, StandardCharsets.UTF_8));
      } catch (Exception var2) {
         SessionDiagnostics.error("biome-replacer-config", var2);
         return List.of();
      }
   }

   private static String autoModpackWithBiomeReplacer(String var0) {
      Path var1 = FabricLoader.getInstance().getGameDir();
      Path var2 = var1.resolve("automodpack/automodpack-client.json");

      try (BufferedReader var3 = Files.newBufferedReader(var2, StandardCharsets.UTF_8)) {
         JsonObject var4 = JsonParser.parseReader(var3).getAsJsonObject();
         JsonObject var5 = object(var4, "installedModpacks");
         if (var5 == null) {
            return null;
         }

         for (String var7 : var5.keySet()) {
            JsonObject var8 = var5.getAsJsonObject(var7);
            if (addressMatches(var0, string(var8, "serverAddress")) || addressMatches(var0, string(var8, "hostAddress"))) {
               Path var9 = var1.resolve("automodpack").resolve("modpacks").normalize();
               Path var10 = var9.resolve(var7).normalize();
               if (var10.startsWith(var9) && contentListsBiomeReplacer(var10.resolve("automodpack-content.json"))) {
                  return var7;
               }
            }
         }
      } catch (Exception var14) {
      }

      return null;
   }

   private static boolean contentListsBiomeReplacer(Path var0) {
      try (BufferedReader var1 = Files.newBufferedReader(var0, StandardCharsets.UTF_8)) {
         JsonObject var2 = JsonParser.parseReader(var1).getAsJsonObject();
         JsonArray var3 = var2.getAsJsonArray("list");
         if (var3 == null) {
            return false;
         }

         for (JsonElement var5 : var3) {
            if (var5.isJsonObject()) {
               String var6 = string(var5.getAsJsonObject(), "file").toLowerCase(Locale.ROOT);
               if (var6.contains("biomereplacer") || var6.contains("biome-replacer") || var6.contains("biome_replacer")) {
                  return true;
               }
            }
         }
      } catch (Exception var10) {
      }

      return false;
   }

   private static boolean addressMatches(String var0, String var1) {
      String var2 = normalizeServerAddress(var0);
      String var3 = normalizeServerAddress(var1);
      return !var2.isBlank() && var2.equals(var3);
   }

   private static JsonObject object(JsonObject var0, String var1) {
      JsonElement var2 = var0 == null ? null : var0.get(var1);
      return var2 != null && var2.isJsonObject() ? var2.getAsJsonObject() : null;
   }

   private static String string(JsonObject var0, String var1) {
      JsonElement var2 = var0 == null ? null : var0.get(var1);
      return var2 != null && !var2.isJsonNull() ? var2.getAsString() : "";
   }

   private static String normalizeBiomeSelector(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT);
      if (var1.isBlank()) {
         return "";
      }

      boolean var2 = var1.startsWith("#");
      String var3 = var2 ? var1.substring(1).trim() : var1;
      if (var3.isBlank()) {
         return "";
      }

      if (!var3.contains(":")) {
         var3 = "minecraft:" + var3;
      }

      return var2 ? "#" + var3 : var3;
   }

   private static String shortHash(String var0) {
      try {
         byte[] var1 = MessageDigest.getInstance("SHA-256").digest((var0 == null ? "" : var0).getBytes(StandardCharsets.UTF_8));
         StringBuilder var2 = new StringBuilder(16);

         for (int var3 = 0; var3 < 8; var3++) {
            var2.append(String.format(Locale.ROOT, "%02x", var1[var3] & 255));
         }

         return var2.toString();
      } catch (Exception var4) {
         return Integer.toHexString(String.valueOf(var0).hashCode());
      }
   }

   private record ConnectionContext(String connectionId, boolean singleplayer, boolean disconnected, String serverAddress) {
   }

   private record State(String connectionId, String source, List<String> sourceSelectors, String cacheIdentity) {
      private State {
         sourceSelectors = sourceSelectors == null ? List.of() : List.copyOf(sourceSelectors);
      }

      private static BiomeReplacementPolicy.State initial() {
         return new BiomeReplacementPolicy.State("uninitialized", "none", List.of(), "uninitialized|none");
      }
   }
}
