package cfr.gui.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceExporter {
    public void saveCurrent(Path destination, DecompiledSource source) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(destination, source.java(), StandardCharsets.UTF_8);
    }

    public Path defaultClassName(DecompiledSource source) {
        String name = source.className() == null || source.className().isBlank()
                ? "Decompiled" : source.className();
        name = name.replace('$', '_').replaceAll("[^A-Za-z0-9_\\-]", "_");
        return Path.of(name + ".java");
    }
}
