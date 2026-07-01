package com.ohyesai.next.trace;


import org.slf4j.MDC;

public class ProxyThread {

    public static Thread startVirtualThread(Runnable task) {
        String traceId = MDC.get(TraceInterceptor.TRACE_ID);
        return Thread.startVirtualThread(() -> {
            try (MDC.MDCCloseable _ = MDC.putCloseable(TraceInterceptor.TRACE_ID, traceId)) {
                task.run();
            }
        });
    }
}
