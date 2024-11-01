package net.devnguyen.hermes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class HVirtualThreadExecutor {
    private final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
    private final int maxRunningThread;


    private final AtomicInteger currentRunningThreads = new AtomicInteger(0);

    public HVirtualThreadExecutor(int maxRunningThread) {
        this.maxRunningThread = maxRunningThread;
    }

    public AtomicInteger getCurrentRunningThreads() {
        return currentRunningThreads;
    }

    public boolean isOverload() {
        return currentRunningThreads.get() > maxRunningThread;
    }

    public Future<?> submit(Runnable task) {
        try {
            currentRunningThreads.incrementAndGet();
            return threadPool.submit(task);
        } finally {
            currentRunningThreads.decrementAndGet();
        }
    }


    public void shutdown() {
        threadPool.shutdown();
    }


}
