package dev.cobblesnack.client;

import dev.cobblesnack.calc.Seasoning;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SeasoningSelectionScreen extends Screen {
   private static final int ROW_HEIGHT = 18;
   private static final int BUTTON_HEIGHT = 16;
   private static final int ICON_BOX = 16;
   private static Seasoning.PickerGroup lastGroup = Seasoning.PickerGroup.ALL;
   private final Screen parent;
   private final Seasoning selected;
   private final Consumer<Seasoning> onSelected;
   private TextFieldWidget searchField;
   private final List<ButtonWidget> rowButtons = new ArrayList<>();
   private final List<ButtonWidget> tabButtons = new ArrayList<>();
   private List<Seasoning> filtered = List.of();
   private int scrollOffset;
   private int visibleRows;
   private int left;
   private int boxWidth;
   private int rowButtonWidth;
   private int listTop;
   private int listBottom;
   private boolean draggingScrollbar;
   private int searchClearX;
   private int searchClearY;
   private static final int SEARCH_CLEAR_SIZE = 14;

   public SeasoningSelectionScreen(Screen var1, Seasoning var2, Consumer<Seasoning> var3) {
      super(Text.literal("Select seasoning"));
      this.parent = var1;
      this.selected = var2 == null ? Seasoning.NONE : var2;
      this.onSelected = var3;
   }

   @Override
   protected void init() {
      int var2 = Arrays.stream(Seasoning.values()).map(var0 -> var0.displayName).mapToInt(var1x -> this.textRenderer.getWidth(var1x)).max().orElse(160);
      this.rowButtonWidth = Math.min(this.width - 76, Math.max(190, var2 + 22));
      this.boxWidth = Math.min(this.width - 20, this.rowButtonWidth + 16 + 18);
      this.left = (this.width - this.boxWidth) / 2;
      this.searchField = new TextFieldWidget(this.textRenderer, this.left, 32, this.boxWidth, 18, Text.literal("Search seasonings"));
      this.searchField.setMaxLength(100);
      this.searchField.setPlaceholder(Text.literal("Search berry or effect..."));
      this.searchField.setChangedListener(var1x -> {
         this.scrollOffset = 0;
         this.rebuildFilter();
      });
      this.addDrawableChild(this.searchField);
      this.searchClearX = this.left + this.boxWidth - 14 - 2;
      this.searchClearY = 34;
      Seasoning.PickerGroup[] var3 = Seasoning.PickerGroup.values();
      byte var4 = 3;
      int var5 = Math.max(44, (this.boxWidth - var4 * (var3.length - 1)) / var3.length);
      this.tabButtons.clear();

      for (int var1 = 0; var1 < var3.length; var1++) {
         Seasoning.PickerGroup var6 = var3[var1];
         int var7 = this.left + var1 * (var5 + var4);
         ButtonWidget var8 = this.addDrawableChild(ButtonWidget.builder(Text.literal(tabLabel(var6)), var2x -> {
            lastGroup = var6;
            this.scrollOffset = 0;
            this.rebuildFilter();
         }).dimensions(var7, 56, var5, 17).build());
         this.tabButtons.add(var8);
      }

      this.listTop = 79;
      this.listBottom = Math.max(this.listTop + 18, this.height - 56);
      this.visibleRows = Math.max(1, (this.listBottom - this.listTop) / 18);
      this.rowButtons.clear();

      for (int var9 = 0; var9 < this.visibleRows; var9++) {
         int var10 = var9;
         ButtonWidget var11 = ButtonWidget.builder(Text.empty(), var2x -> this.selectRow(var10))
            .dimensions(this.left + 16 + 4, this.listTop + var9 * 18, this.rowButtonWidth, 16)
            .build();
         this.rowButtons.add(var11);
         this.addDrawableChild(var11);
      }

      this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), var1x -> this.close()).dimensions(this.width / 2 - 40, this.height - 25, 80, 17).build());
      this.rebuildFilter();
      this.setInitialFocus(this.searchField);
   }

   private static String tabLabel(Seasoning.PickerGroup var0) {
      return switch (var0) {
         case ALL -> "All";
         case EV -> "EV";
         case TYPE -> "Type";
         case EGG_GROUP -> "Egg";
         case NATURE -> "Nature";
         case SPAWN -> "Spawn";
         case OTHER -> "Other";
      };
   }

   private void rebuildFilter() {
      String var1 = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase(Locale.ROOT).replace('_', ' ');
      this.filtered = Arrays.stream(Seasoning.values())
         .filter(var0 -> var0 == Seasoning.NONE || lastGroup == Seasoning.PickerGroup.ALL || var0.pickerGroup() == lastGroup)
         .filter(var1x -> var1.isBlank() || var1x.searchText().contains(var1))
         .toList();
      this.scrollOffset = Math.max(0, Math.min(this.maxScrollOffset(), this.scrollOffset));
      this.updateTabs();
      this.updateRows();
   }

   private void updateTabs() {
      Seasoning.PickerGroup[] var1 = Seasoning.PickerGroup.values();

      for (int var2 = 0; var2 < this.tabButtons.size() && var2 < var1.length; var2++) {
         ButtonWidget var3 = this.tabButtons.get(var2);
         Seasoning.PickerGroup var4 = var1[var2];
         var3.setMessage(Text.literal((var4 == lastGroup ? "• " : "") + tabLabel(var4)));
      }
   }

   private void updateRows() {
      for (int var1 = 0; var1 < this.rowButtons.size(); var1++) {
         ButtonWidget var2 = this.rowButtons.get(var1);
         int var3 = this.scrollOffset + var1;
         if (var3 < this.filtered.size()) {
            Seasoning var4 = this.filtered.get(var3);
            String var5 = var4 == this.selected ? "✓ " : "";
            var2.setMessage(Text.literal(var5 + var4.displayName));
            var2.visible = true;
            var2.active = true;
         } else {
            var2.visible = false;
            var2.active = false;
         }
      }
   }

   private int maxScrollOffset() {
      return Math.max(0, this.filtered.size() - this.visibleRows);
   }

   private void selectRow(int var1) {
      int var2 = this.scrollOffset + var1;
      if (var2 >= 0 && var2 < this.filtered.size()) {
         if (this.onSelected != null) {
            this.onSelected.accept(this.filtered.get(var2));
         }

         MinecraftClient.getInstance().setScreen(this.parent);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount != 0.0 && this.maxScrollOffset() > 0) {
         this.scrollOffset = Math.max(0, Math.min(this.maxScrollOffset(), this.scrollOffset + (verticalAmount < 0.0 ? 2 : -2)));
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

   private boolean isOnScrollbar(double var1, double var3) {
      int var5 = this.left + this.boxWidth - 5;
      return var1 >= var5 && var1 <= var5 + 4 && var3 >= this.listTop && var3 <= this.listBottom;
   }

   private void setScrollFromMouse(double var1) {
      int var3 = this.maxScrollOffset();
      if (var3 > 0) {
         int var4 = Math.max(1, this.listBottom - this.listTop);
         int var5 = this.scrollbarThumbHeight(var4);
         double var6 = Math.max(1, var4 - var5);
         double var8 = (var1 - this.listTop - var5 / 2.0) / var6;
         this.scrollOffset = (int)Math.round(Math.max(0.0, Math.min(1.0, var8)) * var3);
         this.updateRows();
      }
   }

   private int scrollbarThumbHeight(int var1) {
      int var2 = Math.max(1, this.filtered.size());
      return Math.max(18, Math.min(var1, (int)Math.round(var1 * ((double)this.visibleRows / var2))));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 16777215);
      this.renderSearchClear(context, mouseX, mouseY);
      this.renderRowIcons(context);
      this.renderScrollbar(context);
      if (this.filtered.isEmpty()) {
         context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("No matching seasonings"),
            this.width / 2,
            this.listTop + Math.max(8, (this.listBottom - this.listTop) / 2 - 4),
            10526880
         );
      }

      String var5 = this.filtered.size() + " seasoning" + (this.filtered.size() == 1 ? "" : "s") + " • " + tabLabel(lastGroup);
      context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(var5), this.width / 2, this.height - 42, 10526880);
      SeasoningUiHelper.afterPickerRender(this, context, mouseX, mouseY);
   }

   private void renderRowIcons(DrawContext var1) {
      for (int var2 = 0; var2 < this.rowButtons.size(); var2++) {
         ButtonWidget var4 = this.rowButtons.get(var2);
         int var3;
         if (var4.visible && (var3 = this.scrollOffset + var2) >= 0 && var3 < this.filtered.size()) {
            Seasoning var5 = this.filtered.get(var3);
            int var6 = var4.getX() - 16 - 4;
            int var7 = var4.getY();
            var1.fill(var6, var7, var6 + 16, var7 + 16, -1067491489);
            var1.fill(var6, var7, var6 + 16, var7 + 1, -4737097);
            var1.fill(var6, var7 + 16 - 1, var6 + 16, var7 + 16, -4737097);
            var1.fill(var6, var7, var6 + 1, var7 + 16, -4737097);
            var1.fill(var6 + 16 - 1, var7, var6 + 16, var7 + 16, -4737097);
            ItemStack var8 = this.seasoningStack(var5);
            if (!var8.isEmpty()) {
               var1.drawItem(var8, var6, var7);
            }
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

   private void renderScrollbar(DrawContext var1) {
      int var2 = this.left + this.boxWidth - 5;
      int var3 = Math.max(1, this.listBottom - this.listTop);
      var1.fill(var2, this.listTop, var2 + 4, this.listBottom, 1345664309);
      if (this.maxScrollOffset() > 0) {
         int var4 = this.scrollbarThumbHeight(var3);
         int var5 = this.listTop + (int)Math.round((var3 - var4) * ((double)this.scrollOffset / this.maxScrollOffset()));
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

   @Override
   public void close() {
      MinecraftClient.getInstance().setScreen(this.parent);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return SeasoningUiHelper.handlePickerKey(this, keyCode, scanCode, modifiers) ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }
}
