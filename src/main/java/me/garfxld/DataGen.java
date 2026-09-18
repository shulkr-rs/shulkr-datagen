package me.garfxld;

import me.garfxld.generators.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataGen {

    static void main(String[] args) throws IOException {
        Path output = parseOutput(args);

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        write(output.resolve("version.json"), new VersionGenerator().generate());

        write(output.resolve("block.json"), new BlockGenerator().generate());
        write(output.resolve("block_entity_type.json"), new BlockEntityTypeGenerator().generate());
        write(output.resolve("entity_type.json"), new EntityTypeGenerator().generate());
        write(output.resolve("environment_attribute.json"), new EnvironmentAttributeGenerator().generate());
        write(output.resolve("item.json"), new ItemGenerator().generate());
        write(output.resolve("sound_event.json"), new SoundEventGenerator().generate());

        new DatapackGenerator().generate(output.resolve("datapack"));

        IO.println("Generated build assets in " + output.toAbsolutePath().normalize());
    }

    static Path parseOutput(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (!args[i].equals("--output")) {
                continue;
            }

            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("--output requires a directory");
            }
            return Path.of(args[i + 1]);
        }

        throw new IllegalArgumentException("--output <directory> is required");
    }

    static void write(Path path, String json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
    }

}
