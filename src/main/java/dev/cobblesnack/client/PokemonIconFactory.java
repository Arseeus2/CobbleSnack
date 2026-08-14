package dev.cobblesnack.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.cobblesnack.data.SpeciesInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class PokemonIconFactory {
   private static final Identifier MODEL_ITEM_ID = Identifier.of("cobblemon", "pokemon_model");
   private static final Identifier POKEMON_COMPONENT_ID = Identifier.of("cobblemon", "pokemon_item");
   private static final Map<String, ItemStack> CACHE = new ConcurrentHashMap<>();

   private PokemonIconFactory() {
   }

   public static ItemStack iconFor(SpeciesInfo info) {
      return info == null ? ItemStack.EMPTY : CACHE.computeIfAbsent(info.resourceId(), ignored -> create(info));
   }

   private static ItemStack create(SpeciesInfo info) {
      try {
         Item item = Registries.ITEM.get(MODEL_ITEM_ID);
         if (item != null && item != Items.AIR) {
            ComponentType component = Registries.DATA_COMPONENT_TYPE.get(POKEMON_COMPONENT_ID);
            if (component == null) {
               return ItemStack.EMPTY;
            }

            Codec codec = component.getCodec();
            if (codec == null) {
               return ItemStack.EMPTY;
            }

            JsonObject json = new JsonObject();
            json.addProperty("species", info.resourceId());
            json.add("aspects", new JsonArray());
            Object value = codec.parse(JsonOps.INSTANCE, json).result().orElse(null);
            if (value == null) {
               return ItemStack.EMPTY;
            }

            ItemStack stack = new ItemStack(item);
            stack.set(component, value);
            return stack;
         } else {
            return ItemStack.EMPTY;
         }
      } catch (RuntimeException ignored) {
         return ItemStack.EMPTY;
      }
   }
}
