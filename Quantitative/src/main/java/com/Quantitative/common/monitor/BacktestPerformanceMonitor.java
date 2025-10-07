
package com.Quantitative.common.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 回测性能监控器
 */
public class BacktestPerformanceMonitor {
	private static final BacktestPerformanceMonitor INSTANCE = new BacktestPerformanceMonitor();

	private final Map<String, ComponentMetrics> componentMetrics = new ConcurrentHashMap<>();
	private final LongAdder totalEventsProcessed = new LongAdder();
	private final LongAdder totalBarsProcessed = new LongAdder();
	private long startTime;
	private boolean monitoring = false;

	private BacktestPerformanceMonitor() {
	}

	public static BacktestPerformanceMonitor getInstance() {
		return INSTANCE;
	}

	public void startMonitoring() {
		this.monitoring = true;
		this.startTime = System.currentTimeMillis();
		componentMetrics.clear();
		totalEventsProcessed.reset();
		totalBarsProcessed.reset();
		System.out.println("🎯 回测性能监控已启动");
	}

	public void stopMonitoring() {
		this.monitoring = false;
		System.out.println("🎯 回测性能监控已停止");
		printSummary();
	}

	public void recordEventProcessing(String component, String eventType, long processingTimeNanos) {
		if (!monitoring)
			return;

		ComponentMetrics metrics = componentMetrics.computeIfAbsent(component, k -> new ComponentMetrics());
		metrics.recordEvent(eventType, processingTimeNanos);
		totalEventsProcessed.increment();
	}

	public void recordBarProcessing(long processingTimeNanos) {
		if (!monitoring)
			return;

		totalBarsProcessed.increment();
		ComponentMetrics metrics = componentMetrics.computeIfAbsent("BarProcessor", k -> new ComponentMetrics());
		metrics.recordEvent("BAR", processingTimeNanos);
	}

	public void printSummary() {
		if (!monitoring)
			return;

		long totalDuration = System.currentTimeMillis() - startTime;
		// System.out.println("\n" + "=".repeat(80));
		System.out.println("\n" + String.join("", Collections.nCopies(80, "=")));
		System.out.println("🎯 回测性能报告");
		// System.out.println("=".repeat(80));
		System.out.println(String.join("", Collections.nCopies(80, "=")));
		System.out.printf("总运行时间: %.2f 秒\n", totalDuration / 1000.0);
		System.out.printf("处理Bar数量: %,d\n", totalBarsProcessed.longValue());
		System.out.printf("处理事件数量: %,d\n", totalEventsProcessed.longValue());
		System.out.printf("处理速度: %.1f Bar/秒\n", totalBarsProcessed.doubleValue() / (totalDuration / 1000.0));

		System.out.println("\n组件性能详情:");
		System.out.println("组件名称                | 事件数量 | 平均耗时(ms) | 最大耗时(ms) | 总耗时(ms)");
		System.out.println("------------------------|----------|--------------|--------------|-----------");

		componentMetrics.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue().getTotalEvents(), a.getValue().getTotalEvents()))
				.forEach(entry -> {
					ComponentMetrics metrics = entry.getValue();
					System.out.printf("%-24s | %,8d | %12.3f | %12.3f | %,9.1f\n", entry.getKey(),
							metrics.getTotalEvents(), metrics.getAverageTimeMs(), metrics.getMaxTimeMs(),
							metrics.getTotalTimeMs());
				});

		// 缓存统计
		com.Quantitative.common.cache.UnifiedCacheManager cacheManager = com.Quantitative.common.cache.UnifiedCacheManager
				.getInstance();
		Map<String, com.Quantitative.common.cache.UnifiedCacheManager.CacheStats> cacheStats = cacheManager
				.getAllStats();

		if (!cacheStats.isEmpty()) {
			System.out.println("\n缓存性能详情:");
			cacheStats.forEach((name, stats) -> {
				System.out.printf("  %s: %s\n", name, stats);
			});
		}

		// System.out.println("=".repeat(80));

		// System.out.println(String.join("", Collections.nCopies(80, "=")));

		for (int i = 0; i < 80; i++) {
			System.out.print("=");
		}
		System.out.println(); // 换行

	}

	/**
	 * 组件性能指标
	 */
	private static class ComponentMetrics {
		private final LongAdder totalEvents = new LongAdder();
		private final LongAdder totalProcessingTimeNanos = new LongAdder();
		private long maxProcessingTimeNanos = 0;
		private final Map<String, LongAdder> eventTypeCounts = new ConcurrentHashMap<>();

		public void recordEvent(String eventType, long processingTimeNanos) {
			totalEvents.increment();
			totalProcessingTimeNanos.add(processingTimeNanos);

			if (processingTimeNanos > maxProcessingTimeNanos) {
				maxProcessingTimeNanos = processingTimeNanos;
			}

			eventTypeCounts.computeIfAbsent(eventType, k -> new LongAdder()).increment();
		}

		public long getTotalEvents() {
			return totalEvents.longValue();
		}

		public double getAverageTimeMs() {
			return totalEvents.longValue() > 0
					? (totalProcessingTimeNanos.doubleValue() / totalEvents.longValue()) / 1_000_000.0 : 0.0;
		}

		public double getMaxTimeMs() {
			return maxProcessingTimeNanos / 1_000_000.0;
		}

		public double getTotalTimeMs() {
			return totalProcessingTimeNanos.doubleValue() / 1_000_000.0;
		}

		public Map<String, Long> getEventTypeCounts() {
			Map<String, Long> counts = new HashMap<>();
			eventTypeCounts.forEach((type, adder) -> counts.put(type, adder.longValue()));
			return counts;
		}
	}
}