package dev.cobblesnack.client;

import dev.cobblesnack.calc.BiomeCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class BiomeSelectionScreen extends Screen {
   private static final int ROW_HEIGHT = 18;
   private static final int BUTTON_HEIGHT = 16;
   private final Screen parent;
   private final List<String> sourceBiomes;
   private final String selectedBiome;
   private final Consumer<String> onSelected;
   private TextFieldWidget searchField;
   private final List<ButtonWidget> rowButtons = new ArrayList<>();
   private List<String> filtered = List.of();
   private int scrollOffset;
   private int visibleRows;
   private int left;
   private int boxWidth;
   private int listTop;
   private int listBottom;
   private int searchClearX;
   private int searchClearY;
   private static final int SEARCH_CLEAR_SIZE = 14;
   private boolean draggingScrollbar;

   public BiomeSelectionScreen(Screen var1, List<String> var2, String var3, Consumer<String> var4) {
      super(Text.literal("Select biome"));
      this.parent = var1;
      this.sourceBiomes = var2 == null ? List.of() : List.copyOf(var2);
      this.selectedBiome = var3;
      this.onSelected = var4;
   }

   @Override
   protected void init() {
      int var1 = this.sourceBiomes.stream().map(BiomeCatalog::friendlyName).mapToInt(var1x -> this.textRenderer.getWidth(var1x)).max().orElse(120);
      int var2 = Math.min(this.width - 24, Math.max(180, var1 + 22));
      int var3 = (this.width - var2) / 2;
      this.boxWidth = var2;
      this.left = var3;
      this.searchField = new TextFieldWidget(this.textRenderer, var3, 34, var2, 18, Text.literal("Search biomes"));
      this.searchField.setMaxLength(100);
      this.searchField.setPlaceholder(Text.literal("Search biome names..."));
      this.searchField.setChangedListener(var1x -> {
         this.scrollOffset = 0;
         this.rebuildFilter();
      });
      this.addDrawableChild(this.searchField);
      this.searchClearX = var3 + var2 - 14 - 2;
      this.searchClearY = 36;
      byte var4 = 58;
      int var5 = Math.max(var4 + 18, this.height - 58);
      this.listTop = var4;
      this.listBottom = var5;
      this.visibleRows = Math.max(1, (var5 - var4) / 18);
      this.rowButtons.clear();

      for (int var6 = 0; var6 < this.visibleRows; var6++) {
         int var7 = var6;
         ButtonWidget var8 = ButtonWidget.builder(Text.empty(), var2x -> this.selectRow(var7))
            .dimensions(var3, var4 + var6 * 18, Math.max(20, var2 - 7), 16)
            .build();
         this.rowButtons.add(var8);
         this.addDrawableChild(var8);
      }

      this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), var1x -> this.close()).dimensions(this.width / 2 - 40, this.height - 27, 80, 18).build());
      this.rebuildFilter();
      this.setInitialFocus(this.searchField);
   }

   private void rebuildFilter() {
      String var1 = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT);
      this.filtered = this.sourceBiomes
         .stream()
         .filter(
            var1x -> var1.isBlank()
               || var1x.toLowerCase(Locale.ROOT).contains(var1)
               || BiomeCatalog.friendlyName(var1x).toLowerCase(Locale.ROOT).contains(var1)
         )
         .toList();
      this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, this.filtered.size() - this.visibleRows));
      this.updateRows();
   }

   private void updateRows() {
      if (!this.rowButtons.isEmpty()) {
         for (int var1 = 0; var1 < this.visibleRows; var1++) {
            ButtonWidget var2 = this.rowButtons.get(var1);
            int var3 = this.scrollOffset + var1;
            if (var3 < this.filtered.size()) {
               String var4 = this.filtered.get(var3);
               String var5 = var4.equals(this.selectedBiome) ? "✓ " : "";
               var2.setMessage(Text.literal(var5 + BiomeCatalog.friendlyName(var4)));
               var2.visible = true;
               var2.active = true;
            } else {
               var2.visible = false;
               var2.active = false;
            }
         }
      }
   }

   private void selectRow(int var1) {
      int var2 = this.scrollOffset + var1;
      if (var2 >= 0 && var2 < this.filtered.size()) {
         String var3 = this.filtered.get(var2);
         if (this.onSelected != null) {
            this.onSelected.accept(var3);
         }

         MinecraftClient.getInstance().setScreen(this.parent);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (!this.filtered.isEmpty() && verticalAmount != 0.0) {
         int var9 = Math.max(0, this.filtered.size() - this.visibleRows);
         this.scrollOffset = Math.max(0, Math.min(var9, this.scrollOffset + (verticalAmount < 0.0 ? 3 : -3)));
         this.updateRows();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.searchClearVisible() && this.insideSearchClear(mouseX, mouseY)) {
         this.searchField.setText("");
         return true;
      } else if (button == 0 && this.isOnScrollbar(mouseX, mouseY) && this.maxScrollOffset() > 0) {
         this.draggingScrollbar = true;
         this.setScrollFromMouse(mouseY);
         return true;
      } else {
         return SeasoningUiHelper.afterMouseClicked(super.mouseClicked(mouseX, mouseY, button), this, button);
      }
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

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 16777215);
      this.renderSearchClear(context, mouseX, mouseY);
      this.renderScrollbar(context);
      if (this.filtered.isEmpty()) {
         context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("No matching biomes"),
            this.width / 2,
            this.listTop + Math.max(8, (this.listBottom - this.listTop) / 2 - 4),
            10526880
         );
      }

      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(this.filtered.size() + " biome" + (this.filtered.size() == 1 ? "" : "s") + " available"),
         this.width / 2,
         this.height - 42,
         10526880
      );
   }

   @Override
   public void close() {
      MinecraftClient.getInstance().setScreen(this.parent);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private int maxScrollOffset() {
      return Math.max(0, this.filtered.size() - this.visibleRows);
   }

   private boolean isOnScrollbar(double var1, double var3) {
      int var5 = this.left + this.boxWidth - 5;
      return var1 >= var5 && var1 <= var5 + 4 && var3 >= this.listTop && var3 <= this.listBottom;
   }

   private int scrollbarThumbHeight() {
      int var1 = Math.max(1, this.listBottom - this.listTop);
      int var2 = Math.max(1, this.filtered.size());
      return Math.max(18, Math.min(var1, (int)Math.round(var1 * ((double)this.visibleRows / var2))));
   }

   private void setScrollFromMouse(double var1) {
      int var3 = this.maxScrollOffset();
      if (var3 > 0) {
         int var4 = Math.max(1, this.listBottom - this.listTop);
         int var5 = this.scrollbarThumbHeight();
         double var6 = Math.max(1, var4 - var5);
         double var8 = (var1 - this.listTop - var5 / 2.0) / var6;
         this.scrollOffset = (int)Math.round(Math.max(0.0, Math.min(1.0, var8)) * var3);
         this.updateRows();
      }
   }

   private void renderScrollbar(DrawContext var1) {
      int var2 = this.left + this.boxWidth - 5;
      var1.fill(var2, this.listTop, var2 + 4, this.listBottom, 1345664309);
      int var3 = this.maxScrollOffset();
      if (var3 > 0) {
         int var4 = this.scrollbarThumbHeight();
         int var5 = this.listTop + (int)Math.round((this.listBottom - this.listTop - var4) * ((double)this.scrollOffset / var3));
         var1.fill(var2, var5, var2 + 4, var5 + var4, this.draggingScrollbar ? -2039584 : -5592406);
      }
   }

   private boolean searchClearVisible() {
      return this.searchField != null && !this.searchField.getText().isBlank();
   }

   private boolean insideSearchClear(double var1, double var3) {
      return var1 >= this.searchClearX && var1 < this.searchClearX + 14 && var3 >= this.searchClearY && var3 < this.searchClearY + 14;
   }

   private void renderSearchClear(DrawContext var1, int var2, int var3) {
      if (this.searchClearVisible()) {
         boolean var4 = this.insideSearchClear(var2, var3);
         var1.fill(this.searchClearX, this.searchClearY, this.searchClearX + 14, this.searchClearY + 14, var4 ? -8947849 : -11250604);
         var1.drawCenteredTextWithShadow(this.textRenderer, Text.literal("×"), this.searchClearX + 7, this.searchClearY + 3, 16777215);
      }
   }
}
