package me.garfxld.generators;

import com.google.gson.JsonObject;
import me.garfxld.DataGenerator;
import net.minecraft.core.registries.BuiltInRegistries;

public class SoundEventGenerator implements DataGenerator {

    @Override
    public String generate() {
        var registry = BuiltInRegistries.SOUND_EVENT;

        var soundEventsJson = new JsonObject();

        for (var soundEvent : registry) {
            final var key = registry.getKey(soundEvent);

            var soundEventJson = new JsonObject();
            soundEventJson.addProperty("id", registry.getId(soundEvent));
            soundEvent.fixedRange().ifPresent(range -> soundEventJson.addProperty("fixedRange", range));

            soundEventsJson.add(key.toString(), soundEventJson);
        }

        return soundEventsJson.toString();
    }

}
