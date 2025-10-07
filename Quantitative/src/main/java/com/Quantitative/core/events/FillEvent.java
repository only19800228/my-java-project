package com.Quantitative.core.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 成交事件 - 订单执行后的成交记录
 */
public class FillEvent extends Event {
	private final String symbol;
	private final String direction;
	private final int quantity;
	private final double fillPrice;
	private final double commission;
	private final String orderId;
	private final String executionId;
	private final String fillType; // FULL, PARTIAL
	private final double realizedPnl; // 已实现盈亏

	// 成交类型常量
	public static final String FULL_FILL = "FULL";
	public static final String PARTIAL_FILL = "PARTIAL";

	/**
	 * 基础构造函数
	 */
	public FillEvent(LocalDateTime timestamp, String symbol, String direction, int quantity, double fillPrice,
			double commission, String orderId) {
		this(timestamp, symbol, direction, quantity, fillPrice, commission, orderId, generateExecutionId(), FULL_FILL,
				0.0);
	}

	/**
	 * 完整构造函数
	 */
	public FillEvent(LocalDateTime timestamp, String symbol, String direction, int quantity, double fillPrice,
			double commission, String orderId, String executionId, String fillType, double realizedPnl) {
		super(timestamp, "FILL", "ExecutionEngine", PRIORITY_HIGH);

		this.symbol = symbol;
		this.direction = validateDirection(direction);
		this.quantity = validateQuantity(quantity);
		this.fillPrice = validatePrice(fillPrice);
		this.commission = validateCommission(commission);
		this.orderId = validateOrderId(orderId);
		this.executionId = executionId != null ? executionId : generateExecutionId();
		this.fillType = validateFillType(fillType);
		this.realizedPnl = realizedPnl;
	}

	// ==================== 验证方法 ====================

	private String validateDirection(String direction) {
		if (!OrderEvent.BUY.equals(direction) && !OrderEvent.SELL.equals(direction)) {
			throw new IllegalArgumentException("无效的成交方向: " + direction);
		}
		return direction;
	}

	private int validateQuantity(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("成交数量必须大于0: " + quantity);
		}
		return quantity;
	}

	private double validatePrice(double price) {
		if (price <= 0) {
			throw new IllegalArgumentException("成交价格必须大于0: " + price);
		}
		return price;
	}

	private double validateCommission(double commission) {
		if (commission < 0) {
			throw new IllegalArgumentException("手续费不能为负数: " + commission);
		}
		return commission;
	}

	private String validateOrderId(String orderId) {
		if (orderId == null || orderId.trim().isEmpty()) {
			throw new IllegalArgumentException("订单ID不能为空");
		}
		return orderId;
	}

	private String validateFillType(String fillType) {
		if (!FULL_FILL.equals(fillType) && !PARTIAL_FILL.equals(fillType)) {
			throw new IllegalArgumentException("无效的成交类型: " + fillType);
		}
		return fillType;
	}

	// ==================== Getter方法 ====================

	public String getSymbol() {
		return symbol;
	}

	public String getDirection() {
		return direction;
	}

	public int getQuantity() {
		return quantity;
	}

	public double getFillPrice() {
		return fillPrice;
	}

	public double getCommission() {
		return commission;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getExecutionId() {
		return executionId;
	}

	public String getFillType() {
		return fillType;
	}

	public double getRealizedPnl() {
		return realizedPnl;
	}

	// ==================== 业务方法 ====================

	/**
	 * 判断是否是买入成交
	 */
	public boolean isBuyFill() {
		return OrderEvent.BUY.equals(direction);
	}

	/**
	 * 判断是否是卖出成交
	 */
	public boolean isSellFill() {
		return OrderEvent.SELL.equals(direction);
	}

	/**
	 * 判断是否是完全成交
	 */
	public boolean isFullFill() {
		return FULL_FILL.equals(fillType);
	}

	/**
	 * 判断是否是部分成交
	 */
	public boolean isPartialFill() {
		return PARTIAL_FILL.equals(fillType);
	}

	/**
	 * 计算成交总金额
	 */
	public double getTotalAmount() {
		return fillPrice * quantity;
	}

	/**
	 * 计算净金额（考虑手续费）
	 */
	public double getNetAmount() {
		double amount = getTotalAmount();
		if (isBuyFill()) {
			return -(amount + commission); // 买入：现金减少
		} else {
			return amount - commission; // 卖出：现金增加
		}
	}

	/**
	 * 计算成本（买入时使用）
	 */
	public double getCost() {
		if (!isBuyFill()) {
			return 0.0;
		}
		return getTotalAmount() + commission;
	}

	/**
	 * 获取成交描述
	 */
	public String getFillDescription() {
		return String.format("%s %s %d@%.2f 手续费:%.2f %s", symbol, direction, quantity, fillPrice, commission, fillType);
	}

	/**
	 * 生成执行ID
	 */
	private static String generateExecutionId() {
		return "EXEC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Override
	public String toString() {
		return String.format("FillEvent{%s %s %s %d@%.2f commission=%.2f orderId=%s}", getTimestamp(), symbol,
				direction, quantity, fillPrice, commission, orderId);
	}

	/**
	 * 成交构建器 - 支持链式调用
	 */
	public static class Builder {
		private LocalDateTime timestamp;
		private String symbol;
		private String direction;
		private int quantity;
		private double fillPrice;
		private double commission;
		private String orderId;
		private String executionId;
		private String fillType = FULL_FILL;
		private double realizedPnl = 0.0;

		public Builder(LocalDateTime timestamp, String symbol, String direction, int quantity, double fillPrice,
				double commission, String orderId) {
			this.timestamp = timestamp;
			this.symbol = symbol;
			this.direction = direction;
			this.quantity = quantity;
			this.fillPrice = fillPrice;
			this.commission = commission;
			this.orderId = orderId;
		}

		public Builder executionId(String executionId) {
			this.executionId = executionId;
			return this;
		}

		public Builder fillType(String fillType) {
			this.fillType = fillType;
			return this;
		}

		public Builder realizedPnl(double realizedPnl) {
			this.realizedPnl = realizedPnl;
			return this;
		}

		public FillEvent build() {
			return new FillEvent(timestamp, symbol, direction, quantity, fillPrice, commission, orderId, executionId,
					fillType, realizedPnl);
		}
	}
}