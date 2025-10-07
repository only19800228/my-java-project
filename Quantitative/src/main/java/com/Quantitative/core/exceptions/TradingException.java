package com.Quantitative.core.exceptions;

/**
 * 交易系统基础异常类
 */
public class TradingException extends RuntimeException {
	private final ErrorCode errorCode;
	private final String context;
	private final String component;

	public TradingException(ErrorCode errorCode, String message) {
		this(errorCode, message, null, null, "Unknown");
	}

	public TradingException(ErrorCode errorCode, String message, String context) {
		this(errorCode, message, context, null, "Unknown");
	}

	public TradingException(ErrorCode errorCode, String message, Throwable cause) {
		this(errorCode, message, null, cause, "Unknown");
	}

	public TradingException(ErrorCode errorCode, String message, String context, Throwable cause) {
		this(errorCode, message, context, cause, "Unknown");
	}

	public TradingException(ErrorCode errorCode, String message, String context, Throwable cause, String component) {
		super(message, cause);
		this.errorCode = errorCode;
		this.context = context;
		this.component = component != null ? component : deduceComponent();
	}

	/**
	 * 推断组件名称
	 */
	private String deduceComponent() {
		StackTraceElement[] stackTrace = getStackTrace();
		if (stackTrace.length > 0) {
			String className = stackTrace[0].getClassName();
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
		return "Unknown";
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public String getContext() {
		return context;
	}

	public String getComponent() {
		return component;
	}

	@Override
	public String toString() {
		return String.format("TradingException{errorCode=%s, context='%s', component='%s', message='%s'}", errorCode,
				context, component, getMessage());
	}
}