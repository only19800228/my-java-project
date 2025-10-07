package com.Quantitative.portfolio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 持仓信息类 - 管理单个品种的持仓状态
 */
public class Position {
	private final String symbol;
	private int quantity;
	private double avgCost;
	private double marketValue;
	private double unrealizedPnl;
	private double unrealizedPnlPercent;

	// 交易记录
	private List<TradeRecord> tradeHistory;

	// 持仓统计
	private LocalDateTime firstBuyTime;
	private LocalDateTime lastTradeTime;
	private double totalBuyAmount;
	private double totalSellAmount;
	private double realizedPnl;
	private double totalCommission;

	// 风险管理
	private Double stopLossPrice;
	private Double takeProfitPrice;
	private Double trailingStopPrice;

	public Position(String symbol) {
		this.symbol = symbol;
		this.quantity = 0;
		this.avgCost = 0.0;
		this.marketValue = 0.0;
		this.unrealizedPnl = 0.0;
		this.unrealizedPnlPercent = 0.0;

		this.tradeHistory = new ArrayList<>();
		this.totalBuyAmount = 0.0;
		this.totalSellAmount = 0.0;
		this.realizedPnl = 0.0;
		this.totalCommission = 0.0;
	}

	// ==================== 核心操作方法 ====================

	/**
	 * 买入股票 - 更新平均成本
	 */
	public void addBuy(int addQuantity, double price) {
		addBuy(addQuantity, price, 0.0, LocalDateTime.now());
	}

	/**
	 * 买入股票 - 完整版本
	 */
	public void addBuy(int addQuantity, double price, double commission, LocalDateTime timestamp) {
		if (addQuantity <= 0) {
			throw new IllegalArgumentException("买入数量必须大于0: " + addQuantity);
		}
		if (price <= 0) {
			throw new IllegalArgumentException("买入价格必须大于0: " + price);
		}

		double addAmount = addQuantity * price;
		double totalCost = (this.quantity * this.avgCost) + addAmount;

		this.quantity += addQuantity;
		this.avgCost = totalCost / this.quantity;
		this.totalBuyAmount += addAmount;
		this.totalCommission += commission;

		// 记录交易
		tradeHistory.add(new TradeRecord(timestamp, "BUY", addQuantity, price, commission));

		// 更新时间戳
		updateTimestamps(timestamp);

		// 更新市值（使用买入价格作为当前价格）
		updateMarketValue(price);
	}

	/**
	 * 修复卖出股票逻辑 - 先进先出原则
	 */
	public void addSell(int reduceQuantity, double price, double commission, LocalDateTime timestamp) {
		if (reduceQuantity <= 0) {
			throw new IllegalArgumentException("卖出数量必须大于0: " + reduceQuantity);
		}
		if (reduceQuantity > this.quantity) {
			throw new IllegalArgumentException("卖出数量不能超过持仓数量: " + reduceQuantity + " > " + this.quantity);
		}
		if (price <= 0) {
			throw new IllegalArgumentException("卖出价格必须大于0: " + price);
		}

		// 计算已实现盈亏（基于平均成本）
		double sellAmount = reduceQuantity * price;
		double costBasis = reduceQuantity * this.avgCost;
		double tradePnl = sellAmount - costBasis - commission;

		// 更新持仓
		this.quantity -= reduceQuantity;
		this.realizedPnl += tradePnl;
		this.totalSellAmount += sellAmount;
		this.totalCommission += commission;

		// 记录交易
		tradeHistory.add(new TradeRecord(timestamp, "SELL", reduceQuantity, price, commission));

		// 更新时间戳
		updateTimestamps(timestamp);

		// 如果持仓为0，重置平均成本
		if (this.quantity == 0) {
			this.avgCost = 0.0;
		}

		// 更新市值（使用卖出价格作为当前价格）
		updateMarketValue(price);

		System.out.printf("[仓位管理] 卖出 %s %d股 @%.2f, 实现盈亏: %.2f%n", symbol, reduceQuantity, price, tradePnl);
	}

	/**
	 * 简化的卖出方法（兼容原有代码）
	 */
	public void addSell(int reduceQuantity, double price) {
		addSell(reduceQuantity, price, 0.0, LocalDateTime.now());
	}

	/**
	 * 更新持仓市值
	 */
	/**
	 * 更新持仓市值 - 修复版本
	 */
	public void updateMarketValue(double currentPrice) {
		this.marketValue = this.quantity * currentPrice;

		if (this.quantity > 0 && this.avgCost > 0) {
			double totalCost = this.quantity * this.avgCost;
			this.unrealizedPnl = this.marketValue - totalCost;
			this.unrealizedPnlPercent = (this.unrealizedPnl / totalCost) * 100;
		} else {
			this.unrealizedPnl = 0.0;
			this.unrealizedPnlPercent = 0.0;
		}
	}
	// ==================== 风险管理方法 ====================

	/**
	 * 设置止损价格
	 */
	public void setStopLossPrice(double stopLossPrice) {
		this.stopLossPrice = stopLossPrice;
	}

	/**
	 * 设置止盈价格
	 */
	public void setTakeProfitPrice(double takeProfitPrice) {
		this.takeProfitPrice = takeProfitPrice;
	}

	/**
	 * 设置移动止损
	 */
	public void setTrailingStopPrice(double trailingStopPrice) {
		this.trailingStopPrice = trailingStopPrice;
	}

	/**
	 * 检查是否触发止损
	 */
	public boolean isStopLossTriggered(double currentPrice) {
		return stopLossPrice != null && currentPrice <= stopLossPrice;
	}

	/**
	 * 检查是否触发止盈
	 */
	public boolean isTakeProfitTriggered(double currentPrice) {
		return takeProfitPrice != null && currentPrice >= takeProfitPrice;
	}

	/**
	 * 更新移动止损（基于新的高点）
	 */
	public void updateTrailingStop(double newHighPrice) {
		if (trailingStopPrice != null && this.avgCost > 0) {
			double newStopPrice = newHighPrice - trailingStopPrice;
			if (newStopPrice > this.stopLossPrice) {
				this.stopLossPrice = newStopPrice;
			}
		}
	}

	// ==================== 查询方法 ====================

	/**
	 * 获取总盈亏（已实现 + 未实现）
	 */
	public double getTotalPnl() {
		return realizedPnl + unrealizedPnl;
	}

	/**
	 * 获取总盈亏百分比
	 */
	public double getTotalPnlPercent() {
		double totalCost = totalBuyAmount - totalSellAmount;
		return totalCost > 0 ? (getTotalPnl() / totalCost) * 100 : 0.0;
	}

	/**
	 * 获取持仓天数
	 */
	public long getHoldingDays() {
		if (firstBuyTime == null) {
			return 0;
		}
		LocalDateTime endTime = lastTradeTime != null ? lastTradeTime : LocalDateTime.now();
		return java.time.Duration.between(firstBuyTime, endTime).toDays();
	}

	/**
	 * 获取平均持仓成本（考虑手续费）
	 */
	public double getTotalAvgCost() {
		if (quantity == 0) {
			return 0.0;
		}
		double totalCost = quantity * avgCost;
		double commissionPerShare = totalCommission / (quantity + getTotalTradedQuantity());
		return avgCost + commissionPerShare;
	}

	/**
	 * 获取总交易数量（买入 + 卖出）
	 */
	public int getTotalTradedQuantity() {
		return tradeHistory.stream().mapToInt(TradeRecord::getQuantity).sum();
	}

	/**
	 * 获取买入次数
	 */
	public long getBuyCount() {
		return tradeHistory.stream().filter(trade -> "BUY".equals(trade.getDirection())).count();
	}

	/**
	 * 获取卖出次数
	 */
	public long getSellCount() {
		return tradeHistory.stream().filter(trade -> "SELL".equals(trade.getDirection())).count();
	}

	/**
	 * 获取最近交易价格
	 */
	public double getLastTradePrice() {
		if (tradeHistory.isEmpty()) {
			return 0.0;
		}
		return tradeHistory.get(tradeHistory.size() - 1).getPrice();
	}

	/**
	 * 检查是否为空仓
	 */
	public boolean isEmpty() {
		return quantity == 0;
	}

	/**
	 * 检查是否为多仓（持仓大于0）
	 */
	public boolean isLong() {
		return quantity > 0;
	}

	/**
	 * 检查是否为空仓（持仓小于0 - 支持做空）
	 */
	public boolean isShort() {
		return quantity < 0;
	}

	// ==================== 统计报告方法 ====================

	/**
	 * 获取持仓统计信息
	 */
	public PositionStats getPositionStats() {
		return new PositionStats(symbol, quantity, avgCost, marketValue, unrealizedPnl, unrealizedPnlPercent,
				realizedPnl, getTotalPnl(), getTotalPnlPercent(), getHoldingDays(), totalBuyAmount, totalSellAmount,
				totalCommission, getBuyCount(), getSellCount());
	}

	/**
	 * 获取交易历史
	 */
	public List<TradeRecord> getTradeHistory() {
		return new ArrayList<>(tradeHistory);
	}

	/**
	 * 打印持仓详情
	 */
	public void printDetails() {
		System.out.println("\n=== 持仓详情: " + symbol + " ===");
		System.out.printf("持仓数量: %,d%n", quantity);
		System.out.printf("平均成本: %.4f%n", avgCost);
		System.out.printf("当前市值: %.2f%n", marketValue);
		System.out.printf("未实现盈亏: %.2f (%.2f%%)%n", unrealizedPnl, unrealizedPnlPercent);
		System.out.printf("已实现盈亏: %.2f%n", realizedPnl);
		System.out.printf("总盈亏: %.2f (%.2f%%)%n", getTotalPnl(), getTotalPnlPercent());
		System.out.printf("持仓天数: %d%n", getHoldingDays());
		System.out.printf("总手续费: %.2f%n", totalCommission);
		System.out.printf("交易次数: %d (买入: %d, 卖出: %d)%n", tradeHistory.size(), getBuyCount(), getSellCount());

		if (stopLossPrice != null) {
			System.out.printf("止损价格: %.4f%n", stopLossPrice);
		}
		if (takeProfitPrice != null) {
			System.out.printf("止盈价格: %.4f%n", takeProfitPrice);
		}
	}

	// ==================== 私有方法 ====================

	/**
	 * 更新时间戳
	 */
	private void updateTimestamps(LocalDateTime timestamp) {
		if (firstBuyTime == null) {
			firstBuyTime = timestamp;
		}
		lastTradeTime = timestamp;
	}

	// ==================== Getter方法 ====================

	public String getSymbol() {
		return symbol;
	}

	public int getQuantity() {
		return quantity;
	}

	public double getAvgCost() {
		return avgCost;
	}

	public double getMarketValue() {
		return marketValue;
	}

	public double getUnrealizedPnl() {
		return unrealizedPnl;
	}

	public double getUnrealizedPnlPercent() {
		return unrealizedPnlPercent;
	}

	public double getRealizedPnl() {
		return realizedPnl;
	}

	public double getTotalBuyAmount() {
		return totalBuyAmount;
	}

	public double getTotalSellAmount() {
		return totalSellAmount;
	}

	public double getTotalCommission() {
		return totalCommission;
	}

	public LocalDateTime getFirstBuyTime() {
		return firstBuyTime;
	}

	public LocalDateTime getLastTradeTime() {
		return lastTradeTime;
	}

	public Double getStopLossPrice() {
		return stopLossPrice;
	}

	public Double getTakeProfitPrice() {
		return takeProfitPrice;
	}

	public Double getTrailingStopPrice() {
		return trailingStopPrice;
	}

	@Override
	public String toString() {
		return String.format("Position{%s Qty:%,d AvgCost:%.4f MV:%.2f PnL:%.2f(%.2f%%)}", symbol, quantity, avgCost,
				marketValue, unrealizedPnl, unrealizedPnlPercent);
	}

	// ==================== 内部类 ====================

	/**
	 * 交易记录类
	 */
	public static class TradeRecord {
		private final LocalDateTime timestamp;
		private final String direction; // BUY, SELL
		private final int quantity;
		private final double price;
		private final double commission;

		public TradeRecord(LocalDateTime timestamp, String direction, int quantity, double price, double commission) {
			this.timestamp = timestamp;
			this.direction = direction;
			this.quantity = quantity;
			this.price = price;
			this.commission = commission;
		}

		// Getter方法
		public LocalDateTime getTimestamp() {
			return timestamp;
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

		public double getCommission() {
			return commission;
		}

		public double getAmount() {
			return quantity * price;
		}

		@Override
		public String toString() {
			return String.format("TradeRecord{%s %s %d@%.4f commission=%.2f}", timestamp.toLocalDate(), direction,
					quantity, price, commission);
		}
	}

	/**
	 * 持仓统计类
	 */
	public static class PositionStats {
		private final String symbol;
		private final int quantity;
		private final double avgCost;
		private final double marketValue;
		private final double unrealizedPnl;
		private final double unrealizedPnlPercent;
		private final double realizedPnl;
		private final double totalPnl;
		private final double totalPnlPercent;
		private final long holdingDays;
		private final double totalBuyAmount;
		private final double totalSellAmount;
		private final double totalCommission;
		private final long buyCount;
		private final long sellCount;

		public PositionStats(String symbol, int quantity, double avgCost, double marketValue, double unrealizedPnl,
				double unrealizedPnlPercent, double realizedPnl, double totalPnl, double totalPnlPercent,
				long holdingDays, double totalBuyAmount, double totalSellAmount, double totalCommission, long buyCount,
				long sellCount) {
			this.symbol = symbol;
			this.quantity = quantity;
			this.avgCost = avgCost;
			this.marketValue = marketValue;
			this.unrealizedPnl = unrealizedPnl;
			this.unrealizedPnlPercent = unrealizedPnlPercent;
			this.realizedPnl = realizedPnl;
			this.totalPnl = totalPnl;
			this.totalPnlPercent = totalPnlPercent;
			this.holdingDays = holdingDays;
			this.totalBuyAmount = totalBuyAmount;
			this.totalSellAmount = totalSellAmount;
			this.totalCommission = totalCommission;
			this.buyCount = buyCount;
			this.sellCount = sellCount;
		}

		// Getter方法
		public String getSymbol() {
			return symbol;
		}

		public int getQuantity() {
			return quantity;
		}

		public double getAvgCost() {
			return avgCost;
		}

		public double getMarketValue() {
			return marketValue;
		}

		public double getUnrealizedPnl() {
			return unrealizedPnl;
		}

		public double getUnrealizedPnlPercent() {
			return unrealizedPnlPercent;
		}

		public double getRealizedPnl() {
			return realizedPnl;
		}

		public double getTotalPnl() {
			return totalPnl;
		}

		public double getTotalPnlPercent() {
			return totalPnlPercent;
		}

		public long getHoldingDays() {
			return holdingDays;
		}

		public double getTotalBuyAmount() {
			return totalBuyAmount;
		}

		public double getTotalSellAmount() {
			return totalSellAmount;
		}

		public double getTotalCommission() {
			return totalCommission;
		}

		public long getBuyCount() {
			return buyCount;
		}

		public long getSellCount() {
			return sellCount;
		}

		public long getTotalTradeCount() {
			return buyCount + sellCount;
		}

		@Override
		public String toString() {
			return String.format("PositionStats{%s Qty:%,d Cost:%.4f MV:%.2f PnL:%.2f(%.2f%%) Days:%d}", symbol,
					quantity, avgCost, marketValue, totalPnl, totalPnlPercent, holdingDays);
		}

		/**
		 * 转换为Map格式
		 */
		public java.util.Map<String, Object> toMap() {
			java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
			map.put("symbol", symbol);
			map.put("quantity", quantity);
			map.put("avgCost", avgCost);
			map.put("marketValue", marketValue);
			map.put("unrealizedPnl", unrealizedPnl);
			map.put("unrealizedPnlPercent", unrealizedPnlPercent);
			map.put("realizedPnl", realizedPnl);
			map.put("totalPnl", totalPnl);
			map.put("totalPnlPercent", totalPnlPercent);
			map.put("holdingDays", holdingDays);
			map.put("totalBuyAmount", totalBuyAmount);
			map.put("totalSellAmount", totalSellAmount);
			map.put("totalCommission", totalCommission);
			map.put("buyCount", buyCount);
			map.put("sellCount", sellCount);
			map.put("totalTradeCount", getTotalTradeCount());
			return map;
		}
	}
}