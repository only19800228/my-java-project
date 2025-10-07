package com.Quantitative.common.exception;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.exceptions.CriticalTradingException;

/**
 * 全局异常处理器
 */
public class ExceptionHandler {

	/**
	 * 处理交易异常
	 */
	public static void handleTradingException(Exception e, String context) {
		TradingLogger.logSystemError("Trading", context, e);

		// 发送警报（如果配置了警报系统）
		if (e instanceof CriticalTradingException) {
			sendAlert(e, context);
		}
	}

	/**
	 * 处理系统异常
	 */
	public static void handleSystemException(Exception e, String component, String operation) {
		TradingLogger.logSystemError(component, operation, e);
	}

	/**
	 * 安全执行（带异常处理）
	 */
	public static <T> T executeSafely(SupplierWithException<T> supplier, T defaultValue, String operation) {
		try {
			return supplier.get();
		} catch (Exception e) {
			handleSystemException(e, "SafeExecutor", operation);
			return defaultValue;
		}
	}

	/**
	 * 安全执行（无返回值）
	 */
	public static void executeSafely(RunnableWithException runnable, String operation) {
		executeSafely(() -> {
			runnable.run();
			return null;
		}, null, operation);
	}

	private static void sendAlert(Exception e, String context) {
		// 实现警报逻辑（邮件、钉钉、企业微信等）
		TradingLogger.logRisk("ERROR", "Alert", "系统警报: {} - {}", context, e.getMessage());
	}

	@FunctionalInterface
	public interface SupplierWithException<T> {
		T get() throws Exception;
	}

	@FunctionalInterface
	public interface RunnableWithException {
		void run() throws Exception;
	}
}