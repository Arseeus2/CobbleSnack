package dev.cobblesnack.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PokeSnackIconButton extends ButtonWidget {
   private static final Identifier POKE_SNACK_ID = Identifier.of("cobblemon", "poke_snack");

   public PokeSnackIconButton(int x, int y, PressAction onPress) {
      super(x, y, 20, 20, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
      this.setTooltip(Tooltip.of(Text.literal("Open CobbleSnack")));
   }

   @Override
   protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      super.renderWidget(context, mouseX, mouseY, delta);
      Item item = Registries.ITEM.get(POKE_SNACK_ID);
      ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
      if (!stack.isEmpty()) {
         context.drawItem(stack, this.getX() + 2, this.getY() + 2);
      }
   }
}
