// 新增：并发执行工具
package com.Quantitative.common.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高性能并发执行器
 */
public class TradingExecutor {
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger taskCount;
    private final String name;

    public TradingExecutor(String name, int corePoolSize) {
        this.name = name;
        this.taskCount = new AtomicInteger(0);
        
        // 创建线程池
        this.executor = new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new TradingThreadFactory(name),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    /**
     * 提交任务
     */
    public <T> Future<T> submit(Callable<T> task) {
        taskCount.incrementAndGet();
        return executor.submit(wrapTask(task));
    }

    /**
     * 提交异步任务
     */
    public CompletableFuture<Void> submitAsync(Runnable task) {
        taskCount.incrementAndGet();
        return CompletableFuture.runAsync(wrapRunnable(task), executor);
    }

    /**
     * 定时任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
    }

    /**
     * 包装任务（添加监控）
     */
    private <T> Callable<T> wrapTask(Callable<T> task) {
        return () -> {
            long startTime = System.currentTimeMillis();
            try {
                return task.call();
            } finally {
                taskCount.decrementAndGet();
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 1000) { // 记录慢任务
                    System.out.printf("⚠️ 慢任务检测: %s 耗时 %dms%n", task.getClass().getSimpleName(), duration);
                }
            }
        };
    }

    private Runnable wrapRunnable(Runnable task) {
        return () -> {
            long startTime = System.currentTimeMillis();
            try {
                task.run();
            } finally {
                taskCount.decrementAndGet();
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 1000) {
                    System.out.printf("⚠️ 慢任务检测: %s 耗时 %dms%n", task.getClass().getSimpleName(), duration);
                }
            }
        };
    }

    /**
     * 批量执行任务
     */
    public <T> List<T> executeAll(List<Callable<T>> tasks, long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(submit(task));
        }

        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            try {
                results.add(future.get(timeout, unit));
            } catch (TimeoutException e) {
                future.cancel(true);
                throw e;
            }
        }
        return results;
    }

    /**
     * 获取活跃任务数
     */
    public int getActiveTaskCount() {
        return taskCount.get();
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        System.out.printf("🛑 关闭执行器: %s (剩余任务: %d)%n", name, taskCount.get());
        
        executor.shutdown();
        scheduler.shutdown();
        
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 自定义线程工厂
     */
    private static class TradingThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public TradingThreadFactory(String poolName) {
            this.namePrefix = "Trading-" + poolName + "-Thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}