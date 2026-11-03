package cfr.gui.service;

import java.util.List;

/** 即使某个成员反编译失败，也保留 CFR 返回的其他源码和诊断。 */
public record DecompiledSource(String className, String java, List<String> diagnostics) {
    public DecompiledSource {
        diagnostics = List.copyOf(diagnostics);
    }
}
