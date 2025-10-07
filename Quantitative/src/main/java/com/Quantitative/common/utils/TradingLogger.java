// 新增：增强的日志工具
package com.Quantitative.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 交易系统专用日志工具
 */
public class TradingLogger {
	// 交易事件专用日志
	private static final Logger TRADING_LOGGER = LoggerFactory.getLogger("TRADING");
	private static final Logger PERFORMANCE_LOGGER = LoggerFactory.getLogger("PERFORMANCE");
	private static final Logger RISK_LOGGER = LoggerFactory.getLogger("RISK");

	/**
	 * 记录交易事件
	 */
	public static void logTrade(String symbol, String action, double price, int quantity, double pnl) {
		TRADING_LOGGER.info("TRADE|{}|{}|{:.4f}|{}|{:.2f}", symbol, action, price, quantity, pnl);
	}

	/**
	 * 记录信号事件
	 */
	public static void logSignal(String strategy, String symbol, String signalType, double strength, String reason) {
		TRADING_LOGGER.info("SIGNAL|{}|{}|{}|{:.2f}|{}", strategy, symbol, signalType, strength, reason);
	}

	/**
	 * 记录风险事件
	 */
	public static void logRisk(String level, String component, String message, Object... params) {
		String formattedMessage = String.format(message, params);
		switch (level.toUpperCase()) {
		case "WARN":
			RISK_LOGGER.warn("RISK_WARN|{}|{}", component, formattedMessage);
			break;
		case "ERROR":
			RISK_LOGGER.error("RISK_ERROR|{}|{}", component, formattedMessage);
			break;
		default:
			RISK_LOGGER.info("RISK_INFO|{}|{}", component, formattedMessage);
		}
	}

	/**
	 * 记录性能指标
	 */
	public static void logPerformance(String operation, long durationMs, String context) {
		PERFORMANCE_LOGGER.debug("PERF|{}|{}ms|{}", operation, durationMs, context);
	}

	/**
	 * 记录系统异常
	 */
	public static void logSystemError(String component, String operation, Throwable error) {
		TRADING_LOGGER.error("SYSTEM_ERROR|{}|{}|{}", component, operation, error.getMessage(), error);
	}

	/**
	 * 记录投资组合状态
	 */
	public static void logPortfolioStatus(double totalValue, double cash, double pnl, int positions) {
		TRADING_LOGGER.info("PORTFOLIO|{:.2f}|{:.2f}|{:.2f}|{}", totalValue, cash, pnl, positions);
	}

	/**
	 * 是否启用调试日志
	 */
	public static boolean isDebugEnabled() {
		return TRADING_LOGGER.isDebugEnabled();
	}

	/**
	 * 调试日志
	 */
	public static void debug(String component, String message, Object... params) {
		if (TRADING_LOGGER.isDebugEnabled()) {
			String formattedMessage = String.format(message, params);
			TRADING_LOGGER.debug("DEBUG|{}|{}", component, formattedMessage);
		}
	}
}