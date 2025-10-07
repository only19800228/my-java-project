package com.Quantitative.monitoring;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统监控器
 */
public class SystemMonitor {
    private static final SystemMonitor INSTANCE = new SystemMonitor();
    
    // 监控指标
    private final AtomicLong totalBarsProcessed = new AtomicLong(0);
    private final AtomicLong totalSignalsGenerated = new AtomicLong(0);
    private final AtomicLong totalOrdersExecuted = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong memoryUsage = new AtomicLong(0);
    
    private SystemMonitor() {}
    
    public static SystemMonitor getInstance() {
        return INSTANCE;
    }
    
    /**
     * 记录处理的Bar数量
     */
    public void recordBarProcessed() {
        totalBarsProcessed.incrementAndGet();
    }
    
    /**
     * 记录生成的信号数量
     */
    public void recordSignalGenerated() {
        totalSignalsGenerated.incrementAndGet();
    }
    
    /**
     * 记录执行的订单数量
     */
    public void recordOrderExecuted() {
        totalOrdersExecuted.incrementAndGet();
    }
    
    /**
     * 记录错误
     */
    public void recordError() {
        totalErrors.incrementAndGet();
    }
    
    /**
     * 更新内存使用情况
     */
    public void updateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryUsage.set(usedMemory);
    }
    
    /**
     * 获取监控快照
     */
    public MonitoringSnapshot getSnapshot() {
        updateMemoryUsage();
        return new MonitoringSnapshot(
            totalBarsProcessed.get(),
            totalSignalsGenerated.get(),
            totalOrdersExecuted.get(),
            totalErrors.get(),
            memoryUsage.get()
        );
    }
    
    /**
     * 检查系统健康状态
     */
    public HealthStatus checkHealth() {
        MonitoringSnapshot snapshot = getSnapshot();
        Runtime runtime = Runtime.getRuntime();
        
        double memoryUsageRatio = (double) snapshot.getMemoryUsage() / runtime.maxMemory();
        
        if (memoryUsageRatio > 0.9) {
            return HealthStatus.CRITICAL;
        } else if (memoryUsageRatio > 0.7) {
            return HealthStatus.WARNING;
        } else if (snapshot.getTotalErrors() > 100) {
            return HealthStatus.WARNING;
        } else {
            return HealthStatus.HEALTHY;
        }
    }
    
    /**
     * 重置监控器
     */
    public void reset() {
        totalBarsProcessed.set(0);
        totalSignalsGenerated.set(0);
        totalOrdersExecuted.set(0);
        totalErrors.set(0);
        memoryUsage.set(0);
    }
    
    /**
     * 监控快照
     */
    public static class MonitoringSnapshot {
        private final long totalBarsProcessed;
        private final long totalSignalsGenerated;
        private final long totalOrdersExecuted;
        private final long totalErrors;
        private final long memoryUsage;
        
        public MonitoringSnapshot(long bars, long signals, long orders, long errors, long memory) {
            this.totalBarsProcessed = bars;
            this.totalSignalsGenerated = signals;
            this.totalOrdersExecuted = orders;
            this.totalErrors = errors;
            this.memoryUsage = memory;
        }
        
        // Getter方法
        public long getTotalBarsProcessed() { return totalBarsProcessed; }
        public long getTotalSignalsGenerated() { return totalSignalsGenerated; }
        public long getTotalOrdersExecuted() { return totalOrdersExecuted; }
        public long getTotalErrors() { return totalErrors; }
        public long getMemoryUsage() { return memoryUsage; }
        
        @Override
        public String toString() {
            return String.format(
                "MonitoringSnapshot{bars=%d, signals=%d, orders=%d, errors=%d, memory=%.2fMB}",
                totalBarsProcessed, totalSignalsGenerated, totalOrdersExecuted, totalErrors,
                memoryUsage / (1024.0 * 1024.0)
            );
        }
    }
    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        HEALTHY("正常"),
        WARNING("警告"),
        CRITICAL("严重");
        
        private final String description;
        
        HealthStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}