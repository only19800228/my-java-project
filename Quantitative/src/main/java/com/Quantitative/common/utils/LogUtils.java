package com.Quantitative.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * 日志工具类 - 带容错处理
 */
public class LogUtils {

	// 使用静态初始化块确保日志系统正常初始化
	static {
		try {
			// 强制初始化日志系统
			LoggerFactory.getLogger(LogUtils.class);
		} catch (Exception e) {
			System.err.println("日志系统初始化失败: " + e.getMessage());
			// 使用System.out作为后备
		}
	}

	// 交易事件专用日志
	private static final Logger TRADING_LOGGER = getSafeLogger("TRADING");
	private static final Logger PERFORMANCE_LOGGER = getSafeLogger("PERFORMANCE");

	/**
	 * 安全的获取Logger，如果失败则返回一个后备的Logger
	 */
	private static Logger getSafeLogger(String name) {
		try {
			return LoggerFactory.getLogger(name);
		} catch (Exception e) {
			return new FallbackLogger(name);
		}
	}

	public static Logger getLogger(Class<?> clazz) {
		return getSafeLogger(clazz.getName());
	}

	/**
	 * 记录交易事件
	 */
	public static void logTradeEvent(String eventType, String symbol, String details) {
		try {
			TRADING_LOGGER.info("{} | {} | {}", eventType, symbol, details);
		} catch (Exception e) {
			System.out.printf("[TRADE] %s | %s | %s%n", eventType, symbol, details);
		}
	}

	/**
	 * 记录性能指标
	 */
	public static void logPerformance(String operation, long durationMs, String context) {
		try {
			PERFORMANCE_LOGGER.debug("{} | {}ms | {}", operation, durationMs, context);
		} catch (Exception e) {
			// 静默失败，性能日志不重要
		}
	}

	/**
	 * 记录系统异常
	 */
	public static void logSystemError(String component, String operation, Throwable error) {
		try {
			LoggerFactory.getLogger(component).error("操作失败: {} - {}", operation, error.getMessage(), error);
		} catch (Exception e) {
			System.err.printf("[ERROR] %s::%s - %s%n", component, operation, error.getMessage());
			error.printStackTrace();
		}
	}

	/**
	 * 后备日志实现（当SLF4J不可用时）
	 */
	private static class FallbackLogger implements Logger {
		private final String name;

		public FallbackLogger(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isTraceEnabled() {
			return false;
		}

		@Override
		public void trace(String msg) {
		}

		@Override
		public void trace(String format, Object arg) {
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
		}

		@Override
		public void trace(String format, Object... arguments) {
		}

		@Override
		public void trace(String msg, Throwable t) {
		}

		@Override
		public boolean isDebugEnabled() {
			return false;
		}

		@Override
		public void debug(String msg) {
		}

		@Override
		public void debug(String format, Object arg) {
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
		}

		@Override
		public void debug(String format, Object... arguments) {
		}

		@Override
		public void debug(String msg, Throwable t) {
		}

		@Override
		public boolean isInfoEnabled() {
			return true;
		}

		@Override
		public void info(String msg) {
			System.out.println("[INFO] " + name + " - " + msg);
		}

		@Override
		public void info(String format, Object arg) {
			System.out.println("[INFO] " + name + " - " + String.format(format, arg));
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
			System.out.println("[INFO] " + name + " - " + String.format(format, arg1, arg2));
		}

		@Override
		public void info(String format, Object... arguments) {
			System.out.println("[INFO] " + name + " - " + String.format(format, arguments));
		}

		@Override
		public void info(String msg, Throwable t) {
			System.out.println("[INFO] " + name + " - " + msg);
			t.printStackTrace();
		}

		@Override
		public boolean isWarnEnabled() {
			return true;
		}

		@Override
		public void warn(String msg) {
			System.out.println("[WARN] " + name + " - " + msg);
		}

		@Override
		public void warn(String format, Object arg) {
			System.out.println("[WARN] " + name + " - " + String.format(format, arg));
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
			System.out.println("[WARN] " + name + " - " + String.format(format, arg1, arg2));
		}

		@Override
		public void warn(String format, Object... arguments) {
			System.out.println("[WARN] " + name + " - " + String.format(format, arguments));
		}

		@Override
		public void warn(String msg, Throwable t) {
			System.out.println("[WARN] " + name + " - " + msg);
			t.printStackTrace();
		}

		@Override
		public boolean isErrorEnabled() {
			return true;
		}

		@Override
		public void error(String msg) {
			System.err.println("[ERROR] " + name + " - " + msg);
		}

		@Override
		public void error(String format, Object arg) {
			System.err.println("[ERROR] " + name + " - " + String.format(format, arg));
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
			System.err.println("[ERROR] " + name + " - " + String.format(format, arg1, arg2));
		}

		@Override
		public void error(String format, Object... arguments) {
			System.err.println("[ERROR] " + name + " - " + String.format(format, arguments));
		}

		@Override
		public void error(String msg, Throwable t) {
			System.err.println("[ERROR] " + name + " - " + msg);
			t.printStackTrace();
		}

		@Override
		public void debug(Marker arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void debug(Marker arg0, String arg1, Object arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void debug(Marker arg0, String arg1, Object... arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void debug(Marker arg0, String arg1, Throwable arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public void error(Marker arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void error(Marker arg0, String arg1, Object arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void error(Marker arg0, String arg1, Object... arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void error(Marker arg0, String arg1, Throwable arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public void info(Marker arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void info(Marker arg0, String arg1, Object arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void info(Marker arg0, String arg1, Object... arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void info(Marker arg0, String arg1, Throwable arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean isDebugEnabled(Marker arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isErrorEnabled(Marker arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isInfoEnabled(Marker arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isTraceEnabled(Marker arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isWarnEnabled(Marker arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void trace(Marker arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void trace(Marker arg0, String arg1, Object arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void trace(Marker arg0, String arg1, Object... arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void trace(Marker arg0, String arg1, Throwable arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public void warn(Marker arg0, String arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void warn(Marker arg0, String arg1, Object arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void warn(Marker arg0, String arg1, Object... arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void warn(Marker arg0, String arg1, Throwable arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
			// TODO Auto-generated method stub

		}
	}
}