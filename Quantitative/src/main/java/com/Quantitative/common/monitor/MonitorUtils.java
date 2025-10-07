package com.Quantitative.common.monitor;

/**
 * 监控工具类 - 提供静态方法简化监控调用
 */
public class MonitorUtils {
	private static final UnifiedMonitorManager monitorManager = UnifiedMonitorManager.getInstance();

	// 私有构造函数，防止实例化
	private MonitorUtils() {
	}

	/**
	 * 监控代码块执行
	 */
	public static <T> T monitor(String component, String operation, MonitorableOperation<T> operationToMonitor) {
		long startTime = System.nanoTime();
		try {
			T result = operationToMonitor.execute();
			long duration = System.nanoTime() - startTime;
			monitorManager.recordOperation(component, operation, duration);
			return result;
		} catch (Exception e) {
			long duration = System.nanoTime() - startTime;
			monitorManager.recordOperation(component, operation + "_ERROR", duration);
			throw e;
		}
	}

	/**
	 * 监控无返回值的操作
	 */
	public static void monitorVoid(String component, String operation, Runnable operationToMonitor) {
		long startTime = System.nanoTime();
		try {
			operationToMonitor.run();
			long duration = System.nanoTime() - startTime;
			monitorManager.recordOperation(component, operation, duration);
		} catch (Exception e) {
			long duration = System.nanoTime() - startTime;
			monitorManager.recordOperation(component, operation + "_ERROR", duration);
			throw e;
		}
	}

	/**
	 * 开始监控操作（用于复杂操作）
	 */
	public static OperationTracker startTracking(String component, String operation) {
		return new OperationTracker(component, operation);
	}

	/**
	 * 记录缓存操作
	 */
	public static void recordCacheOperation(String operation, long durationNanos, boolean hit) {
		monitorManager.recordCacheOperation(operation, durationNanos, hit);
	}

	/**
	 * 检查系统健康
	 */
	public static boolean isSystemHealthy() {
		return monitorManager.getSystemHealthStatus().isOverallHealthy();
	}

	/**
	 * 可监控操作接口
	 */
	@FunctionalInterface
	public interface MonitorableOperation<T> {
		T execute();
	}

	/**
	 * 操作跟踪器
	 */
	public static class OperationTracker {
		private final String component;
		private final String operation;
		private final long startTime;
		private final String trackId;

		public OperationTracker(String component, String operation) {
			this.component = component;
			this.operation = operation;
			this.startTime = System.nanoTime();
			this.trackId = monitorManager.startOperation(component, operation);
		}

		/**
		 * 完成操作并记录耗时
		 */
		public void complete() {
			long duration = System.nanoTime() - startTime;
			monitorManager.recordOperation(component, operation, duration);
			monitorManager.endOperation(trackId);
		}

		/**
		 * 完成操作并返回耗时（纳秒）
		 */
		public long completeAndGetDuration() {
			long duration = System.nanoTime() - startTime;
			monitorManager.recordOperation(component, operation, duration);
			monitorManager.endOperation(trackId);
			return duration;
		}
	}
}