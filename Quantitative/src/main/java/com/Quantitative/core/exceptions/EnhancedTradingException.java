package com.Quantitative.core.exceptions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 增强的交易异常类 - 修复版本
 */
public class EnhancedTradingException extends TradingException {
	private final LocalDateTime timestamp;
	private final Map<String, Object> contextData;
	private final boolean recoverable;

	public EnhancedTradingException(ErrorCode errorCode, String message, String context) {
		this(errorCode, message, context, null, false);
	}

	public EnhancedTradingException(ErrorCode errorCode, String message, String context, Throwable cause,
			boolean recoverable) {
		super(errorCode, message, context, cause, deduceComponentFromStackTrace());
		this.timestamp = LocalDateTime.now();
		this.contextData = new HashMap<>();
		this.recoverable = recoverable;
	}

	/**
	 * 从堆栈跟踪推断组件名称
	 */
	private static String deduceComponentFromStackTrace() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		if (stackTrace.length > 0) {
			for (StackTraceElement element : stackTrace) {
				String className = element.getClassName();
				if (className.contains("com.Quantitative")) {
					if (className.contains("strategy"))
						return "Strategy";
					if (className.contains("portfolio"))
						return "Portfolio";
					if (className.contains("execution"))
						return "Execution";
					if (className.contains("data"))
						return "DataFeed";
					if (className.contains("risk"))
						return "RiskManager";
				}
			}
		}
		return "Unknown";
	}

	/**
	 * 添加上下文数据
	 */
	public EnhancedTradingException withContext(String key, Object value) {
		this.contextData.put(key, value);
		return this;
	}

	// Getter方法
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public Map<String, Object> getContextData() {
		return new HashMap<>(contextData);
	}

	public boolean isRecoverable() {
		return recoverable;
	}

	@Override
	public String toString() {
		return String.format("EnhancedTradingException{time=%s, component=%s, code=%s, recoverable=%s, context=%s}",
				timestamp, getComponent(), getErrorCode(), recoverable, getContext());
	}
}