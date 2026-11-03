package cfr.gui.service;

/** 归档中的一个类；binaryName 保留 CFR jarfilter 使用的完整名称。 */
public record ClassEntry(String entryName, String binaryName, String displayName) {
    public static ClassEntry from(String entryName) {
        String binaryName = entryName.endsWith(".class")
                ? entryName.substring(0, entryName.length() - 6).replace('/', '.') : entryName;
        String displayName = binaryName.startsWith("WEB-INF.classes.")
                ? binaryName.substring("WEB-INF.classes.".length()) : binaryName;
        return new ClassEntry(entryName, binaryName, displayName);
    }

    public ClassEntry(String entryName, String displayName) {
        this(entryName, displayName, displayName);
    }
}
