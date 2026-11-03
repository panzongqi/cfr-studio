package cfr.gui.service;

import cfr.gui.i18n.I18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** 用户打开的 JAR、WAR 或 CLASS 文件，并记录读取时的版本。 */
public record InputFile(Path path, Kind kind, long size, long modifiedMillis) {
    public enum Kind { ARCHIVE, CLASS }

    public static InputFile open(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new IOException(I18n.t("error.fileMissing", absolute));
        }
        if (!Files.isReadable(absolute)) {
            throw new IOException(I18n.t("error.fileUnreadable", absolute));
        }
        String name = absolute.getFileName().toString().toLowerCase(Locale.ROOT);
        Kind kind = name.endsWith(".jar") || name.endsWith(".war") ? Kind.ARCHIVE
                : name.endsWith(".class") ? Kind.CLASS : null;
        if (kind == null) {
            throw new IOException(I18n.t("error.fileUnsupported"));
        }
        return new InputFile(absolute, kind, Files.size(absolute),
                Files.getLastModifiedTime(absolute).toMillis());
    }
}
