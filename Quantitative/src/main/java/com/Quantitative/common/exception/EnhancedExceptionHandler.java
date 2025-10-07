package com.Quantitative.common.exception;

import java.util.HashMap;
import java.util.Map;

import com.Quantitative.common.exception.ExceptionHandler.SupplierWithException;
import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.exceptions.CriticalTradingException;

/**
 * 增强的全局异常处理器
 */
public class EnhancedExceptionHandler {

	/**
	 * 处理交易异常 - 增强版本
	 */
	public static void handleTradingException(Exception e, String context, String symbol) {
		TradingLogger.logSystemError("Trading", String.format("%s [%s]", context, symbol), e);

		// 根据异常类型采取不同措施
		if (e instanceof CriticalTradingException) {
			sendCriticalAlert(e, context, symbol);
		} else if (e instanceof java.net.ConnectException) {
			handleConnectionError(e, context);
		} else if (e instanceof java.util.concurrent.TimeoutException) {
			handleTimeoutError(e, context);
		}

		// 记录详细上下文信息
		logExceptionContext(e, context, symbol);
	}

	/**
	 * 安全执行 - 增强版本
	 */
	public static <T> T executeSafely(SupplierWithException<T> supplier, T defaultValue, String operation,
			String symbol) {
		long startTime = System.nanoTime();
		try {
			T result = supplier.get();
			long duration = System.nanoTime() - startTime;

			// 记录慢操作
			if (duration > 10_000_000) { // 10ms
				TradingLogger.debug("Performance", "%s 操作耗时: %.3fms", operation, duration / 1_000_000.0);
			}

			return result;
		} catch (Exception e) {
			handleTradingException(e, operation, symbol);
			return defaultValue;
		}
	}

	/**
	 * 重试机制
	 */
	public static <T> T executeWithRetry(SupplierWithException<T> supplier, int maxRetries, long retryIntervalMs,
			String operation) {
		int retries = 0;
		while (retries <= maxRetries) {
			try {
				return supplier.get();
			} catch (Exception e) {
				retries++;
				if (retries > maxRetries) {
					throw new RuntimeException(String.format("操作 %s 重试 %d 次后失败", operation, maxRetries), e);
				}

				TradingLogger.debug("Retry", "%s 第 %d 次重试...", operation, retries);
				try {
					Thread.sleep(retryIntervalMs * retries); // 指数退避
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("重试被中断", ie);
				}
			}
		}
		return null;
	}

	private static void sendCriticalAlert(Exception e, String context, String symbol) {
		String alertMessage = String.format("严重交易异常: %s - %s [%s]", context, e.getMessage(), symbol);
		TradingLogger.logRisk("CRITICAL", "Alert", alertMessage);

		// 这里可以集成邮件、钉钉等报警系统
		System.err.println("🚨 " + alertMessage);
	}

	private static void handleConnectionError(Exception e, String context) {
		TradingLogger.logRisk("WARN", "Connection", "连接错误: %s - %s", context, e.getMessage());
	}

	private static void handleTimeoutError(Exception e, String context) {
		TradingLogger.logRisk("WARN", "Timeout", "超时错误: %s - %s", context, e.getMessage());
	}

	private static void logExceptionContext(Exception e, String context, String symbol) {
		Map<String, Object> contextInfo = new HashMap<>();
		contextInfo.put("timestamp", java.time.LocalDateTime.now());
		contextInfo.put("context", context);
		contextInfo.put("symbol", symbol);
		contextInfo.put("exceptionType", e.getClass().getSimpleName());
		contextInfo.put("thread", Thread.currentThread().getName());

		TradingLogger.debug("ExceptionContext", "异常上下文: %s", contextInfo);
	}
}