package me.garfxld.generators;

import com.google.gson.JsonObject;
import me.garfxld.DataGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;

public class EntityTypeGenerator implements DataGenerator {

    @Override
    public String generate() {
        var registry = BuiltInRegistries.ENTITY_TYPE;

        var entityTypesJson = new JsonObject();
        for (var entityType : registry) {
            final var key = registry.getKey(entityType);

            var entityTypeJson = new JsonObject();

            EntityDimensions dimensions = entityType.getDimensions();
            entityTypeJson.addProperty("width", dimensions.width());
            entityTypeJson.addProperty("height", dimensions.height());
            entityTypeJson.addProperty("eyeHeight", dimensions.eyeHeight());

            entityTypesJson.add(key.toString(),  entityTypeJson);
        }

        return entityTypesJson.toString();
    }

}
