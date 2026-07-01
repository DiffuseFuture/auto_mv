package com.ohyesai.next.trace;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public record ProxyExecutors(ExecutorService executorService) implements ExecutorService {

    public static ProxyExecutors newVirtualThreadPerTaskExecutor() {
        return new ProxyExecutors(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public @NonNull List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        return executorService.submit(wrap(task));
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        return executorService.submit(wrap(task), result);
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
        return executorService.submit(wrap(task));
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        executorService.execute(wrap(command));
    }

    private Runnable wrap(Runnable command) {
        String traceId = MDC.get(TraceInterceptor.TRACE_ID);
        return () -> {
            try (MDC.MDCCloseable _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, traceId);) {
                command.run();
            }
        };
    }

    private <T> Callable<T> wrap(Callable<T> command) {
        String traceId = MDC.get(TraceInterceptor.TRACE_ID);
        return () -> {
            try (MDC.MDCCloseable _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, traceId);) {
                return command.call();
            }
        };
    }
}
