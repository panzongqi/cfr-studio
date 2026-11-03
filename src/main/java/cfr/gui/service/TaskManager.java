package cfr.gui.service;

import javax.swing.SwingUtilities;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** 有界后台任务；结果必须经 EDT 且仍是当前请求才能进入界面。 */
public final class TaskManager implements AutoCloseable {
    private final AtomicLong sequence = new AtomicLong();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "CFR-Worker");
        thread.setDaemon(true);
        return thread;
    });
    private volatile Future<?> currentTask;
    private volatile boolean closed;

    public void submit(Task<?> task) {
        if (closed) {
            return;
        }
        long id = sequence.incrementAndGet();
        Future<?> previous = currentTask;
        if (previous != null) {
            previous.cancel(true);
        }
        currentTask = executor.submit(() -> {
            try {
                Object result = task.work().get();
                SwingUtilities.invokeLater(() -> {
                    if (!closed && sequence.get() == id && !Thread.currentThread().isInterrupted()) {
                        @SuppressWarnings("unchecked")
                        Consumer<Object> success = (Consumer<Object>) task.success();
                        success.accept(result);
                    }
                });
            } catch (Throwable throwable) {
                SwingUtilities.invokeLater(() -> {
                    if (!closed && sequence.get() == id && !(throwable instanceof InterruptedException)) {
                        task.failure().accept(throwable);
                    }
                });
            }
        });
    }

    /** 废弃尚未完成的预览，并尽可能中断当前任务。 */
    public void cancelCurrent() {
        sequence.incrementAndGet();
        Future<?> task = currentTask;
        if (task != null) {
            task.cancel(true);
        }
    }

    @Override
    public void close() {
        closed = true;
        sequence.incrementAndGet();
        executor.shutdownNow();
    }

    public record Task<T>(Work<T> work, Consumer<T> success, Consumer<Throwable> failure) { }

    @FunctionalInterface
    public interface Work<T> {
        T get() throws Exception;
    }
}
