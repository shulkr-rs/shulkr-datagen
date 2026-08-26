package me.garfxld.generators;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;


public class DatapackGenerator {

    private static final String ROOT = "/data";

    public void generate(Path output) throws IOException {
        URL url = DatapackGenerator.class.getResource(ROOT);
        if (url == null) {
            throw new IOException("Could not locate '" + ROOT + "' on the classpath");
        }

        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI for '" + ROOT + "': " + url, e);
        }

        if (Files.exists(output)) {
            deleteDirectory(output);
        }

        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = newFileSystem(uri)) {
                copyDirectory(fs.getPath(ROOT), output);
            }
        } else {
            copyDirectory(Path.of(uri), output);
        }
    }

    private static FileSystem newFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.newFileSystem(uri, Map.of());
        } catch (java.nio.file.FileSystemAlreadyExistsException e) {
            return FileSystems.getFileSystem(uri);
        }
    }

    private static void copyDirectory(Path source, Path destination) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths.sorted()::iterator) {
                Path target = resolve(destination, source.relativize(path));

                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path resolve(Path base, Path relative) {
        Path resolved = base;

        for (Path name : relative) {
            resolved = resolved.resolve(name.toString());
        }

        return resolved;
    }

    private static void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

}
