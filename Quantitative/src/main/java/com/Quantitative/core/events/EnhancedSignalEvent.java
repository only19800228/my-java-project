package com.Quantitative.core.events;

import java.time.LocalDateTime;

/**
 * 
 * 
 * 
 * 增强版交易信号事件 - 支持仓位管理和风险管理
 */
public class EnhancedSignalEvent extends SignalEvent {
	private Double positionSize; // 仓位比例，如0.02表示2%
	private Double stopLossPrice; // 止损价格
	private Double takeProfitPrice; // 止盈价格
	private Double trailingStop; // 移动止损距离
	private String riskLevel; // 风险等级：LOW, MEDIUM, HIGH
	private String signalCategory; // 信号类别：ENTRY, EXIT, ADJUST
	private Integer validityPeriod; // 信号有效期（分钟）

	// 风险等级常量
	public static final String RISK_LOW = "LOW";
	public static final String RISK_MEDIUM = "MEDIUM";
	public static final String RISK_HIGH = "HIGH";

	// 信号类别常量
	public static final String CATEGORY_ENTRY = "ENTRY";
	public static final String CATEGORY_EXIT = "EXIT";
	public static final String CATEGORY_ADJUST = "ADJUST";

	/**
	 * 基础构造函数
	 */
	public EnhancedSignalEvent(LocalDateTime timestamp, String symbol, String signalType, double strength,
			String strategyName) {
		this(timestamp, symbol, signalType, strength, strategyName, "", null, null, null, null, RISK_MEDIUM,
				CATEGORY_ENTRY, 60);
	}

	/**
	 * 完整构造函数
	 */
	public EnhancedSignalEvent(LocalDateTime timestamp, String symbol, String signalType, double strength,
			String strategyName, String comment, Double positionSize, Double stopLossPrice, Double takeProfitPrice,
			Double trailingStop, String riskLevel, String signalCategory, Integer validityPeriod) {
		super(timestamp, symbol, signalType, strength, strategyName, comment);

		this.positionSize = validatePositionSize(positionSize);
		this.stopLossPrice = stopLossPrice;
		this.takeProfitPrice = takeProfitPrice;
		this.trailingStop = trailingStop;
		this.riskLevel = validateRiskLevel(riskLevel);
		this.signalCategory = validateSignalCategory(signalCategory);
		this.validityPeriod = validateValidityPeriod(validityPeriod);
	}

	// ==================== 验证方法 ====================

	private Double validatePositionSize(Double positionSize) {
		if (positionSize != null && (positionSize <= 0 || positionSize > 1)) {
			throw new IllegalArgumentException("仓位比例必须在0到1之间: " + positionSize);
		}
		return positionSize;
	}

	private String validateRiskLevel(String riskLevel) {
		if (!RISK_LOW.equals(riskLevel) && !RISK_MEDIUM.equals(riskLevel) && !RISK_HIGH.equals(riskLevel)) {
			throw new IllegalArgumentException("无效的风险等级: " + riskLevel);
		}
		return riskLevel;
	}

	private String validateSignalCategory(String signalCategory) {
		if (!CATEGORY_ENTRY.equals(signalCategory) && !CATEGORY_EXIT.equals(signalCategory)
				&& !CATEGORY_ADJUST.equals(signalCategory)) {
			throw new IllegalArgumentException("无效的信号类别: " + signalCategory);
		}
		return signalCategory;
	}

	private Integer validateValidityPeriod(Integer validityPeriod) {
		if (validityPeriod != null && validityPeriod <= 0) {
			throw new IllegalArgumentException("有效期必须大于0: " + validityPeriod);
		}
		return validityPeriod;
	}

	// ==================== Getter和Setter方法 ====================

	public Double getPositionSize() {
		return positionSize;
	}

	public Double getStopLossPrice() {
		return stopLossPrice;
	}

	public Double getTakeProfitPrice() {
		return takeProfitPrice;
	}

	public Double getTrailingStop() {
		return trailingStop;
	}

	public String getRiskLevel() {
		return riskLevel;
	}

	public String getSignalCategory() {
		return signalCategory;
	}

	public Integer getValidityPeriod() {
		return validityPeriod;
	}

	// ==================== 链式设置方法 ====================

	public EnhancedSignalEvent setPositionSize(double positionSize) {
		this.positionSize = validatePositionSize(positionSize);
		return this;
	}

	public EnhancedSignalEvent setStopLossPrice(double stopLossPrice) {
		this.stopLossPrice = stopLossPrice;
		return this;
	}

	public EnhancedSignalEvent setTakeProfitPrice(double takeProfitPrice) {
		this.takeProfitPrice = takeProfitPrice;
		return this;
	}

	public EnhancedSignalEvent setTrailingStop(double trailingStop) {
		this.trailingStop = trailingStop;
		return this;
	}

	public EnhancedSignalEvent setRiskLevel(String riskLevel) {
		this.riskLevel = validateRiskLevel(riskLevel);
		return this;
	}

	public EnhancedSignalEvent setSignalCategory(String signalCategory) {
		this.signalCategory = validateSignalCategory(signalCategory);
		return this;
	}

	public EnhancedSignalEvent setValidityPeriod(int validityPeriod) {
		this.validityPeriod = validateValidityPeriod(validityPeriod);
		return this;
	}

	// ==================== 业务方法 ====================

	/**
	 * 检查是否有风险管理设置
	 */
	public boolean hasRiskManagement() {
		return stopLossPrice != null || takeProfitPrice != null || trailingStop != null;
	}

	/**
	 * 检查是否有仓位管理设置
	 */
	public boolean hasPositionManagement() {
		return positionSize != null;
	}

	/**
	 * 检查信号是否过期
	 */
	public boolean isExpired() {
		if (validityPeriod == null) {
			return false;
		}
		long ageInMinutes = java.time.Duration.between(getTimestamp(), LocalDateTime.now()).toMinutes();
		return ageInMinutes > validityPeriod;
	}

	/**
	 * 计算风险回报比
	 */
	public Double getRiskRewardRatio() {
		if (stopLossPrice == null || takeProfitPrice == null) {
			return null;
		}

		// 这里需要实际入场价格，暂时返回null
		// 实际使用时应该基于实际入场价格计算
		return null;
	}

	/**
	 * 获取止损距离（百分比）
	 */
	public Double getStopLossDistancePercent(double currentPrice) {
		if (stopLossPrice == null) {
			return null;
		}
		return Math.abs(stopLossPrice - currentPrice) / currentPrice * 100;
	}

	/**
	 * 获取止盈距离（百分比）
	 */
	public Double getTakeProfitDistancePercent(double currentPrice) {
		if (takeProfitPrice == null) {
			return null;
		}
		return Math.abs(takeProfitPrice - currentPrice) / currentPrice * 100;
	}

	/**
	 * 判断是否是入场信号
	 */
	public boolean isEntrySignal() {
		return CATEGORY_ENTRY.equals(signalCategory) && isBuySignal();
	}

	/**
	 * 判断是否是出场信号
	 */
	public boolean isExitSignal() {
		return CATEGORY_EXIT.equals(signalCategory) && isSellSignal();
	}

	/**
	 * 获取信号详细信息
	 */
	public String getEnhancedDescription() {
		StringBuilder sb = new StringBuilder(super.toString());

		if (positionSize != null) {
			sb.append(String.format(", 仓位=%.1f%%", positionSize * 100));
		}
		if (stopLossPrice != null) {
			sb.append(String.format(", 止损=%.2f", stopLossPrice));
		}
		if (takeProfitPrice != null) {
			sb.append(String.format(", 止盈=%.2f", takeProfitPrice));
		}
		if (trailingStop != null) {
			sb.append(String.format(", 移动止损=%.2f", trailingStop));
		}
		if (riskLevel != null) {
			sb.append(", 风险等级=").append(riskLevel);
		}
		if (signalCategory != null) {
			sb.append(", 信号类别=").append(signalCategory);
		}
		if (validityPeriod != null) {
			sb.append(", 有效期=").append(validityPeriod).append("分钟");
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return getEnhancedDescription();
	}

	/**
	 * 转换为基础信号事件
	 */
	public SignalEvent toBasicSignal() {
		return new SignalEvent(getTimestamp(), getSymbol(), getSignalType(), getStrength(), getStrategyName(),
				getComment());
	}

	/**
	 * 创建入场信号构建器
	 */
	public static Builder entryBuilder(LocalDateTime timestamp, String symbol, double strength, String strategyName) {
		return new Builder(timestamp, symbol, OrderEvent.BUY, strength, strategyName).signalCategory(CATEGORY_ENTRY);
	}

	/**
	 * 创建出场信号构建器
	 */
	public static Builder exitBuilder(LocalDateTime timestamp, String symbol, double strength, String strategyName) {
		return new Builder(timestamp, symbol, OrderEvent.SELL, strength, strategyName).signalCategory(CATEGORY_EXIT);
	}

	/**
	 * 增强信号构建器
	 */
	public static class Builder {
		private LocalDateTime timestamp;
		private String symbol;
		private String signalType;
		private double strength;
		private String strategyName;
		private String comment = "";
		private Double positionSize;
		private Double stopLossPrice;
		private Double takeProfitPrice;
		private Double trailingStop;
		private String riskLevel = RISK_MEDIUM;
		private String signalCategory = CATEGORY_ENTRY;
		private Integer validityPeriod = 60;

		public Builder(LocalDateTime timestamp, String symbol, String signalType, double strength,
				String strategyName) {
			this.timestamp = timestamp;
			this.symbol = symbol;
			this.signalType = signalType;
			this.strength = strength;
			this.strategyName = strategyName;
		}

		public Builder comment(String comment) {
			this.comment = comment;
			return this;
		}

		public Builder positionSize(double positionSize) {
			this.positionSize = positionSize;
			return this;
		}

		public Builder stopLossPrice(double stopLossPrice) {
			this.stopLossPrice = stopLossPrice;
			return this;
		}

		public Builder takeProfitPrice(double takeProfitPrice) {
			this.takeProfitPrice = takeProfitPrice;
			return this;
		}

		public Builder trailingStop(double trailingStop) {
			this.trailingStop = trailingStop;
			return this;
		}

		public Builder riskLevel(String riskLevel) {
			this.riskLevel = riskLevel;
			return this;
		}

		public Builder signalCategory(String signalCategory) {
			this.signalCategory = signalCategory;
			return this;
		}

		public Builder validityPeriod(int validityPeriod) {
			this.validityPeriod = validityPeriod;
			return this;
		}

		public EnhancedSignalEvent build() {
			return new EnhancedSignalEvent(timestamp, symbol, signalType, strength, strategyName, comment, positionSize,
					stopLossPrice, takeProfitPrice, trailingStop, riskLevel, signalCategory, validityPeriod);
		}
	}
}