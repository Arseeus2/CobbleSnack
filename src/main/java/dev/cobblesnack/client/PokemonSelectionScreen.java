package dev.cobblesnack.client;

import dev.cobblesnack.data.SpeciesInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class PokemonSelectionScreen extends Screen {
   private static final int CELL_TARGET_WIDTH = 50;
   private static final int CELL_HEIGHT = 44;
   private static final int GAP = 2;
   private static final int ICON_SIZE = 35;
   private static PokemonSelectionScreen.SortMode lastSortMode = PokemonSelectionScreen.SortMode.POKEDEX;
   private static boolean lastFavoritesOnly = false;
   private final Screen parent;
   private final List<SpeciesInfo> species;
   private final SpeciesInfo selected;
   private final Consumer<SpeciesInfo> onSelected;
   private TextFieldWidget searchField;
   private ButtonWidget sortButton;
   private ButtonWidget favoritesOnlyButton;
   private final List<PokemonSelectionScreen.GridCell> cells = new ArrayList<>();
   private List<SpeciesInfo> filtered = List.of();
   private int left;
   private int boxWidth;
   private int gridTop;
   private int gridBottom;
   private int gridWidth;
   private int columns;
   private int visibleRows;
   private int cellWidth;
   private int rowOffset;
   private boolean draggingScrollbar;

   public PokemonSelectionScreen(Screen parent, List<SpeciesInfo> species, SpeciesInfo selected, Consumer<SpeciesInfo> onSelected) {
      super(Text.literal("Select Pokémon"));
      this.parent = parent;
      this.species = species == null ? List.of() : List.copyOf(species);
      this.selected = selected;
      this.onSelected = onSelected;
   }

   @Override
   protected void init() {
      MinimapSpriteResolver.invalidate();
      this.boxWidth = Math.min(700, this.width - 16);
      this.left = (this.width - this.boxWidth) / 2;
      int controlsWidth = Math.min(214, Math.max(170, this.boxWidth / 3));
      int searchWidth = Math.max(120, this.boxWidth - controlsWidth - 6);
      this.searchField = new TextFieldWidget(this.textRenderer, this.left, 32, searchWidth, 20, Text.literal("Search Pokémon"));
      this.searchField.setMaxLength(100);
      this.searchField.setPlaceholder(Text.literal("Search Pokémon..."));
      this.searchField.setChangedListener(value -> {
         this.rowOffset = 0;
         this.rebuildFilter();
      });
      this.addDrawableChild(this.searchField);
      int sortX = this.left + searchWidth + 6;
      int sortWidth = Math.max(88, controlsWidth / 2);
      this.sortButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
         lastSortMode = lastSortMode.next();
         this.rowOffset = 0;
         this.rebuildFilter();
      }).dimensions(sortX, 32, sortWidth, 20).build());
      this.favoritesOnlyButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
         lastFavoritesOnly = !lastFavoritesOnly;
         this.rowOffset = 0;
         this.rebuildFilter();
      }).dimensions(sortX + sortWidth + 4, 32, Math.max(82, controlsWidth - sortWidth - 4), 20).build());
      this.gridTop = 60;
      this.gridBottom = Math.max(this.gridTop + 44, this.height - 58);
      this.gridWidth = this.boxWidth - 10;
      this.columns = Math.max(3, Math.min(14, (this.gridWidth + 2) / 52));
      this.cellWidth = Math.max(45, (this.gridWidth - 2 * (this.columns - 1)) / this.columns);
      this.visibleRows = Math.max(1, (this.gridBottom - this.gridTop + 2) / 46);
      this.cells.clear();

      for (int row = 0; row < this.visibleRows; row++) {
         for (int col = 0; col < this.columns; col++) {
            int x = this.left + col * (this.cellWidth + 2);
            int y = this.gridTop + row * 46;
            PokemonSelectionScreen.GridCell cell = new PokemonSelectionScreen.GridCell();
            cell.card = this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> this.selectCell(cell)).dimensions(x, y, this.cellWidth, 44).build());
            this.cells.add(cell);
         }
      }

      this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.close()).dimensions(this.width / 2 - 50, this.height - 27, 100, 20).build());
      this.rebuildFilter();
      this.setInitialFocus(this.searchField);
   }

   private void rebuildFilter() {
      String raw = this.searchField == null ? "" : this.searchField.getText();
      String q = SpeciesInfo.normalize(raw);
      Set<String> favorites = MenuStateStore.favoritePokemonKeys();
      List<PokemonSelectionScreen.Scored> matches = new ArrayList<>();

      for (SpeciesInfo info : this.species) {
         if (!lastFavoritesOnly || favorites.contains(info.key())) {
            int score = q.isBlank() ? 0 : score(info, q);
            if (score < 1000) {
               matches.add(new PokemonSelectionScreen.Scored(info, score));
            }
         }
      }

      Comparator<PokemonSelectionScreen.Scored> comparator;
      if (!q.isBlank()) {
         comparator = Comparator.comparingInt(PokemonSelectionScreen.Scored::score).thenComparing(scoredComparator(lastSortMode, favorites));
      } else {
         comparator = scoredComparator(lastSortMode, favorites);
      }

      this.filtered = matches.stream().sorted(comparator).map(PokemonSelectionScreen.Scored::info).toList();
      this.rowOffset = Math.max(0, Math.min(this.maxRowOffset(), this.rowOffset));
      this.updateControls();
      this.updateCells();
   }

   private static Comparator<PokemonSelectionScreen.Scored> scoredComparator(PokemonSelectionScreen.SortMode mode, Set<String> favorites) {
      Comparator<SpeciesInfo> dex = Comparator.comparingInt(SpeciesInfo::nationalPokedexNumber)
         .thenComparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER);

      Comparator<SpeciesInfo> infoComparator = switch (mode) {
         case POKEDEX -> dex;
         case NAME_AZ -> Comparator.comparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER);
         case NAME_ZA -> Comparator.comparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER).reversed();
         case FAVORITES -> Comparator.<SpeciesInfo, Boolean>comparing(info -> !favorites.contains(info.key())).thenComparing(dex);
      };
      return Comparator.comparing(PokemonSelectionScreen.Scored::info, infoComparator);
   }

   private void updateControls() {
      if (this.sortButton != null) {
         this.sortButton.setMessage(Text.literal("Sort: " + lastSortMode.label));
      }

      if (this.favoritesOnlyButton != null) {
         this.favoritesOnlyButton.setMessage(Text.literal(lastFavoritesOnly ? "★ Favorites" : "☆ Favorites"));
      }
   }

   private void updateCells() {
      if (!this.cells.isEmpty()) {
         int start = this.rowOffset * this.columns;

         for (int i = 0; i < this.cells.size(); i++) {
            PokemonSelectionScreen.GridCell cell = this.cells.get(i);
            int index = start + i;
            if (index < this.filtered.size()) {
               cell.info = this.filtered.get(index);
               cell.card.visible = true;
               cell.card.active = true;
            } else {
               cell.info = null;
               cell.card.visible = false;
               cell.card.active = false;
            }
         }
      }
   }

   private void selectCell(PokemonSelectionScreen.GridCell cell) {
      if (cell != null && cell.info != null) {
         if (this.onSelected != null) {
            this.onSelected.accept(cell.info);
         }

         MinecraftClient.getInstance().setScreen(this.parent);
      }
   }

   private void toggleFavorite(PokemonSelectionScreen.GridCell cell) {
      if (cell != null && cell.info != null) {
         MenuStateStore.toggleFavoritePokemon(cell.info.key());
         this.rebuildFilter();
      }
   }

   private int totalRows() {
      return this.filtered.isEmpty() ? 0 : (this.filtered.size() + this.columns - 1) / this.columns;
   }

   private int maxRowOffset() {
      return Math.max(0, this.totalRows() - this.visibleRows);
   }

   private static int score(SpeciesInfo info, String q) {
      String name = SpeciesInfo.normalize(info.displayName());
      String key = SpeciesInfo.normalize(info.key());
      if (name.equals(q) || key.equals(q)) {
         return 0;
      }

      if (!name.startsWith(q) && !key.startsWith(q)) {
         int nameContains = name.indexOf(q);
         int keyContains = key.indexOf(q);
         int contains = nameContains >= 0 ? nameContains : keyContains;
         if (contains >= 0) {
            return 100 + contains;
         }

         if (q.length() >= 3) {
            int distance = Math.min(levenshtein(q, name), levenshtein(q, key));
            int limit = q.length() <= 5 ? 2 : 3;
            if (distance <= limit) {
               return 300 + distance * 20 + Math.abs(name.length() - q.length());
            }
         }

         return 1000;
      } else {
         return 10 + Math.min(name.length(), key.length()) - q.length();
      }
   }

   private static int levenshtein(String a, String b) {
      int[] prev = new int[b.length() + 1];
      int[] curr = new int[b.length() + 1];
      int j = 0;

      while (j <= b.length()) {
         prev[j] = j++;
      }

      for (int i = 1; i <= a.length(); i++) {
         curr[0] = i;

         for (int jx = 1; jx <= b.length(); jx++) {
            int cost = a.charAt(i - 1) == b.charAt(jx - 1) ? 0 : 1;
            curr[jx] = Math.min(Math.min(curr[jx - 1] + 1, prev[jx] + 1), prev[jx - 1] + cost);
         }

         int[] tmp = prev;
         prev = curr;
         curr = tmp;
      }

      return prev[b.length()];
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount != 0.0 && this.maxRowOffset() > 0) {
         this.rowOffset = Math.max(0, Math.min(this.maxRowOffset(), this.rowOffset + (verticalAmount < 0.0 ? 1 : -1)));
         this.updateCells();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         for (PokemonSelectionScreen.GridCell cell : this.cells) {
            if (cell.info != null && cell.card.visible) {
               int x = cell.card.getX();
               int y = cell.card.getY();
               if (mouseX >= x + this.cellWidth - 12 && mouseX < x + this.cellWidth && mouseY >= y && mouseY < y + 12) {
                  this.toggleFavorite(cell);
                  return true;
               }
            }
         }

         if (this.isOnScrollbar(mouseX, mouseY) && this.maxRowOffset() > 0) {
            this.draggingScrollbar = true;
            this.setScrollFromMouse(mouseY);
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (button == 0 && this.draggingScrollbar) {
         this.setScrollFromMouse(mouseY);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.draggingScrollbar) {
         this.draggingScrollbar = false;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   private boolean isOnScrollbar(double mouseX, double mouseY) {
      int x = this.left + this.boxWidth - 6;
      return mouseX >= x && mouseX <= x + 4 && mouseY >= this.gridTop && mouseY <= this.gridBottom;
   }

   private void setScrollFromMouse(double mouseY) {
      int max = this.maxRowOffset();
      if (max > 0) {
         int trackHeight = Math.max(1, this.gridBottom - this.gridTop);
         int thumbHeight = this.scrollbarThumbHeight(trackHeight);
         double usable = Math.max(1, trackHeight - thumbHeight);
         double pct = (mouseY - this.gridTop - thumbHeight / 2.0) / usable;
         this.rowOffset = (int)Math.round(Math.max(0.0, Math.min(1.0, pct)) * max);
         this.updateCells();
      }
   }

   private int scrollbarThumbHeight(int trackHeight) {
      int rows = Math.max(1, this.totalRows());
      return Math.max(18, Math.min(trackHeight, (int)Math.round(trackHeight * ((double)this.visibleRows / rows))));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 16777215);
      PokemonSelectionScreen.GridCell hovered = null;

      for (PokemonSelectionScreen.GridCell cell : this.cells) {
         if (cell.info != null && cell.card.visible) {
            int x = cell.card.getX();
            int y = cell.card.getY();
            if (mouseX >= x && mouseX < x + this.cellWidth && mouseY >= y && mouseY < y + 44) {
               hovered = cell;
            }

            int iconX = x + (this.cellWidth - 35) / 2;
            int iconY = y + 2;
            MinimapSpriteResolver.SpriteRef sprite = MinimapSpriteResolver.spriteFor(cell.info);
            if (sprite != null) {
               context.drawTexture(
                  sprite.texture(),
                  iconX,
                  iconY,
                  35,
                  35,
                  0.0F,
                  0.0F,
                  sprite.textureWidth(),
                  sprite.textureHeight(),
                  sprite.textureWidth(),
                  sprite.textureHeight()
               );
            } else {
               ItemStack fallback = PokemonIconFactory.iconFor(cell.info);
               if (!fallback.isEmpty()) {
                  context.drawItem(fallback, x + (this.cellWidth - 16) / 2, y + 10);
               }
            }

            String label = this.labelFor(cell.info);
            String fitted = this.trimToWidth(label, Math.max(8, this.cellWidth - 4));
            int labelX = x + (this.cellWidth - this.textRenderer.getWidth(fitted)) / 2;
            int labelColor = this.selected != null && this.selected.key().equals(cell.info.key()) ? 16777045 : 16777215;
            context.drawTextWithShadow(this.textRenderer, fitted, labelX, y + 35, labelColor);
            String star = MenuStateStore.isFavoritePokemon(cell.info.key()) ? "★" : "☆";
            context.drawTextWithShadow(
               this.textRenderer, star, x + this.cellWidth - 9, y + 1, MenuStateStore.isFavoritePokemon(cell.info.key()) ? 16777045 : 13684944
            );
         }
      }

      this.renderScrollbar(context);
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(this.filtered.size() + " Pokémon" + (lastFavoritesOnly ? " • favorites only" : "")),
         this.width / 2,
         this.height - 42,
         10526880
      );
      if (hovered != null) {
         this.renderPokemonTooltip(context, hovered.info, mouseX, mouseY);
      }
   }

   private String labelFor(SpeciesInfo info) {
      return switch (lastSortMode) {
         case POKEDEX -> info.nationalPokedexNumber() == Integer.MAX_VALUE ? "—" : String.format(Locale.ROOT, "#%03d", info.nationalPokedexNumber());
         case NAME_AZ, NAME_ZA, FAVORITES -> info.displayName();
      };
   }

   private void renderPokemonTooltip(DrawContext context, SpeciesInfo info, int mouseX, int mouseY) {
      List<Text> lines = new ArrayList<>();
      lines.add(Text.literal(info.displayName()));
      if (info.nationalPokedexNumber() != Integer.MAX_VALUE) {
         lines.add(Text.literal(String.format(Locale.ROOT, "#%03d", info.nationalPokedexNumber())));
      }

      String types = typeLabel(info);
      if (!types.isBlank()) {
         lines.add(Text.literal(types));
      }

      lines.add(Text.literal(MenuStateStore.isFavoritePokemon(info.key()) ? "★ Favorite" : "☆ Not favorited"));
      context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
   }

   private static String typeLabel(SpeciesInfo info) {
      String primary = pretty(info.primaryType());
      String secondary = pretty(info.secondaryType());
      if (primary.isBlank()) {
         return secondary;
      } else {
         return secondary.isBlank() ? primary : primary + " / " + secondary;
      }
   }

   private static String pretty(String value) {
      if (value != null && !value.isBlank()) {
         String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
         StringBuilder out = new StringBuilder(lower.length());
         boolean upper = true;

         for (char c : lower.toCharArray()) {
            if (upper && Character.isLetter(c)) {
               out.append(Character.toUpperCase(c));
               upper = false;
            } else {
               out.append(c);
            }

            if (c == ' ') {
               upper = true;
            }
         }

         return out.toString();
      } else {
         return "";
      }
   }

   private String trimToWidth(String text, int maxWidth) {
      if (this.textRenderer.getWidth(text) <= maxWidth) {
         return text;
      }

      String ellipsis = "…";
      int usable = Math.max(0, maxWidth - this.textRenderer.getWidth(ellipsis));
      return this.textRenderer.trimToWidth(text, usable) + ellipsis;
   }

   private void renderScrollbar(DrawContext context) {
      int trackX = this.left + this.boxWidth - 6;
      int trackHeight = Math.max(1, this.gridBottom - this.gridTop);
      context.fill(trackX, this.gridTop, trackX + 4, this.gridBottom, 1345664309);
      if (this.maxRowOffset() > 0) {
         int thumbHeight = this.scrollbarThumbHeight(trackHeight);
         int thumbY = this.gridTop + (int)Math.round((trackHeight - thumbHeight) * ((double)this.rowOffset / this.maxRowOffset()));
         context.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, this.draggingScrollbar ? -1776412 : -4737097);
      }
   }

   @Override
   public void close() {
      MinecraftClient.getInstance().setScreen(this.parent);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private static final class GridCell {
      private ButtonWidget card;
      private SpeciesInfo info;
   }

   private record Scored(SpeciesInfo info, int score) {
   }

   private enum SortMode {
      POKEDEX("Dex #"),
      NAME_AZ("A-Z"),
      NAME_ZA("Z-A"),
      FAVORITES("Favorites");

      private final String label;

      SortMode(String label) {
         this.label = label;
      }

      PokemonSelectionScreen.SortMode next() {
         return values()[(this.ordinal() + 1) % values().length];
      }
   }
}
