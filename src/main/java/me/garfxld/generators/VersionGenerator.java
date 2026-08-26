package me.garfxld.generators;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.garfxld.DataGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class VersionGenerator implements DataGenerator {

    @Override
    public String generate() {
        JsonObject version;

        try (InputStream in = VersionGenerator.class.getResourceAsStream("/version.json")) {
            if (in == null) {
                throw new IOException("Could not locate '/version.json' on the classpath");
            }

            version = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new GsonBuilder().setPrettyPrinting().create().toJson(version);
    }

}
