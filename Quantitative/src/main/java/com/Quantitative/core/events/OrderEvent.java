package com.Quantitative.core.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单事件 - 投资组合模块生成的订单
 */
public class OrderEvent extends Event {
	private final String symbol;
	private final String direction; // BUY, SELL
	private final int quantity;
	private final double price;
	private final String orderType; // MARKET, LIMIT, STOP
	private final String orderId;
	private final Double stopPrice; // 止损价格
	private final Double limitPrice; // 限价价格
	private final String timeInForce; // GTC, IOC, FOK

	// 订单类型常量
	public static final String MARKET_ORDER = "MARKET";
	public static final String LIMIT_ORDER = "LIMIT";
	public static final String STOP_ORDER = "STOP";
	public static final String STOP_LIMIT_ORDER = "STOP_LIMIT";

	// 订单方向常量
	public static final String BUY = "BUY";
	public static final String SELL = "SELL";

	// 有效时间常量
	public static final String GTC = "GTC"; // 取消前有效
	public static final String IOC = "IOC"; // 立即或取消
	public static final String FOK = "FOK"; // 全部或取消

	/**
	 * 市价单构造函数
	 */
	public OrderEvent(LocalDateTime timestamp, String symbol, String direction, int quantity, double price,
			String orderType) {
		this(timestamp, symbol, direction, quantity, price, orderType, generateOrderId(), null, null, GTC);
	}

	/**
	 * 完整构造函数
	 */
	public OrderEvent(LocalDateTime timestamp, String symbol, String direction, int quantity, double price,
			String orderType, String orderId, Double stopPrice, Double limitPrice, String timeInForce) {
		super(timestamp, "ORDER", "Portfolio", "MARKET".equals(orderType) ? PRIORITY_HIGH : PRIORITY_NORMAL);

		this.symbol = symbol;
		this.direction = validateDirection(direction);
		this.quantity = validateQuantity(quantity);
		this.price = validatePrice(price);
		this.orderType = validateOrderType(orderType);
		this.orderId = orderId != null ? orderId : generateOrderId();
		this.stopPrice = stopPrice;
		this.limitPrice = limitPrice;
		this.timeInForce = validateTimeInForce(timeInForce);

		// 验证订单参数一致性
		validateOrderConsistency();
	}

	// ==================== 验证方法 ====================

	private String validateDirection(String direction) {
		if (!BUY.equals(direction) && !SELL.equals(direction)) {
			throw new IllegalArgumentException("无效的订单方向: " + direction);
		}
		return direction;
	}

	private int validateQuantity(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("订单数量必须大于0: " + quantity);
		}
		return quantity;
	}

	private double validatePrice(double price) {
		if (price <= 0) {
			throw new IllegalArgumentException("订单价格必须大于0: " + price);
		}
		return price;
	}

	private String validateOrderType(String orderType) {
		if (!MARKET_ORDER.equals(orderType) && !LIMIT_ORDER.equals(orderType) && !STOP_ORDER.equals(orderType)
				&& !STOP_LIMIT_ORDER.equals(orderType)) {
			throw new IllegalArgumentException("无效的订单类型: " + orderType);
		}
		return orderType;
	}

	private String validateTimeInForce(String timeInForce) {
		if (!GTC.equals(timeInForce) && !IOC.equals(timeInForce) && !FOK.equals(timeInForce)) {
			throw new IllegalArgumentException("无效的有效时间: " + timeInForce);
		}
		return timeInForce;
	}

	private void validateOrderConsistency() {
		// 限价单必须有limitPrice
		if (LIMIT_ORDER.equals(orderType) && limitPrice == null) {
			throw new IllegalArgumentException("限价单必须设置limitPrice");
		}

		// 止损单必须有stopPrice
		if (STOP_ORDER.equals(orderType) && stopPrice == null) {
			throw new IllegalArgumentException("止损单必须设置stopPrice");
		}

		// 止损限价单必须有stopPrice和limitPrice
		if (STOP_LIMIT_ORDER.equals(orderType) && (stopPrice == null || limitPrice == null)) {
			throw new IllegalArgumentException("止损限价单必须设置stopPrice和limitPrice");
		}
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

	public double getPrice() {
		return price;
	}

	public String getOrderType() {
		return orderType;
	}

	public String getOrderId() {
		return orderId;
	}

	public Double getStopPrice() {
		return stopPrice;
	}

	public Double getLimitPrice() {
		return limitPrice;
	}

	public String getTimeInForce() {
		return timeInForce;
	}

	// ==================== 业务方法 ====================

	/**
	 * 判断是否是买入订单
	 */
	public boolean isBuyOrder() {
		return BUY.equals(direction);
	}

	/**
	 * 判断是否是卖出订单
	 */
	public boolean isSellOrder() {
		return SELL.equals(direction);
	}

	/**
	 * 判断是否是市价单
	 */
	public boolean isMarketOrder() {
		return MARKET_ORDER.equals(orderType);
	}

	/**
	 * 判断是否是限价单
	 */
	public boolean isLimitOrder() {
		return LIMIT_ORDER.equals(orderType);
	}

	/**
	 * 判断是否是止损单
	 */
	public boolean isStopOrder() {
		return STOP_ORDER.equals(orderType);
	}

	/**
	 * 计算订单总金额
	 */
	public double getTotalAmount() {
		return price * quantity;
	}

	/**
	 * 获取订单描述
	 */
	public String getOrderDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append(symbol).append(" ").append(direction).append(" ").append(quantity).append(" @").append(price);

		if (isLimitOrder()) {
			sb.append(" (限价:").append(limitPrice).append(")");
		} else if (isStopOrder()) {
			sb.append(" (止损:").append(stopPrice).append(")");
		} else if (STOP_LIMIT_ORDER.equals(orderType)) {
			sb.append(" (止损限价:").append(stopPrice).append("/").append(limitPrice).append(")");
		}

		sb.append(" [").append(timeInForce).append("]");
		return sb.toString();
	}

	/**
	 * 生成订单ID
	 */
	private static String generateOrderId() {
		return "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Override
	public String toString() {
		return String.format("OrderEvent{%s %s %s %d@%.2f type=%s id=%s}", getTimestamp(), symbol, direction, quantity,
				price, orderType, orderId);
	}

	/**
	 * 订单构建器 - 支持链式调用
	 */
	public static class Builder {
		private LocalDateTime timestamp;
		private String symbol;
		private String direction;
		private int quantity;
		private double price;
		private String orderType = MARKET_ORDER;
		private String orderId;
		private Double stopPrice;
		private Double limitPrice;
		private String timeInForce = GTC;

		public Builder(LocalDateTime timestamp, String symbol, String direction, int quantity, double price) {
			this.timestamp = timestamp;
			this.symbol = symbol;
			this.direction = direction;
			this.quantity = quantity;
			this.price = price;
		}

		public Builder orderType(String orderType) {
			this.orderType = orderType;
			return this;
		}

		public Builder orderId(String orderId) {
			this.orderId = orderId;
			return this;
		}

		public Builder stopPrice(Double stopPrice) {
			this.stopPrice = stopPrice;
			return this;
		}

		public Builder limitPrice(Double limitPrice) {
			this.limitPrice = limitPrice;
			return this;
		}

		public Builder timeInForce(String timeInForce) {
			this.timeInForce = timeInForce;
			return this;
		}

		public OrderEvent build() {
			return new OrderEvent(timestamp, symbol, direction, quantity, price, orderType, orderId, stopPrice,
					limitPrice, timeInForce);
		}
	}
}