package com.Quantitative.core.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 交易信号事件 - 优化版本
 */
public class SignalEvent extends Event {
	private final String symbol;
	private final String signalType; // BUY, SELL, HOLD, CLOSE
	private final double strength; // 信号强度 0-1
	private final String strategyName;
	private final String comment;

	// 构造函数
	public SignalEvent(LocalDateTime timestamp, String symbol, String signalType, double strength,
			String strategyName) {
		this(timestamp, symbol, signalType, strength, strategyName, "");
	}

	public SignalEvent(LocalDateTime timestamp, String symbol, String signalType, double strength, String strategyName,
			String comment) {
		super(timestamp, "SIGNAL");

		// 参数验证
		if (symbol == null || symbol.trim().isEmpty()) {
			throw new IllegalArgumentException("标的代码不能为空");
		}
		if (strategyName == null || strategyName.trim().isEmpty()) {
			throw new IllegalArgumentException("策略名称不能为空");
		}
		if (strength < 0.0 || strength > 1.0) {
			throw new IllegalArgumentException("信号强度必须在 0-1 之间: " + strength);
		}
		if (!isValidSignalType(signalType)) {
			throw new IllegalArgumentException("无效的信号类型: " + signalType);
		}

		this.symbol = symbol.trim();
		this.signalType = signalType;
		this.strength = strength;
		this.strategyName = strategyName.trim();
		this.comment = comment != null ? comment : "";
	}

	// 工厂方法
	public static SignalEvent createBuy(LocalDateTime timestamp, String symbol, double strength, String strategyName) {
		return new SignalEvent(timestamp, symbol, "BUY", strength, strategyName, "买入信号");
	}

	public static SignalEvent createSell(LocalDateTime timestamp, String symbol, double strength, String strategyName) {
		return new SignalEvent(timestamp, symbol, "SELL", strength, strategyName, "卖出信号");
	}

	public static SignalEvent createHold(LocalDateTime timestamp, String symbol, String strategyName) {
		return new SignalEvent(timestamp, symbol, "HOLD", 0.5, strategyName, "持有信号");
	}

	// Getter方法
	public String getSymbol() {
		return symbol;
	}

	public String getSignalType() {
		return signalType;
	}

	public double getStrength() {
		return strength;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public String getComment() {
		return comment;
	}

	public String getDirection() {
		return signalType;
	}

	// 验证方法
	private boolean isValidSignalType(String signalType) {
		return "BUY".equals(signalType) || "SELL".equals(signalType) || "HOLD".equals(signalType)
				|| "CLOSE".equals(signalType);
	}

	// 判断方法
	public boolean isBuySignal() {
		return "BUY".equals(signalType);
	}

	public boolean isSellSignal() {
		return "SELL".equals(signalType) || "CLOSE".equals(signalType);
	}

	public boolean isHoldSignal() {
		return "HOLD".equals(signalType);
	}

	// 获取格式化信息
	public String getFormattedStrength() {
		return String.format("%.0f%%", strength * 100);
	}

	@Override
	public String toString() {
		return String.format("SignalEvent{时间=%s 标的=%s 类型=%s 强度=%s 策略=%s}",
				getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE), symbol, signalType, getFormattedStrength(),
				strategyName);
	}

}