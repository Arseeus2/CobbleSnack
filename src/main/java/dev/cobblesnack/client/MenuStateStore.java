package dev.cobblesnack.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cobblesnack.calc.Seasoning;
import dev.cobblesnack.data.SpeciesInfo;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public final class MenuStateStore {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("cobblesnack.json");
   private static MenuStateStore.Config config;

   private MenuStateStore() {
   }

   public static synchronized boolean rememberEnabled() {
      return loadConfig().rememberMenuState;
   }

   public static synchronized void setRememberEnabled(boolean var0) {
      MenuStateStore.Config var1 = loadConfig();
      var1.rememberMenuState = var0;
      if (!var0) {
         var1.menuState = null;
      }

      saveConfig(var1);
   }

   public static synchronized boolean requireBiteReducer() {
      return loadConfig().requireBiteReducer;
   }

   public static synchronized void setRequireBiteReducer(boolean var0) {
      MenuStateStore.Config var1 = loadConfig();
      var1.requireBiteReducer = var0;
      saveConfig(var1);
   }

   public static synchronized boolean practicalHabitats() {
      MenuStateStore.Config var0 = loadConfig();
      return var0.practicalHabitats == null || var0.practicalHabitats;
   }

   public static synchronized void setPracticalHabitats(boolean var0) {
      MenuStateStore.Config var1 = loadConfig();
      var1.practicalHabitats = var0;
      saveConfig(var1);
   }

   public static synchronized boolean avoidEnchantedGoldenApple() {
      return Boolean.TRUE.equals(loadConfig().avoidEnchantedGoldenApple);
   }

   public static synchronized void setAvoidEnchantedGoldenApple(boolean var0) {
      MenuStateStore.Config var1 = loadConfig();
      var1.avoidEnchantedGoldenApple = var0;
      saveConfig(var1);
   }

   public static synchronized boolean maximizeShinyChance() {
      return Boolean.TRUE.equals(loadConfig().maximizeShinyChance);
   }

   public static synchronized void setMaximizeShinyChance(boolean var0) {
      MenuStateStore.Config var1 = loadConfig();
      var1.maximizeShinyChance = var0;
      saveConfig(var1);
   }

   public static synchronized boolean cyclePokemonSprites() {
      MenuStateStore.Config var0 = loadConfig();
      return var0.cyclePokemonSprites == null || var0.cyclePokemonSprites;
   }

   public static synchronized void setCyclePokemonSprites(boolean var0) {
      MenuStateStore.Config var1 = loadConfig();
      var1.cyclePokemonSprites = var0;
      saveConfig(var1);
   }

   public static synchronized MenuStateStore.BrowserPreferences browserPreferences() {
      MenuStateStore.Config var0 = loadConfig();
      String var1 = var0.pokemonSortMode != null && !var0.pokemonSortMode.isBlank() ? var0.pokemonSortMode : "POKEDEX";
      return new MenuStateStore.BrowserPreferences(var1, var0.pokemonNamesShown, var0.pokemonFormsOnly, var0.pokemonRegionGrouped);
   }

   public static synchronized void setBrowserPreferences(String var0, boolean var1, boolean var2, boolean var3) {
      MenuStateStore.Config var4 = loadConfig();
      var4.pokemonSortMode = var0 != null && !var0.isBlank() ? var0 : "POKEDEX";
      var4.pokemonNamesShown = var1;
      var4.pokemonFormsOnly = var2;
      var4.pokemonRegionGrouped = var3;
      saveConfig(var4);
   }

   public static synchronized boolean isFavoritePokemon(String var0) {
      String var1 = SpeciesInfo.normalize(var0);
      return var1.isBlank() ? false : loadConfig().favoritePokemon.stream().anyMatch(var1x -> SpeciesInfo.normalize(var1x).equals(var1));
   }

   public static synchronized Set<String> favoritePokemonKeys() {
      MenuStateStore.Config var0 = loadConfig();
      LinkedHashSet var1 = new LinkedHashSet();
      if (var0.favoritePokemon != null) {
         for (String var3 : var0.favoritePokemon) {
            String var4 = SpeciesInfo.normalize(var3);
            if (!var4.isBlank()) {
               var1.add(var4);
            }
         }
      }

      return Set.copyOf(var1);
   }

   public static synchronized boolean toggleFavoritePokemon(String var0) {
      String var2 = SpeciesInfo.normalize(var0);
      if (var2.isBlank()) {
         return false;
      }

      MenuStateStore.Config var3 = loadConfig();
      if (var3.favoritePokemon == null) {
         var3.favoritePokemon = new ArrayList<>();
      }

      boolean var1;
      if (!(var1 = var3.favoritePokemon.removeIf(var1x -> SpeciesInfo.normalize(var1x).equals(var2)))) {
         var3.favoritePokemon.add(var2);
      }

      saveConfig(var3);
      return !var1;
   }

   public static synchronized MenuStateStore.Snapshot loadSnapshot() {
      MenuStateStore.Config var0 = loadConfig();
      return var0.rememberMenuState && var0.menuState != null ? var0.menuState.copy() : null;
   }

   public static synchronized void saveSnapshot(MenuStateStore.Snapshot var0) {
      MenuStateStore.Config var1 = loadConfig();
      if (var1.rememberMenuState) {
         var1.menuState = var0 == null ? null : var0.copy();
         saveConfig(var1);
      }
   }

   public static synchronized void clearSnapshot() {
      MenuStateStore.Config var0 = loadConfig();
      var0.menuState = null;
      saveConfig(var0);
   }

   private static MenuStateStore.Config loadConfig() {
      if (config != null) {
         return config;
      }

      if (Files.isRegularFile(CONFIG_FILE)) {
         try (BufferedReader var0 = Files.newBufferedReader(CONFIG_FILE)) {
            MenuStateStore.Config var1 = GSON.fromJson(var0, MenuStateStore.Config.class);
            if (var1 != null) {
               if (var1.favoritePokemon == null) {
                  var1.favoritePokemon = new ArrayList<>();
               }

               if (var1.cyclePokemonSprites == null) {
                  var1.cyclePokemonSprites = Boolean.TRUE;
               }

               if (var1.practicalHabitats == null) {
                  var1.practicalHabitats = Boolean.TRUE;
               }

               config = var1;
               return var1;
            }
         } catch (Exception var6) {
         }
      }

      config = new MenuStateStore.Config();
      saveConfig(config);
      return config;
   }

   private static void saveConfig(MenuStateStore.Config var0) {
      config = var0;

      try {
         Files.createDirectories(CONFIG_FILE.getParent());

         try (BufferedWriter var1 = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(var0, var1);
         }
      } catch (IOException var6) {
      }
   }

   public record BrowserPreferences(String pokemonSortMode, boolean pokemonNamesShown, boolean pokemonFormsOnly, boolean pokemonRegionGrouped) {
   }

   private static final class Config {
      boolean rememberMenuState = true;
      boolean requireBiteReducer = false;
      Boolean practicalHabitats = Boolean.TRUE;
      Boolean avoidEnchantedGoldenApple = Boolean.FALSE;
      Boolean maximizeShinyChance = Boolean.FALSE;
      Boolean cyclePokemonSprites = Boolean.TRUE;
      String pokemonSortMode = "POKEDEX";
      boolean pokemonNamesShown = false;
      boolean pokemonFormsOnly = false;
      boolean pokemonRegionGrouped = false;
      List<String> favoritePokemon = new ArrayList<>();
      MenuStateStore.Snapshot menuState;
   }

   public static final class Snapshot {
      public String pokemonKey;
      public String pokemonForm;
      public String biomeId;
      public List<String> seasonings = new ArrayList<>();
      public List<String> outputLines = new ArrayList<>();
      public String statusLine = "";
      public int giveAmount = 1;
      public String environmentIdentity = "";

      public MenuStateStore.Snapshot copy() {
         MenuStateStore.Snapshot var1 = new MenuStateStore.Snapshot();
         var1.pokemonKey = this.pokemonKey;
         var1.pokemonForm = this.pokemonForm;
         var1.biomeId = this.biomeId;
         var1.seasonings = this.seasonings == null ? new ArrayList<>() : new ArrayList<>(this.seasonings);
         var1.outputLines = this.outputLines == null ? new ArrayList<>() : new ArrayList<>(this.outputLines);
         var1.statusLine = this.statusLine == null ? "" : this.statusLine;
         var1.giveAmount = Math.max(1, Math.min(64, this.giveAmount));
         var1.environmentIdentity = this.environmentIdentity == null ? "" : this.environmentIdentity;
         return var1;
      }

      public static MenuStateStore.Snapshot of(String var0, String var1, String var2, Seasoning[] var3, List<String> var4, String var5, int var6) {
         return of(var0, var1, var2, var3, var4, var5, var6, "");
      }

      public static MenuStateStore.Snapshot of(String var0, String var1, String var2, Seasoning[] var3, List<String> var4, String var5, int var6, String var7) {
         MenuStateStore.Snapshot var8 = new MenuStateStore.Snapshot();
         var8.pokemonKey = var0;
         var8.pokemonForm = var1;
         var8.biomeId = var2;
         if (var3 != null) {
            for (Seasoning var12 : var3) {
               var8.seasonings.add((var12 == null ? Seasoning.NONE : var12).name());
            }
         }

         if (var4 != null) {
            var8.outputLines.addAll(var4);
         }

         var8.statusLine = var5 == null ? "" : var5;
         var8.giveAmount = Math.max(1, Math.min(64, var6));
         var8.environmentIdentity = var7 == null ? "" : var7;
         return var8;
      }
   }
}
