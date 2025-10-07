package com.Quantitative.common.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.Quantitative.common.utils.MemoryMonitor;

/**
 * 统一监控管理器 - 整合所有监控功能
 */
public class UnifiedMonitorManager {
	private static final UnifiedMonitorManager INSTANCE = new UnifiedMonitorManager();

	private final EnhancedPerformanceMonitor performanceMonitor;
	private final BacktestPerformanceMonitor backtestMonitor;
	private boolean enabled = true;
	private String currentBacktestId;

	private UnifiedMonitorManager() {
		this.performanceMonitor = EnhancedPerformanceMonitor.getInstance();
		this.backtestMonitor = BacktestPerformanceMonitor.getInstance();
	}

	public static UnifiedMonitorManager getInstance() {
		return INSTANCE;
	}

	/**
	 * 开始回测监控
	 */
	public void startBacktestMonitoring(String backtestId) {
		if (!enabled)
			return;

		this.currentBacktestId = backtestId;
		backtestMonitor.startMonitoring();
		performanceMonitor.setEnabled(true);

		System.out.printf("🎯 开始监控回测: %s\n", backtestId);
		MemoryMonitor.printMemoryUsage("回测开始");
	}

	/**
	 * 停止回测监控并生成报告
	 */
	public void stopBacktestMonitoring() {
		if (!enabled)
			return;

		// System.out.println("\n" + "=".repeat(80));
		System.out.println("\n" + String.join("", Collections.nCopies(80, "=")));
		System.out.println("🎯 回测监控报告: " + currentBacktestId);
		// System.out.println("=".repeat(80));
		System.out.println(String.join("", Collections.nCopies(80, "=")));
		// 生成性能报告
		backtestMonitor.stopMonitoring();

		// 生成详细性能分析
		performanceMonitor.generateReport().printReport();

		// 内存使用报告
		MemoryMonitor.printMemoryUsage("回测结束");

		// System.out.println("=".repeat(80));
		System.out.println(String.join("", Collections.nCopies(80, "=")));
		this.currentBacktestId = null;
	}

	/**
	 * 记录组件操作 - 统一入口
	 */
	public void recordOperation(String component, String operation, long durationNanos) {
		if (!enabled)
			return;

		// 记录到性能监控器
		performanceMonitor.recordOperation(operation, durationNanos);

		// 记录到回测监控器
		backtestMonitor.recordEventProcessing(component, operation, durationNanos);

		// 记录慢操作警告
		if (durationNanos > 100_000_000L) { // 100ms
			System.out.printf("⚠️ 慢操作检测: %s.%s 耗时 %.1fms\n", component, operation, durationNanos / 1_000_000.0);
		}
	}

	/**
	 * 记录操作开始 - 返回跟踪ID用于结束计时
	 */
	public String startOperation(String component, String operation) {
		if (!enabled)
			return null;

		String trackId = component + "." + operation + "." + System.nanoTime();
		performanceMonitor.startOperation(operation);
		return trackId;
	}

	/**
	 * 记录操作结束
	 */
	public void endOperation(String trackId) {
		if (!enabled || trackId == null)
			return;

		String[] parts = trackId.split("\\.");
		if (parts.length >= 3) {
			String operation = parts[1];
			performanceMonitor.endOperation(operation);
		}
	}

	/**
	 * 记录Bar处理
	 */
	public void recordBarProcessing(long processingTimeNanos) {
		if (!enabled)
			return;

		backtestMonitor.recordBarProcessing(processingTimeNanos);

		// 记录到性能监控
		performanceMonitor.recordOperation("BarProcessing", processingTimeNanos);
	}

	/**
	 * 记录缓存操作
	 */
	public void recordCacheOperation(String operation, long durationNanos, boolean hit) {
		if (!enabled)
			return;

		String cacheOp = hit ? "CacheHit" : "CacheMiss";
		performanceMonitor.recordOperation(cacheOp, durationNanos);

		if (!hit && durationNanos > 10_000_000L) { // 10ms
			System.out.printf("💾 缓存未命中: %s 计算耗时 %.1fms\n", operation, durationNanos / 1_000_000.0);
		}
	}

	/**
	 * 检查系统健康状态
	 */
	public SystemHealthStatus getSystemHealthStatus() {
		double memoryUsage = MemoryMonitor.getMemoryUsagePercent();
		boolean memoryHealthy = MemoryMonitor.isMemoryHealthy();

		return new SystemHealthStatus(memoryUsage, memoryHealthy);
	}

	/**
	 * 获取监控统计摘要
	 */
	public Map<String, Object> getMonitoringSummary() {
		Map<String, Object> summary = new HashMap<>();

		// 性能监控统计
		EnhancedPerformanceMonitor.PerformanceReport perfReport = performanceMonitor.generateReport();
		summary.put("performanceReport", perfReport);

		// 回测监控统计
		summary.put("backtestMonitoring", backtestMonitor != null);

		// 内存状态
		SystemHealthStatus healthStatus = getSystemHealthStatus();
		summary.put("systemHealth", healthStatus);
		summary.put("memoryUsagePercent", healthStatus.getMemoryUsagePercent());
		summary.put("memoryHealthy", healthStatus.isMemoryHealthy());

		return summary;
	}

	/**
	 * 打印实时监控状态
	 */
	public void printRealtimeStatus() {
		if (!enabled)
			return;

		SystemHealthStatus health = getSystemHealthStatus();
		System.out.printf("📊 系统状态: %s\n", health);

		// 可以添加更多实时状态信息
	}

	// Getter/Setter
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		performanceMonitor.setEnabled(enabled);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getCurrentBacktestId() {
		return currentBacktestId;
	}

	/**
	 * 系统健康状态
	 */
	public static class SystemHealthStatus {
		private final double memoryUsagePercent;
		private final boolean memoryHealthy;

		public SystemHealthStatus(double memoryUsagePercent, boolean memoryHealthy) {
			this.memoryUsagePercent = memoryUsagePercent;
			this.memoryHealthy = memoryHealthy;
		}

		public double getMemoryUsagePercent() {
			return memoryUsagePercent;
		}

		public boolean isMemoryHealthy() {
			return memoryHealthy;
		}

		public boolean isOverallHealthy() {
			return memoryHealthy;
		}

		@Override
		public String toString() {
			return String.format("内存使用:%.1f%%, 健康:%s", memoryUsagePercent * 100, memoryHealthy ? "✅" : "❌");
		}
	}
}