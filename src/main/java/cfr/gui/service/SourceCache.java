package cfr.gui.service;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 有界 LRU 缓存；文件内容或反编译参数变化后不会复用旧源码。 */
public final class SourceCache {
    private final int maximumEntries;
    private final LinkedHashMap<Key, DecompiledSource> entries = new LinkedHashMap<>(16, 0.75f, true);

    public SourceCache(int maximumEntries) {
        if (maximumEntries < 1) {
            throw new IllegalArgumentException("缓存容量必须大于 0");
        }
        this.maximumEntries = maximumEntries;
    }

    public synchronized Optional<DecompiledSource> get(InputFile input, ClassEntry entry,
                                                       DecompilerOptions options) {
        return Optional.ofNullable(entries.get(new Key(input, entry, options)));
    }

    public synchronized void put(InputFile input, ClassEntry entry, DecompilerOptions options,
                                 DecompiledSource source) {
        entries.put(new Key(input, entry, options), source);
        while (entries.size() > maximumEntries) {
            var iterator = entries.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    private record Key(InputFile input, ClassEntry entry, DecompilerOptions options) { }
}
