package com.Quantitative.common.health;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

import com.Quantitative.common.utils.TradingLogger;

/**
 * 系统健康检查器 - 修复版本
 */
public class SystemHealthChecker {
	private static final SystemHealthChecker INSTANCE = new SystemHealthChecker();

	private final MemoryMXBean memoryMXBean;
	private final OperatingSystemMXBean osMXBean;
	private final ThreadMXBean threadMXBean;

	private SystemHealthChecker() {
		this.memoryMXBean = ManagementFactory.getMemoryMXBean();
		this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
		this.threadMXBean = ManagementFactory.getThreadMXBean();
	}

	public static SystemHealthChecker getInstance() {
		return INSTANCE;
	}

	/**
	 * 执行完整健康检查
	 */
	public HealthCheckResult performHealthCheck() {
		HealthCheckResult result = new HealthCheckResult();

		// 内存健康检查
		checkMemoryHealth(result);

		// CPU健康检查
		checkCpuHealth(result);

		// 线程健康检查
		checkThreadHealth(result);

		// 磁盘空间检查（需要额外实现）
		checkDiskSpace(result);

		result.setOverallHealthy(determineOverallHealth(result));

		TradingLogger.debug("HealthCheck", "健康检查完成: {}", result.isOverallHealthy() ? "健康" : "异常");

		return result;
	}

	/**
	 * 快速健康检查
	 */
	public boolean quickHealthCheck() {
		MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
		double memoryUsage = (double) heapUsage.getUsed() / heapUsage.getMax();
		return memoryUsage < 0.9; // 内存使用率低于90%
	}

	private void checkMemoryHealth(HealthCheckResult result) {
		MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

		double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax();
		double nonHeapUsagePercent = nonHeapUsage.getMax() > 0 ? (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax()
				: 0.0;

		result.addMetric("memory.heap.usage", heapUsagePercent);
		result.addMetric("memory.nonheap.usage", nonHeapUsagePercent);
		result.addMetric("memory.heap.usedMB", heapUsage.getUsed() / (1024 * 1024));
		result.addMetric("memory.heap.maxMB", heapUsage.getMax() / (1024 * 1024));

		// 内存健康标准
		if (heapUsagePercent > 0.9) {
			result.addIssue("CRITICAL", "堆内存使用率过高: " + String.format("%.1f%%", heapUsagePercent * 100));
		} else if (heapUsagePercent > 0.8) {
			result.addIssue("WARN", "堆内存使用率较高: " + String.format("%.1f%%", heapUsagePercent * 100));
		}
	}

	private void checkCpuHealth(HealthCheckResult result) {
		double systemLoad = osMXBean.getSystemLoadAverage();
		int availableProcessors = osMXBean.getAvailableProcessors();

		result.addMetric("cpu.processors", availableProcessors);
		result.addMetric("cpu.systemLoad", systemLoad);

		if (systemLoad > availableProcessors * 0.8) {
			result.addIssue("WARN", "系统负载较高: " + systemLoad);
		}
	}

	private void checkThreadHealth(HealthCheckResult result) {
		int threadCount = threadMXBean.getThreadCount();
		int peakThreadCount = threadMXBean.getPeakThreadCount();
		long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();

		result.addMetric("thread.count", threadCount);
		result.addMetric("thread.peak", peakThreadCount);
		result.addMetric("thread.totalStarted", totalStartedThreadCount);

		if (threadCount > 1000) {
			result.addIssue("WARN", "线程数过多: " + threadCount);
		}
	}

	private void checkDiskSpace(HealthCheckResult result) {
		// 这里可以检查重要目录的磁盘空间
		// 暂时跳过具体实现
		result.addMetric("disk.space.check", 1.0);
	}

	// 修复这里：使用 values().stream() 而不是直接对 Map 使用 stream()
	private boolean determineOverallHealth(HealthCheckResult result) {
		// 如果有CRITICAL问题，系统不健康
		return result.getIssues().values().stream().noneMatch(issue -> "CRITICAL".equals(issue.get("level")));
	}

	/**
	 * 健康检查结果 - 修复版本
	 */
	public static class HealthCheckResult {
		private final Map<String, Object> metrics = new LinkedHashMap<>();
		private final Map<String, Map<String, String>> issues = new LinkedHashMap<>();
		private boolean overallHealthy = true;

		public void addMetric(String key, Object value) {
			metrics.put(key, value);
		}

		public void addIssue(String level, String message) {
			Map<String, String> issue = new LinkedHashMap<>();
			issue.put("level", level);
			issue.put("message", message);
			issue.put("timestamp", java.time.LocalDateTime.now().toString());

			// 使用消息作为key，避免重复
			String issueKey = level + "_" + System.currentTimeMillis();
			issues.put(issueKey, issue);
		}

		public Map<String, Object> generateReport() {
			Map<String, Object> report = new LinkedHashMap<>();
			report.put("timestamp", java.time.LocalDateTime.now());
			report.put("overallHealthy", overallHealthy);
			report.put("metrics", new LinkedHashMap<>(metrics));
			report.put("issues", new LinkedHashMap<>(issues));
			report.put("issueCount", issues.size());
			return report;
		}

		public void printReport() {
			Map<String, Object> report = generateReport();

			// System.out.println("\n" + "=".repeat(60));
			StringBuilder sb = new StringBuilder("\n");
			for (int i = 0; i < 60; i++) {
				sb.append("=");
			}
			System.out.println(sb.toString());

			System.out.println("?? 系统健康检查报告");
			// System.out.println("=".repeat(60));
			for (int i = 0; i < 60; i++) {
				System.out.print("=");
			}
			System.out.println(); // 换行

			System.out.printf("总体状态: %s\n", overallHealthy ? "? 健康" : "? 异常");
			System.out.printf("问题数量: %d\n", issues.size());

			if (!issues.isEmpty()) {
				System.out.println("\n发现问题:");
				issues.values().forEach(issue -> {
					System.out.printf("  [%s] %s\n", issue.get("level"), issue.get("message"));
				});
			}

			System.out.println("\n系统指标:");
			metrics.forEach((key, value) -> {
				System.out.printf("  %s: %s\n", key, value);
			});
			// System.out.println("=".repeat(60));
			for (int i = 0; i < 60; i++) {
				System.out.print("=");
			}
			System.out.println(); // 换行

		}

		// Getter方法
		public Map<String, Object> getMetrics() {
			return new LinkedHashMap<>(metrics);
		}

		public Map<String, Map<String, String>> getIssues() {
			return new LinkedHashMap<>(issues);
		}

		public boolean isOverallHealthy() {
			return overallHealthy;
		}

		public void setOverallHealthy(boolean overallHealthy) {
			this.overallHealthy = overallHealthy;
		}
	}
}