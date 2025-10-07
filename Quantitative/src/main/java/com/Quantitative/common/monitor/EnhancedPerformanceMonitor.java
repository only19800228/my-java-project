// 增强性能监控
package com.Quantitative.common.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 增强版性能监控
 */
public class EnhancedPerformanceMonitor {
	private static final EnhancedPerformanceMonitor INSTANCE = new EnhancedPerformanceMonitor();
	private final LongAdder totalOperations = new LongAdder();
	private final Map<String, OperationStats> operationStats = new ConcurrentHashMap<>();

	private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
	private volatile boolean enabled = true;

	private EnhancedPerformanceMonitor() {
	}

	public static EnhancedPerformanceMonitor getInstance() {
		return INSTANCE;
	}

	/**
	 * 开始记录操作
	 */
	public void startOperation(String operationName) {
		if (!enabled)
			return;

		OperationStats stats = operationStats.computeIfAbsent(operationName, k -> new OperationStats());
		stats.start();
	}

	/**
	 * 结束记录操作
	 */
	public void endOperation(String operationName) {
		if (!enabled)
			return;

		OperationStats stats = operationStats.get(operationName);
		if (stats != null) {
			stats.end();
			totalOperations.increment();
		}
	}

	/**
	 * 记录操作耗时
	 */
	public void recordOperation(String operationName, long durationNanos) {
		// if (!enabled)
		// return;

		// OperationStats stats = operationStats.computeIfAbsent(operationName,
		// k -> new OperationStats());
		// stats.record(durationNanos);
		// totalOperations.increment();

		OperationStats stats = operationStats.computeIfAbsent(operationName, k -> new OperationStats());
		stats.record(durationNanos);
		totalOperations.increment();

	}

	/**
	 * 获取操作统计
	 */
	public OperationStats getOperationStats(String operationName) {
		return operationStats.get(operationName);
	}

	/**
	 * 生成性能报告
	 */
	public PerformanceReport generateReport() {
		Map<String, OperationStats> snapshot = new ConcurrentHashMap<>(operationStats);
		long totalDuration = System.currentTimeMillis() - startTime.get();

		return new PerformanceReport(snapshot, totalOperations.sum(), totalDuration);
	}

	/**
	 * 重置监控器
	 */
	public void reset() {
		operationStats.clear();
		totalOperations.reset();
		startTime.set(System.currentTimeMillis());
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * 操作统计
	 */
	public static class OperationStats {
		private final LongAdder count = new LongAdder();
		private final LongAdder totalTimeNanos = new LongAdder();
		private final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
		private final AtomicLong maxTimeNanos = new AtomicLong(Long.MIN_VALUE);
		private final ThreadLocal<Long> startTime = new ThreadLocal<>();

		public void start() {
			startTime.set(System.nanoTime());
		}

		public void end() {
			Long start = startTime.get();
			if (start != null) {
				long duration = System.nanoTime() - start;
				record(duration);
				startTime.remove();
			}
		}

		public void record(long durationNanos) {
			count.increment();
			totalTimeNanos.add(durationNanos);

			minTimeNanos.updateAndGet(current -> Math.min(current, durationNanos));
			maxTimeNanos.updateAndGet(current -> Math.max(current, durationNanos));
		}

		// Getter方法
		public long getCount() {
			return count.sum();
		}

		public long getTotalTimeNanos() {
			return totalTimeNanos.sum();
		}

		public long getMinTimeNanos() {
			long min = minTimeNanos.get();
			return min == Long.MAX_VALUE ? 0 : min;
		}

		public long getMaxTimeNanos() {
			long max = maxTimeNanos.get();
			return max == Long.MIN_VALUE ? 0 : max;
		}

		public double getAverageTimeNanos() {
			return count.sum() > 0 ? (double) totalTimeNanos.sum() / count.sum() : 0.0;
		}

		public double getAverageTimeMillis() {
			return getAverageTimeNanos() / 1_000_000.0;
		}
	}

	/**
	 * 性能报告
	 */
	public static class PerformanceReport {
		private final Map<String, OperationStats> operationStats;
		private final long totalOperations;
		private final long totalDurationMs;

		public PerformanceReport(Map<String, OperationStats> operationStats, long totalOperations,
				long totalDurationMs) {
			this.operationStats = operationStats;
			this.totalOperations = totalOperations;
			this.totalDurationMs = totalDurationMs;
		}

		public void printReport() {
			// System.out.println("\n" + "=".repeat(80));
			// System.out.println(Stream.generate(() -> "\n" +
			// "=").limit(50).collect(Collectors.joining()));
			// System.err.printf("[增强性能报告]
			// com.Quantitative.common.monitor.EnhancedPerformanceMonitor "); //
			// 红字的错误报告
			StringBuilder sb = new StringBuilder("\n");
			for (int i = 0; i < 80; i++) {
				sb.append("=");
			}
			System.out.println(sb.toString());

			System.out.println("📊 增强性能报告 _com.Quantitative.common.monitor.EnhancedPerformanceMonitor");
			// System.out.println("=".repeat(80));
			System.out.println(Stream.generate(() -> "=").limit(80).collect(Collectors.joining()));
			System.out.printf("总运行时间: %.2f 秒%n", totalDurationMs / 1000.0);
			System.out.printf("总操作次数: %,d%n", totalOperations);
			System.out.printf("操作频率: %.1f 次/秒%n",
					totalDurationMs > 0 ? totalOperations / (totalDurationMs / 1000.0) : 0.0);

			System.out.println("\n操作详情:");
			System.out.println("操作名称                | 次数     | 平均耗时(ms) | 最小(ms) | 最大(ms) | 总耗时(s)");
			System.out.println("------------------------|----------|--------------|----------|----------|----------");

			operationStats.entrySet().stream()
					.sorted((a, b) -> Long.compare(b.getValue().getCount(), a.getValue().getCount())).forEach(entry -> {
						OperationStats stats = entry.getValue();
						System.out.printf("%-24s | %,7d | %11.3f | %8.3f | %8.3f | %8.2f%n", entry.getKey(),
								stats.getCount(), stats.getAverageTimeMillis(), stats.getMinTimeNanos() / 1_000_000.0,
								stats.getMaxTimeNanos() / 1_000_000.0, stats.getTotalTimeNanos() / 1_000_000_000.0);
					});

			// System.out.println("=".repeat(80));
			System.out.println(Stream.generate(() -> "=").limit(80).collect(Collectors.joining()));
		}
	}
}