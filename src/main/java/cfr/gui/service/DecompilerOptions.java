package cfr.gui.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 只包含界面明确支持的 CFR 参数；其余保持 CFR 默认值。 */
public record DecompilerOptions(
        boolean comments, boolean showVersion, boolean decodeLambdas,
        boolean innerClasses, boolean renameIllegalIdentifiers, boolean antiObfuscation,
        boolean lenient, boolean removeBoilerplate, boolean bytecodeLocations,
        String extraClassPath) {

    public static DecompilerOptions defaults() {
        return builder().build();
    }

    public Map<String, String> toCfrOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("comments", Boolean.toString(comments));
        options.put("showversion", Boolean.toString(showVersion));
        options.put("decodelambdas", Boolean.toString(decodeLambdas));
        options.put("innerclasses", Boolean.toString(innerClasses));
        options.put("renameillegalidents", Boolean.toString(renameIllegalIdentifiers));
        options.put("antiobf", Boolean.toString(antiObfuscation));
        options.put("lenient", Boolean.toString(lenient));
        options.put("removeboilerplate", Boolean.toString(removeBoilerplate));
        options.put("trackbytecodeloc", Boolean.toString(bytecodeLocations));
        options.put("hideutf", "false");
        if (!extraClassPath.isBlank()) {
            options.put("extraclasspath", extraClassPath.trim());
        }
        return options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean comments = true;
        private boolean showVersion;
        private boolean decodeLambdas = true;
        private boolean innerClasses = true;
        private boolean renameIllegalIdentifiers = true;
        private boolean antiObfuscation;
        private boolean lenient;
        private boolean removeBoilerplate = true;
        private boolean bytecodeLocations;
        private String extraClassPath = "";

        public Builder comments(boolean value) { comments = value; return this; }
        public Builder showVersion(boolean value) { showVersion = value; return this; }
        public Builder decodeLambdas(boolean value) { decodeLambdas = value; return this; }
        public Builder innerClasses(boolean value) { innerClasses = value; return this; }
        public Builder renameIllegalIdentifiers(boolean value) { renameIllegalIdentifiers = value; return this; }
        public Builder antiObfuscation(boolean value) { antiObfuscation = value; return this; }
        public Builder lenient(boolean value) { lenient = value; return this; }
        public Builder removeBoilerplate(boolean value) { removeBoilerplate = value; return this; }
        public Builder bytecodeLocations(boolean value) { bytecodeLocations = value; return this; }
        public Builder extraClassPath(String value) { extraClassPath = Objects.requireNonNull(value); return this; }
        public DecompilerOptions build() {
            return new DecompilerOptions(comments, showVersion, decodeLambdas, innerClasses,
                    renameIllegalIdentifiers, antiObfuscation, lenient, removeBoilerplate,
                    bytecodeLocations, extraClassPath);
        }
    }
}
