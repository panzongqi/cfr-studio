package cfr.gui.service;

import cfr.gui.i18n.I18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public final class ArchiveService {
    public ArchiveContent inspect(InputFile input) throws IOException {
        if (input.kind() == InputFile.Kind.CLASS) {
            String name = input.path().getFileName().toString();
            return new ArchiveContent(List.of(new ClassEntry(name, name)), 1);
        }
        try (ZipFile zip = new ZipFile(input.path().toFile(), StandardCharsets.UTF_8)) {
            List<ClassEntry> entries = new ArrayList<>();
            for (var iterator = zip.entries(); iterator.hasMoreElements(); ) {
                var entry = iterator.nextElement();
                if (!entry.isDirectory() && isDecompilableClass(entry.getName())) {
                    entries.add(ClassEntry.from(entry.getName()));
                }
            }
            entries.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
            return new ArchiveContent(List.copyOf(entries), entries.size());
        } catch (ZipException exception) {
            throw new IOException(I18n.t("error.archiveUnreadable"), exception);
        }
    }

    private boolean isDecompilableClass(String name) {
        return name.endsWith(".class") && !name.equals("module-info.class")
                && !name.equals("package-info.class") && !name.endsWith("/module-info.class")
                && !name.endsWith("/package-info.class") && !name.startsWith("META-INF/versions/");
    }

    public List<ClassEntry> filter(List<ClassEntry> entries, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return entries;
        }
        return entries.stream().filter(entry ->
                entry.displayName().toLowerCase(Locale.ROOT).contains(normalized)).toList();
    }

    public record ArchiveContent(List<ClassEntry> entries, int totalClasses) { }
}
