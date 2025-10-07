package com.Quantitative.portfolio;

import java.util.HashMap;
import java.util.Map;

import com.Quantitative.common.utils.PerformanceMonitor;
import com.Quantitative.core.events.FillEvent;
import com.Quantitative.core.events.OrderEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.core.interfaces.TradingComponent;

/**
 * 投资组合管理类
 */
public class Portfolio implements TradingComponent {
	private double initialCash;
	private double cash;
	private double totalValue;
	private Map<String, Position> positions;
	private Map<String, Double> currentPrices;
	private String status = "CREATED";

	// 交易参数
	private double commissionRate = 0.0003; // 万三手续费
	private int minTradeQuantity = 100; // 最小交易单位（A股整手）
	private double maxPositionRatio = 0.1; // 单品种最大仓位比例

	// 性能监控
	private PerformanceMonitor performanceMonitor;

	public Portfolio(double initialCash) {
		this.initialCash = initialCash;
		this.cash = initialCash;
		this.totalValue = initialCash;
		this.positions = new HashMap<>();
		this.currentPrices = new HashMap<>();
		this.performanceMonitor = PerformanceMonitor.getInstance();
	}

	@Override
	public void initialize() {
		System.out.printf("初始化投资组合: 初始资金=%,.2f%n", initialCash);
		this.status = "INITIALIZED";
	}

	@Override
	public void configure(Map<String, Object> config) {
		if (config != null) {
			if (config.containsKey("commissionRate")) {
				this.commissionRate = (Double) config.get("commissionRate");
			}
			if (config.containsKey("minTradeQuantity")) {
				this.minTradeQuantity = (Integer) config.get("minTradeQuantity");
			}
			if (config.containsKey("maxPositionRatio")) {
				this.maxPositionRatio = (Double) config.get("maxPositionRatio");
			}
		}
	}

	@Override
	public String getName() {
		return "Portfolio";
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		this.cash = initialCash;
		this.totalValue = initialCash;
		this.positions.clear();
		this.currentPrices.clear();
		this.status = "RESET";
		System.out.println("✓ 投资组合已重置");
	}

	@Override
	public void shutdown() {
		System.out.println("关闭投资组合...");
		printStatus();
		this.status = "SHUTDOWN";
	}

	/**
	 * 修复信号处理逻辑 - 支持持仓管理
	 */
	public OrderEvent processSignal(SignalEvent signal) {
		PerformanceMonitor performanceMonitor = PerformanceMonitor.getInstance();
		performanceMonitor.startOperation("Portfolio.processSignal");

		try {
			String symbol = signal.getSymbol();
			String signalType = signal.getSignalType();
			double currentPrice = this.currentPrices.getOrDefault(symbol, 0.0);

			if (currentPrice <= 0) {
				System.out.printf("? 警告: 无法获取 %s 的当前价格%n", symbol);
				return null;
			}

			// 修复：检查信号有效性
			if (!isValidSignal(signal, symbol)) {
				return null;
			}

			// 计算交易数量
			int quantity = calculateOrderQuantity(signal, currentPrice);
			if (quantity <= 0) {
				return null;
			}

			// 创建订单
			OrderEvent order = new OrderEvent(signal.getTimestamp(), symbol, signalType, quantity, currentPrice,
					"MARKET");

			System.out.printf("[投资组合] 生成订单: %s%n", order.getOrderDescription());
			return order;

		} finally {
			performanceMonitor.endOperation("Portfolio.processSignal");
		}
	}

	/**
	 * 修复信号验证逻辑
	 */
	private boolean isValidSignal(SignalEvent signal, String symbol) {
		String signalType = signal.getSignalType();
		boolean hasPosition = hasPosition(symbol);

		// 买入信号验证
		if ("BUY".equals(signalType)) {
			if (hasPosition) {
				System.out.printf("[信号验证] %s 已有持仓，跳过买入信号%n", symbol);
				return false;
			}

			// 检查资金是否足够
			double requiredCash = calculateRequiredCash(signal, this.currentPrices.get(symbol));
			if (requiredCash > this.cash) {
				System.out.printf("[信号验证] 资金不足，需要 %.2f，当前现金 %.2f%n", requiredCash, this.cash);
				return false;
			}
		}

		// 卖出信号验证
		if ("SELL".equals(signalType) && !hasPosition) {
			System.out.printf("[信号验证] %s 没有持仓，无法卖出%n", symbol);
			return false;
		}

		return true;
	}

	/**
	 * 计算所需资金
	 */
	private double calculateRequiredCash(SignalEvent signal, double price) {
		// 根据信号强度计算仓位
		double strength = signal.getStrength();
		double positionRatio = this.maxPositionRatio * strength;
		double requiredAmount = this.totalValue * positionRatio;

		// 考虑手续费
		double commission = calculateCommission(requiredAmount);
		return requiredAmount + commission;
	}

	/**
	 * 修复订单数量计算
	 */
	private int calculateOrderQuantity(SignalEvent signal, double price) {
		String signalType = signal.getSignalType();
		String symbol = signal.getSymbol();

		if ("BUY".equals(signalType)) {
			// 使用信号强度调整仓位
			double strength = signal.getStrength();
			double positionRatio = this.maxPositionRatio * strength;
			double availableAmount = this.cash * positionRatio;

			if (availableAmount <= 0) {
				System.out.println("? 现金不足，无法买入");
				return 0;
			}

			// 限制单次买入金额
			double maxSingleTrade = this.initialCash * 0.05; // 单笔交易不超过5%
			availableAmount = Math.min(availableAmount, maxSingleTrade);

			int quantity = (int) (availableAmount / price);
			// A股整手交易
			quantity = (quantity / this.minTradeQuantity) * this.minTradeQuantity;

			if (quantity < this.minTradeQuantity) {
				System.out.printf("[仓位管理] 计算数量%d小于最小交易单位%d%n", quantity, this.minTradeQuantity);
				return 0;
			}

			System.out.printf("[仓位管理] 价格=%.2f, 可用资金=%.2f, 买入数量=%d%n", price, availableAmount, quantity);
			return Math.max(this.minTradeQuantity, quantity);

		} else if ("SELL".equals(signalType)) {
			// 卖出：卖出全部持仓
			Position position = this.positions.get(symbol);
			if (position == null || position.getQuantity() == 0) {
				System.out.printf("? 没有 %s 的持仓可卖出%n", symbol);
				return 0;
			}
			return position.getQuantity();
		}

		return 0;
	}

	// ------

	/**
	 * 处理成交事件
	 */
	public void processFill(FillEvent fill) {
		performanceMonitor.startOperation("Portfolio.processFill");

		try {
			String symbol = fill.getSymbol();
			String direction = fill.getDirection();
			int quantity = fill.getQuantity();
			double price = fill.getFillPrice();
			double commission = fill.getCommission();

			// 更新现金
			if ("BUY".equals(direction)) {
				updateCash(-(price * quantity + commission));
			} else {
				updateCash(price * quantity - commission);
			}

			// 更新持仓
			Position position = this.positions.get(symbol);
			if (position == null) {
				position = new Position(symbol);
				this.positions.put(symbol, position);
			}

			if ("BUY".equals(direction)) {
				position.addBuy(quantity, price);
			} else {
				position.addSell(quantity, price);
				// 如果持仓为0，移除该持仓记录
				if (position.getQuantity() == 0) {
					this.positions.remove(symbol);
				}
			}

			// 更新市值
			updateTotalValue();

			System.out.printf("[投资组合] 成交处理: %s, 现金: %.2f%n", fill, this.cash);

		} finally {
			performanceMonitor.endOperation("Portfolio.processFill");
		}
	}

	/**
	 * 更新市场价格
	 */
	public void updateMarketPrice(String symbol, double price) {
		this.currentPrices.put(symbol, price);

		// 更新对应持仓的市值
		Position position = this.positions.get(symbol);
		if (position != null) {
			position.updateMarketValue(price);
			updateTotalValue();
		}
	}

	/**
	 * 更新现金
	 */
	private void updateCash(double amount) {
		this.cash += amount;
		updateTotalValue();
	}

	/**
	 * 更新总资产
	 */
	private void updateTotalValue() {
		double stockValue = 0.0;
		for (Position position : this.positions.values()) {
			stockValue += position.getMarketValue();
		}
		this.totalValue = this.cash + stockValue;
	}

	/**
	 * 计算手续费
	 */
	public double calculateCommission(double amount) {
		return amount * this.commissionRate;
	}

	// ==================== 查询方法 ====================

	/**
	 * 获取指定品种的持仓信息
	 */
	public Map<String, Object> getPositionInfo(String symbol) {
		Map<String, Object> positionInfo = new HashMap<>();

		Position position = this.positions.get(symbol);
		if (position != null && position.getQuantity() > 0) {
			// 有持仓的情况
			positionInfo.put("quantity", position.getQuantity());
			positionInfo.put("avgCost", position.getAvgCost());
			positionInfo.put("marketValue", position.getMarketValue());
			positionInfo.put("unrealizedPnl", position.getUnrealizedPnl());
			positionInfo.put("unrealizedPnlPercent", position.getUnrealizedPnlPercent());

			// 添加当前价格
			Double currentPrice = this.currentPrices.get(symbol);
			if (currentPrice != null) {
				positionInfo.put("currentPrice", currentPrice);
			}

			// 计算持仓比例
			double positionRatio = (position.getMarketValue() / this.totalValue) * 100;
			positionInfo.put("positionRatio", positionRatio);

		} else {
			// 无持仓的情况，返回默认值
			positionInfo.put("quantity", 0);
			positionInfo.put("avgCost", 0.0);
			positionInfo.put("marketValue", 0.0);
			positionInfo.put("unrealizedPnl", 0.0);
			positionInfo.put("unrealizedPnlPercent", 0.0);
			positionInfo.put("positionRatio", 0.0);
		}

		return positionInfo;
	}

	/**
	 * 获取所有持仓的详细信息
	 */
	public Map<String, Map<String, Object>> getAllPositionInfo() {
		Map<String, Map<String, Object>> allPositionsInfo = new HashMap<>();

		for (String symbol : this.positions.keySet()) {
			allPositionsInfo.put(symbol, getPositionInfo(symbol));
		}

		return allPositionsInfo;
	}

	/**
	 * 获取指定品种的当前价格
	 */
	public Double getCurrentPrice(String symbol) {
		return currentPrices.get(symbol);
	}

	public double getInitialCash() {
		return this.initialCash;
	}

	public double getCash() {
		return this.cash;
	}

	public double getTotalValue() {
		return this.totalValue;
	}

	public double getTotalReturn() {
		return ((this.totalValue - this.initialCash) / this.initialCash) * 100;
	}

	public boolean hasPosition(String symbol) {
		Position position = this.positions.get(symbol);
		return position != null && position.getQuantity() > 0;
	}

	public int getPositionQuantity(String symbol) {
		Position position = this.positions.get(symbol);
		return position != null ? position.getQuantity() : 0;
	}

	public Map<String, Position> getPositions() {
		return new HashMap<>(this.positions);
	}

	// ==================== 报告方法 ====================

	/**
	 * 打印投资组合状态
	 */
	public void printStatus() {
		System.out.println("\n=== 投资组合状态 ===");
		System.out.printf("初始资金: %,.2f%n", this.initialCash);
		System.out.printf("当前现金: %,.2f%n", this.cash);
		System.out.printf("总资产: %,.2f%n", this.totalValue);
		System.out.printf("总收益率: %.2f%%%n", getTotalReturn());

		if (!this.positions.isEmpty()) {
			System.out.println("\n持仓明细:");
			for (Position position : this.positions.values()) {
				System.out.println("  " + position);
			}
		} else {
			System.out.println("暂无持仓");
		}
	}

	/**
	 * 打印所有持仓的详细信息
	 */
	public void printPositionDetails() {
		System.out.println("\n=== 持仓详细信息 ===");

		if (this.positions.isEmpty()) {
			System.out.println("暂无持仓");
			return;
		}

		for (Map.Entry<String, Position> entry : this.positions.entrySet()) {
			String symbol = entry.getKey();
			Map<String, Object> info = getPositionInfo(symbol);

			System.out.printf("品种: %s%n", symbol);
			System.out.printf("  持仓数量: %,d%n", info.get("quantity"));
			System.out.printf("  平均成本: %.2f%n", info.get("avgCost"));
			System.out.printf("  当前市值: %.2f%n", info.get("marketValue"));
			System.out.printf("  未实现盈亏: %.2f (%.2f%%)%n", info.get("unrealizedPnl"), info.get("unrealizedPnlPercent"));
			System.out.printf("  持仓比例: %.2f%%%n", info.get("positionRatio"));

			if (info.containsKey("currentPrice")) {
				System.out.printf("  当前价格: %.2f%n", info.get("currentPrice"));
			}
			System.out.println();
		}
	}

	/**
	 * 获取投资组合统计
	 */
	public Map<String, Object> getPortfolioStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("initialCash", initialCash);
		stats.put("currentCash", cash);
		stats.put("totalValue", totalValue);
		stats.put("totalReturn", getTotalReturn());
		stats.put("positionCount", positions.size());
		stats.put("commissionRate", commissionRate);
		stats.put("minTradeQuantity", minTradeQuantity);
		stats.put("maxPositionRatio", maxPositionRatio);
		return stats;
	}
}