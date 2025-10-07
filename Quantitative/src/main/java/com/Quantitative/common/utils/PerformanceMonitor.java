package com.Quantitative.common.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控器
 */
public class PerformanceMonitor {
    private static PerformanceMonitor instance;
    private final Map<String, AtomicLong> operationCounts;
    private final Map<String, AtomicLong> operationDurations;
    private final Map<String, Long> operationStartTimes;
    private final boolean enabled;
    
    private PerformanceMonitor() {
        this.operationCounts = new ConcurrentHashMap<>();
        this.operationDurations = new ConcurrentHashMap<>();
        this.operationStartTimes = new ConcurrentHashMap<>();
        this.enabled = true; // 可以从配置读取
    }
    
    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            synchronized (PerformanceMonitor.class) {
                if (instance == null) {
                    instance = new PerformanceMonitor();
                }
            }
        }
        return instance;
    }
    
    /**
     * 开始计时
     */
    public void startOperation(String operationName) {
        if (!enabled) return;
        
        operationStartTimes.put(operationName, System.nanoTime());
    }
    
    /**
     * 结束计时
     */
    public void endOperation(String operationName) {
        if (!enabled) return;
        
        Long startTime = operationStartTimes.remove(operationName);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            
            // 更新计数
            operationCounts.computeIfAbsent(operationName, k -> new AtomicLong(0))
                          .incrementAndGet();
            
            // 更新总时长
            operationDurations.computeIfAbsent(operationName, k -> new AtomicLong(0))
                             .addAndGet(duration);
        }
    }
    
    /**
     * 记录操作
     */
    public void recordOperation(String operationName, long durationNanos) {
        if (!enabled) return;
        
        operationCounts.computeIfAbsent(operationName, k -> new AtomicLong(0))
                      .incrementAndGet();
        
        operationDurations.computeIfAbsent(operationName, k -> new AtomicLong(0))
                         .addAndGet(durationNanos);
    }
    
    /**
     * 获取操作平均耗时（纳秒）
     */
    public double getAverageDuration(String operationName) {
        AtomicLong count = operationCounts.get(operationName);
        AtomicLong totalDuration = operationDurations.get(operationName);
        
        if (count == null || totalDuration == null || count.get() == 0) {
            return 0.0;
        }
        
        return (double) totalDuration.get() / count.get();
    }
    
    /**
     * 获取操作总次数
     */
    public long getOperationCount(String operationName) {
        AtomicLong count = operationCounts.get(operationName);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 生成性能报告
     */
    public Map<String, Object> generateReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        
        // 按耗时排序
        List<Map.Entry<String, AtomicLong>> sortedEntries = new ArrayList<>(
            operationCounts.entrySet()
        );
        
        sortedEntries.sort((a, b) -> {
            double avgA = getAverageDuration(a.getKey());
            double avgB = getAverageDuration(b.getKey());
            return Double.compare(avgB, avgA); // 降序排列
        });
        
        List<Map<String, Object>> operationStats = new ArrayList<>();
        long totalOperations = 0;
        double totalTimeMs = 0;
        
        for (Map.Entry<String, AtomicLong> entry : sortedEntries) {
            String operation = entry.getKey();
            long count = entry.getValue().get();
            double avgNs = getAverageDuration(operation);
            double avgMs = avgNs / 1_000_000.0;
            double totalMs = (operationDurations.get(operation).get() / 1_000_000.0);
            
            Map<String, Object> stat = new HashMap<>();
            stat.put("operation", operation);
            stat.put("count", count);
            stat.put("avgDurationMs", String.format("%.3f", avgMs));
            stat.put("totalDurationMs", String.format("%.3f", totalMs));
            stat.put("percentage", String.format("%.1f%%", (totalMs / (totalTimeMs > 0 ? totalTimeMs : 1)) * 100));
            
            operationStats.add(stat);
            totalOperations += count;
            totalTimeMs += totalMs;
        }
        
        report.put("totalOperations", totalOperations);
        report.put("totalTimeMs", String.format("%.3f", totalTimeMs));
        report.put("operationsPerSecond", totalTimeMs > 0 ? 
            String.format("%.1f", totalOperations / (totalTimeMs / 1000.0)) : "0.0");
        report.put("operationDetails", operationStats);
        
        return report;
    }
    
    /**
     * 打印性能报告
     */
    public void printReport() {
        if (!enabled) return;
        
        Map<String, Object> report = generateReport();
        System.out.println("\n=== 性能报告 ===");
        System.out.printf("总操作次数: %d%n", report.get("totalOperations"));
        System.out.printf("总耗时: %s ms%n", report.get("totalTimeMs"));
        System.out.printf("操作频率: %s 次/秒%n", report.get("operationsPerSecond"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) report.get("operationDetails");
        
        System.out.println("\n操作详情:");
        System.out.println("操作名称                | 次数   | 平均耗时(ms) | 总耗时(ms) | 占比");
        System.out.println("-----------------------|--------|--------------|------------|------");
        
        for (Map<String, Object> detail : details) {
            System.out.printf("%-20s | %6d | %12s | %10s | %s%n",
                detail.get("operation"),
                detail.get("count"),
                detail.get("avgDurationMs"),
                detail.get("totalDurationMs"),
                detail.get("percentage"));
        }
    }
    
    /**
     * 重置监控器
     */
    public void reset() {
        operationCounts.clear();
        operationDurations.clear();
        operationStartTimes.clear();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}