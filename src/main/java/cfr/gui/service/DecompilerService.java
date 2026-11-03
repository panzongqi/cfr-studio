package cfr.gui.service;

import cfr.gui.i18n.I18n;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** CFR 官方 API 封装，不依赖 CFR 的内部实现或控制台输出。 */
public final class DecompilerService {
    public DecompiledSource decompileClass(InputFile input, ClassEntry entry, DecompilerOptions options)
            throws IOException, InterruptedException {
        if (input.kind() == InputFile.Kind.CLASS) {
            return decompileClass(input.path(), options);
        }
        Map<String, String> cfrOptions = new java.util.HashMap<>(options.toCfrOptions());
        // CFR 的 jarfilter 按顶层类匹配；嵌套类需要随其外部类一起分析。
        String className = input.path().getFileName().toString()
                .toLowerCase(java.util.Locale.ROOT).endsWith(".war")
                ? entry.displayName() : entry.binaryName();
        int dollar = className.indexOf('$');
        if (dollar >= 0) {
            className = className.substring(0, dollar);
        }
        String prefix = input.path().getFileName().toString()
                .toLowerCase(java.util.Locale.ROOT).endsWith(".war") ? ".*" : "^";
        cfrOptions.put("jarfilter", prefix + java.util.regex.Pattern.quote(className));
        return analyse(input.path(), cfrOptions);
    }

    public DecompiledSource decompileClass(Path classFile, DecompilerOptions options)
            throws IOException, InterruptedException {
        return analyse(classFile, options.toCfrOptions());
    }

    /** 每次回调只保留一个类的源码，按 CFR 返回的包路径写入 UTF-8 文件。 */
    public ExportResult export(InputFile input, DecompilerOptions options, Path outputDirectory,
                               java.util.function.IntConsumer progress,
                               java.util.function.BooleanSupplier cancelled)
            throws IOException, InterruptedException {
        Files.createDirectories(outputDirectory);
        Map<String, String> cfrOptions = new java.util.HashMap<>(options.toCfrOptions());
        cfrOptions.put("outputencoding", "UTF-8");
        cfrOptions.put("silent", "true");
        AtomicInteger count = new AtomicInteger();
        List<String> diagnostics = java.util.Collections.synchronizedList(new ArrayList<>());
        OutputSinkFactory sink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                if (sinkType == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                    return List.of(SinkClass.DECOMPILED);
                }
                if (sinkType == SinkType.EXCEPTION && available.contains(SinkClass.EXCEPTION_MESSAGE)) {
                    return List.of(SinkClass.EXCEPTION_MESSAGE);
                }
                return List.of();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                    return value -> {
                        SinkReturns.Decompiled decompiled = (SinkReturns.Decompiled) value;
                        try {
                            writeSource(outputDirectory, decompiled);
                            count.incrementAndGet();
                            progress.accept(count.get());
                        } catch (IOException exception) {
                            diagnostics.add(I18n.t("error.writeFailed",
                                    decompiled.getClassName(), exception.getMessage()));
                        }
                    };
                }
                if (sinkType == SinkType.EXCEPTION && sinkClass == SinkClass.EXCEPTION_MESSAGE) {
                    return value -> diagnostics.add(describe((SinkReturns.ExceptionMessage) value));
                }
                return value -> { };
            }
        };
        new CfrDriver.Builder().withOptions(cfrOptions).withOutputSink(sink).build()
                .analyse(List.of(input.path().toString()));
        if (cancelled.getAsBoolean()) {
            throw new InterruptedException(I18n.t("error.exportCancelled"));
        }
        return new ExportResult(count.get(), List.copyOf(diagnostics));
    }

    private void writeSource(Path root, SinkReturns.Decompiled source) throws IOException {
        String packageName = source.getPackageName() == null ? "" : source.getPackageName();
        Path directory = root;
        if (!packageName.isBlank()) {
            for (String part : packageName.split("\\.")) {
                directory = directory.resolve(validFileName(part));
            }
        }
        Files.createDirectories(directory);
        String className = source.getClassName() == null ? "Decompiled" : source.getClassName();
        Path output = directory.resolve(validFileName(className.replace('$', '_')) + ".java").normalize();
        if (!output.startsWith(root)) {
            throw new IOException(I18n.t("error.invalidClassPath"));
        }
        if (Files.exists(output)) {
            throw new IOException(I18n.t("error.outputExists", output));
        }
        Files.writeString(output, source.getJava(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String validFileName(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9_$\\-]", "_");
        return safe.isBlank() ? "_" : safe;
    }

    public record ExportResult(int classes, List<String> diagnostics) {
        public ExportResult { diagnostics = List.copyOf(diagnostics); }
    }

    private DecompiledSource analyse(Path classFile, Map<String, String> options)
            throws IOException, InterruptedException {
        List<DecompiledSource> results = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        OutputSinkFactory sink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                if (sinkType == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                    return List.of(SinkClass.DECOMPILED);
                }
                if (sinkType == SinkType.EXCEPTION && available.contains(SinkClass.EXCEPTION_MESSAGE)) {
                    return List.of(SinkClass.EXCEPTION_MESSAGE);
                }
                if (sinkType == SinkType.PROGRESS && available.contains(SinkClass.STRING)) {
                    return List.of(SinkClass.STRING);
                }
                return List.of();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                    return value -> results.add(new DecompiledSource(
                            ((SinkReturns.Decompiled) value).getClassName(),
                            ((SinkReturns.Decompiled) value).getJava(), List.of()));
                }
                if (sinkType == SinkType.EXCEPTION && sinkClass == SinkClass.EXCEPTION_MESSAGE) {
                    return value -> diagnostics.add(describe((SinkReturns.ExceptionMessage) value));
                }
                return value -> { };
            }
        };
        try {
            new CfrDriver.Builder().withOptions(options).withOutputSink(sink).build()
                    .analyse(List.of(classFile.toString()));
        } catch (RuntimeException exception) {
            diagnostics.add(exception.getMessage() == null ? exception.toString() : exception.getMessage());
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException(I18n.t("error.decompileCancelled"));
        }
        if (results.isEmpty()) {
            throw new IOException(diagnostics.isEmpty()
                    ? I18n.t("error.noSource")
                    : String.join("\n", diagnostics));
        }
        DecompiledSource first = results.getFirst();
        return new DecompiledSource(first.className(), first.java(), diagnostics);
    }

    private String describe(SinkReturns.ExceptionMessage message) {
        StringBuilder text = new StringBuilder();
        if (message.getPath() != null) {
            text.append(I18n.t("diagnostic.location", message.getPath()));
        }
        text.append(message.getMessage());
        if (message.getThrownException() != null) {
            text.append(I18n.t("diagnostic.cause", message.getThrownException()));
        }
        return text.toString();
    }
}
