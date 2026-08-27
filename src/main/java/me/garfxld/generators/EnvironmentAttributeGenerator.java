package me.garfxld.generators;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.garfxld.DataGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.EnvironmentAttribute;

import java.util.Locale;

public class EnvironmentAttributeGenerator implements DataGenerator {

    @Override
    public String generate() {
        var registry = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE;
        var typeRegistry = BuiltInRegistries.ATTRIBUTE_TYPE;

        var json = new JsonObject();

        for (var attribute : registry) {
            final var key = registry.getKey(attribute);
            final var typeKey = typeRegistry.getKey(attribute.type());

            var entry = new JsonObject();
            entry.addProperty("type", toPascalCase(typeKey.getPath()));
            entry.add("value", encodeDefaultValue(attribute));

            json.add(key.toString(), entry);
        }

        return json.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JsonElement encodeDefaultValue(EnvironmentAttribute<?> attribute) {
        AttributeType type = attribute.type();
        Object value = attribute.defaultValue();

        DataResult<JsonElement> result = type.valueCodec().encodeStart(JsonOps.INSTANCE, value);
        return result.getOrThrow(
                error -> new IllegalStateException("failed to encode default value for " + attribute + ": " + error));
    }

    private static String toPascalCase(String snakeCase) {
        var sb = new StringBuilder();
        for (String part : snakeCase.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

}
