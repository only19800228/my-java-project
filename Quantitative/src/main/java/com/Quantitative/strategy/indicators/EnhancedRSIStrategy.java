package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.monitor.MonitorUtils;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.EnhancedSignalEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 修复后的增强版RSI策略 - 修复 getLastRSI() 方法
 */
public class EnhancedRSIStrategy extends BaseStrategy {
	// 配置参数
	private int rsiPeriod;
	private double overbought;
	private double oversold;
	private double positionSizeRatio;
	private double signalThreshold;
	private boolean useTrendFilter;
	private double trendThreshold;

	// 状态跟踪
	private Map<String, List<Double>> priceHistoryMap = new HashMap<>();
	private Map<String, Double> lastRSIMap = new HashMap<>();
	private Map<String, Boolean> positionMap = new HashMap<>();

	// 用于单symbol场景的最后一个RSI值
	private Double lastSingleRSI = null;

	public EnhancedRSIStrategy() {
		super("增强RSI策略");
		loadConfigFromSystem();
	}

	public EnhancedRSIStrategy(int period, double overbought, double oversold, double positionSizeRatio) {
		super("增强RSI策略");
		this.rsiPeriod = period;
		this.overbought = overbought;
		this.oversold = oversold;
		this.positionSizeRatio = positionSizeRatio;
		setDefaultParameters();
	}

	/**
	 * 从系统配置加载参数
	 */
	private void loadConfigFromSystem() {
		this.rsiPeriod = 14;
		this.overbought = 70.0;
		this.oversold = 30.0;
		this.positionSizeRatio = 0.02;
		this.signalThreshold = 5.0;
		this.useTrendFilter = false;
		this.trendThreshold = 0.0;

		setDefaultParameters();
	}

	private void setDefaultParameters() {
		setParameter("rsiPeriod", rsiPeriod);
		setParameter("overbought", overbought);
		setParameter("oversold", oversold);
		setParameter("positionSizeRatio", positionSizeRatio);
		setParameter("signalThreshold", signalThreshold);
		setParameter("useTrendFilter", useTrendFilter);
		setParameter("trendThreshold", trendThreshold);
	}

	@Override
	protected void init() {
		System.out.printf("RSI策略初始化: 周期=%d, 超买=%.1f, 超卖=%.1f%n", rsiPeriod, overbought, oversold);

		priceHistoryMap.clear();
		lastRSIMap.clear();
		positionMap.clear();
		lastSingleRSI = null;

		if (debugMode) {
			System.out.println("RSI策略初始化完成");
		}
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		// 使用监控工具监控策略计算
		MonitorUtils.monitorVoid("RSIStrategy", "calculateSignals", () -> {
			String symbol = bar.getSymbol();
			double currentPrice = bar.getClose();

			// 更新价格历史
			updatePriceHistory(symbol, currentPrice);

			if (!hasEnoughDataForRSI(symbol)) {
				return;
			}

			// 计算RSI - 监控缓存操作
			Double rsi = MonitorUtils.monitor("RSIStrategy", "calculateRSI", () -> {
				return calculateCurrentRSI(symbol);
			});

			if (rsi == null)
				return;

			lastRSIMap.put(symbol, rsi);
			lastSingleRSI = rsi;

			// 生成信号
			SignalEvent signal = generateSignal(bar, symbol, rsi, currentPrice,
					portfolio != null && portfolio.hasPosition(symbol));

			if (signal != null) {
				signals.add(signal);
			}
		});
	}

	/**
	 * 带缓存监控的RSI计算
	 */
	private Double calculateCurrentRSI(String symbol) {
		List<Double> prices = getPriceHistory(symbol, getRequiredBars());
		if (prices.size() < getRequiredBars()) {
			return null;
		}

		// 使用缓存计算，并监控缓存性能
		long startTime = System.nanoTime();
		Double result = calculateWithCache("RSI", prices, rsiPeriod);
		long duration = System.nanoTime() - startTime;

		// 记录缓存操作（这里简化，实际应该在calculateWithCache内部记录）
		boolean cacheHit = result != null; // 简化判断
		MonitorUtils.recordCacheOperation("RSI_" + rsiPeriod, duration, cacheHit);

		return result;
	}

	// ==================== 价格历史管理方法 ====================

	/**
	 * 更新价格历史
	 */
	private void updatePriceHistory(String symbol, double price) {
		List<Double> priceHistory = priceHistoryMap.get(symbol);
		if (priceHistory == null) {
			priceHistory = new ArrayList<>();
			priceHistoryMap.put(symbol, priceHistory);
		}

		priceHistory.add(price);

		// 限制历史数据大小，避免内存泄漏
		int maxHistorySize = getRequiredBars() + 100;
		if (priceHistory.size() > maxHistorySize) {
			priceHistory = priceHistory.subList(priceHistory.size() - maxHistorySize, priceHistory.size());
			priceHistoryMap.put(symbol, priceHistory);
		}
	}

	/**
	 * 获取所需的最小数据条数
	 */
	private int getRequiredBars() {
		return rsiPeriod + 1;
	}

	/**
	 * 检查是否有足够数据计算RSI
	 */
	private boolean hasEnoughDataForRSI(String symbol) {
		return getPriceHistorySize(symbol) >= getRequiredBars();
	}

	/**
	 * 获取价格历史大小
	 */
	private int getPriceHistorySize(String symbol) {
		List<Double> priceHistory = priceHistoryMap.get(symbol);
		return priceHistory != null ? priceHistory.size() : 0;
	}

	/**
	 * 获取价格历史
	 */
	private List<Double> getPriceHistory(String symbol, int count) {
		List<Double> priceHistory = priceHistoryMap.get(symbol);
		if (priceHistory == null || priceHistory.size() < count) {
			return new ArrayList<>();
		}

		int startIndex = priceHistory.size() - count;
		return new ArrayList<>(priceHistory.subList(startIndex, priceHistory.size()));
	}

	// ==================== 信号生成方法 ====================

	/**
	 * 生成交易信号
	 */
	private SignalEvent generateSignal(BarEvent bar, String symbol, double rsi, double currentPrice,
			boolean hasPosition) {
		// 趋势过滤
		if (useTrendFilter && !isUptrend(symbol)) {
			return null;
		}

		if (!hasPosition) {
			return generateBuySignal(bar, symbol, rsi, currentPrice);
		} else {
			return generateSellSignal(bar, symbol, rsi, currentPrice);
		}
	}

	/**
	 * 判断是否处于上升趋势
	 */
	private boolean isUptrend(String symbol) {
		List<Double> prices = getPriceHistory(symbol, 20);
		if (prices.size() < 20)
			return true;

		double currentPrice = prices.get(prices.size() - 1);
		double pastPrice = prices.get(0);
		double trend = (currentPrice - pastPrice) / pastPrice * 100;

		return trend >= trendThreshold;
	}

	/**
	 * 生成买入信号
	 */
	private SignalEvent generateBuySignal(BarEvent bar, String symbol, double rsi, double currentPrice) {
		boolean buyCondition = rsi < oversold && (oversold - rsi) >= signalThreshold;

		if (buyCondition) {
			double strength = calculateBuySignalStrength(rsi);

			if (debugMode) {
				System.out.printf("[RSI买入] %s RSI=%.1f < %.1f, 强度=%.2f%n", bar.getTimestamp().toLocalDate(), rsi,
						oversold, strength);
			}

			// 创建增强信号，包含止损止盈信息
			return createEnhancedSignal(bar, symbol, "BUY", strength);
		}

		return null;
	}

	/**
	 * 生成卖出信号
	 */
	private SignalEvent generateSellSignal(BarEvent bar, String symbol, double rsi, double currentPrice) {
		boolean sellCondition = rsi > overbought && (rsi - overbought) >= signalThreshold;

		if (sellCondition) {
			double strength = calculateSellSignalStrength(rsi);

			if (debugMode) {
				System.out.printf("[RSI卖出] %s RSI=%.1f > %.1f, 强度=%.2f%n", bar.getTimestamp().toLocalDate(), rsi,
						overbought, strength);
			}

			return createEnhancedSignal(bar, symbol, "SELL", strength);
		}

		return null;
	}

	/**
	 * 创建增强信号（包含风险管理）
	 */
	private SignalEvent createEnhancedSignal(BarEvent bar, String symbol, String direction, double strength) {
		// 计算止损止盈价格
		Double stopLoss = calculateStopLossPrice(bar, direction);
		Double takeProfit = calculateTakeProfitPrice(bar, direction);
		Double trailingStop = direction.equals("BUY") ? 0.05 : null; // 买入时设置5%移动止损

		return new EnhancedSignalEvent(bar.getTimestamp(), symbol, direction, strength, getName(), "RSI策略信号",
				positionSizeRatio, stopLoss, takeProfit, trailingStop, "MEDIUM", "ENTRY", 60);
	}

	/**
	 * 计算止损价格
	 */
	private Double calculateStopLossPrice(BarEvent bar, String direction) {
		double currentPrice = bar.getClose();
		if (direction.equals("BUY")) {
			return currentPrice * 0.95; // 5%止损
		} else {
			return currentPrice * 1.05; // 5%止损（对于做空）
		}
	}

	/**
	 * 计算止盈价格
	 */
	private Double calculateTakeProfitPrice(BarEvent bar, String direction) {
		double currentPrice = bar.getClose();
		if (direction.equals("BUY")) {
			return currentPrice * 1.10; // 10%止盈
		} else {
			return currentPrice * 0.90; // 10%止盈（对于做空）
		}
	}

	/**
	 * 计算买入信号强度
	 */
	private double calculateBuySignalStrength(double rsi) {
		double distanceFromOversold = Math.max(0, oversold - rsi);
		double maxDistance = oversold;
		double baseStrength = distanceFromOversold / maxDistance;
		return Math.min(1.0, baseStrength * 0.8 + 0.2);
	}

	/**
	 * 计算卖出信号强度
	 */
	private double calculateSellSignalStrength(double rsi) {
		double distanceFromOverbought = Math.max(0, rsi - overbought);
		double maxDistance = 100 - overbought;
		double baseStrength = distanceFromOverbought / maxDistance;
		return Math.min(1.0, baseStrength * 0.8 + 0.2);
	}

	// ==================== RSI 获取方法 ====================

	/**
	 * 获取最后一个RSI值（无参数版本）- 修复方法 用于单symbol场景，返回第一个可用的RSI值
	 */
	public Double getLastRSI() {
		// 1. 首先尝试单symbol的RSI值
		if (lastSingleRSI != null) {
			return lastSingleRSI;
		}

		// 2. 如果没有单symbol值，返回第一个symbol的RSI值
		if (!lastRSIMap.isEmpty()) {
			return lastRSIMap.values().iterator().next();
		}

		return null;
	}

	/**
	 * 获取指定symbol的最新RSI值
	 */
	public Double getLastRSI(String symbol) {
		return lastRSIMap.get(symbol);
	}

	/**
	 * 获取所有symbol的RSI值
	 */
	public Map<String, Double> getAllLastRSI() {
		return new HashMap<>(lastRSIMap);
	}

	// ==================== 其他方法 ====================

	@Override
	public void reset() {
		super.reset();
		priceHistoryMap.clear();
		lastRSIMap.clear();
		positionMap.clear();
		lastSingleRSI = null;
		System.out.println("RSI策略状态已重置");
	}

	// ==================== Getter和Setter方法 ====================

	public int getRsiPeriod() {
		return rsiPeriod;
	}

	public void setRsiPeriod(int rsiPeriod) {
		this.rsiPeriod = rsiPeriod;
		setParameter("rsiPeriod", rsiPeriod);
	}

	public double getOverbought() {
		return overbought;
	}

	public void setOverbought(double overbought) {
		this.overbought = overbought;
		setParameter("overbought", overbought);
	}

	public double getOversold() {
		return oversold;
	}

	public void setOversold(double oversold) {
		this.oversold = oversold;
		setParameter("oversold", oversold);
	}

	public double getPositionSizeRatio() {
		return positionSizeRatio;
	}

	public void setPositionSizeRatio(double positionSizeRatio) {
		this.positionSizeRatio = positionSizeRatio;
		setParameter("positionSizeRatio", positionSizeRatio);
	}

	/**
	 * 获取策略状态信息
	 */
	public Map<String, Object> getEnhancedStatus() {
		Map<String, Object> status = super.getStrategyStatus();
		status.put("rsiPeriod", rsiPeriod);
		status.put("overbought", overbought);
		status.put("oversold", oversold);
		status.put("positionSizeRatio", positionSizeRatio);
		status.put("signalThreshold", signalThreshold);
		status.put("useTrendFilter", useTrendFilter);
		status.put("trendThreshold", trendThreshold);
		status.put("symbolCount", priceHistoryMap.size());
		status.put("lastSingleRSI", lastSingleRSI);

		// 添加每个symbol的数据状态
		Map<String, Integer> symbolDataStatus = new HashMap<>();
		for (Map.Entry<String, List<Double>> entry : priceHistoryMap.entrySet()) {
			symbolDataStatus.put(entry.getKey(), entry.getValue().size());
		}
		status.put("symbolDataStatus", symbolDataStatus);

		return status;
	}

	public void setUseTrendFilter(boolean b) {
		// TODO Auto-generated method stub
		this.useTrendFilter = b;
	}

	public void setTrendThreshold(double d) {
		// TODO Auto-generated method stub
		this.trendThreshold = d;
	}
}