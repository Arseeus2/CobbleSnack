package dev.cobblesnack.client;

import dev.cobblesnack.cache.DiskCacheStore;
import dev.cobblesnack.cache.SessionDiagnostics;
import dev.cobblesnack.calc.BestPokeSnackOptimizer;
import dev.cobblesnack.calc.BiomeCatalog;
import dev.cobblesnack.calc.BiomeMatcher;
import dev.cobblesnack.calc.BiomeReplacementPolicy;
import dev.cobblesnack.calc.HabitatPolicy;
import dev.cobblesnack.calc.Seasoning;
import dev.cobblesnack.calc.SpawnCalculator;
import dev.cobblesnack.calc.SpawnEnvironmentProfile;
import dev.cobblesnack.calc.StructureEstimateOptimizer;
import dev.cobblesnack.calc.StructureResultPolicy;
import dev.cobblesnack.calc.StructureSelectorPolicy;
import dev.cobblesnack.calc.StructureSnackAdvisor;
import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList.Named;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public final class SnackCalculatorScreen extends Screen {
   private static final int PANEL_WIDTH = 760;
   private static final String ALT_ACTION_SEPARATOR = "\u001f";
   private static final ExecutorService OPTIMIZER_EXECUTOR = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1 = new Thread(var0, "CobbleSnack Optimizer");
      var1.setDaemon(true);
      var1.setPriority(1);
      return var1;
   });
   private final Screen parent;
   private SpeciesInfo selectedPokemon;
   private String selectedSpawnForm;
   private String selectedBiomeId;
   private List<String> availableBiomes = List.of();
   private ButtonWidget pokemonButton;
   private ButtonWidget calculateButton;
   private ButtonWidget biomeButton;
   private ButtonWidget currentBiomeButton;
   private ButtonWidget deselectPokemonButton;
   private ButtonWidget rememberButton;
   private ButtonWidget cycleSpritesButton;
   private ButtonWidget reloadButton;
   private ButtonWidget biteReducerButton;
   private ButtonWidget habitatButton;
   private ButtonWidget avoidEnchantedAppleButton;
   private ButtonWidget shinyChanceButton;
   private ButtonWidget giveSnackButton;
   private ButtonWidget copyItemButton;
   private ButtonWidget giveMinusButton;
   private TextFieldWidget giveAmountField;
   private ButtonWidget givePlusButton;
   private final List<ButtonWidget> givePresetButtons = new ArrayList<>();
   private ButtonWidget simulateButton;
   private ButtonWidget clearInputsButton;
   private ButtonWidget pullIngredientsButton;
   private ButtonWidget pullFullRecipeButton;
   private ItemStack snackPreviewStack = ItemStack.EMPTY;
   private List<Seasoning> snackPreviewSignature = List.of();
   private int snackPreviewX;
   private int snackPreviewY;
   private int biomeHeaderX;
   private int searchClearX;
   private int searchClearY;
   private int searchClearSize;
   private final Seasoning[] selected = new Seasoning[]{Seasoning.NONE, Seasoning.NONE, Seasoning.NONE};
   private final List<ButtonWidget> seasoningButtons = new ArrayList<>();
   private List<String> outputLines = List.of();
   private String statusLine = "";
   private List<Seasoning> resultSeasoningSignature = List.of();
   private String hoveredResultAction;
   private boolean calculating;
   private boolean restoredRememberedState;
   private int giveAmount = 1;
   private static final int BASE_MAIN_CELL_SIZE = 48;
   private static final int MAIN_CELL_GAP = 2;
   private static final int MAIN_ICON_SIZE = 32;
   private static final int REGION_HEADER_HEIGHT = 20;
   private static final int SEASONING_ICON_BOX = 20;
   private static final int MIN_PRESENTATION_WIDTH = 800;
   private static final int MIN_PRESENTATION_HEIGHT = 450;
   private TextFieldWidget pokemonSearchField;
   private ButtonWidget pokemonSortButton;
   private ButtonWidget pokemonFavoritesButton;
   private ButtonWidget pokemonNamesButton;
   private ButtonWidget pokemonFormsButton;
   private ButtonWidget pokemonRegionButton;
   private final List<SnackCalculatorScreen.RenderedPokemonCell> renderedPokemonCells = new ArrayList<>();
   private List<SpeciesInfo> mainPokemonFiltered = List.of();
   private List<SnackCalculatorScreen.PokemonLayoutRow> pokemonLayoutRows = List.of();
   private SnackCalculatorScreen.PokemonSortMode pokemonSortMode = SnackCalculatorScreen.PokemonSortMode.POKEDEX;
   private SnackCalculatorScreen.PokemonFavoriteMode pokemonFavoriteMode = SnackCalculatorScreen.PokemonFavoriteMode.OFF;
   private boolean pokemonFormsOnly;
   private boolean pokemonRegionGrouped;
   private boolean pokemonNamesShown;
   private boolean pokemonSpritesCycle;
   private boolean pokemonSortMenuOpen;
   private int pokemonGridX;
   private int pokemonGridTop;
   private int pokemonGridBottom;
   private int pokemonGridWidth;
   private int pokemonColumns;
   private int pokemonCellWidth;
   private int pokemonCellSize = 48;
   private double pokemonScrollPixels;
   private double pokemonScrollTarget;
   private long pokemonLastScrollNanos;
   private boolean draggingPokemonScrollbar;
   private int pokemonLowerControlsY;
   private int uiLeft;
   private int uiTop;
   private int uiWidth;
   private int leftPanelWidth;
   private int rightPanelX;
   private int rightPanelWidth;
   private int resultsX;
   private int resultsY;
   private int resultsWidth;
   private int resultsHeight;
   private int resultsScrollOffset;
   private boolean draggingResultsScrollbar;
   private Set<String> selectableFormPokemonKeys;
   private int presentationVirtualWidth;
   private int presentationVirtualHeight;
   private double presentationScale = 1.0;
   private boolean refreshingVisualLayout;

   public SnackCalculatorScreen() {
      this(null);
   }

   public SnackCalculatorScreen(Screen var1) {
      super(Text.literal("CobbleSnack - Poké Snack Calculator"));
      this.parent = var1;
      this.pokemonSpritesCycle = MenuStateStore.cyclePokemonSprites();
      BiomeReplacementPolicy.refreshForCurrentConnection();
      MenuStateStore.BrowserPreferences var2 = MenuStateStore.browserPreferences();

      try {
         this.pokemonSortMode = SnackCalculatorScreen.PokemonSortMode.valueOf(var2.pokemonSortMode());
      } catch (IllegalArgumentException var4) {
         this.pokemonSortMode = SnackCalculatorScreen.PokemonSortMode.POKEDEX;
      }

      this.pokemonNamesShown = var2.pokemonNamesShown();
      this.pokemonFormsOnly = var2.pokemonFormsOnly();
      this.pokemonRegionGrouped = var2.pokemonRegionGrouped();
      this.restoreRememberedState();
   }

   @Override
   protected void init() {
      this.configurePresentationSpace();
      String var1 = this.pokemonSearchField == null ? "" : this.pokemonSearchField.getText();
      this.uiWidth = Math.max(280, this.width - 16);
      this.uiLeft = Math.max(8, (this.width - this.uiWidth) / 2);
      this.uiTop = 24;
      boolean var2 = TomStorageBridge.isTomTerminalScreen(this.parent);
      this.pokemonCellSize = PokemonZoomOverlay.boxSize();
      int var3 = this.longestSeasoningButtonWidth();
      int var4 = Math.max(144, this.textRenderer.getWidth("Select biome for Pokémon...") + 16);
      int var5 = Math.max(var3 + 30 + 10 + var4, var2 ? 326 : 290);
      this.rightPanelWidth = Math.max(250, Math.min(var5, Math.max(250, this.uiWidth - 270 - 12)));
      this.leftPanelWidth = Math.max(220, this.uiWidth - this.rightPanelWidth - 12);
      int var6 = Math.max(1, this.leftPanelWidth - 7);
      int var7 = Math.max(1, (var6 + 2) / (this.pokemonCellSize + 2));
      int var8 = var7 * (this.pokemonCellSize + 2) - 2;
      if (var8 + 7 >= 220 && var8 < var6) {
         this.leftPanelWidth = var8 + 7;
      }

      this.rightPanelX = this.uiLeft + this.leftPanelWidth + 12;
      this.rightPanelWidth = Math.max(1, this.uiWidth - this.leftPanelWidth - 12);
      if (this.outputLines.isEmpty()) {
         this.statusLine = PerformanceWarmup.species().size() + " Pokémon detected";
         this.outputLines = List.of(
            "Select seasonings and a biome to simulate a snack.",
            "Or select a Pokémon and click Calculate best PokéSnack to auto-fill the best recipe and biome."
         );
      }

      int var9 = this.uiTop + 18;
      byte var10 = 68;
      byte var11 = 20;
      byte var12 = 4;
      int var13 = Math.min(260, Math.max(112, this.leftPanelWidth - var10 - var11 * 4 - var12 * 5));
      this.pokemonSearchField = new TextFieldWidget(this.textRenderer, this.uiLeft, var9, var13, 20, Text.literal("Search Pokémon"));
      this.pokemonSearchField.setMaxLength(100);
      this.pokemonSearchField.setPlaceholder(Text.literal("Search Pokémon..."));
      this.pokemonSearchField.setChangedListener(var1x -> {
         this.resetPokemonScroll();
         this.rebuildMainPokemonFilter();
      });
      this.addDrawableChild(this.pokemonSearchField);
      this.searchClearSize = 16;
      this.searchClearX = this.uiLeft + var13 - this.searchClearSize - 2;
      this.searchClearY = var9 + 2;
      int var14 = this.uiLeft + var13 + var12;
      this.pokemonSortButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.pokemonSortMenuOpen = !this.pokemonSortMenuOpen).dimensions(var14, var9, var10, 20).build()
      );
      int var15 = var14 + var10 + var12;
      this.pokemonNamesButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("N"), var1x -> {
         this.pokemonNamesShown = !this.pokemonNamesShown;
         this.saveBrowserPreferences();
         this.updateMainPokemonControls();
      }).dimensions(var15, var9, var11, 20).build());
      int var16 = var15 + var11 + var12;
      this.pokemonFormsButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("F"), var1x -> {
         this.pokemonFormsOnly = !this.pokemonFormsOnly;
         this.saveBrowserPreferences();
         this.resetPokemonScroll();
         this.rebuildMainPokemonFilter();
      }).dimensions(var16, var9, var11, 20).build());
      int var17 = var16 + var11 + var12;
      this.pokemonRegionButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("R"), var1x -> {
         this.pokemonRegionGrouped = !this.pokemonRegionGrouped;
         this.saveBrowserPreferences();
         this.resetPokemonScroll();
         this.rebuildMainPokemonFilter();
      }).dimensions(var17, var9, var11, 20).build());
      int var18 = var17 + var11 + var12;
      this.pokemonFavoritesButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), var1x -> {
         this.pokemonFavoriteMode = this.pokemonFavoriteMode.next();
         this.resetPokemonScroll();
         this.rebuildMainPokemonFilter();
      }).dimensions(var18, var9, var11, 20).build());
      this.pokemonGridX = this.uiLeft;
      this.pokemonGridTop = var9 + 26;
      this.pokemonGridBottom = Math.max(this.pokemonGridTop + 48, this.height - 86);
      this.pokemonGridWidth = this.leftPanelWidth - 7;
      this.pokemonColumns = Math.max(1, (this.pokemonGridWidth + 2) / (this.pokemonCellSize + 2));
      this.pokemonCellWidth = this.pokemonCellSize;
      int var19 = this.pokemonLowerControlsY = this.pokemonGridBottom + 30;
      byte var20 = 20;
      byte var21 = 20;
      byte var22 = 20;
      byte var23 = 20;
      int var24 = Math.max(112, this.textRenderer.getWidth("Calculate best PokéSnack") + 14);
      byte var25 = 20;
      int var26 = Math.max(108, Math.min(this.longestSelectedPokemonButtonWidth(), this.leftPanelWidth - var25 - var24 - var20 - var21 - var22 - var23 - 30));
      int var27 = this.uiLeft + var25 + 4;
      this.deselectPokemonButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("×"), var1x -> this.deselectPokemon()).dimensions(this.uiLeft, var19, var25, 20).build()
      );
      this.deselectPokemonButton.active = this.selectedPokemon != null;
      this.pokemonButton = this.addDrawableChild(
         ButtonWidget.builder(this.selectedPokemonButtonText(), var1x -> this.openSelectedFormPicker()).dimensions(var27, var19, var26, 20).build()
      );
      this.pokemonButton.active = this.selectedPokemon != null && !this.allFormChoices(this.selectedPokemon).isEmpty();
      int var28 = var27 + var26 + 6;
      this.calculateButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal(this.calculating ? "Calculating..." : "Calculate best PokéSnack"), var1x -> this.calculateBestPokeSnack())
            .dimensions(var28, var19, var24, 20)
            .build()
      );
      this.calculateButton.active = !this.calculating;
      int var29 = var28 + var24 + 4;
      this.biteReducerButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.toggleBiteReducerRequirement()).dimensions(var29, var19, var20, 20).build()
      );
      int var30 = var29 + var20 + 4;
      this.habitatButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.togglePracticalHabitats()).dimensions(var30, var19, var21, 20).build()
      );
      int var31 = var30 + var21 + 4;
      this.avoidEnchantedAppleButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.toggleAvoidEnchantedGoldenApple()).dimensions(var31, var19, var22, 20).build()
      );
      int var32 = var31 + var22 + 4;
      this.shinyChanceButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.toggleMaximizeShinyChance()).dimensions(var32, var19, var23, 20).build()
      );
      int var33 = this.uiTop + 36;
      int var34 = Math.max(72, Math.min(var3, Math.max(72, this.rightPanelWidth - 122)));
      int var35 = var34 + 30;
      this.seasoningButtons.clear();

      for (int var36 = 0; var36 < 3; var36++) {
         int var37 = var36;
         ButtonWidget var38 = ButtonWidget.builder(Text.literal(shortName(this.selected[var36])), var2x -> this.openSeasoningPicker(var37))
            .dimensions(this.rightPanelX + 20 + 10, var33 + var36 * 28, var34, 20)
            .build();
         this.seasoningButtons.add(var38);
         this.addDrawableChild(var38);
      }

      int var55 = this.rightPanelX + var35 + 10;
      int var56 = Math.max(44, this.rightPanelX + this.rightPanelWidth - var55);
      this.biomeHeaderX = var55 + 4;
      this.biomeButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal(this.biomeButtonText()), var1x -> this.openBiomePicker()).dimensions(var55, var33, var56, 20).build()
      );
      this.currentBiomeButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Current biome"), var1x -> this.useCurrentBiome())
            .dimensions(var55, var33 + 28, Math.max(20, var56 - 26), 20)
            .build()
      );
      this.clearInputsButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("×"), var1x -> {
         TomStorageBridge.clearInputs(this);
         this.refreshButtonLabels();
      }).dimensions(var55 + var56 - 22, var33 + 28, 22, 20).build());
      int var57 = var33 + 92;
      int[] var39 = new int[]{1, 16, 32, 64};
      int[] var40 = new int[]{18, 24, 24, 24};
      byte var41 = 3;
      this.snackPreviewX = this.rightPanelX + 2;
      this.snackPreviewY = var57;
      int var42 = this.snackPreviewX + 26;
      boolean var43 = this.rightPanelWidth < (var2 ? 314 : 278);
      int var44 = this.textRenderer.getWidth("Give") + 12;
      this.giveSnackButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Give"), var1x -> this.giveSelectedSnack()).dimensions(var42, var57, var44, 20).build()
      );
      int var45 = var42 + var44 + 3;
      this.copyItemButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.copySelectedSnackItem()).dimensions(var45, var57, 20, 20).build()
      );
      int var46 = var45 + 23;
      if (var2) {
         int var47 = this.textRenderer.getWidth("Pull") + 12;
         this.pullIngredientsButton = this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Pull"), var1x -> TomStorageBridge.beginPull(this)).dimensions(var46, var57, var47, 20).build()
         );
         var46 += var47 + 3;
      } else {
         this.pullIngredientsButton = null;
         this.pullFullRecipeButton = null;
      }

      int var58 = var43 ? var57 + 24 : var57;
      int var48 = var43 ? this.rightPanelX + 2 : var46;
      this.giveMinusButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("-"), var1x -> this.changeGiveAmount(-1)).dimensions(var48, var58, 18, 20).build()
      );
      int var49 = var48 + 21;
      this.giveAmountField = new TextFieldWidget(this.textRenderer, var49, var58, 24, 20, Text.literal("Give amount"));
      this.giveAmountField.setMaxLength(2);
      this.giveAmountField.setText(String.valueOf(this.giveAmount));
      this.giveAmountField.setChangedListener(this::onGiveAmountTyped);
      this.addDrawableChild(this.giveAmountField);
      int var50 = var49 + 27;
      this.givePlusButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("+"), var1x -> this.changeGiveAmount(1)).dimensions(var50, var58, 18, 20).build()
      );
      int var51 = var50 + 24;
      this.givePresetButtons.clear();

      for (int var52 = 0; var52 < var39.length; var52++) {
         int var53 = var39[var52];
         ButtonWidget var54 = this.addDrawableChild(
            ButtonWidget.builder(Text.literal(String.valueOf(var53)), var2x -> this.setGiveAmount(var53))
               .dimensions(var51, var58 + 1, var40[var52], 18)
               .build()
         );
         this.givePresetButtons.add(var54);
         var51 += var40[var52] + var41;
      }

      int var59 = var58 + 28;
      this.simulateButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Simulate snack"), var1x -> this.simulate()).dimensions(this.rightPanelX + 2, var59, 112, 20).build()
      );
      if (var2) {
         this.pullFullRecipeButton = this.addDrawableChild(
            ButtonWidget.builder(Text.empty(), var1x -> this.togglePullFullRecipeAmount()).dimensions(this.rightPanelX + 120, var59, 126, 20).build()
         );
      }

      this.resultsX = this.rightPanelX;
      this.resultsY = var59 + 32;
      this.resultsWidth = this.rightPanelWidth;
      this.resultsHeight = Math.max(100, this.height - this.resultsY - 38);
      this.rememberButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.toggleRememberState()).dimensions(this.uiLeft, this.height - 28, 20, 20).build()
      );
      this.cycleSpritesButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.toggleSpriteCycling()).dimensions(this.uiLeft + 24, this.height - 28, 20, 20).build()
      );
      if (!this.refreshingVisualLayout) {
         MinimapSpriteResolver.auditInstalledMappings();
      }

      int var60 = this.uiLeft + 48;
      this.reloadButton = this.addDrawableChild(
         ButtonWidget.builder(Text.empty(), var1x -> this.reloadData()).dimensions(var60, this.height - 28, 20, 20).build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Done"), var1x -> this.close()).dimensions(this.uiLeft + this.uiWidth - 50, this.height - 28, 50, 20).build()
      );
      if (this.availableBiomes.isEmpty()) {
         this.refreshAvailableBiomes();
      }

      if (this.selectedBiomeId != null && !this.availableBiomes.contains(this.selectedBiomeId)) {
         this.selectedBiomeId = null;
         if (this.hasComputedResults()) {
            this.statusLine = "Saved biome is not available in this world/server.";
            this.outputLines = List.of("Your Pokémon and seasonings were kept. Calculate again for an accessible location.");
            this.resultsScrollOffset = 0;
         }
      }

      DataIndex var61 = DataIndex.get();
      if (!this.refreshingVisualLayout) {
         StructureSelectorPolicy.logAudit(var61);
      }

      if (this.statusLine.isBlank() && !this.restoredRememberedState) {
         this.statusLine = var61.browserSpecies().size() + " wild Pokémon detected";
         this.outputLines = List.of(
            "Select seasonings and a biome to simulate a snack.",
            "Or select a Pokémon and click Calculate best PokéSnack to auto-fill the best seasonings and biome."
         );
      }

      if (!var1.isEmpty()) {
         this.pokemonSearchField.setText(var1);
      }

      this.rebuildMainPokemonFilter();
      this.refreshButtonLabels();
   }

   private void configurePresentationSpace() {
      if (!this.refreshingVisualLayout
         || this.presentationVirtualWidth <= 0
         || this.presentationVirtualHeight <= 0
         || this.width != this.presentationVirtualWidth
         || this.height != this.presentationVirtualHeight) {
         double var1 = 2.0;

         try {
            var1 = Math.max(1.0, this.client.getWindow().getScaleFactor());
         } catch (Throwable var9) {
         }

         double var3 = this.width * var1;
         double var5 = this.height * var1;
         double var7 = Math.min(2.0, Math.min(var3 / 800.0, var5 / 450.0));
         var7 = Math.max(0.1, var7);
         this.presentationScale = var7 / var1;
         this.presentationVirtualWidth = Math.max(1, (int)Math.round(this.width / this.presentationScale));
         this.presentationVirtualHeight = Math.max(1, (int)Math.round(this.height / this.presentationScale));
         this.width = this.presentationVirtualWidth;
         this.height = this.presentationVirtualHeight;
      }
   }

   private double presentationMouseX(double var1) {
      return var1 / Math.max(1.0E-4, this.presentationScale);
   }

   private double presentationMouseY(double var1) {
      return var1 / Math.max(1.0E-4, this.presentationScale);
   }

   private int presentationClipFloor(int var1) {
      return (int)Math.floor(var1 * this.presentationScale);
   }

   private int presentationClipCeil(int var1) {
      return (int)Math.ceil(var1 * this.presentationScale);
   }

   private void rebuildMainPokemonFilter() {
      if (this.pokemonSearchField != null) {
         String var1 = SpeciesInfo.normalize(this.pokemonSearchField.getText());
         Set<String> var2 = MenuStateStore.favoritePokemonKeys();
         Set<String> var3 = this.pokemonFormsOnly ? this.selectableFormPokemonKeys() : Set.of();
         List<SnackCalculatorScreen.MainScoredPokemon> var4 = new ArrayList<>();

         for (SpeciesInfo var6 : PerformanceWarmup.species()) {
            int var7;
            if ((!this.pokemonFormsOnly || var3.contains(var6.key()))
               && (this.pokemonFavoriteMode != SnackCalculatorScreen.PokemonFavoriteMode.ONLY || var2.contains(var6.key()))
               && (var7 = var1.isBlank() ? 0 : mainPokemonScore(var6, var1)) < 1000) {
               var4.add(new SnackCalculatorScreen.MainScoredPokemon(var6, var7));
            }
         }

         Comparator<SnackCalculatorScreen.MainScoredPokemon> var9 = this.mainPokemonComparator(this.pokemonSortMode);
         Comparator<SnackCalculatorScreen.MainScoredPokemon> var10 = var9;
         if (this.pokemonFavoriteMode == SnackCalculatorScreen.PokemonFavoriteMode.FIRST) {
            var10 = Comparator.<SnackCalculatorScreen.MainScoredPokemon>comparingInt(var1x -> var2.contains(var1x.info().key()) ? 0 : 1).thenComparing(var10);
         }

         if (this.pokemonRegionGrouped) {
            var10 = Comparator.<SnackCalculatorScreen.MainScoredPokemon>comparingInt(var0 -> regionOrder(var0.info())).thenComparing(var10);
         }

         this.mainPokemonFiltered = var4.stream().sorted(var10).map(SnackCalculatorScreen.MainScoredPokemon::info).toList();
         this.rebuildPokemonLayoutRows();
         double var11 = this.mainPokemonMaxScrollPixels();
         this.pokemonScrollTarget = Math.max(0.0, Math.min(var11, this.pokemonScrollTarget));
         this.pokemonScrollPixels = Math.max(0.0, Math.min(var11, this.pokemonScrollPixels));
         this.updateMainPokemonControls();
         this.updatePokemonLowerControlsPosition();
      }
   }

   private Comparator<SnackCalculatorScreen.MainScoredPokemon> mainPokemonComparator(SnackCalculatorScreen.PokemonSortMode var1) {
      Comparator var2 = Comparator.comparingInt(SpeciesInfo::nationalPokedexNumber).thenComparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER);

      Comparator var3 = switch (var1) {
         case POKEDEX -> var2;
         case NAME_AZ -> Comparator.comparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER);
         case NAME_ZA -> Comparator.comparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER).reversed();
      };
      return Comparator.comparing(SnackCalculatorScreen.MainScoredPokemon::info, var3);
   }

   private Set<String> selectableFormPokemonKeys() {
      if (this.selectableFormPokemonKeys != null) {
         return this.selectableFormPokemonKeys;
      }

      HashSet var1 = new HashSet<>(DataIndex.get().selectableSpawnFormSpeciesKeys());
      var1.remove("spinda");
      this.selectableFormPokemonKeys = Set.copyOf(var1);
      return this.selectableFormPokemonKeys;
   }

   private void rebuildPokemonLayoutRows() {
      ArrayList var1 = new ArrayList();
      if (!this.pokemonRegionGrouped) {
         for (int var2 = 0; var2 < this.mainPokemonFiltered.size(); var2 += this.pokemonColumns) {
            var1.add(
               SnackCalculatorScreen.PokemonLayoutRow.pokemon(
                  this.mainPokemonFiltered.subList(var2, Math.min(var2 + this.pokemonColumns, this.mainPokemonFiltered.size())), this.pokemonCellSize + 2
               )
            );
         }
      } else {
         String var7 = null;
         ArrayList var3 = new ArrayList();

         for (SpeciesInfo var5 : this.mainPokemonFiltered) {
            String var6 = regionFor(var5);
            if (!Objects.equals(var7, var6)) {
               if (var7 != null) {
                  this.appendRegionRows(var1, var7, var3);
               }

               var7 = var6;
               var3 = new ArrayList();
            }

            var3.add(var5);
         }

         if (var7 != null) {
            this.appendRegionRows(var1, var7, var3);
         }
      }

      this.pokemonLayoutRows = List.copyOf(var1);
   }

   private void appendRegionRows(List<SnackCalculatorScreen.PokemonLayoutRow> var1, String var2, List<SpeciesInfo> var3) {
      var1.add(SnackCalculatorScreen.PokemonLayoutRow.header(var2));
      int var4 = 0;

      while (var4 < var3.size()) {
         var1.add(
            SnackCalculatorScreen.PokemonLayoutRow.pokemon(var3.subList(var4, Math.min(var4 + this.pokemonColumns, var3.size())), this.pokemonCellSize + 2)
         );
         var4 += this.pokemonColumns;
      }
   }

   private void updateMainPokemonControls() {
      if (this.pokemonSortButton != null) {
         this.pokemonSortButton.setMessage(Text.literal(this.pokemonSortMode.label + " ▾"));
      }

      if (this.pokemonNamesButton != null) {
         this.pokemonNamesButton.setMessage(Text.literal("N"));
      }

      if (this.pokemonFormsButton != null) {
         this.pokemonFormsButton.setMessage(Text.literal("F"));
      }

      if (this.pokemonRegionButton != null) {
         this.pokemonRegionButton.setMessage(Text.literal("R"));
      }

      if (this.pokemonFavoritesButton != null) {
         this.pokemonFavoritesButton.setMessage(Text.empty());
      }
   }

   private void selectMainPokemon(SpeciesInfo var1) {
      if (var1 != null) {
         if (this.selectedPokemon != null && this.selectedPokemon.key().equals(var1.key())) {
            this.deselectPokemon();
         } else {
            this.selectPokemon(var1);
         }
      }
   }

   private void deselectPokemon() {
      if (this.selectedPokemon != null) {
         this.selectedPokemon = null;
         this.selectedSpawnForm = null;
         this.refreshAvailableBiomes();
         this.statusLine = PerformanceWarmup.species().size() + " Pokémon detected";
         this.outputLines = List.of(
            "Select seasonings and a biome to simulate a snack.",
            "Or select a Pokémon and click Calculate best PokéSnack to auto-fill the best recipe and biome."
         );
         this.refreshButtonLabels();
         this.rebuildMainPokemonFilter();
         this.saveRememberedState();
      }
   }

   private void updatePokemonLowerControlsPosition() {
      int var1;
      this.pokemonLowerControlsY = var1 = Math.min(this.pokemonGridBottom, this.currentVisiblePokemonContentBottom()) + 30;
      if (this.pokemonButton != null) {
         this.pokemonButton.setY(var1);
      }

      if (this.calculateButton != null) {
         this.calculateButton.setY(var1);
      }

      if (this.deselectPokemonButton != null) {
         this.deselectPokemonButton.setY(var1);
      }

      if (this.biteReducerButton != null) {
         this.biteReducerButton.setY(var1);
      }

      if (this.habitatButton != null) {
         this.habitatButton.setY(var1);
      }

      if (this.avoidEnchantedAppleButton != null) {
         this.avoidEnchantedAppleButton.setY(var1);
      }

      if (this.shinyChanceButton != null) {
         this.shinyChanceButton.setY(var1);
      }
   }

   private void onPokemonBoxScaleChanged() {
      int var1 = PokemonZoomOverlay.boxSize();
      if (var1 != this.pokemonCellSize) {
         if (!this.refreshingVisualLayout) {
            this.refreshingVisualLayout = true;

            try {
               this.clearChildren();
               this.init();
            } finally {
               this.refreshingVisualLayout = false;
            }
         }
      }
   }

   private int currentVisiblePokemonContentBottom() {
      if (this.pokemonLayoutRows.isEmpty()) {
         return this.pokemonGridTop;
      }

      int var1 = this.pokemonLayoutRows.stream().mapToInt(SnackCalculatorScreen.PokemonLayoutRow::height).sum();
      return Math.max(this.pokemonGridTop, Math.min(this.pokemonGridBottom, this.pokemonGridTop + var1 - (int)Math.round(this.pokemonScrollPixels)));
   }

   private int mainPokemonTotalRows() {
      return this.pokemonLayoutRows.size();
   }

   private int mainPokemonMaxScrollPixels() {
      int var1 = Math.max(1, this.pokemonGridBottom - this.pokemonGridTop);
      int var2 = this.pokemonLayoutRows.stream().mapToInt(SnackCalculatorScreen.PokemonLayoutRow::height).sum();
      return Math.max(0, var2 - var1);
   }

   private void resetPokemonScroll() {
      this.pokemonScrollPixels = 0.0;
      this.pokemonScrollTarget = 0.0;
      this.pokemonLastScrollNanos = 0L;
   }

   private void animatePokemonScroll() {
      int var1 = this.mainPokemonMaxScrollPixels();
      this.pokemonScrollTarget = Math.max(0.0, Math.min(var1, this.pokemonScrollTarget));
      long var2 = System.nanoTime();
      if (this.pokemonLastScrollNanos == 0L) {
         this.pokemonLastScrollNanos = var2;
      }

      double var4 = Math.min(0.05, Math.max(0.0, (var2 - this.pokemonLastScrollNanos) / 1.0E9));
      this.pokemonLastScrollNanos = var2;
      double var6 = 1.0 - Math.exp(-14.0 * var4);
      this.pokemonScrollPixels = this.pokemonScrollPixels + (this.pokemonScrollTarget - this.pokemonScrollPixels) * var6;
      if (Math.abs(this.pokemonScrollTarget - this.pokemonScrollPixels) < 0.1) {
         this.pokemonScrollPixels = this.pokemonScrollTarget;
      }

      this.pokemonScrollPixels = Math.max(0.0, Math.min(var1, this.pokemonScrollPixels));
   }

   private static int mainPokemonScore(SpeciesInfo var0, String var1) {
      String var3 = SpeciesInfo.normalize(var0.displayName());
      String var4 = SpeciesInfo.normalize(var0.key());
      if (var3.equals(var1) || var4.equals(var1)) {
         return 0;
      }

      if (!var3.startsWith(var1) && !var4.startsWith(var1)) {
         int var5 = var3.indexOf(var1);
         int var6 = var4.indexOf(var1);
         int var2 = var5 >= 0 ? var5 : var6;
         if (var2 >= 0) {
            return 100 + var2;
         }

         if (var1.length() >= 3) {
            int var9 = Math.min(mainLevenshtein(var1, var3), mainLevenshtein(var1, var4));
            int var8 = var1.length() <= 5 ? 2 : 3;
            if (var9 <= var8) {
               return 300 + var9 * 20 + Math.abs(var3.length() - var1.length());
            }
         }

         return 1000;
      } else {
         return 10 + Math.min(var3.length(), var4.length()) - var1.length();
      }
   }

   private static int mainLevenshtein(String var0, String var1) {
      int[] var2 = new int[var1.length() + 1];
      int[] var3 = new int[var1.length() + 1];
      int var4 = 0;

      while (var4 <= var1.length()) {
         var2[var4] = var4++;
      }

      for (int var7 = 1; var7 <= var0.length(); var7++) {
         var3[0] = var7;

         for (int var5 = 1; var5 <= var1.length(); var5++) {
            int var6 = var0.charAt(var7 - 1) == var1.charAt(var5 - 1) ? 0 : 1;
            var3[var5] = Math.min(Math.min(var3[var5 - 1] + 1, var2[var5] + 1), var2[var5 - 1] + var6);
         }

         int[] var8 = var2;
         var2 = var3;
         var3 = var8;
      }

      return var2[var1.length()];
   }

   private String mainPokemonLabel(SpeciesInfo var1) {
      if (this.pokemonNamesShown) {
         return var1.displayName();
      } else {
         return var1.nationalPokedexNumber() == Integer.MAX_VALUE ? "—" : String.format(Locale.ROOT, "#%03d", var1.nationalPokedexNumber());
      }
   }

   private static String regionFor(SpeciesInfo var0) {
      int var1 = var0 == null ? Integer.MAX_VALUE : var0.nationalPokedexNumber();
      if (var1 < 1) {
         return "Other";
      } else if (var1 <= 151) {
         return "Kanto";
      } else if (var1 <= 251) {
         return "Johto";
      } else if (var1 <= 386) {
         return "Hoenn";
      } else if (var1 <= 493) {
         return "Sinnoh";
      } else if (var1 <= 649) {
         return "Unova";
      } else if (var1 <= 721) {
         return "Kalos";
      } else if (var1 <= 809) {
         return "Alola";
      } else if (var1 <= 898) {
         return "Galar";
      } else if (var1 <= 905) {
         return "Hisui";
      } else if ((var1 < 1011 || var1 > 1017) && var1 != 1025) {
         return var1 <= 1024 ? "Paldea" : "Other";
      } else {
         return "Kitakami";
      }
   }

   private static int regionOrder(SpeciesInfo var0) {
      return switch (regionFor(var0)) {
         case "Kanto" -> 0;
         case "Johto" -> 1;
         case "Hoenn" -> 2;
         case "Sinnoh" -> 3;
         case "Unova" -> 4;
         case "Kalos" -> 5;
         case "Alola" -> 6;
         case "Galar" -> 7;
         case "Hisui" -> 8;
         case "Paldea" -> 9;
         case "Kitakami" -> 10;
         default -> 11;
      };
   }

   private String trimToPixelWidth(String var1, int var2) {
      if (var1 == null) {
         return "";
      }

      if (this.textRenderer.getWidth(var1) <= var2) {
         return var1;
      }

      String var3 = "…";
      return this.textRenderer.trimToWidth(var1, Math.max(0, var2 - this.textRenderer.getWidth(var3))) + var3;
   }

   private List<String> wrapToPixelWidth(String var1, int var2) {
      if (var1 != null && !var1.isBlank()) {
         if (this.textRenderer.getWidth(var1) <= var2) {
            return List.of(var1);
         }

         ArrayList var3 = new ArrayList();
         StringBuilder var4 = new StringBuilder();

         for (String var8 : var1.split(" +")) {
            String var10 = var4.isEmpty() ? var8 : var4 + " " + var8;
            if (this.textRenderer.getWidth(var10) <= var2) {
               var4.setLength(0);
               var4.append(var10);
            } else {
               if (!var4.isEmpty()) {
                  var3.add(var4.toString());
                  var4.setLength(0);
               }

               if (this.textRenderer.getWidth(var8) <= var2) {
                  var4.append(var8);
               } else {
                  String var12 = var8;

                  String var9;
                  while (!var12.isEmpty() && !(var9 = this.textRenderer.trimToWidth(var12, var2)).isEmpty()) {
                     var3.add(var9);
                     var12 = var12.substring(var9.length());
                  }
               }
            }
         }

         if (!var4.isEmpty()) {
            var3.add(var4.toString());
         }

         return var3.isEmpty() ? List.of(var1) : var3;
      } else {
         return List.of("");
      }
   }

   private String routeOutputLineForSummary(String var1, int var2) {
      if (this.selectedPokemon != null) {
         String var3 = var1 == null ? "" : var1.trim();

         for (SpawnEntry var5 : DataIndex.get().spawnsForSpecies(this.selectedPokemon.key(), this.selectedSpawnFilter())) {
            String var6 = String.join(" • ", var5.requirementSummaryParts());
            if (var6.equals(var3)) {
               SessionDiagnostics.event(
                  "result-route",
                  "pokemon="
                     + this.selectedPokemon.key()
                     + " biome="
                     + this.selectedBiomeId
                     + " route="
                     + var5.id
                     + " context="
                     + var5.context
                     + " habitatRank="
                     + var2
                     + " baseSelectors="
                     + var5.conditions.stream().mapToInt(var0 -> var0.neededBaseBlocks.size()).sum()
                     + " nearbySelectors="
                     + var5.conditions.stream().mapToInt(var0 -> var0.neededNearbyBlocks.size()).sum()
                     + " betterChance="
                     + chanceModifierOutput(var5, true)
                     + " lowerChance="
                     + chanceModifierOutput(var5, false)
               );
               return this.routeOutputLine(var5);
            }
         }
      }

      return "ROUTE|" + trim(var1, 180) + "|||";
   }

   private String routeOutputLine(SpawnEntry var1) {
      return this.routeOutputLine(var1, false);
   }

   private String fishingConditionsOutputLine(SpawnEntry var1) {
      return this.routeOutputLine(var1, true);
   }

   private String routeOutputLine(SpawnEntry var1, boolean var2) {
      if (var1 == null) {
         return "ROUTE|No extra conditions|||||";
      }

      LinkedHashSet var3 = new LinkedHashSet();
      LinkedHashSet var4 = new LinkedHashSet();
      LinkedHashSet var5 = new LinkedHashSet();
      LinkedHashSet var6 = new LinkedHashSet();
      List var7 = var1.requirementSummaryParts();
      if (var1.isFishingRoute() && !var2) {
         var3.add("Fishing");
      } else if (!var1.isFishingRoute() && !var7.isEmpty()) {
         var3.add((String)var7.get(0));
      }

      for (SpawnCondition var9 : var1.conditions) {
         var4.addAll(var9.neededBaseBlocks);
         var5.addAll(var9.neededNearbyBlocks);
         var6.addAll(var9.fluid);

         for (String var11 : var9.conciseSummaryParts()) {
            if (!var11.startsWith("On: ")
               && !var11.startsWith("Near: ")
               && !var11.startsWith("Fluid: ")
               && !var11.startsWith("Structure: ")
               && (!var2 || !var11.startsWith("Needs a "))) {
               var3.add(friendlyRequirement(var11));
            }
         }
      }

      for (SpawnCondition var12 : var1.antiConditions) {
         for (String var14 : var12.conciseAvoidSummaryParts()) {
            var3.add(friendlyRequirement(var14));
         }
      }

      List<String> var15 = StructureSelectorPolicy.displaySelectors(List.of(var1));
      if (!var15.isEmpty()) {
         var3.add("Structure: " + StructureSelectorPolicy.displaySummary(var15));
      }

      String var13 = var3.isEmpty() ? "No extra conditions" : String.join(" • ", var3);
      return "ROUTE|"
         + var13
         + "|"
         + String.join(",", var4)
         + "|"
         + String.join(",", var5)
         + "|"
         + String.join(",", var6)
         + "|"
         + chanceModifierOutput(var1, true)
         + "|"
         + chanceModifierOutput(var1, false);
   }

   private static String chanceModifierOutput(SpawnEntry var0, boolean var1) {
      LinkedHashSet<String> var2 = new LinkedHashSet<>();

      for (SpawnEntry.WeightMultiplier var4 : var0.weightMultipliers) {
         boolean var5 = var4.multiplier() > 1.0;
         boolean var6 = var4.multiplier() < 1.0;
         if ((var1 && var5) || (!var1 && var6)) {
            LinkedHashSet<String> var7 = new LinkedHashSet<>();

            for (SpawnCondition var9 : var4.conditions()) {
               for (String var11 : var9.conciseSummaryParts()) {
                  String var12 = friendlyChanceModifierRequirement(var11);
                  if (!var12.isBlank()) {
                     var7.add(var12);
                  }
               }
            }

            for (SpawnCondition var14 : var4.antiConditions()) {
               for (String var16 : var14.conciseSummaryParts()) {
                  String var17 = negatedChanceModifierRequirement(var16);
                  if (!var17.isBlank()) {
                     var7.add(var17);
                  }
               }
            }

            if (!var7.isEmpty()) {
               var2.add(String.join(" • ", var7) + " (x" + chanceMultiplierText(var4.multiplier()) + ")");
            }
         }
      }

      return String.join(" • ", var2);
   }

   private static String friendlyChanceModifierRequirement(String var0) {
      if (var0 == null || var0.isBlank()) {
         return "";
      } else if (var0.startsWith("Moon: ")) {
         return var0.substring("Moon: ".length()) + " moon";
      } else if (var0.startsWith("Time: ")) {
         return titleWords(var0.substring("Time: ".length()));
      } else {
         return var0;
      }
   }

   private static String negatedChanceModifierRequirement(String var0) {
      String var1 = friendlyChanceModifierRequirement(var0);
      if (var1.isBlank()) {
         return "";
      } else if (var1.equals("Rain")) {
         return "No rain";
      } else if (var1.equals("No rain")) {
         return "Rain";
      } else if (var1.equals("Thunder")) {
         return "No thunder";
      } else if (var1.equals("No thunder")) {
         return "Thunder";
      } else if (var1.equals("Sky visible")) {
         return "No sky visibility";
      } else if (var1.equals("No sky visibility")) {
         return "Sky visible";
      } else if (var1.equals("Slime chunk")) {
         return "Not a slime chunk";
      } else if (var1.equals("Not a slime chunk")) {
         return "Slime chunk";
      } else {
         return "Not " + var1;
      }
   }

   private static String chanceMultiplierText(double var0) {
      double var2 = Math.rint(var0);
      return Math.abs(var0 - var2) < 1.0E-6 ? String.valueOf((long)var2) : String.format(Locale.ROOT, "%.2f", var0).replaceAll("0+$", "").replaceAll("\\.$", "");
   }

   private List<SpawnEntry> selectedStructureFallbackRoutes(boolean var1) {
      if (this.selectedPokemon == null) {
         return List.of();
      }

      List<SpawnEntry> var2 = DataIndex.get()
         .spawnsForSpecies(this.selectedPokemon.key(), this.selectedSpawnFilter())
         .stream()
         .filter(var0 -> !var0.isFishingRoute())
         .toList();
      List<SpawnEntry> var3 = var2.stream().filter(SpawnEntry::hasStructureConstraint).filter(StructureSelectorPolicy::routeIsUsable).toList();
      boolean var4 = !var2.isEmpty() && var2.stream().allMatch(SpawnEntry::hasStructureConstraint);
      return StructureResultPolicy.shouldPresent(var1, var4, !var3.isEmpty()) ? var3 : List.of();
   }

   private static String structureDestination(List<SpawnEntry> var0) {
      List<String> var1 = StructureSelectorPolicy.displaySelectors(var0);
      String var2 = StructureSelectorPolicy.displaySummary(var1);
      return var2.isBlank() ? "Required structure" : var2;
   }

   private static String friendlyRequirement(String var0) {
      if (var0 != null && !var0.isBlank()) {
         int var1 = var0.indexOf(58);
         if (var1 >= 0 && var1 + 1 < var0.length()) {
            String var2 = var0.substring(0, var1 + 1);
            String var3 = var0.substring(var1 + 1).trim();
            if (var3.contains(":")) {
               return var2 + " " + friendlySelectorLabel(var3);
            }
         }

         return var0;
      } else {
         return "";
      }
   }

   private static List<String> selectorsFromPart(String var0) {
      return var0 != null && !var0.isBlank() ? Arrays.stream(var0.split(",")).map(String::trim).filter(var0x -> !var0x.isBlank()).toList() : List.of();
   }

   private static String friendlySelectorSummary(List<String> var0) {
      List<String> var1 = var0.stream().map(SnackCalculatorScreen::friendlySelectorLabel).distinct().toList();
      if (var1.isEmpty()) {
         return "";
      }

      int var2 = Math.min(2, var1.size());
      String var3 = String.join(", ", var1.subList(0, var2));
      return var1.size() > var2 ? var3 + " +" + (var1.size() - var2) : var3;
   }

   private static String friendlySelectorLabel(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim();
         if (var1.startsWith("#")) {
            var1 = var1.substring(1);
         }

         int var2 = var1.indexOf(58);
         String var3 = var2 >= 0 ? var1.substring(var2 + 1) : var1;
         String var4 = var3.toLowerCase(Locale.ROOT);
         if (!var4.equals("natural") && !var4.endsWith("/natural")) {
            String[] var5 = var4.split("/");
            String var6 = var5.length == 0 ? var4 : var5[var5.length - 1];
            String var7 = var5.length > 1 ? var5[var5.length - 2] : "";
            if (!var7.contains("flower") && !var6.contains("flower")) {
               return titleWords(var6.replace('_', ' '));
            }

            String var8 = var6.contains("flower") ? var6.replace("flowers", "").replace("flower", "") : var6;
            var8 = var8.replace('_', ' ').trim();
            return var8.isBlank() ? "Flowers" : titleWords(var8) + " flowers";
         } else {
            return "Natural ground";
         }
      } else {
         return "Blocks";
      }
   }

   private static String titleWords(String var0) {
      StringBuilder var1 = new StringBuilder();
      boolean var2 = true;

      for (char var6 : var0.toCharArray()) {
         if (var6 == '_' || var6 == '-') {
            var6 = ' ';
         }

         if (var2 && Character.isLetter(var6)) {
            var1.append(Character.toUpperCase(var6));
            var2 = false;
         } else {
            var1.append(var6);
         }

         if (var6 == ' ') {
            var2 = true;
         }
      }

      return var1.toString();
   }

   private List<ItemStack> selectorTooltip(List<String> var1, boolean var2) {
      if (var1.isEmpty()) {
         return List.of();
      }

      LinkedHashMap<String, Block> var3 = new LinkedHashMap<>();
      LinkedHashSet<String> var4 = new LinkedHashSet<>();

      for (String var6 : var1) {
         try {
            String var7 = fluidSelectorId(var6);
            if (var7 != null) {
               var4.add(var7);
               continue;
            }

            if (var6.startsWith("#")) {
               boolean var8 = addBlockTag(var6.substring(1), var3);
               if (!var8) {
                  switch (var6) {
                     case "#c:leaves":
                        addBlockTag("minecraft:leaves", var3);
                        break;
                     case "#c:redstone_ores":
                        addBlockTag("minecraft:redstone_ores", var3);
                        break;
                     case "#minecraft:mossy_cobblestone":
                        addDirectBlock("minecraft:mossy_cobblestone", var3);
                        break;
                  }
               }
            } else {
               boolean var14 = addDirectBlock(var6, var3);
               if (!var14) {
                  switch (var6) {
                     case "minecraft:double_smooth_stone_slab":
                        addDirectBlock("minecraft:smooth_stone_slab", var3);
                        break;
                     case "minecraft:light_grey_carpet":
                        addDirectBlock("minecraft:light_gray_carpet", var3);
                        break;
                  }
               }
            }
         } catch (Throwable var11) {
         }
      }

      List<Entry<String, Block>> var12 = new ArrayList<>(var3.entrySet());
      if (var2) {
         var12.sort(Comparator.comparingInt(var1x -> this.blockBiomePriority(var1x.getKey())));
      }

      Map<String, ItemStack> var13 = new LinkedHashMap<>();

      for (Entry<String, Block> var18 : var12) {
         ItemStack var21 = new ItemStack(var18.getValue());
         if (!var21.isEmpty() && var21.getItem() instanceof BlockItem) {
            Identifier var10 = Registries.ITEM.getId(var21.getItem());
            if (var10 != null) {
               var13.putIfAbsent(var10.toString(), var21);
            }
         }
      }

      for (String var16 : var4) {
         ItemStack var19 = registeredItemStack(var16 + "_bucket");
         if (!var19.isEmpty()) {
            var13.putIfAbsent(var16 + "_bucket", var19);
         }
      }

      return List.copyOf(var13.values());
   }

   private static String fluidSelectorId(String var0) {
      if (var0 == null || var0.isBlank()) {
         return null;
      }

      String var1 = var0.trim().toLowerCase(Locale.ROOT);
      if (var1.startsWith("#")) {
         var1 = var1.substring(1);
      }

      int var2 = var1.indexOf(58);
      String var4 = var2 >= 0 ? var1.substring(var2 + 1) : var1;
      if (var4.startsWith("flowing_")) {
         var4 = var4.substring("flowing_".length());
      }

      return var4.equals("water") || var4.equals("lava") ? "minecraft:" + var4 : null;
   }

   private List<ItemStack> fluidSourceTooltip(List<String> var1) {
      if (var1 != null && !var1.isEmpty()) {
         LinkedHashMap var2 = new LinkedHashMap();

         for (String var4 : var1) {
            if (var4 != null && !var4.isBlank()) {
               String var5 = var4.trim().toLowerCase(Locale.ROOT);
               if (var5.startsWith("#")) {
                  var5 = var5.substring(1);
               }

               if (!var5.contains(":")) {
                  var5 = "minecraft:" + var5;
               }

               int var6 = var5.indexOf(58);
               String var7 = var5.substring(0, var6);
               String var8 = var5.substring(var6 + 1);
               if (var8.startsWith("flowing_")) {
                  var8 = var8.substring("flowing_".length());
               }

               if (var8.equals("water") || var8.equals("lava")) {
                  var7 = "minecraft";
               }

               String var9 = var8.endsWith("_bucket") ? var7 + ":" + var8 : var7 + ":" + var8 + "_bucket";
               ItemStack var10 = registeredItemStack(var9);
               if (!var10.isEmpty()) {
                  var2.putIfAbsent(var9, var10);
               } else {
                  Identifier var11 = Identifier.of(var7 + ":" + var8);
                  Block var12 = Registries.BLOCK.get(var11);
                  if (var12 != null) {
                     ItemStack var13 = new ItemStack(var12);
                     if (!var13.isEmpty()) {
                        var2.putIfAbsent(var7 + ":" + var8, var13);
                     }
                  }
               }
            }
         }

         return List.copyOf(var2.values());
      } else {
         return List.of();
      }
   }

   private static ItemStack registeredItemStack(String var0) {
      Identifier var1 = Identifier.of(var0);
      Item var2 = Registries.ITEM.get(var1);
      if (var2 == null) {
         return ItemStack.EMPTY;
      }

      Identifier var3 = Registries.ITEM.getId(var2);
      return var1.equals(var3) ? new ItemStack(var2) : ItemStack.EMPTY;
   }

   private static boolean addBlockTag(String var0, LinkedHashMap<String, Block> var1) {
      Identifier var2 = Identifier.of(var0);
      TagKey<Block> var3 = TagKey.of(RegistryKeys.BLOCK, var2);
      Optional<Named<Block>> var4 = Registries.BLOCK.getEntryList(var3);
      if (var4.isEmpty()) {
         return false;
      }

      boolean var5 = false;

      for (RegistryEntry<Block> var7 : var4.get()) {
         Block var8 = var7.value();
         Identifier var9 = Registries.BLOCK.getId(var8);
         if (var9 != null) {
            var1.putIfAbsent(var9.toString(), var8);
            var5 = true;
         }
      }

      return var5;
   }

   private static boolean addDirectBlock(String var0, LinkedHashMap<String, Block> var1) {
      Identifier var2 = Identifier.of(var0);
      Block var3 = Registries.BLOCK.get(var2);
      if (var3 == null) {
         return false;
      } else {
         Identifier var4 = Registries.BLOCK.getId(var3);
         if (var4 != null && var4.equals(var2)) {
            var1.putIfAbsent(var4.toString(), var3);
            return true;
         } else {
            return false;
         }
      }
   }

   private int blockBiomePriority(String var1) {
      String var2 = ((this.selectedBiomeId == null ? "" : this.selectedBiomeId)
            + " "
            + (this.selectedBiomeId == null ? "" : BiomeCatalog.friendlyName(this.selectedBiomeId)))
         .toLowerCase(Locale.ROOT);
      String var3 = var1 == null ? "" : var1.toLowerCase(Locale.ROOT);
      if (var2.contains("nether")) {
         if (var3.contains("netherrack")) {
            return 0;
         }

         if (var3.contains("basalt") || var3.contains("blackstone")) {
            return 1;
         }
      }

      if (var2.contains("end") && var3.contains("end_stone")) {
         return 0;
      }

      if (var2.contains("cave") || var2.contains("underground") || var2.contains("dripstone") || var2.contains("deep")) {
         if (var3.endsWith(":stone") || var3.contains("deepslate")) {
            return 0;
         }

         if (var3.contains("tuff") || var3.contains("dripstone")) {
            return 1;
         }
      }

      if (var2.contains("desert") || var2.contains("badlands")) {
         if (var3.contains("sand")) {
            return 0;
         }

         if (var3.contains("terracotta")) {
            return 1;
         }
      }

      if (var2.contains("snow") || var2.contains("frozen") || var2.contains("ice")) {
         if (var3.contains("snow")) {
            return 0;
         }

         if (var3.contains("ice")) {
            return 1;
         }
      }

      if (var2.contains("beach") || var2.contains("ocean") || var2.contains("river")) {
         if (var3.contains("sand")) {
            return 0;
         }

         if (var3.contains("gravel") || var3.contains("clay")) {
            return 1;
         }
      }

      if (var2.contains("swamp")) {
         if (var3.contains("mud") || var3.contains("grass_block")) {
            return 0;
         }

         if (var3.endsWith(":dirt")) {
            return 1;
         }
      }

      if (var2.contains("plain") || var2.contains("forest") || var2.contains("meadow") || var2.contains("field") || var2.contains("grove")) {
         if (var3.contains("grass_block")) {
            return 0;
         }

         if (var3.endsWith(":dirt")) {
            return 1;
         }
      }

      return 100;
   }

   private List<SnackCalculatorScreen.ResultVisualLine> resultVisualLines(int var1) {
      ArrayList var2 = new ArrayList();
      this.addResultWrapped(var2, this.statusLine, 10551200, var1);
      if (!this.statusLine.isBlank() && !this.outputLines.isEmpty()) {
         var2.add(new SnackCalculatorScreen.ResultVisualLine("", 16777215, 1.0F, List.of()));
      }

      for (String var4 : this.outputLines) {
         if (var4 != null) {
            String[] var6 = var4.split("\\|", -1);
            switch (var6.length > 1 ? var6[0] : "TEXT") {
               case "TARGET":
                  this.addResultWrapped(var2, "Target: " + part(var6, 1), 5636095, var1);
                  break;
               case "BIOME":
                  this.addResultWrapped(var2, "Go to: " + part(var6, 1), 8454016, var1, 1.1F, List.of());
                  break;
               case "CHANCE":
                  this.addResultWrappedTextTooltip(
                     var2,
                     "Target odds: " + part(var6, 1),
                     16777045,
                     var1,
                     1.1F,
                     List.of("Target odds", "How often a matching spawn should be this Pokémon.", "This does not guarantee a spawn.")
                  );
                  break;
               case "STRUCTURECHANCE":
                  this.addResultWrappedTextTooltip(
                     var2, "Target odds: " + part(var6, 1), 16777045, var1, 1.1F, List.of("Actual odds depend on the biome and where you place the snack.")
                  );
                  break;
               case "RECIPE":
                  this.addResultWrapped(var2, "Use: " + part(var6, 1), 16755200, var1, 1.1F, List.of());
                  break;
               case "CATCH":
                  this.addResultWrapped(var2, "Catch by: " + part(var6, 1), 8454016, var1, 1.1F, List.of());
                  break;
               case "FISHROUTE":
                  var2.add(new SnackCalculatorScreen.ResultVisualLine("", 16777215, 1.0F, List.of()));
                  this.addResultWrapped(var2, "#" + part(var6, 1), 16777130, var1);
                  break;
               case "FISHUSE":
                  String var17 = part(var6, 1);
                  List var19 = selectorsFromPart(part(var6, 2))
                     .stream()
                     .map(SnackCalculatorScreen::registeredItemStack)
                     .filter(var0 -> !var0.isEmpty())
                     .toList();
                  this.addResultWrappedTooltipSpan(var2, "Use: Poké Bait + " + var17, 16755200, var1, 1.1F, var17, var19);
                  break;
               case "BITE":
                  this.addResultWrapped(var2, "Bite speed: " + part(var6, 1), 10551200, var1);
                  break;
               case "SHINY":
                  this.addResultWrapped(var2, "Shiny odds: " + part(var6, 1), 16777215, var1);
                  break;
               case "ROUTE":
                  String var16 = part(var6, 1);
                  String var18 = cleanLegacyRouteSummary(var16);
                  List var11 = selectorsFromPart(part(var6, 4));
                  String var12 = "";
                  if (!var11.isEmpty()) {
                     var12 = "Fluid: " + friendlySelectorSummary(var11);
                     var18 = !var18.isBlank() && !"No extra conditions".equals(var18) ? var18 + " • " + var12 : var12;
                  } else if (var18.contains("Fluid: Water")) {
                     var11 = List.of("minecraft:water");
                     var12 = "Fluid: Water";
                  } else if (var18.contains("Fluid: Lava")) {
                     var11 = List.of("minecraft:lava");
                     var12 = "Fluid: Lava";
                  }

                  List var13 = this.fluidSourceTooltip(var11);
                  if (var13.isEmpty()) {
                     this.addResultWrapped(var2, "Conditions: " + var18, 14737632, var1, 1.1F, List.of());
                  } else {
                     this.addResultWrappedTooltipSpan(var2, "Conditions: " + var18, 14737632, var1, 1.1F, var12, var13);
                  }

                  List var14 = selectorsFromPart(part(var6, 2));
                  if (!var14.isEmpty()) {
                     this.addResultWrapped(var2, "On: " + friendlySelectorSummary(var14), 12114104, var1, 1.0F, this.selectorTooltip(var14, true));
                  }

                  List var15 = selectorsFromPart(part(var6, 3));
                  if (!var15.isEmpty()) {
                     this.addResultWrapped(var2, "Near: " + friendlySelectorSummary(var15), 12114175, var1, 1.0F, this.selectorTooltip(var15, false));
                  }

                  String var20 = part(var6, 5);
                  if (!var20.isBlank()) {
                     this.addResultWrappedTextTooltip(
                        var2,
                        "Better chance: " + var20,
                        8454016,
                        var1,
                        1.0F,
                        List.of("This route is more common when these rules are true.", "The shown odds do not assume this boost.")
                     );
                  }

                  String var21 = part(var6, 6);
                  if (!var21.isBlank()) {
                     this.addResultWrappedTextTooltip(
                        var2,
                        "Lower chance: " + var21,
                        16755200,
                        var1,
                        1.0F,
                        List.of("This route is less common when these rules are true.")
                     );
                  }
                  break;
               case "FORMNOTE":
                  this.addResultWrapped(var2, part(var6, 1), 16755455, var1);
                  break;
               case "HEADER":
                  var2.add(new SnackCalculatorScreen.ResultVisualLine("", 16777215, 1.0F, List.of()));
                  this.addResultWrapped(var2, part(var6, 1) + ":", 16777215, var1);
                  break;
               case "ALT":
                  String var9 = "#" + part(var6, 1) + "  " + part(var6, 2) + "  •  " + part(var6, 3);
                  String var10 = !part(var6, 5).isBlank() && !part(var6, 6).isBlank()
                     ? String.join("\u001f", part(var6, 1), part(var6, 5), part(var6, 6))
                     : null;
                  this.addResultWrappedAction(var2, var9, 16777130, var1, var10);
                  this.addResultWrapped(var2, "   " + part(var6, 4), 14211288, var1);
                  break;
               case "STALE":
                  this.addResultWrapped(var2, "⚠ " + part(var6, 1), 16755200, var1);
                  break;
               case "SPAWN":
                  this.addResultWrapped(var2, "#" + part(var6, 1) + "  " + part(var6, 2) + "  " + part(var6, 3), 15263976, var1);
                  break;
               case "NOTE":
                  this.addResultWrapped(var2, part(var6, 1), 13158600, var1);
                  break;
               default:
                  this.addResultWrapped(var2, var4, 15263976, var1);
            }
         }
      }

      return var2;
   }

   private static String part(String[] var0, int var1) {
      return var1 >= 0 && var1 < var0.length ? var0[var1] : "";
   }

   private void addResultWrapped(List<SnackCalculatorScreen.ResultVisualLine> var1, String var2, int var3, int var4) {
      this.addResultWrapped(var1, var2, var3, var4, 1.0F, List.of());
   }

   private void addResultWrapped(List<SnackCalculatorScreen.ResultVisualLine> var1, String var2, int var3, int var4, float var5, List<ItemStack> var6) {
      int var7 = Math.max(12, (int)Math.floor((double)var4 / Math.max(0.1F, var5)));

      for (String var9 : this.wrapToPixelWidth(var2 == null ? "" : var2, var7)) {
         var1.add(new SnackCalculatorScreen.ResultVisualLine(var9, var3, var5, var6 == null ? List.of() : var6));
      }
   }

   private void addResultWrappedTextTooltip(List<SnackCalculatorScreen.ResultVisualLine> var1, String var2, int var3, int var4, float var5, List<String> var6) {
      int var7 = Math.max(12, (int)Math.floor((double)var4 / Math.max(0.1F, var5)));

      for (String var9 : this.wrapToPixelWidth(var2 == null ? "" : var2, var7)) {
         var1.add(new SnackCalculatorScreen.ResultVisualLine(var9, var3, var5, List.of(), -1, -1, var6 == null ? List.of() : List.copyOf(var6), null, -1, -1));
      }
   }

   private void addResultWrappedAction(List<SnackCalculatorScreen.ResultVisualLine> var1, String var2, int var3, int var4, String var5) {
      String var6 = var2 == null ? "" : var2;
      int var7 = var6.indexOf(32);
      String var8 = var7 < 0 ? var6 : var6.substring(0, var7);
      boolean var9 = false;

      for (String var11 : this.wrapToPixelWidth(var6, Math.max(12, var4))) {
         int var12 = var5 != null && !var9 && !var8.isEmpty() ? var11.indexOf(var8) : -1;
         int var13 = var12 < 0 ? -1 : var12 + var8.length();
         if (var12 >= 0) {
            var9 = true;
         }

         List var14 = var12 < 0 ? List.of() : List.of("Click the number to use this alternative.");
         var1.add(new SnackCalculatorScreen.ResultVisualLine(var11, var3, 1.0F, List.of(), -1, -1, var14, var12 < 0 ? null : var5, var12, var13));
      }
   }

   private void addResultWrappedTooltipSpan(
      List<SnackCalculatorScreen.ResultVisualLine> var1, String var2, int var3, int var4, float var5, String var6, List<ItemStack> var7
   ) {
      int var8 = Math.max(12, (int)Math.floor((double)var4 / Math.max(0.1F, var5)));

      for (String var10 : this.wrapToPixelWidth(var2 == null ? "" : var2, var8)) {
         int var11 = var6 == null ? -1 : var10.indexOf(var6);
         int var12 = var11 < 0 ? -1 : var11 + var6.length();
         var1.add(new SnackCalculatorScreen.ResultVisualLine(var10, var3, var5, var11 < 0 ? List.of() : var7, var11, var12));
      }
   }

   private static String cleanLegacyRouteSummary(String var0) {
      return var0 != null && !var0.isBlank()
         ? Arrays.stream(var0.split(" \\u2022 "))
            .filter(var0x -> !var0x.startsWith("On: ") && !var0x.startsWith("Near: "))
            .map(SnackCalculatorScreen::friendlyRequirement)
            .collect(Collectors.joining(" • "))
         : "No extra conditions";
   }

   private int resultsVisibleLineCount() {
      return Math.max(1, (this.resultsHeight - 14) / 12);
   }

   private int resultsMaxScrollOffset() {
      int var1 = Math.max(20, this.resultsWidth - 22);
      return Math.max(0, this.resultVisualLines(var1).size() - this.resultsVisibleLineCount());
   }

   private boolean resultsOnScrollbar(double var1, double var3) {
      int var5 = this.resultsX + this.resultsWidth - 5;
      return var1 >= var5 && var1 <= var5 + 4 && var3 >= this.resultsY + 4 && var3 <= this.resultsY + this.resultsHeight - 4;
   }

   private int resultsThumbHeight() {
      int var1 = Math.max(1, this.resultsHeight - 8);
      int var2 = Math.max(1, this.resultVisualLines(Math.max(20, this.resultsWidth - 22)).size());
      return Math.max(18, Math.min(var1, (int)Math.round(var1 * ((double)this.resultsVisibleLineCount() / var2))));
   }

   private void setResultsScrollFromMouse(double var1) {
      int var3 = this.resultsMaxScrollOffset();
      if (var3 > 0) {
         int var4 = Math.max(1, this.resultsHeight - 8);
         int var5 = this.resultsThumbHeight();
         double var6 = (var1 - (this.resultsY + 4) - var5 / 2.0) / Math.max(1.0, var4 - var5);
         this.resultsScrollOffset = (int)Math.round(Math.max(0.0, Math.min(1.0, var6)) * var3);
      }
   }

   private boolean mainPokemonOnScrollbar(double var1, double var3) {
      int var5 = this.pokemonGridX + this.pokemonGridWidth + 2;
      return var1 >= var5 && var1 <= var5 + 4 && var3 >= this.pokemonGridTop && var3 <= this.pokemonGridBottom;
   }

   private int mainPokemonThumbHeight() {
      int var1 = Math.max(1, this.pokemonGridBottom - this.pokemonGridTop);
      int var2 = this.pokemonLayoutRows.stream().mapToInt(SnackCalculatorScreen.PokemonLayoutRow::height).sum();
      return var2 <= 0 ? var1 : Math.max(16, Math.min(var1, (int)Math.round(var1 * ((double)var1 / var2))));
   }

   private void setMainPokemonScrollFromMouse(double var1) {
      int var3 = this.mainPokemonMaxScrollPixels();
      if (var3 > 0) {
         int var4 = Math.max(1, this.pokemonGridBottom - this.pokemonGridTop);
         int var5 = this.mainPokemonThumbHeight();
         double var6 = Math.max(1, var4 - var5);
         double var8 = (var1 - this.pokemonGridTop - var5 / 2.0) / var6;
         this.pokemonScrollPixels = Math.max(0.0, Math.min(1.0, var8)) * var3;
         this.pokemonScrollTarget = this.pokemonScrollPixels;
         this.updatePokemonLowerControlsPosition();
      }
   }

   private void selectPokemon(SpeciesInfo var1) {
      this.selectedPokemon = var1;
      this.selectedSpawnForm = null;
      this.refreshAvailableBiomes();
      if (this.selectedBiomeId != null && !this.availableBiomes.contains(this.selectedBiomeId)) {
         this.selectedBiomeId = null;
      }

      this.statusLine = var1.displayName() + " • " + this.availableBiomes.size() + " viable biome(s)";
      this.outputLines = List.of(
         "Click Calculate best PokéSnack to auto-fill the best seasonings and biome.", "Or pick seasonings manually and simulate them in a biome."
      );
      this.resultsScrollOffset = 0;
      this.refreshButtonLabels();
      this.rebuildMainPokemonFilter();
   }

   private void openBiomePicker() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.world == null) {
         this.statusLine = "Join a world/server first so the synced biome registry is available.";
      } else {
         if (this.availableBiomes.isEmpty()) {
            this.refreshAvailableBiomes();
         }

         var1.setScreen(new BiomeSelectionScreen(this, this.availableBiomes, this.selectedBiomeId, this::selectBiome));
      }
   }

   private void selectBiome(String var1) {
      this.selectedBiomeId = var1;
      this.statusLine = "Selected biome: " + BiomeCatalog.friendlyName(var1);
      this.showTargetRequirements();
      this.resultsScrollOffset = 0;
      this.refreshButtonLabels();
   }

   private void refreshAvailableBiomes() {
      InlineFormDropdown.ensureSelectedForm(this);
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world == null) {
         this.availableBiomes = List.of();
      } else {
         if (this.selectedPokemon != null
            && this.selectedSpawnForm != null
            && !this.allFormChoices(this.selectedPokemon).stream().anyMatch(var1 -> formKeysMatch(var1.key(), this.selectedSpawnForm))) {
            this.selectedSpawnForm = null;
         }

         Registry var3 = var2.world.getRegistryManager().get(RegistryKeys.BIOME);
         this.availableBiomes = this.selectedPokemon == null
            ? BiomeCatalog.naturallySupportedBiomeIds(var3, DataIndex.get())
            : BiomeCatalog.possibleBiomesForSpecies(var3, DataIndex.get(), this.selectedPokemon, this.selectedSpawnFilter(), MenuStateStore.practicalHabitats());
      }
   }

   private void useCurrentBiome() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.world != null && var1.player != null) {
         Optional var2 = var1.world.getBiome(var1.player.getBlockPos()).getKey();
         if (!var2.isEmpty()) {
            String var3 = ((RegistryKey)var2.get()).getValue().toString();
            if (this.selectedPokemon != null && !this.availableBiomes.contains(var3)) {
               this.statusLine = this.selectedPokemon.displayName() + " has no viable Poké Snack spawn route in your current biome.";
            } else {
               this.selectedBiomeId = var3;
               this.statusLine = "Using current biome: " + BiomeCatalog.friendlyName(var3);
               this.showTargetRequirements();
               this.resultsScrollOffset = 0;
               this.refreshButtonLabels();
            }
         }
      } else {
         this.statusLine = "Join a world/server first.";
      }
   }

   private void openSeasoningPicker(int var1) {
      MinecraftClient.getInstance().setScreen(new SeasoningSelectionScreen(this, this.selected[var1], var2 -> {
         this.selected[var1] = var2;
         this.refreshButtonLabels();
         this.saveRememberedState();
      }));
   }

   private List<Seasoning> currentSeasoningSignature() {
      return List.copyOf(Arrays.asList((Seasoning[])this.selected.clone()));
   }

   private boolean hasComputedResults() {
      return this.outputLines
         .stream()
         .anyMatch(var0 -> var0 != null && (var0.startsWith("CHANCE|") || var0.startsWith("STRUCTURECHANCE|") || var0.startsWith("SPAWN|")));
   }

   private void rememberCurrentResultIngredients() {
      this.resultSeasoningSignature = this.currentSeasoningSignature();
   }

   private void invalidateResultsForInputChange() {
      if (!this.resultSeasoningSignature.isEmpty()
         && !this.resultSeasoningSignature.equals(this.currentSeasoningSignature())
         && this.hasComputedResults()
         && !this.outputLines.stream().anyMatch(var0 -> var0 != null && var0.startsWith("STALE|"))) {
         ArrayList var1 = new ArrayList();
         var1.add("STALE|Ingredients changed — simulate or calculate again to update these results.");
         var1.addAll(this.outputLines);
         this.outputLines = List.copyOf(var1);
         this.statusLine = "Results are out of date";
         this.resultsScrollOffset = 0;
      }
   }

   private void applyAlternativeAction(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String[] var2 = var1.split("\u001f", -1);
         if (var2.length >= 3) {
            String var3 = var2[0];
            String var4 = var2[1];
            if (!this.availableBiomes.contains(var4)) {
               this.refreshAvailableBiomes();
            }

            if (!this.availableBiomes.contains(var4)) {
               this.statusLine = "That alternative's biome is no longer available. Recalculate to refresh the results.";
            } else {
               Seasoning[] var5 = new Seasoning[]{Seasoning.NONE, Seasoning.NONE, Seasoning.NONE};
               String[] var6 = var2[2].split(",", -1);

               for (int var7 = 0; var7 < Math.min(var5.length, var6.length); var7++) {
                  try {
                     var5[var7] = Seasoning.valueOf(var6[var7]);
                  } catch (IllegalArgumentException var9) {
                     var5[var7] = Seasoning.NONE;
                  }
               }

               this.selectedBiomeId = var4;
               System.arraycopy(var5, 0, this.selected, 0, this.selected.length);
               this.rememberCurrentResultIngredients();
               this.outputLines = this.outputLines.stream().filter(var0 -> var0 == null || !var0.startsWith("STALE|")).toList();
               this.statusLine = "Applied alternative #" + var3 + " • " + BiomeCatalog.friendlyName(var4);
               this.refreshButtonLabels();
               this.saveRememberedState();
            }
         }
      }
   }

   private void giveSelectedSnack() {
      PokeSnackGiver.GiveResult var1 = PokeSnackGiver.giveSelected(MinecraftClient.getInstance(), this.selected, this.giveAmount);
      this.statusLine = var1.message();
      this.saveRememberedState();
      this.refreshGiveControls();
   }

   private void copySelectedSnackItem() {
      PokeSnackGiver.GiveResult var1 = PokeSnackGiver.copySelectedItemArgument(MinecraftClient.getInstance(), this.selected);
      this.statusLine = var1.message();
      this.saveRememberedState();
   }

   private void changeGiveAmount(int var1) {
      this.giveAmount = Math.max(1, Math.min(64, this.giveAmount + var1));
      this.refreshGiveControls();
      this.saveRememberedState();
   }

   private void setGiveAmount(int var1) {
      this.giveAmount = Math.max(1, Math.min(64, var1));
      if (this.giveAmountField != null) {
         this.giveAmountField.setText(String.valueOf(this.giveAmount));
      }

      this.refreshGiveControls();
      this.saveRememberedState();
   }

   private void commitGiveAmountField() {
      if (this.giveAmountField != null) {
         String var1 = this.giveAmountField.getText().replaceAll("[^0-9]", "");
         int var2 = 1;
         if (!var1.isBlank()) {
            try {
               var2 = Integer.parseInt(var1);
            } catch (NumberFormatException var4) {
               var2 = 1;
            }
         }

         this.giveAmount = Math.max(1, Math.min(64, var2));
         this.giveAmountField.setText(String.valueOf(this.giveAmount));
         this.refreshGiveControls();
         this.saveRememberedState();
      }
   }

   private void onGiveAmountTyped(String var1) {
      if (var1 == null) {
         var1 = "";
      }

      String var2;
      if (!(var2 = var1.replaceAll("[^0-9]", "")).equals(var1) && this.giveAmountField != null) {
         this.giveAmountField.setText(var2);
      } else if (!var2.isBlank()) {
         try {
            int var3 = Integer.parseInt(var2);
            this.giveAmount = Math.max(1, Math.min(64, var3));
            this.refreshGiveControls();
            this.saveRememberedState();
         } catch (NumberFormatException var4) {
         }
      }
   }

   private Optional<BiomeMatcher> resolveBiome() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.world == null) {
         this.statusLine = "Join a world/server first.";
         return Optional.empty();
      }

      if (this.selectedBiomeId != null && !this.selectedBiomeId.isBlank()) {
         Registry var2 = var1.world.getRegistryManager().get(RegistryKeys.BIOME);
         Optional var3 = BiomeMatcher.create(var2, this.selectedBiomeId);
         if (var3.isEmpty()) {
            this.statusLine = "Biome is no longer present in the synced registry: " + this.selectedBiomeId;
         }

         return var3;
      } else {
         this.statusLine = "Select a biome first.";
         return Optional.empty();
      }
   }

   private void simulate() {
      Optional var2 = this.resolveBiome();
      if (!var2.isEmpty()) {
         SpawnCalculator var3 = new SpawnCalculator(DataIndex.get());
         List var4 = Arrays.asList((Seasoning[])this.selected.clone());
         SpawnEnvironmentProfile var5 = null;
         SpawnCalculator.SimulationResult var1;
         if (this.selectedPokemon != null) {
            SpawnCalculator.TargetedSimulationResult var6 = var3.simulateForTarget(
               (BiomeMatcher)var2.get(), var4, this.selectedPokemon.key(), this.selectedSpawnFilter(), MenuStateStore.practicalHabitats()
            );
            var1 = var6.simulation();
            var5 = var6.profile();
         } else {
            var1 = var3.simulate((BiomeMatcher)var2.get(), var4);
         }

         if (var1 == null) {
            this.outputLines = List.of("No viable Poké Snack spawn route was found for this Pokémon in the selected biome.");
            this.statusLine = "No condition-compatible spawn route.";
         } else {
            ArrayList var9 = new ArrayList();
            var9.add("HEADER|Simulation");
            var9.add("BIOME|" + BiomeCatalog.friendlyName(this.selectedBiomeId));
            if (this.selectedPokemon != null) {
               var9.add("TARGET|" + this.selectedTargetDisplayName());
            }

            if (var5 != null) {
               var9.add(this.routeOutputLine(var5.targetEntry));
            }

            var9.add("HEADER|Likely spawns");

            for (int var7 = 0; var7 < Math.min(20, var1.results().size()); var7++) {
               SpawnCalculator.SpeciesChance var8 = var1.results().get(var7);
               var9.add(String.format(Locale.ROOT, "SPAWN|%d|%.2f%%|%s", var7 + 1, var8.chance() * 100.0, var8.displayName()));
            }

            if (var1.results().isEmpty()) {
               var9.add("NOTE|No condition-compatible local spawn entries were found.");
            }

            this.outputLines = List.copyOf(var9);
            this.statusLine = var5 == null
               ? "Simulation ready"
               : "Simulation ready • " + var1.conditionFilteredSpawnEntries() + " incompatible entries filtered";
            this.rememberCurrentResultIngredients();
            this.resultsScrollOffset = 0;
         }
      }
   }

   private void calculateBestPokeSnack() {
      if (!this.calculating) {
         if (this.selectedPokemon == null) {
            this.statusLine = "Select a Pokémon first.";
         } else {
            MinecraftClient var1 = MinecraftClient.getInstance();
            if (var1.world == null) {
               this.statusLine = "Join a world/server first so the synced biome registry is available.";
            } else {
               String var2 = this.selectedPokemon.key();
               String var3 = this.selectedTargetDisplayName();
               DataIndex var4 = DataIndex.get();
               StructureSelectorPolicy.logAudit(var4);
                Registry<Biome> var5 = var1.world.getRegistryManager().get(RegistryKeys.BIOME);
               boolean var6 = MenuStateStore.requireBiteReducer();
               boolean var7 = MenuStateStore.practicalHabitats();
               boolean var8 = MenuStateStore.avoidEnchantedGoldenApple();
               boolean var9 = MenuStateStore.maximizeShinyChance();
               String var10 = this.selectedSpawnFilter();
                List<SpawnEntry> var11 = var4.spawnsForSpecies(var2, var10);
                List<SpawnEntry> var12 = var11.stream().filter(SpawnEntry::isFishingRoute).toList();
               if (!var11.isEmpty() && var12.size() == var11.size()) {
                  this.showFishingOnlyRoutes(var3, var2, var12);
               } else {
                  this.calculating = true;
                  this.statusLine = "Searching all viable biomes for the best "
                     + (var9 ? "shiny-boosting " : "")
                     + "PokéSnack for "
                     + var3
                     + (var6 ? " with faster bites..." : "...");
                  this.outputLines = List.of("NOTE|Searching viable biomes and recipes...", "NOTE|The best seasonings and biome will be filled automatically.");
                  this.resultsScrollOffset = 0;
                  this.refreshButtonLabels();
                  CompletableFuture.<SnackCalculatorScreen.BestCalculation>supplyAsync(
                        () -> {
                           BestPokeSnackOptimizer.OptimizationResult var8x = new BestPokeSnackOptimizer(var4)
                              .optimizeAllBiomes(var5, var2, var10, 10, var6, var7, var8, var9);
                           StructureEstimateOptimizer.Result var9x = null;
                           if (var8x.combinations().isEmpty() || var8x.combinations().get(0).chance() <= 0.0) {
                              var9x = new StructureEstimateOptimizer(var4).optimize(var5, var2, var10, var6, var7, var8, var9);
                           }

                           return new SnackCalculatorScreen.BestCalculation(var8x, var9x);
                        },
                        OPTIMIZER_EXECUTOR
                     )
                     .whenComplete(
                        (var7x, var8x) -> MinecraftClient.getInstance()
                           .execute(
                              () -> {
                                 this.calculating = false;
                                 if (var8x != null) {
                                    this.statusLine = "Calculation failed: " + var8x.getClass().getSimpleName();
                                    this.outputLines = List.of(var8x.getMessage() == null ? "Unknown calculation error." : var8x.getMessage());
                                    this.refreshButtonLabels();
                                 } else {
                                    BestPokeSnackOptimizer.OptimizationResult var9x = var7x.biomeResult();
                                    StructureEstimateOptimizer.Result var10x = var7x.structureEstimate();
                                    ArrayList var11x = new ArrayList();
                                    if (!var9x.combinations().isEmpty() && !(var9x.combinations().get(0).chance() <= 0.0)) {
                                       BestPokeSnackOptimizer.GlobalCombinationResult var20 = var9x.combinations().get(0);
                                       this.selectedBiomeId = var20.biomeId();
                                       Arrays.fill(this.selected, Seasoning.NONE);

                                       for (int var21 = 0; var21 < Math.min(this.selected.length, var20.seasonings().size()); var21++) {
                                          this.selected[var21] = var20.seasonings().get(var21);
                                       }

                                       this.rememberCurrentResultIngredients();
                                       var11x.add("TARGET|" + var3);
                                       var11x.add("BIOME|" + BiomeCatalog.friendlyName(var20.biomeId()));
                                       var11x.add(String.format(Locale.ROOT, "CHANCE|%.2f%%", var20.chance() * 100.0));
                                       var11x.add("RECIPE|" + comboText(var20.seasonings()));
                                       long var22 = var20.seasonings().stream().filter(var0 -> var0 != null && var0 != Seasoning.NONE).count();
                                       if (var22 == 1L) {
                                          var11x.add("NOTE|One seasoning gives the best target odds here. Adding more would not help.");
                                       } else if (var22 == 2L) {
                                          var11x.add("NOTE|Two seasonings give the best target odds here. A third would not help.");
                                       }

                                       for (int var23 = 0; var23 < var9x.combinations().size(); var23++) {
                                          BestPokeSnackOptimizer.GlobalCombinationResult var25 = var9x.combinations().get(var23);
                                          SessionDiagnostics.event(
                                             "optimizer-result",
                                             String.format(
                                                Locale.ROOT,
                                                "pokemon=%s rank=%d chance=%.4f%% biome=%s recipe=%s habitatRank=%d bite=%s noEnchantedApple=%s shiny=%s",
                                                var2,
                                                var23 + 1,
                                                var25.chance() * 100.0,
                                                var25.biomeId(),
                                                var25.seasonings()
                                                   .stream()
                                                   .filter(var0 -> var0 != Seasoning.NONE)
                                                   .map(Enum::name)
                                                   .collect(Collectors.joining(",")),
                                                var25.habitatRank(),
                                                var6,
                                                var8,
                                                var9
                                             )
                                          );
                                       }

                                       if (var6) {
                                          var11x.add(
                                             String.format(
                                                Locale.ROOT,
                                                "BITE|%.0f%% faster average bite interval",
                                                Seasoning.expectedBiteTimeReduction(var20.seasonings()) * 100.0
                                             )
                                          );
                                       }

                                       if (var9) {
                                          var11x.add("SHINY|x" + Seasoning.totalShinyMultiplier(var20.seasonings()));
                                       }

                                       String var24 = this.selectedSpawnFilter();
                                       if (this.selectedSpawnForm != null && var24 == null) {
                                          var11x.add("FORMNOTE|Selected form uses the species' shared spawn pool in the installed data.");
                                       }

                                       var11x.add(this.routeOutputLineForSummary(var20.routeSummary(), var20.habitatRank()));
                                       if (var9x.combinations().size() > 1) {
                                          var11x.add("HEADER|Alternatives");

                                          for (int var26 = 1; var26 < var9x.combinations().size(); var26++) {
                                             BestPokeSnackOptimizer.GlobalCombinationResult var27 = var9x.combinations().get(var26);
                                             String var18 = var6
                                                ? String.format(Locale.ROOT, " • bite -%.0f%%", Seasoning.expectedBiteTimeReduction(var27.seasonings()) * 100.0)
                                                : "";
                                             String var19 = var27.seasonings().stream().map(Enum::name).collect(Collectors.joining(","));
                                             var11x.add(
                                                String.format(
                                                   Locale.ROOT,
                                                   "ALT|%d|%.2f%%|%s|%s%s|%s|%s",
                                                   var26 + 1,
                                                   var27.chance() * 100.0,
                                                   BiomeCatalog.friendlyName(var27.biomeId()),
                                                   comboText(var27.seasonings()),
                                                   var18,
                                                   var27.biomeId(),
                                                   var19
                                                )
                                             );
                                          }
                                       }

                                       this.statusLine = (var9 ? "Best shiny-focused match found" : "Best match found")
                                          + " • "
                                          + var9x.biomeCount()
                                          + " biome(s) / "
                                          + var9x.preparedRouteCount()
                                          + " route(s) checked";
                                    } else {
                                       String var12x = Arrays.stream(this.selected)
                                          .filter(var0 -> var0 != null && var0 != Seasoning.NONE)
                                          .map(Enum::name)
                                          .collect(Collectors.joining(","));
                                       Arrays.fill(this.selected, Seasoning.NONE);
                                       this.selectedBiomeId = null;
                                       this.resultSeasoningSignature = List.of();
                                       var11x.add("TARGET|" + var3);
                                       boolean var15 = var10x != null && var10x.available();
                                       List<SpawnEntry> var13 = this.selectedStructureFallbackRoutes(var15);
                                       if (!var13.isEmpty()) {
                                          SpawnEntry var14 = (SpawnEntry)var13.get(0);
                                          List<Seasoning> var16 = var15 ? var10x.recipe() : StructureSnackAdvisor.recommend(this.selectedPokemon, var13, var6, var8, var9);

                                          for (int var17 = 0; var17 < Math.min(this.selected.length, var16.size()); var17++) {
                                             this.selected[var17] = (Seasoning)var16.get(var17);
                                          }

                                          this.rememberCurrentResultIngredients();
                                          var11x.add("BIOME|" + structureDestination(var13));
                                          var11x.add("STRUCTURECHANCE|" + (var15 ? structureEstimateText(var10x) : "Varies by location"));
                                          var11x.add("RECIPE|" + comboText(var16));
                                          if (var6) {
                                             var11x.add(
                                                String.format(
                                                   Locale.ROOT, "BITE|%.0f%% faster average bite interval", Seasoning.expectedBiteTimeReduction(var16) * 100.0
                                                )
                                             );
                                          }

                                          if (var9) {
                                             var11x.add("SHINY|x" + Seasoning.totalShinyMultiplier(var16));
                                          }

                                          var11x.add("NOTE|Place this snack inside the required structure.");
                                          var11x.add(this.routeOutputLine(var14));
                                          this.statusLine = var15
                                             ? "Structure estimate ready • " + var10x.biomeCount() + " biome(s) checked"
                                             : "Structure recipe ready • exact odds vary by location";
                                          SessionDiagnostics.event(
                                             "structure-only-route",
                                             "pokemon="
                                                + var2
                                                + " route="
                                                + var14.id
                                                + " structures="
                                                + var13.stream().flatMap(var0 -> var0.conditions.stream()).mapToInt(var0 -> var0.structures.size()).sum()
                                                + " recipe="
                                                + var16.stream().filter(var0 -> var0 != Seasoning.NONE).map(Enum::name).collect(Collectors.joining(","))
                                                + (
                                                   var15
                                                      ? String.format(
                                                         Locale.ROOT,
                                                         " estimateLow=%.4f%% estimateTypical=%.4f%% estimateHigh=%.4f%% estimateBiomes=%d",
                                                         var10x.lowChance() * 100.0,
                                                         var10x.typicalChance() * 100.0,
                                                         var10x.highChance() * 100.0,
                                                         var10x.biomeCount()
                                                      )
                                                      : " odds=location-dependent"
                                                )
                                                + " replacedIngredients="
                                                + var12x
                                                + " bite="
                                                + var6
                                                + " noEnchantedApple="
                                                + var8
                                                + " shiny="
                                                + var9
                                          );
                                       } else {
                                          var11x.add("NOTE|No condition-compatible PokéSnack route was found in the available biomes.");
                                          this.statusLine = "No viable route found";
                                          SessionDiagnostics.event(
                                             "optimizer-no-result",
                                             "pokemon="
                                                + var2
                                                + " recipe=not-calculated clearedIngredients="
                                                + var12x
                                                + " habitat="
                                                + (var7 ? "practical" : "raw")
                                                + " noEnchantedApple="
                                                + var8
                                                + " shiny="
                                                + var9
                                          );
                                       }
                                    }

                                    this.outputLines = List.copyOf(var11x);
                                    this.resultsScrollOffset = 0;
                                    this.refreshButtonLabels();
                                    this.saveRememberedState();
                                 }
                              }
                           )
                     );
               }
            }
         }
      }
   }

   private void showFishingOnlyRoutes(String var1, String var2, List<SpawnEntry> var3) {
      String var4 = Arrays.stream(this.selected).filter(var0 -> var0 != null && var0 != Seasoning.NONE).map(Enum::name).collect(Collectors.joining(","));
      Arrays.fill(this.selected, Seasoning.NONE);
      this.resultSeasoningSignature = List.of();
      ArrayList var5 = new ArrayList();
      var5.add("TARGET|" + var1);
      var5.add("CATCH|Fishing");
      var5.add("NOTE|PokéSnacks do not affect fishing. Seasonings cleared.");

      for (int var6 = 0; var6 < var3.size(); var6++) {
         SpawnEntry var7 = (SpawnEntry)var3.get(var6);
         var5.add("FISHROUTE|" + (var6 + 1));
         var5.add(FishingRoutePresentation.useOutputLine(var7));
         var5.add(this.fishingConditionsOutputLine(var7));
      }

      this.outputLines = List.copyOf(var5);
      this.statusLine = "Fishing route found";
      this.resultsScrollOffset = 0;
      SessionDiagnostics.event(
         "fishing-only-route",
         "pokemon="
            + var2
            + " routes="
            + var3.size()
            + " rods="
            + var3.stream().flatMap(var0 -> var0.conditions.stream()).flatMap(var0 -> var0.rodTypes.stream()).distinct().collect(Collectors.joining(","))
            + " clearedIngredients="
            + var4
      );
      this.refreshButtonLabels();
      this.saveRememberedState();
   }

   private void showTargetRequirements() {
      if (this.selectedPokemon != null && this.selectedBiomeId != null) {
         Optional var1 = this.resolveBiome();
         if (!var1.isEmpty()) {
            List var2 = DataIndex.get()
               .spawnsForSpecies(this.selectedPokemon.key(), this.selectedSpawnFilter())
               .stream()
               .filter(var0 -> !var0.isFishingRoute())
               .filter(StructureSelectorPolicy::routeIsAvailable)
               .filter(((BiomeMatcher)var1.get())::satisfiesEntryBiome)
               .filter(var1x -> HabitatPolicy.allows((BiomeMatcher)var1.get(), var1x, MenuStateStore.practicalHabitats()))
               .toList();
            ArrayList var3 = new ArrayList();
            var3.add("HEADER|Spawn requirements");

            for (int var4 = 0; var4 < Math.min(4, var2.size()); var4++) {
               SpawnEntry var5 = (SpawnEntry)var2.get(var4);
               var3.add(this.routeOutputLine(var5));
            }

            if (var2.size() > 4) {
               var3.add("+ " + (var2.size() - 4) + " more spawn route(s)");
            }

            if (var2.isEmpty()) {
               var3.add("No placed-snack route matches this biome.");
            }

            this.outputLines = var3;
         }
      }
   }

   private void reloadData() {
      if (this.calculating) {
         this.statusLine = "Wait for the current calculation to finish before reloading data.";
      } else {
         DataIndex.reload();
         PerformanceWarmup.refreshDataSnapshot();
         BiomeCatalog.clearCache();
         BestPokeSnackOptimizer.clearCache();
         MinimapSpriteResolver.invalidate();
         PerformanceWarmup.resetSpriteWarmup();
         this.selectableFormPokemonKeys = null;
         if (this.selectedPokemon != null) {
            this.selectedPokemon = DataIndex.get().findSpecies(this.selectedPokemon.key());
         }

         this.refreshAvailableBiomes();
         if (this.selectedBiomeId != null && !this.availableBiomes.contains(this.selectedBiomeId)) {
            this.selectedBiomeId = null;
         }

         this.statusLine = "Reloaded " + PerformanceWarmup.species().size() + " Pokémon / " + DataIndex.get().spawns().size() + " spawn entries.";
         this.outputLines = DataIndex.get().warnings().stream().limit(8L).toList();
         this.refreshButtonLabels();
         this.rebuildMainPokemonFilter();
      }
   }

   private void refreshButtonLabels() {
      this.invalidateResultsForInputChange();
      if (this.pokemonButton != null) {
         this.pokemonButton.setMessage(this.selectedPokemonButtonText());
      }

      if (this.pokemonButton != null) {
         boolean var1 = this.pokemonButton.active = this.selectedPokemon != null && !this.allFormChoices(this.selectedPokemon).isEmpty();
      }

      if (this.deselectPokemonButton != null) {
         boolean var2 = this.deselectPokemonButton.active = this.selectedPokemon != null;
      }

      if (this.biomeButton != null) {
         this.biomeButton.setMessage(Text.literal(this.biomeButtonText()));
      }

      this.refreshCurrentBiomeButtonState();
      if (this.calculateButton != null) {
         this.calculateButton.setMessage(Text.literal(this.calculating ? "Calculating..." : "Calculate best PokéSnack"));
         this.calculateButton.active = !this.calculating;
      }

      for (int var3 = 0; var3 < this.seasoningButtons.size(); var3++) {
         this.seasoningButtons.get(var3).setMessage(Text.literal(shortName(this.selected[var3])));
      }

      if (this.rememberButton != null) {
         this.rememberButton.setMessage(Text.empty());
      }

      if (this.cycleSpritesButton != null) {
         this.cycleSpritesButton.setMessage(Text.empty());
      }

      if (this.biteReducerButton != null) {
         this.biteReducerButton.setMessage(Text.empty());
      }

      if (this.habitatButton != null) {
         this.habitatButton.setMessage(Text.empty());
      }

      if (this.avoidEnchantedAppleButton != null) {
         this.avoidEnchantedAppleButton.setMessage(Text.empty());
      }

      if (this.shinyChanceButton != null) {
         this.shinyChanceButton.setMessage(Text.empty());
      }

      if (this.pullFullRecipeButton != null) {
         this.pullFullRecipeButton.setMessage(Text.literal("Full recipe: " + (this.pullFullRecipeAmount() ? "ON" : "OFF")));
      }

      this.refreshSnackPreview();
      this.refreshGiveControls();
      InlineFormDropdown.updateButtonState(this);
   }

   private void refreshSnackPreview() {
      List var1 = List.copyOf(Arrays.asList((Seasoning[])this.selected.clone()));
      if (!var1.equals(this.snackPreviewSignature)) {
         try {
            Method var2 = PokeSnackGiver.class.getDeclaredMethod("buildSelected", Seasoning[].class);
            var2.setAccessible(true);
            Object var3 = var2.invoke(null, (Object)this.selected.clone());
            Method var4 = var3.getClass().getDeclaredMethod("stack");
            var4.setAccessible(true);
            Object var5 = var4.invoke(var3);
            this.snackPreviewStack = var5 instanceof ItemStack ? (ItemStack)var5 : ItemStack.EMPTY;
            this.snackPreviewSignature = var1;
         } catch (Throwable var6) {
            this.snackPreviewStack = ItemStack.EMPTY;
            this.snackPreviewSignature = List.of();
            SessionDiagnostics.event(
               "snack-preview-error",
               "type=" + var6.getClass().getSimpleName() + " message=" + String.valueOf(var6.getMessage())
            );
         }
      }
   }

   private boolean pullFullRecipeAmount() {
      try {
         Field var1 = TomStorageBridge.class.getDeclaredField("pullFullRecipeAmount");
         var1.setAccessible(true);
         return var1.getBoolean(null);
      } catch (Throwable var2) {
         return false;
      }
   }

   private void togglePullFullRecipeAmount() {
      try {
         Field var1 = TomStorageBridge.class.getDeclaredField("pullFullRecipeAmount");
         var1.setAccessible(true);
         var1.setBoolean(null, !var1.getBoolean(null));
      } catch (Throwable var2) {
      }

      this.refreshButtonLabels();
   }

   private void refreshGiveControls() {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (this.giveSnackButton != null) {
         this.giveSnackButton.setMessage(Text.literal("Give"));
         this.giveSnackButton.active = PokeSnackGiver.canGive(var2);
      }

      if (this.copyItemButton != null) {
         boolean var3 = this.copyItemButton.active = var2 != null;
      }

      if (this.giveMinusButton != null) {
         boolean var4 = this.giveMinusButton.active = this.giveAmount > 1;
      }

      String var1;
      if (this.giveAmountField != null && !this.giveAmountField.isFocused() && !(var1 = String.valueOf(this.giveAmount)).equals(this.giveAmountField.getText())
         )
       {
         this.giveAmountField.setText(var1);
      }

      if (this.givePlusButton != null) {
         this.givePlusButton.active = this.giveAmount < 64;
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.refreshGiveControls();
      this.refreshCurrentBiomeButtonState();
   }

   private void toggleRememberState() {
      boolean var1 = !MenuStateStore.rememberEnabled();
      MenuStateStore.setRememberEnabled(var1);
      if (var1) {
         this.saveRememberedState();
         this.statusLine = "Remember choices on - saving your Pokémon, seasonings, biome, and results";
      } else {
         this.statusLine = "Remember choices off - saved Pokémon, seasonings, biome, and results cleared";
      }

      this.refreshButtonLabels();
   }

   private String rememberButtonText() {
      return "M";
   }

   private void toggleSpriteCycling() {
      this.pokemonSpritesCycle = !this.pokemonSpritesCycle;
      MenuStateStore.setCyclePokemonSprites(this.pokemonSpritesCycle);
      this.refreshButtonLabels();
   }

   private void toggleBiteReducerRequirement() {
      if (this.calculating) {
         this.statusLine = "Wait for the current calculation to finish before changing optimizer options.";
      } else {
         boolean var1 = !MenuStateStore.requireBiteReducer();
         MenuStateStore.setRequireBiteReducer(var1);
         this.refreshButtonLabels();
         this.saveRememberedState();
      }
   }

   private void togglePracticalHabitats() {
      if (this.calculating) {
         this.statusLine = "Wait for the current calculation to finish before changing optimizer options.";
      } else {
         boolean var1 = !MenuStateStore.practicalHabitats();
         MenuStateStore.setPracticalHabitats(var1);
         BiomeCatalog.clearCache();
         BestPokeSnackOptimizer.clearCache();
         this.refreshAvailableBiomes();
         if (this.selectedBiomeId != null && !this.availableBiomes.contains(this.selectedBiomeId)) {
            this.selectedBiomeId = null;
         }

         if (this.hasComputedResults()) {
            ArrayList var2 = new ArrayList();
            var2.add("STALE|Habitat mode changed - simulate or calculate again to update these results.");
            this.outputLines.stream().filter(var0 -> var0 == null || !var0.startsWith("STALE|")).forEach(var2::add);
            this.outputLines = List.copyOf(var2);
         }

         this.statusLine = var1
            ? "Realistic habitats on - skipping odd land, water, and open-sky cave matches"
            : "Realistic habitats off - showing every location in the spawn files";
         this.resultsScrollOffset = 0;
         SessionDiagnostics.event(
            "habitat-mode",
            "mode="
               + (var1 ? "practical" : "raw")
               + " pokemon="
               + (this.selectedPokemon == null ? "none" : this.selectedPokemon.key())
               + " biomes="
               + this.availableBiomes.size()
         );
         this.refreshButtonLabels();
         this.saveRememberedState();
      }
   }

   private String biteReducerButtonText() {
      return "Bite reducer: " + (MenuStateStore.requireBiteReducer() ? "REQUIRED" : "OPTIONAL");
   }

   private void toggleAvoidEnchantedGoldenApple() {
      if (this.calculating) {
         this.statusLine = "Wait for the current calculation to finish before changing options.";
      } else {
         boolean var1 = !MenuStateStore.avoidEnchantedGoldenApple();
         MenuStateStore.setAvoidEnchantedGoldenApple(var1);
         BestPokeSnackOptimizer.clearCache();
         this.markOptimizerResultsStale("Enchanted apple setting changed - calculate again to update these results.");
         this.statusLine = var1 ? "Enchanted golden apples blocked from best-snack recipes" : "Enchanted golden apples allowed in best-snack recipes";
         SessionDiagnostics.event("optimizer-option", "noEnchantedApple=" + var1);
         this.refreshButtonLabels();
         this.saveRememberedState();
      }
   }

   private void toggleMaximizeShinyChance() {
      if (this.calculating) {
         this.statusLine = "Wait for the current calculation to finish before changing options.";
      } else {
         boolean var1 = !MenuStateStore.maximizeShinyChance();
         MenuStateStore.setMaximizeShinyChance(var1);
         BestPokeSnackOptimizer.clearCache();
         this.markOptimizerResultsStale("Shiny setting changed - calculate again to update these results.");
         this.statusLine = var1 ? "Shiny boost on - keeping one matching spawn ingredient" : "Shiny boost off - focusing on the highest spawn odds";
         SessionDiagnostics.event("optimizer-option", "maximizeShiny=" + var1);
         this.refreshButtonLabels();
         this.saveRememberedState();
      }
   }

   private void markOptimizerResultsStale(String var1) {
      if (this.hasComputedResults() && !this.outputLines.stream().anyMatch(var0 -> var0 != null && var0.startsWith("STALE|"))) {
         ArrayList var2 = new ArrayList();
         var2.add("STALE|" + var1);
         var2.addAll(this.outputLines);
         this.outputLines = List.copyOf(var2);
         this.resultsScrollOffset = 0;
      }
   }

   private void restoreRememberedState() {
      if (MenuStateStore.rememberEnabled()) {
         MenuStateStore.Snapshot var1 = MenuStateStore.loadSnapshot();
         if (var1 != null) {
            if (var1.pokemonKey != null && !var1.pokemonKey.isBlank()) {
               this.selectedPokemon = DataIndex.get().findSpecies(var1.pokemonKey);
            }

            this.selectedSpawnForm = var1.pokemonForm;
            this.selectedBiomeId = var1.biomeId;
            if (var1.seasonings != null) {
               for (int var2 = 0; var2 < Math.min(this.selected.length, var1.seasonings.size()); var2++) {
                  try {
                     this.selected[var2] = Seasoning.valueOf(var1.seasonings.get(var2));
                  } catch (Exception var4) {
                     this.selected[var2] = Seasoning.NONE;
                  }
               }
            }

            boolean var5 = var1.outputLines != null
               && var1.outputLines
                  .stream()
                  .anyMatch(var0 -> var0 != null && (var0.startsWith("CHANCE|") || var0.startsWith("BIOME|") || var0.startsWith("ALT|")));
            boolean var3 = var1.environmentIdentity != null && var1.environmentIdentity.equals(BiomeReplacementPolicy.cacheIdentity());
            if ((!var5 || var3) && var1.outputLines != null && !var1.outputLines.isEmpty()) {
               this.outputLines = List.copyOf(var1.outputLines);
            }

            if (var5 && !var3) {
               this.statusLine = "World/server changed. Recalculate for accurate locations.";
               this.outputLines = List.of("Your Pokémon, seasonings, and valid biome selection were kept.");
            } else {
               this.statusLine = var1.statusLine == null ? "" : var1.statusLine;
            }

            this.giveAmount = Math.max(1, Math.min(64, var1.giveAmount));
            if (var3 && this.hasComputedResults()) {
               this.rememberCurrentResultIngredients();
            }

            this.restoredRememberedState = true;
         }
      }
   }

   private void saveRememberedState() {
      if (MenuStateStore.rememberEnabled()) {
         MenuStateStore.saveSnapshot(
            MenuStateStore.Snapshot.of(
               this.selectedPokemon == null ? null : this.selectedPokemon.key(),
               this.selectedSpawnForm,
               this.selectedBiomeId,
               this.selected,
               this.outputLines,
               this.statusLine,
               this.giveAmount,
               BiomeReplacementPolicy.cacheIdentity()
            )
         );
      }
   }

   private void saveBrowserPreferences() {
      MenuStateStore.setBrowserPreferences(this.pokemonSortMode.name(), this.pokemonNamesShown, this.pokemonFormsOnly, this.pokemonRegionGrouped);
   }

   private Text selectedPokemonButtonText() {
      if (this.selectedPokemon == null) {
         return Text.literal("Select a Pokémon");
      }

      MutableText var1 = Text.literal("Selected: ").append(Text.literal(this.selectedTargetDisplayName()).formatted(Formatting.YELLOW));
      if (this.allFormChoices(this.selectedPokemon).size() > 1) {
         var1.append(Text.literal(" ▾"));
      }

      return var1;
   }

   private void openSelectedFormPicker() {
      InlineFormDropdown.toggle(this);
   }

   private List<FormSelectionScreen$FormOption> allFormChoices(SpeciesInfo var1) {
      if (var1 == null) {
         return List.of();
      }

      if ("spinda".equals(SpeciesInfo.normalize(var1.key()))) {
         return List.of();
      }

      boolean var2 = "minior".equals(SpeciesInfo.normalize(var1.key()));
      return DataIndex.get()
         .spawnFormOptions(var1.key())
         .stream()
         .map(
            var1x -> new FormSelectionScreen$FormOption(
               var1x.key(),
               FormDisplayNames.dropdownLabel(
                  "__base__".equals(var1x.key()) ? baseFormLabel(var1) : FormDisplayNames.formLabel(var1, var1x.key(), var1x.label())
               ),
               true
            )
         )
         .sorted(Comparator.<FormSelectionScreen$FormOption>comparingInt(var1x -> {
            String var2x = SpeciesInfo.normalize(var1x.key());
            return !"__base__".equals(var1x.key()) && (!var2 || !var2x.contains("meteor") || var2x.contains("core")) ? 1 : 0;
         }).thenComparing(FormSelectionScreen$FormOption::label, String.CASE_INSENSITIVE_ORDER))
         .toList();
   }

   private static String baseFormLabel(SpeciesInfo var0) {
      return FormDisplayNames.baseFormLabel(var0);
   }

   private static boolean isNonWildBattleForm(String var0, String var1) {
      String var2 = ((var0 == null ? "" : var0) + " " + (var1 == null ? "" : var1)).toLowerCase(Locale.ROOT);
      String var3 = SpeciesInfo.normalize(var2);
      return var3.contains("mega")
         || var3.contains("gigantamax")
         || var3.contains("gmax")
         || var3.contains("dynamax")
         || var3.contains("eternamax")
         || var3.contains("terastallized")
         || var3.contains("terastalized");
   }

   private String selectedSpawnFilter() {
      if (this.selectedPokemon != null && this.selectedSpawnForm != null && !this.selectedSpawnForm.isBlank()) {
         String var1 = matchingSpawnFormKey(this.selectedSpawnForm, DataIndex.get().spawnFormOptions(this.selectedPokemon.key()));
         if (var1 != null) {
            return var1;
         } else {
            return !this.selectedSpawnForm.startsWith("sprite:") && !this.selectedSpawnForm.startsWith("form:")
               ? "__missing_form_route__:" + this.selectedSpawnForm
               : "__missing_form_route__:" + this.selectedSpawnForm;
         }
      } else {
         return null;
      }
   }

   private String selectedFormLabel() {
      return this.selectedPokemon != null && this.selectedSpawnForm != null
         ? this.allFormChoices(this.selectedPokemon)
            .stream()
            .filter(var1 -> formKeysMatch(var1.key(), this.selectedSpawnForm))
            .map(FormSelectionScreen$FormOption::label)
            .findFirst()
            .orElse(prettyFormLabel(this.selectedSpawnForm))
         : "Any form";
   }

   private static String matchingSpawnFormKey(String var0, List<DataIndex.FormSpawnOption> var1) {
      if (var0 != null && var0.equalsIgnoreCase("__base__")) {
         for (DataIndex.FormSpawnOption var7 : var1) {
            if (var7.key().equals("__base__")) {
               return "__base__";
            }
         }

         return null;
      } else {
         String var2 = simplifyFormKey(var0);
         if (var2.isBlank()) {
            return null;
         }

         for (DataIndex.FormSpawnOption var4 : var1) {
            String var5 = simplifyFormKey(var4.key());
            if (!var5.isBlank() && (var5.equals(var2) || var5.contains(var2) || var2.contains(var5))) {
               return var4.key();
            }
         }

         return null;
      }
   }

   private static boolean formKeysMatch(String var0, String var1) {
      if (!"__base__".equalsIgnoreCase(var0) && !"__base__".equalsIgnoreCase(var1)) {
         String var2 = simplifyFormKey(var0);
         String var3 = simplifyFormKey(var1);
         return !var2.isBlank() && !var3.isBlank() && (var2.equals(var3) || var2.contains(var3) || var3.contains(var2));
      } else {
         return "__base__".equalsIgnoreCase(var0) && "__base__".equalsIgnoreCase(var1);
      }
   }

   private static String simplifyFormKey(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = SpeciesInfo.normalize(var0);
      var1 = var1.replace("hisuian", "hisui").replace("alolan", "alola").replace("galarian", "galar").replace("paldean", "paldea");

      for (String var3 : List.of("flower", "form", "variant", "aspect", "mode", "style", "pattern", "breed", "props", "sprite", "base")) {
         var1 = var1.replace(var3, "");
      }

      return var1;
   }

   private static String prettyFormLabel(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.replace('_', ' ').replace('-', ' ');
         StringBuilder var2 = new StringBuilder();
         boolean var3 = true;

         for (char var7 : var1.toCharArray()) {
            if (var3 && Character.isLetter(var7)) {
               var2.append(Character.toUpperCase(var7));
               var3 = false;
            } else {
               var2.append(var7);
            }

            if (var7 == ' ') {
               var3 = true;
            }
         }

         return var2.toString();
      } else {
         return "Form";
      }
   }

   private String selectedTargetDisplayName() {
      if (this.selectedPokemon == null) {
         return "Pokémon";
      }

      String var2 = this.selectedPokemon.displayName();
      if (this.selectedSpawnForm != null && !this.selectedSpawnForm.isBlank()) {
         String var3 = this.selectedFormLabel();
         if (var3 != null && !var3.isBlank() && !var3.equalsIgnoreCase("Any form") && !var3.equalsIgnoreCase("Base form") && !var3.equalsIgnoreCase("Base")) {
            String var4 = var3.toLowerCase(Locale.ROOT).replace('_', '-').trim();

            for (String[] var8 : new String[][]{
               {"kanto", "Kantonian"},
               {"kantonian", "Kantonian"},
               {"johto", "Johtonian"},
               {"johtonian", "Johtonian"},
               {"hoenn", "Hoennian"},
               {"hoennian", "Hoennian"},
               {"sinnoh", "Sinnohan"},
               {"sinnohan", "Sinnohan"},
               {"unova", "Unovan"},
               {"unovan", "Unovan"},
               {"kalos", "Kalosian"},
               {"kalosian", "Kalosian"},
               {"hisui", "Hisuian"},
               {"hisuian", "Hisuian"},
               {"alola", "Alolan"},
               {"alolan", "Alolan"},
               {"galar", "Galarian"},
               {"galarian", "Galarian"},
               {"paldea", "Paldean"},
               {"paldean", "Paldean"}
            }) {
               String var9 = var8[0];
               if (var4.equals(var9) || var4.startsWith(var9 + "-") || var4.startsWith(var9 + " ")) {
                  String var10 = var3.substring(Math.min(var3.length(), var9.length())).replaceFirst("^[\\s_-]+", "").trim();
                  return var8[1] + " " + var2 + (var10.isBlank() ? "" : " (" + prettyFormLabel(var10) + ")");
               }
            }

            if (SpeciesInfo.normalize(var2).equals("floette") && SpeciesInfo.normalize(var3).contains("eternalflower")) {
               return "Eternal Flower Floette";
            } else {
               return SpeciesInfo.normalize(var3).contains(SpeciesInfo.normalize(var2)) ? var3 : var2 + " (" + var3 + ")";
            }
         } else {
            return var2;
         }
      } else {
         return var2;
      }
   }

   private String biomeButtonText() {
      if (this.selectedBiomeId != null && !this.selectedBiomeId.isBlank()) {
         return BiomeCatalog.friendlyName(this.selectedBiomeId);
      } else {
         return this.selectedPokemon == null ? "Select biome..." : "Select biome for Pokémon...";
      }
   }

   private void refreshCurrentBiomeButtonState() {
      if (this.currentBiomeButton != null) {
         this.currentBiomeButton.active = this.isCurrentBiomeViable();
      }
   }

   private boolean isCurrentBiomeViable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 == null || var1.world == null || var1.player == null) {
         return false;
      }

      if (this.selectedPokemon == null) {
         return true;
      }

      Optional var2 = var1.world.getBiome(var1.player.getBlockPos()).getKey();
      return var2.isEmpty() ? false : this.availableBiomes.contains(((RegistryKey)var2.get()).getValue().toString());
   }

   private static String comboText(List<Seasoning> var0) {
      if (var0 != null && !var0.isEmpty()) {
         ArrayList var1 = new ArrayList();

         for (Seasoning var3 : var0) {
            if (var3 != Seasoning.NONE) {
               var1.add(shortName(var3));
            }
         }

         return var1.isEmpty() ? "No seasoning" : String.join(" + ", var1);
      } else {
         return "No seasoning";
      }
   }

   private static String structureEstimateText(StructureEstimateOptimizer.Result var0) {
      double var1 = var0.lowChance() * 100.0;
      double var3 = var0.highChance() * 100.0;
      return Math.abs(var3 - var1) < 0.005
         ? String.format(Locale.ROOT, "About %.2f%% (estimate)", var0.typicalChance() * 100.0)
         : String.format(Locale.ROOT, "%.2f%%–%.2f%% estimate", var1, var3);
   }

   private static String shortName(Seasoning var0) {
      String var1 = var0.displayName;
      int var2 = var1.indexOf(" (");
      return var2 > 0 ? var1.substring(0, var2) : var1;
   }

   private int longestSeasoningButtonWidth() {
      int var1 = this.textRenderer.getWidth("No seasoning");

      for (Seasoning var5 : Seasoning.values()) {
         var1 = Math.max(var1, this.textRenderer.getWidth(shortName(var5)));
      }

      return var1 + 14;
   }

   private int longestSelectedPokemonButtonWidth() {
      int var1 = this.textRenderer.getWidth("Select a Pokémon");
      Set var2 = DataIndex.get().selectableSpawnFormSpeciesKeys();

      for (SpeciesInfo var4 : PerformanceWarmup.species()) {
         var1 = Math.max(var1, this.textRenderer.getWidth("Selected: " + var4.displayName()));
         if (var2.contains(var4.key()) && !"spinda".equals(var4.key())) {
            for (FormSelectionScreen$FormOption var6 : this.allFormChoices(var4)) {
               var1 = Math.max(var1, this.textRenderer.getWidth("Selected: " + var4.displayName() + " (" + var6.label() + ") ▾"));
            }
         }
      }

      return var1 + 14;
   }

   private static String trim(String var0, int var1) {
      if (var0 == null) {
         return "";
      } else {
         return var0.length() <= var1 ? var0 : var0.substring(0, Math.max(0, var1 - 3)) + "...";
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      mouseX = this.presentationMouseX(mouseX);
      mouseY = this.presentationMouseY(mouseY);
      if (PokemonZoomOverlay.mouseScrolled(this, mouseX, mouseY, horizontalAmount, verticalAmount)) {
         return true;
      } else if (InlineFormDropdown.mouseScrolled(this, mouseX, mouseY, horizontalAmount, verticalAmount)) {
         return true;
      } else if (mouseX >= this.resultsX
         && mouseX <= this.resultsX + this.resultsWidth
         && mouseY >= this.resultsY
         && mouseY <= this.resultsY + this.resultsHeight
         && verticalAmount != 0.0
         && this.resultsMaxScrollOffset() > 0) {
         this.resultsScrollOffset = Math.max(0, Math.min(this.resultsMaxScrollOffset(), this.resultsScrollOffset + (verticalAmount < 0.0 ? 2 : -2)));
         return true;
      } else if (mouseX >= this.pokemonGridX
         && mouseX <= this.pokemonGridX + this.pokemonGridWidth + 6
         && mouseY >= this.pokemonGridTop
         && mouseY <= this.pokemonGridBottom
         && verticalAmount != 0.0
         && this.mainPokemonMaxScrollPixels() > 0) {
         this.pokemonScrollTarget = Math.max(0.0, Math.min(this.mainPokemonMaxScrollPixels(), this.pokemonScrollTarget - verticalAmount * 42.0));
         this.updatePokemonLowerControlsPosition();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      mouseX = this.presentationMouseX(mouseX);
      mouseY = this.presentationMouseY(mouseY);
      if (button == 0 && this.giveAmountField != null && this.giveAmountField.isFocused() && !isInsideWidget(this.giveAmountField, mouseX, mouseY)) {
         this.commitGiveAmountField();
         this.giveAmountField.setFocused(false);
      }

      if (InlineFormDropdown.mouseClicked(this, mouseX, mouseY, button)) {
         return true;
      }

      if (PokemonZoomOverlay.mouseClicked(this, mouseX, mouseY, button)) {
         return true;
      }

      if (button == 0 && this.hoveredResultAction != null) {
         this.applyAlternativeAction(this.hoveredResultAction);
         return true;
      }

      if (button == 1) {
         if (isInsideButton(this.pokemonButton, mouseX, mouseY)) {
            this.deselectPokemon();
            return true;
         }

         if (isInsideButton(this.biomeButton, mouseX, mouseY)) {
            this.selectedBiomeId = null;
            this.statusLine = "Biome cleared";
            this.refreshButtonLabels();
            this.saveRememberedState();
            return true;
         }

         int var6 = this.uiTop + 36;

         for (int var7 = 0; var7 < this.seasoningButtons.size(); var7++) {
            boolean var8 = mouseX >= this.rightPanelX + 6 && mouseX < this.rightPanelX + 26 && mouseY >= var6 + var7 * 28 && mouseY < var6 + var7 * 28 + 20;
            if (var8 || isInsideButton(this.seasoningButtons.get(var7), mouseX, mouseY)) {
               this.selected[var7] = Seasoning.NONE;
               this.refreshButtonLabels();
               this.saveRememberedState();
               return true;
            }
         }
      }

      if (button == 0
         && this.searchClearVisible()
         && mouseX >= this.searchClearX
         && mouseX < this.searchClearX + this.searchClearSize
         && mouseY >= this.searchClearY
         && mouseY < this.searchClearY + this.searchClearSize) {
         this.pokemonSearchField.setText("");
         this.pokemonSearchField.setFocused(false);
         this.pokemonSortMenuOpen = false;
         return true;
      }

      if (button == 0 && this.pokemonSearchField != null && !isInsideWidget(this.pokemonSearchField, mouseX, mouseY)) {
         this.pokemonSearchField.setFocused(false);
      }

      if (button == 0) {
         if (this.resultsOnScrollbar(mouseX, mouseY) && this.resultsMaxScrollOffset() > 0) {
            this.draggingResultsScrollbar = true;
            this.setResultsScrollFromMouse(mouseY);
            return true;
         }

         if (this.pokemonSortMenuOpen) {
            if (this.handlePokemonSortMenuClick(mouseX, mouseY)) {
               return true;
            }

            if (!isInsideButton(this.pokemonSortButton, mouseX, mouseY)) {
               this.pokemonSortMenuOpen = false;
            }
         }

         for (SnackCalculatorScreen.RenderedPokemonCell var13 : this.renderedPokemonCells) {
            if (!(mouseY < this.pokemonGridTop) && !(mouseY >= this.pokemonGridBottom) && var13.contains(mouseX, mouseY)) {
               if (var13.isOnStar(mouseX, mouseY)) {
                  MenuStateStore.toggleFavoritePokemon(var13.info().key());
                  this.rebuildMainPokemonFilter();
               } else {
                  this.selectMainPokemon(var13.info());
               }

               return true;
            }
         }

         if (this.mainPokemonOnScrollbar(mouseX, mouseY) && this.mainPokemonMaxScrollPixels() > 0) {
            this.draggingPokemonScrollbar = true;
            this.setMainPokemonScrollFromMouse(mouseY);
            return true;
         }
      }

      boolean var12 = SeasoningUiHelper.afterMouseClicked(super.mouseClicked(mouseX, mouseY, button), this, button);
      if (button == 0 && this.getFocused() instanceof ButtonWidget) {
         this.setFocused((Element)null);
      }

      return var12;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      mouseX = this.presentationMouseX(mouseX);
      mouseY = this.presentationMouseY(mouseY);
      deltaX /= Math.max(1.0E-4, this.presentationScale);
      deltaY /= Math.max(1.0E-4, this.presentationScale);
      if (PokemonZoomOverlay.mouseDragged(this, mouseX, mouseY, button, deltaX, deltaY)) {
         return true;
      } else if (button == 0 && this.draggingResultsScrollbar) {
         this.setResultsScrollFromMouse(mouseY);
         return true;
      } else if (button == 0 && this.draggingPokemonScrollbar) {
         this.setMainPokemonScrollFromMouse(mouseY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      mouseX = this.presentationMouseX(mouseX);
      mouseY = this.presentationMouseY(mouseY);
      if (PokemonZoomOverlay.mouseReleased(this, mouseX, mouseY, button)) {
         return true;
      } else if (button == 0 && this.draggingResultsScrollbar) {
         this.draggingResultsScrollbar = false;
         return true;
      } else if (button == 0 && this.draggingPokemonScrollbar) {
         this.draggingPokemonScrollbar = false;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      super.mouseMoved(this.presentationMouseX(mouseX), this.presentationMouseY(mouseY));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      mouseX = (int)Math.floor(this.presentationMouseX(mouseX));
      mouseY = (int)Math.floor(this.presentationMouseY(mouseY));
      context.getMatrices().push();
      context.getMatrices().scale((float)this.presentationScale, (float)this.presentationScale, 1.0F);
      this.animatePokemonScroll();
      this.updatePokemonLowerControlsPosition();
      int var5 = this.pokemonLowerControlsY;
      context.fill(this.uiLeft - 4, this.uiTop - 2, this.uiLeft + this.leftPanelWidth + 4, var5 + 26, 1478694198);
      context.fill(this.rightPanelX - 5, this.uiTop + 20, this.rightPanelX + this.rightPanelWidth + 5, this.resultsY - 9, 1478694198);
      context.fill(this.resultsX - 5, this.resultsY - 5, this.resultsX + this.resultsWidth + 5, this.resultsY + this.resultsHeight + 5, 1880625693);
      super.render(context, mouseX, mouseY, delta);
      this.renderMainControlHighlights(context);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 7, 16777215);
      context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select Pokémon"), this.uiLeft + this.leftPanelWidth / 2, this.uiTop + 2, 16777215);
      context.drawTextWithShadow(this.textRenderer, Text.literal("Seasonings:"), this.rightPanelX + 4, this.uiTop + 22, 15921906);
      context.drawTextWithShadow(this.textRenderer, Text.literal("Biome:"), this.biomeHeaderX, this.uiTop + 22, 15921906);
      this.renderSearchClear(context, mouseX, mouseY);
      this.renderSeasoningIcons(context);
      boolean var6 = this.renderSnackPreview(context, mouseX, mouseY);
      SnackCalculatorScreen.RenderedPokemonCell var7 = this.renderMainPokemonGrid(context, mouseX, mouseY);
      int var8 = this.resultsX + 7;
      int var9 = this.resultsY + 7;
      int var10 = Math.max(20, this.resultsWidth - 22);
      List var11 = this.resultVisualLines(var10);
      this.resultsScrollOffset = Math.max(0, Math.min(this.resultsMaxScrollOffset(), this.resultsScrollOffset));
      int var12 = this.resultsVisibleLineCount();
      List var13 = List.of();
      List var14 = List.of();
      this.hoveredResultAction = null;

      for (int var15 = this.resultsScrollOffset; var15 < Math.min(var11.size(), this.resultsScrollOffset + var12); var15++) {
         SnackCalculatorScreen.ResultVisualLine var16 = (SnackCalculatorScreen.ResultVisualLine)var11.get(var15);
         boolean var17 = mouseY >= var9 && mouseY < var9 + 12 && mouseX >= var8 && mouseX < this.resultsX + this.resultsWidth - 7;
         if (!var16.text().isEmpty()) {
            if (var17 && var16.actionKey() != null && var16.actionStart() >= 0 && var16.actionEnd() > var16.actionStart()) {
               int var18 = var8 + (int)Math.floor(this.textRenderer.getWidth(var16.text().substring(0, var16.actionStart())) * var16.scale());
               int var19 = var8 + (int)Math.ceil(this.textRenderer.getWidth(var16.text().substring(0, var16.actionEnd())) * var16.scale());
               if (mouseX >= var18 && mouseX <= var19) {
                  context.fill(var18 - 1, var9 - 1, var19 + 1, var9 + 11, 1346322208);
                  this.hoveredResultAction = var16.actionKey();
                  var14 = var16.textTooltip();
               }
            }

            this.drawScaledLeftText(context, var16.text(), var8, var9, var16.color(), var16.scale());
            int var27 = var8 + (int)Math.ceil(this.textRenderer.getWidth(var16.text()) * var16.scale());
            if (var17 && mouseX <= var27 && var16.actionKey() == null && !var16.textTooltip().isEmpty()) {
               var14 = var16.textTooltip();
            }

            if (!var16.tooltip().isEmpty() && var16.tooltipStart() >= 0 && var16.tooltipEnd() > var16.tooltipStart() && mouseY >= var9 && mouseY < var9 + 12) {
               int var29 = var8 + (int)Math.floor(this.textRenderer.getWidth(var16.text().substring(0, var16.tooltipStart())) * var16.scale());
               int var20 = var8 + (int)Math.ceil(this.textRenderer.getWidth(var16.text().substring(0, var16.tooltipEnd())) * var16.scale());
               if (mouseX >= var29 && mouseX <= var20) {
                  var13 = var16.tooltip();
               }
            }
         }

         var9 += 12;
      }

      int var23 = this.resultsX + this.resultsWidth - 5;
      context.fill(var23, this.resultsY + 4, var23 + 4, this.resultsY + this.resultsHeight - 4, 1345664309);
      if (this.resultsMaxScrollOffset() > 0) {
         int var24 = this.resultsHeight - 8;
         int var26 = this.resultsThumbHeight();
         int var28 = this.resultsY + 4 + (int)Math.round((var24 - var26) * ((double)this.resultsScrollOffset / this.resultsMaxScrollOffset()));
         context.fill(var23, var28, var23 + 4, var28 + var26, this.draggingResultsScrollbar ? -1644826 : -4671304);
      }

      this.renderPokemonSortDropdown(context, mouseX, mouseY);
      if (var7 != null && !this.pokemonSortMenuOpen) {
         ArrayList var25 = new ArrayList();
         var25.add(Text.literal(var7.info.displayName()));
         if (var7.info.nationalPokedexNumber() != Integer.MAX_VALUE) {
            var25.add(Text.literal(String.format(Locale.ROOT, "#%03d", var7.info.nationalPokedexNumber())));
         }

         var25.add(Text.literal(MenuStateStore.isFavoritePokemon(var7.info.key()) ? "★ Favorite" : "☆ Click star to favorite"));
         context.drawTooltip(this.textRenderer, var25, mouseX, mouseY);
      }

      PokemonZoomOverlay.render(this, context, mouseX, mouseY);
      InlineFormDropdown.render(this, context, mouseX, mouseY);
      SeasoningUiHelper.afterCalculatorRender(this, context, mouseX, mouseY);
      if (var6 && !this.snackPreviewStack.isEmpty()) {
         context.drawItemTooltip(this.textRenderer, this.snackPreviewStack, mouseX, mouseY);
      } else if (!var13.isEmpty()) {
         this.renderBlockIconTooltip(context, var13, mouseX, mouseY);
      } else if (!var14.isEmpty()) {
         this.renderTextTooltip(context, var14, mouseX, mouseY);
      } else {
         this.renderControlTooltip(context, mouseX, mouseY);
      }

      context.getMatrices().pop();
   }

   private void renderTextTooltip(DrawContext var1, List<String> var2, int var3, int var4) {
      ArrayList var5 = new ArrayList();

      for (String var7 : var2) {
         var5.add(Text.literal(var7));
      }

      if (!var5.isEmpty()) {
         var1.drawTooltip(this.textRenderer, var5, var3, var4);
      }
   }

   private void renderBlockIconTooltip(DrawContext var1, List<ItemStack> var2, int var3, int var4) {
      int var5 = Math.min(48, var2.size());
      if (var5 > 0) {
         OverlayZ.push(var1);

         try {
            int var6 = Math.min(8, var5);
            int var7 = (var5 + var6 - 1) / var6;
            int var8 = var6 * 18 + 8;
            int var9 = var7 * 18 + 8;
            int var10 = Math.max(4, Math.min(var3 + 10, this.width - var8 - 4));
            int var11 = Math.max(4, Math.min(var4 + 10, this.height - var9 - 4));
            var1.fill(var10 - 1, var11 - 1, var10 + var8 + 1, var11 + var9 + 1, -11513856);
            var1.fill(var10, var11, var10 + var8, var11 + var9, -15728624);

            for (int var12 = 0; var12 < var5; var12++) {
               int var13 = var10 + 4 + var12 % var6 * 18;
               int var14 = var11 + 4 + var12 / var6 * 18;
               ItemStack var15 = (ItemStack)var2.get(var12);
               String var16 = fluidIdForSourceIcon(var15);
               if (var16 != null) {
                  renderFluidSourceIcon(var1, var13, var14, var16);
               } else {
                  var1.drawItem(var15, var13, var14);
               }
            }
         } finally {
            OverlayZ.pop(var1);
         }
      }
   }

   private static String fluidIdForSourceIcon(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         Identifier var1 = Registries.ITEM.getId(var0.getItem());
         return var1 != null && var1.getPath().endsWith("_bucket") && !var1.getPath().equals("bucket")
            ? var1.getNamespace() + ":" + var1.getPath().substring(0, var1.getPath().length() - "_bucket".length())
            : null;
      } else {
         return null;
      }
   }

   private static void renderFluidSourceIcon(DrawContext var0, int var1, int var2, String var3) {
      String var4 = var3 == null ? "" : var3.toLowerCase(Locale.ROOT);
      if (var4.endsWith(":water")) {
         renderWaterSourceIcon(var0, var1, var2);
      } else if (var4.endsWith(":lava")) {
         renderLavaSourceIcon(var0, var1, var2);
      } else {
         int var5 = var4.hashCode();
         int var6 = 72 + (var5 >>> 16 & 127);
         int var7 = 72 + (var5 >>> 8 & 127);
         int var8 = 72 + (var5 & 127);
         int var9 = 0xFF000000 | var6 << 16 | var7 << 8 | var8;
         int var10 = 0xFF000000 | Math.min(255, var6 + 45) << 16 | Math.min(255, var7 + 45) << 8 | Math.min(255, var8 + 45);
         var0.fill(var1 + 1, var2 + 3, var1 + 15, var2 + 15, -14671840);
         var0.fill(var1 + 2, var2 + 4, var1 + 14, var2 + 14, var9);
         var0.fill(var1 + 3, var2 + 5, var1 + 10, var2 + 7, var10);
         var0.fill(var1 + 5, var2 + 10, var1 + 13, var2 + 12, var10);
      }
   }

   private static void renderWaterSourceIcon(DrawContext var0, int var1, int var2) {
      var0.fill(var1 + 1, var2 + 3, var1 + 15, var2 + 15, -16175778);
      var0.fill(var1 + 2, var2 + 4, var1 + 14, var2 + 14, -534480696);
      var0.fill(var1 + 2, var2 + 4, var1 + 14, var2 + 7, -263542552);
      var0.fill(var1 + 3, var2 + 5, var1 + 8, var2 + 6, -4133121);
      var0.fill(var1 + 9, var2 + 7, var1 + 13, var2 + 8, -8925185);
      var0.fill(var1 + 3, var2 + 10, var1 + 7, var2 + 11, -11160082);
      var0.fill(var1 + 12, var2 + 7, var1 + 14, var2 + 14, -1072276322);
      var0.fill(var1 + 2, var2 + 14, var1 + 14, var2 + 15, -16375995);
   }

   private static void renderLavaSourceIcon(DrawContext var0, int var1, int var2) {
      var0.fill(var1 + 1, var2 + 3, var1 + 15, var2 + 15, -10873088);
      var0.fill(var1 + 2, var2 + 4, var1 + 14, var2 + 14, -960000);
      var0.fill(var1 + 2, var2 + 4, var1 + 14, var2 + 7, -22528);
      var0.fill(var1 + 3, var2 + 5, var1 + 8, var2 + 6, -154);
      var0.fill(var1 + 9, var2 + 8, var1 + 13, var2 + 10, -12246);
      var0.fill(var1 + 3, var2 + 11, var1 + 7, var2 + 13, -30208);
      var0.fill(var1 + 11, var2 + 5, var1 + 13, var2 + 7, -103);
      var0.fill(var1 + 2, var2 + 14, var1 + 14, var2 + 15, -8773376);
   }

   private void drawScaledLeftText(DrawContext var1, String var2, int var3, int var4, int var5, float var6) {
      if (Math.abs(var6 - 1.0F) < 0.001F) {
         var1.drawTextWithShadow(this.textRenderer, Text.literal(var2), var3, var4, var5);
      } else {
         var1.getMatrices().push();
         var1.getMatrices().translate(var3, var4, 0.0F);
         var1.getMatrices().scale(var6, var6, 1.0F);
         var1.drawTextWithShadow(this.textRenderer, Text.literal(var2), 0, 0, var5);
         var1.getMatrices().pop();
      }
   }

   private boolean searchClearVisible() {
      return this.pokemonSearchField != null && !this.pokemonSearchField.getText().isBlank();
   }

   private void renderMainControlHighlights(DrawContext var1) {
      if (this.pokemonNamesShown) {
         this.drawButtonHighlight(var1, this.pokemonNamesButton, -171);
      }

      if (this.pokemonFormsOnly) {
         this.drawButtonHighlight(var1, this.pokemonFormsButton, -43521);
      }

      if (this.pokemonRegionGrouped) {
         this.drawButtonHighlight(var1, this.pokemonRegionButton, -22016);
      }

      if (MenuStateStore.requireBiteReducer()) {
         this.drawButtonHighlight(var1, this.biteReducerButton, -43691);
      }

      if (MenuStateStore.practicalHabitats()) {
         this.drawButtonHighlight(var1, this.habitatButton, -11141121);
      }

      if (MenuStateStore.rememberEnabled()) {
         this.drawButtonHighlight(var1, this.rememberButton, -11141291);
      }

      if (this.pokemonSpritesCycle) {
         this.drawButtonHighlight(var1, this.cycleSpritesButton, -11184641);
      }

      if (MenuStateStore.avoidEnchantedGoldenApple()) {
         this.drawButtonHighlight(var1, this.avoidEnchantedAppleButton, -5635926);
      }

      if (MenuStateStore.maximizeShinyChance()) {
         this.drawButtonHighlight(var1, this.shinyChanceButton, -1);
      }

      if (this.pokemonFavoritesButton != null) {
         String var2 = this.pokemonFavoriteMode == SnackCalculatorScreen.PokemonFavoriteMode.OFF ? "☆" : "★";

         int var3 = switch (this.pokemonFavoriteMode) {
            case OFF -> 12105912;
            case FIRST -> 16777045;
            case ONLY -> 16755200;
         };
         var1.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(var2),
            this.pokemonFavoritesButton.getX() + this.pokemonFavoritesButton.getWidth() / 2,
            this.pokemonFavoritesButton.getY() + 6,
            var3
         );
      }

      this.renderCopyIcon(var1);
      this.renderReloadIcon(var1);
      this.renderCalculationOptionIcons(var1);
   }

   private void renderCalculationOptionIcons(DrawContext var1) {
      this.renderButtonItemIcon(var1, this.biteReducerButton, "minecraft:clock");
      this.renderButtonItemIcon(var1, this.habitatButton, "minecraft:compass");
      this.renderButtonItemIcon(var1, this.rememberButton, "minecraft:book");
      this.renderCycleIcon(var1);
      if (this.avoidEnchantedAppleButton != null) {
         this.renderButtonItemIcon(var1, this.avoidEnchantedAppleButton, "minecraft:enchanted_golden_apple");
      }

      if (this.shinyChanceButton != null) {
         int var2 = MenuStateStore.maximizeShinyChance() ? -1 : -2565928;
         var1.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("✦"),
            this.shinyChanceButton.getX() + this.shinyChanceButton.getWidth() / 2,
            this.shinyChanceButton.getY() + 6,
            var2
         );
      }
   }

   private void renderButtonItemIcon(DrawContext var1, ButtonWidget var2, String var3) {
      if (var2 != null) {
         ItemStack var4 = registeredItemStack(var3);
         if (!var4.isEmpty()) {
            var1.drawItem(var4, var2.getX() + 2, var2.getY() + 2);
         }
      }
   }

   private void renderCycleIcon(DrawContext var1) {
      if (this.cycleSpritesButton != null) {
         int var2 = this.cycleSpritesButton.getX() + 4;
         int var3 = this.cycleSpritesButton.getY() + 5;
         int var4 = -1513240;
         var1.fill(var2 + 2, var3, var2 + 9, var3 + 1, var4);
         var1.fill(var2 + 8, var3 + 1, var2 + 10, var3 + 3, var4);
         var1.fill(var2 + 7, var3 + 3, var2 + 9, var3 + 4, var4);
         var1.fill(var2 + 1, var3 + 7, var2 + 8, var3 + 8, var4);
         var1.fill(var2, var3 + 5, var2 + 2, var3 + 7, var4);
         var1.fill(var2 + 1, var3 + 4, var2 + 3, var3 + 5, var4);
      }
   }

   private void renderReloadIcon(DrawContext var1) {
      if (this.reloadButton != null) {
         int var2 = this.reloadButton.getX() + 5;
         int var3 = this.reloadButton.getY() + 5;
         int var4 = -1513240;
         var1.fill(var2 + 2, var3, var2 + 8, var3 + 1, var4);
         var1.fill(var2 + 1, var3 + 1, var2 + 3, var3 + 3, var4);
         var1.fill(var2, var3 + 2, var2 + 1, var3 + 7, var4);
         var1.fill(var2 + 7, var3, var2 + 10, var3 + 3, var4);
         var1.fill(var2 + 2, var3 + 9, var2 + 8, var3 + 10, var4);
         var1.fill(var2 + 7, var3 + 7, var2 + 9, var3 + 9, var4);
         var1.fill(var2 + 9, var3 + 3, var2 + 10, var3 + 8, var4);
         var1.fill(var2, var3 + 7, var2 + 3, var3 + 10, var4);
      }
   }

   private void renderCopyIcon(DrawContext var1) {
      if (this.copyItemButton != null) {
         int var2 = this.copyItemButton.getX() + 6;
         int var3 = this.copyItemButton.getY() + 5;
         int var4 = this.copyItemButton.active ? -1513240 : -7829368;
         var1.fill(var2 + 2, var3, var2 + 8, var3 + 1, var4);
         var1.fill(var2 + 7, var3, var2 + 8, var3 + 7, var4);
         var1.fill(var2 + 2, var3, var2 + 3, var3 + 3, var4);
         var1.fill(var2, var3 + 3, var2 + 6, var3 + 4, var4);
         var1.fill(var2, var3 + 3, var2 + 1, var3 + 10, var4);
         var1.fill(var2, var3 + 9, var2 + 6, var3 + 10, var4);
         var1.fill(var2 + 5, var3 + 6, var2 + 6, var3 + 10, var4);
      }
   }

   private void renderControlTooltip(DrawContext var1, int var2, int var3) {
      String var4 = null;
      String var5 = null;
      String var6 = "gray";
      if (isInsideButton(this.pokemonNamesButton, var2, var3)) {
         var4 = "Names: " + (this.pokemonNamesShown ? "on" : "off");
         var5 = "Show names instead of Pokédex numbers.";
         var6 = this.pokemonNamesShown ? "yellow" : "gray";
      } else if (isInsideButton(this.pokemonFormsButton, var2, var3)) {
         var4 = "Forms filter: " + (this.pokemonFormsOnly ? "on" : "off");
         var5 = this.pokemonFormsOnly ? "Show only Pokémon with a form menu." : "Show every Pokémon.";
         var6 = this.pokemonFormsOnly ? "light_purple" : "gray";
      } else if (isInsideButton(this.pokemonRegionButton, var2, var3)) {
         var4 = "Region groups: " + (this.pokemonRegionGrouped ? "on" : "off");
         var5 = "Split the list into Kanto, Johto, and other regions.";
         var6 = this.pokemonRegionGrouped ? "gold" : "gray";
      } else if (isInsideButton(this.pokemonFavoritesButton, var2, var3)) {
         var4 = switch (this.pokemonFavoriteMode) {
            case OFF -> "Favorites: off";
            case FIRST -> "Favorites: first";
            case ONLY -> "Favorites: only";
         };

         var5 = switch (this.pokemonFavoriteMode) {
            case OFF -> "Click to put favorites first.";
            case FIRST -> "Click to show only favorites.";
            case ONLY -> "Click to turn this filter off.";
         };

         var6 = switch (this.pokemonFavoriteMode) {
            case OFF -> "gray";
            case FIRST -> "yellow";
            case ONLY -> "gold";
         };
      } else if (isInsideButton(this.biteReducerButton, var2, var3)) {
         boolean var7 = MenuStateStore.requireBiteReducer();
         var4 = "Faster bites: " + (var7 ? "required" : "not required");
         var5 = var7 ? "Only find snacks that make Pokémon bite sooner." : "Allow snacks with or without faster bites.";
         var6 = var7 ? "red" : "gray";
      } else if (isInsideButton(this.habitatButton, var2, var3)) {
         boolean var10 = MenuStateStore.practicalHabitats();
         var4 = "Realistic habitats: " + (var10 ? "on" : "off");
         var5 = var10 ? "Skip odd results, like land Pokémon in oceans." : "Show every location allowed by the spawn files.";
         var6 = var10 ? "aqua" : "gray";
      } else if (isInsideButton(this.avoidEnchantedAppleButton, var2, var3)) {
         boolean var11 = MenuStateStore.avoidEnchantedGoldenApple();
         var4 = "Enchanted apple: " + (var11 ? "blocked" : "allowed");
         var5 = var11 ? "Keep it out of best-snack recipes." : "Allow it in best-snack recipes.";
         var6 = var11 ? "dark_purple" : "gray";
      } else if (isInsideButton(this.shinyChanceButton, var2, var3)) {
         boolean var12 = MenuStateStore.maximizeShinyChance();
         var4 = "Shiny boost: " + (var12 ? "on" : "off");
         var5 = var12 ? "Use the best shiny boosts, plus one ingredient for this Pokémon." : "Focus on the highest spawn odds.";
         var6 = var12 ? "white" : "gray";
      } else if (isInsideButton(this.copyItemButton, var2, var3)) {
         var4 = "Copy /give command";
         var5 = "Copy a ready-to-paste command. Colors are removed.";
         var6 = "aqua";
      } else if (isInsideButton(this.rememberButton, var2, var3)) {
         boolean var13 = MenuStateStore.rememberEnabled();
         var4 = "Remember choices: " + (var13 ? "on" : "off");
         var5 = "Save your Pokémon, seasonings, biome, and results.";
         var6 = var13 ? "green" : "gray";
      } else if (isInsideButton(this.cycleSpritesButton, var2, var3)) {
         var4 = "Moving sprites: " + (this.pokemonSpritesCycle ? "on" : "off");
         var5 = "Let Pokémon pictures switch between their forms.";
         var6 = this.pokemonSpritesCycle ? "blue" : "gray";
      } else if (isInsideButton(this.reloadButton, var2, var3)) {
         var4 = "Reload Pokémon data";
         var5 = "Check your installed mods again for Pokémon and spawn changes.";
         var6 = "aqua";
      }

      if (var4 != null) {
         ArrayList var14 = new ArrayList();
         Formatting var8 = Formatting.byName(var6);
         MutableText var9 = Text.literal(var4);
         if (var8 != null) {
            var9 = var9.formatted(var8);
         }

         var14.add(var9);
         if (var5 != null) {
            var14.add(Text.literal(var5));
         }

         var1.drawTooltip(this.textRenderer, var14, var2, var3);
      }
   }

   private void drawButtonHighlight(DrawContext var1, ButtonWidget var2, int var3) {
      if (var2 != null) {
         int var4 = var2.getX();
         int var5 = var2.getY();
         int var6 = var2.getWidth();
         int var7 = var2.getHeight();
         var1.fill(var4, var5, var4 + var6, var5 + 1, var3);
         var1.fill(var4, var5 + var7 - 1, var4 + var6, var5 + var7, var3);
         var1.fill(var4, var5, var4 + 1, var5 + var7, var3);
         var1.fill(var4 + var6 - 1, var5, var4 + var6, var5 + var7, var3);
      }
   }

   private void renderSearchClear(DrawContext var1, int var2, int var3) {
      if (this.searchClearVisible()) {
         boolean var4 = var2 >= this.searchClearX
            && var2 < this.searchClearX + this.searchClearSize
            && var3 >= this.searchClearY
            && var3 < this.searchClearY + this.searchClearSize;
         var1.fill(
            this.searchClearX,
            this.searchClearY,
            this.searchClearX + this.searchClearSize,
            this.searchClearY + this.searchClearSize,
            var4 ? -8947849 : -11250604
         );
         var1.drawCenteredTextWithShadow(this.textRenderer, Text.literal("×"), this.searchClearX + this.searchClearSize / 2, this.searchClearY + 4, 16777215);
      }
   }

   private boolean renderSnackPreview(DrawContext var1, int var2, int var3) {
      int var4 = this.snackPreviewX;
      int var5 = this.snackPreviewY;
      var1.fill(var4, var5, var4 + 20, var5 + 20, -1067491489);
      var1.fill(var4, var5, var4 + 20, var5 + 1, -4737097);
      var1.fill(var4, var5 + 19, var4 + 20, var5 + 20, -4737097);
      var1.fill(var4, var5, var4 + 1, var5 + 20, -4737097);
      var1.fill(var4 + 19, var5, var4 + 20, var5 + 20, -4737097);
      if (!this.snackPreviewStack.isEmpty()) {
         var1.drawItem(this.snackPreviewStack, var4 + 2, var5 + 2);
      }

      return var2 >= var4 && var2 < var4 + 20 && var3 >= var5 && var3 < var5 + 20;
   }

   private void renderSeasoningIcons(DrawContext var1) {
      int var2 = this.uiTop + 36;

      for (int var3 = 0; var3 < 3; var3++) {
         int var4 = this.rightPanelX + 6;
         int var5 = var2 + var3 * 28;
         var1.fill(var4, var5, var4 + 20, var5 + 20, -1067491489);
         var1.fill(var4, var5, var4 + 20, var5 + 1, -4737097);
         var1.fill(var4, var5 + 20 - 1, var4 + 20, var5 + 20, -4737097);
         var1.fill(var4, var5, var4 + 1, var5 + 20, -4737097);
         var1.fill(var4 + 20 - 1, var5, var4 + 20, var5 + 20, -4737097);
         ItemStack var6 = this.seasoningStack(this.selected[var3]);
         if (!var6.isEmpty()) {
            var1.drawItem(var6, var4 + 2, var5 + 2);
         }
      }
   }

   private ItemStack seasoningStack(Seasoning var1) {
      if (var1 != null && var1 != Seasoning.NONE) {
         String var2 = var1.itemId();
         if (var2 != null && !var2.isBlank()) {
            Item var3 = Registries.ITEM.get(Identifier.of(var2));
            return var3 == null ? ItemStack.EMPTY : new ItemStack(var3);
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private SnackCalculatorScreen.RenderedPokemonCell renderMainPokemonGrid(DrawContext var1, int var2, int var3) {
      this.renderedPokemonCells.clear();
      SnackCalculatorScreen.RenderedPokemonCell var4 = null;
      int var5 = this.pokemonGridTop - (int)Math.round(this.pokemonScrollPixels);
      var1.enableScissor(
         this.presentationClipFloor(this.pokemonGridX),
         this.presentationClipFloor(this.pokemonGridTop),
         this.presentationClipCeil(this.pokemonGridX + this.pokemonGridWidth),
         this.presentationClipCeil(this.pokemonGridBottom)
      );

      try {
         for (SnackCalculatorScreen.PokemonLayoutRow var7 : this.pokemonLayoutRows) {
            if (var5 + var7.height() > this.pokemonGridTop && var5 < this.pokemonGridBottom) {
               if (var7.header() != null) {
                  int var8 = var5 + 5;
                  var1.drawTextWithShadow(this.textRenderer, Text.literal(var7.header()), this.pokemonGridX + 4, var8, -921103);
                  int var9 = var5 + 20 - 3;
                  var1.fill(this.pokemonGridX + 4, var9, this.pokemonGridX + this.pokemonGridWidth - 4, var9 + 1, 1888121487);
               } else {
                  for (int var18 = 0; var18 < var7.pokemon().size(); var18++) {
                     SpeciesInfo var20 = var7.pokemon().get(var18);
                     int var10 = this.pokemonGridX + var18 * (this.pokemonCellWidth + 2);
                     SnackCalculatorScreen.RenderedPokemonCell var11 = new SnackCalculatorScreen.RenderedPokemonCell(
                        var20, var10, var5, this.pokemonCellWidth, this.pokemonCellSize
                     );
                     this.renderedPokemonCells.add(var11);
                     boolean var12 = var3 >= this.pokemonGridTop && var3 < this.pokemonGridBottom && var11.contains(var2, var3);
                     if (var12) {
                        var4 = var11;
                     }

                     this.renderPokemonTile(var1, var11, var12);
                  }
               }
            }

            var5 += var7.height();
         }
      } finally {
         var1.disableScissor();
      }

      int var16 = this.pokemonGridX + this.pokemonGridWidth + 2;
      var1.fill(var16, this.pokemonGridTop, var16 + 4, this.pokemonGridBottom, 1345664309);
      int var17 = this.mainPokemonMaxScrollPixels();
      if (var17 > 0) {
         int var19 = this.pokemonGridBottom - this.pokemonGridTop;
         int var21 = this.mainPokemonThumbHeight();
         int var22 = this.pokemonGridTop + (int)Math.round((var19 - var21) * (this.pokemonScrollPixels / var17));
         var1.fill(var16, var22, var16 + 4, var22 + var21, this.draggingPokemonScrollbar ? -1644826 : -4671304);
      }

      return var4;
   }

   private void renderPokemonTile(DrawContext var1, SnackCalculatorScreen.RenderedPokemonCell var2, boolean var3) {
      int var5 = var2.x();
      int var6 = var2.y();
      boolean var7 = this.selectedPokemon != null && this.selectedPokemon.key().equals(var2.info().key());
      int var8 = var3 ? -797477001 : -1067491489;
      var1.fill(var5, var6, var5 + var2.width(), var6 + var2.height(), var8);
      int var9 = var7 ? -171 : -4737097;
      var1.fill(var5, var6, var5 + var2.width(), var6 + 1, var9);
      var1.fill(var5, var6 + var2.height() - 1, var5 + var2.width(), var6 + var2.height(), var9);
      var1.fill(var5, var6, var5 + 1, var6 + var2.height(), var9);
      var1.fill(var5 + var2.width() - 1, var6, var5 + var2.width(), var6 + var2.height(), var9);
      int var10 = PokemonZoomOverlay.iconSize();
      int var11 = var5 + (var2.width() - var10) / 2;
      int var12 = Math.max(8, Math.min(var2.height() - 8, (int)Math.round(var2.height() * 0.38)));
      int var13 = var6 + var12 - var10 / 2;
      MinimapSpriteResolver.SpriteRef var4 = var7 && this.selectedSpawnForm != null
         ? MinimapSpriteResolver.spriteForSpawnForm(var2.info(), this.selectedSpawnForm, this.selectedFormLabel())
         : MinimapSpriteResolver.spriteFor(var2.info(), this.pokemonSpritesCycle);
      if (var4 != null) {
         MinimapSpriteResolver.useNearest(var4);
         var1.drawTexture(
            var4.texture(), var11, var13, var10, var10, 0.0F, 0.0F, var4.textureWidth(), var4.textureHeight(), var4.textureWidth(), var4.textureHeight()
         );
      } else {
         ItemStack var15 = PokemonIconFactory.iconFor(var2.info());
         if (!var15.isEmpty()) {
            this.drawScaledPokemonItemIcon(var1, var15, var11, var13, var10);
         }
      }

      if (var2.height() >= 26) {
         String var18 = this.mainPokemonLabel(var2.info());
         int var16 = var7 ? 16777045 : 16777215;
         int var17 = var6 + Math.max(4, var2.height() - 11);
         this.drawAdaptiveCenteredText(var1, var18, var5 + var2.width() / 2, var17, Math.max(8, var2.width() - 4), var16);
      }

      boolean var19 = MenuStateStore.isFavoritePokemon(var2.info().key());
      if (var2.width() >= 14 && var2.height() >= 14) {
         var1.drawTextWithShadow(this.textRenderer, var19 ? "★" : "☆", var5 + var2.width() - 8, var6 + 1, var19 ? 16777045 : 14211288);
      }
   }

   private void drawScaledPokemonItemIcon(DrawContext var1, ItemStack var2, int var3, int var4, int var5) {
      float var6 = Math.max(1.0F, var5 / 16.0F);
      var1.getMatrices().push();

      try {
         var1.getMatrices().translate(var3, var4, 0.0F);
         var1.getMatrices().scale(var6, var6, 1.0F);
         var1.drawItem(var2, 0, 0);
      } finally {
         var1.getMatrices().pop();
      }
   }

   private boolean handlePokemonSortMenuClick(double var1, double var3) {
      if (this.pokemonSortButton == null) {
         return false;
      }

      int var5 = this.pokemonSortButton.getX();
      int var6 = this.pokemonSortButton.getY() + this.pokemonSortButton.getHeight() + 1;
      int var7 = Math.max(86, this.pokemonSortButton.getWidth());
      byte var8 = 18;
      SnackCalculatorScreen.PokemonSortMode[] var9 = SnackCalculatorScreen.PokemonSortMode.values();
      if (!(var1 < var5) && !(var1 >= var5 + var7) && !(var3 < var6) && !(var3 >= var6 + var8 * var9.length)) {
         int var10 = (int)((var3 - var6) / var8);
         if (var10 >= 0 && var10 < var9.length) {
            this.pokemonSortMode = var9[var10];
            this.saveBrowserPreferences();
            this.pokemonSortMenuOpen = false;
            this.resetPokemonScroll();
            this.rebuildMainPokemonFilter();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void renderPokemonSortDropdown(DrawContext var1, int var2, int var3) {
      OverlayZ.push(var1);
      if (this.pokemonSortMenuOpen && this.pokemonSortButton != null) {
         int var4 = this.pokemonSortButton.getX();
         int var5 = this.pokemonSortButton.getY() + this.pokemonSortButton.getHeight() + 1;
         int var6 = Math.max(86, this.pokemonSortButton.getWidth());
         byte var7 = 18;
         SnackCalculatorScreen.PokemonSortMode[] var8 = SnackCalculatorScreen.PokemonSortMode.values();
         var1.fill(var4 - 2, var5 - 2, var4 + var6 + 2, var5 + var7 * var8.length + 2, -15265255);

         for (int var9 = 0; var9 < var8.length; var9++) {
            int var10 = var5 + var9 * var7;
            boolean var11 = var2 >= var4 && var2 < var4 + var6 && var3 >= var10 && var3 < var10 + var7;
            var1.fill(var4, var10, var4 + var6, var10 + var7, var11 ? -8947849 : -11250604);
            var1.fill(var4, var10 + var7 - 1, var4 + var6, var10 + var7, 1883258944);
            int var12 = var8[var9] == this.pokemonSortMode ? 16777045 : 16777215;
            var1.drawCenteredTextWithShadow(this.textRenderer, Text.literal(var8[var9].label), var4 + var6 / 2, var10 + 5, var12);
         }

         OverlayZ.pop(var1);
      } else {
         OverlayZ.pop(var1);
      }
   }

   private static boolean isInsideWidget(TextFieldWidget var0, double var1, double var3) {
      return var0 != null && var1 >= var0.getX() && var1 < var0.getX() + var0.getWidth() && var3 >= var0.getY() && var3 < var0.getY() + var0.getHeight();
   }

   private static boolean isInsideButton(ButtonWidget var0, double var1, double var3) {
      return var0 != null && var1 >= var0.getX() && var1 < var0.getX() + var0.getWidth() && var3 >= var0.getY() && var3 < var0.getY() + var0.getHeight();
   }

   private void drawAdaptiveCenteredText(DrawContext var1, String var2, int var3, int var4, int var5, int var6) {
      if (var2 != null && !var2.isBlank()) {
         int var7 = this.textRenderer.getWidth(var2);
         if (var7 <= var5) {
            var1.drawCenteredTextWithShadow(this.textRenderer, Text.literal(var2), var3, var4, var6);
         } else {
            float var8 = Math.max(0.38F, (float)var5 / var7);
            var1.getMatrices().push();
            var1.getMatrices().translate(var3, var4, 0.0F);
            var1.getMatrices().scale(var8, var8, 1.0F);
            var1.drawCenteredTextWithShadow(this.textRenderer, Text.literal(var2), 0, 0, var6);
            var1.getMatrices().pop();
         }
      }
   }

   public boolean isTypingInSearch() {
      return this.pokemonSearchField != null && this.pokemonSearchField.isFocused();
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (InlineFormDropdown.keyPressed(this, keyCode)) {
         return true;
      }

      MinecraftClient var4 = MinecraftClient.getInstance();
      if (this.giveAmountField != null && this.giveAmountField.isFocused()) {
         if (keyCode == 257 || keyCode == 335) {
            this.commitGiveAmountField();
            this.giveAmountField.setFocused(false);
            return true;
         } else if (keyCode == 256) {
            this.close();
            return true;
         } else {
            super.keyPressed(keyCode, scanCode, modifiers);
            return true;
         }
      } else if (this.isTypingInSearch()) {
         if (keyCode == 256) {
            this.close();
            return true;
         } else {
            super.keyPressed(keyCode, scanCode, modifiers);
            return true;
         }
      } else {
         if (!CobbleSnackClient.matchesOpenCalculatorKey(keyCode, scanCode) && (var4 == null || !var4.options.inventoryKey.matchesKey(keyCode, scanCode))) {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }

         this.close();
         return true;
      }
   }

   @Override
   public void close() {
      if (this.giveAmountField != null) {
         this.commitGiveAmountField();
      }

      this.saveBrowserPreferences();
      this.saveRememberedState();
      MinecraftClient.getInstance().setScreen(this.parent);
      DiskCacheStore.releaseRuntimeCaches();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private static boolean lambda$matchingSpawnFormKey$30(DataIndex.FormSpawnOption var0) {
      return var0.key().equals("__base__");
   }

   private void lambda$openSelectedFormPicker$26(String var1) {
      this.selectedSpawnForm = var1 != null && !var1.isBlank() ? var1 : null;
      this.refreshAvailableBiomes();
      if (this.selectedBiomeId != null && !this.availableBiomes.contains(this.selectedBiomeId)) {
         this.selectedBiomeId = null;
      }

      if (this.selectedSpawnForm == null) {
         String var10000 = "Any form";
      } else {
         this.selectedFormLabel();
      }

      this.statusLine = this.selectedTargetDisplayName();
      String var3 = this.selectedSpawnFilter();
      this.outputLines = List.of(
         var3 == null && this.selectedSpawnForm != null
            ? "This is a cosmetic form and uses the species' shared spawn pool."
            : (
               var3 != null && var3.startsWith("__missing_form_route__:")
                  ? "No installed wild spawn route was found for this form. Reload Data after checking the installed datapacks."
                  : "This form has a distinct installed spawn route and will be targeted separately."
            )
      );
      this.resultsScrollOffset = 0;
      this.refreshButtonLabels();
      this.saveRememberedState();
   }

   private record BestCalculation(BestPokeSnackOptimizer.OptimizationResult biomeResult, StructureEstimateOptimizer.Result structureEstimate) {
   }

   private record MainScoredPokemon(SpeciesInfo info, int score) {
   }

   private enum PokemonFavoriteMode {
      OFF,
      FIRST,
      ONLY;

      SnackCalculatorScreen.PokemonFavoriteMode next() {
         return switch (this) {
            case OFF -> FIRST;
            case FIRST -> ONLY;
            case ONLY -> OFF;
         };
      }
   }

   private record PokemonLayoutRow(String header, List<SpeciesInfo> pokemon, int height) {
      static SnackCalculatorScreen.PokemonLayoutRow header(String var0) {
         return new SnackCalculatorScreen.PokemonLayoutRow(var0, List.of(), 20);
      }

      static SnackCalculatorScreen.PokemonLayoutRow pokemon(List<SpeciesInfo> var0, int var1) {
         return new SnackCalculatorScreen.PokemonLayoutRow(null, List.copyOf(var0), var1);
      }
   }

   private enum PokemonSortMode {
      POKEDEX("Dex #"),
      NAME_AZ("A-Z"),
      NAME_ZA("Z-A");

      private final String label;

      PokemonSortMode(String nullxx) {
         this.label = nullxx;
      }
   }

   private record RenderedPokemonCell(SpeciesInfo info, int x, int y, int width, int height) {
      boolean contains(double var1, double var3) {
         return var1 >= this.x && var1 < this.x + this.width && var3 >= this.y && var3 < this.y + this.height;
      }

      boolean isOnStar(double var1, double var3) {
         return var1 >= this.x + this.width - 11 && var1 < this.x + this.width && var3 >= this.y && var3 < this.y + 11;
      }
   }

   private record ResultVisualLine(
      String text,
      int color,
      float scale,
      List<ItemStack> tooltip,
      int tooltipStart,
      int tooltipEnd,
      List<String> textTooltip,
      String actionKey,
      int actionStart,
      int actionEnd
   ) {
      ResultVisualLine(String var1, int var2, float var3, List<ItemStack> var4) {
         this(
            var1,
            var2,
            var3,
            var4,
            var4 != null && !var4.isEmpty() ? 0 : -1,
            var4 != null && !var4.isEmpty() && var1 != null ? var1.length() : -1,
            List.of(),
            null,
            -1,
            -1
         );
      }

      ResultVisualLine(String var1, int var2, float var3, List<ItemStack> var4, int var5, int var6) {
         this(var1, var2, var3, var4, var5, var6, List.of(), null, -1, -1);
      }
   }
}
