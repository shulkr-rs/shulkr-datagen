package me.garfxld.generators;

import com.google.gson.JsonObject;
import me.garfxld.DataGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.AirBlock;

public class ItemGenerator implements DataGenerator {

    @Override
    public String generate() {
        var registry = BuiltInRegistries.ITEM;
        var blockRegistry = BuiltInRegistries.BLOCK;

        var itemsJson = new JsonObject();

        for (var item : registry) {
            final var key = registry.getKey(item);

            var itemJson = new JsonObject();
            itemJson.addProperty("id", registry.getId(item));
            itemJson.addProperty("translationKey", item.getDescriptionId());

            if (item instanceof BlockItem blockItem && !(blockItem.getBlock() instanceof AirBlock)) {
                itemJson.addProperty("correspondingBlock", blockRegistry.getKey(blockItem.getBlock()).toString());
            }

            itemsJson.add(key.toString(), itemJson);
        }

        return itemsJson.toString();
    }

}
