package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 布林带策略 - 均值回归 + 突破
 */
public class BollingerBandsStrategy extends BaseStrategy {
	private int period;
	private double numStdDev;
	private boolean useSqueeze;
	private double squeezeThreshold;

	private Map<String, List<Double>> priceHistoryMap;
	private Map<String, List<Double>> bandwidthHistoryMap;

	public BollingerBandsStrategy() {
		super("布林带策略");
		this.period = 20;
		this.numStdDev = 2.0;
		this.useSqueeze = true;
		this.squeezeThreshold = 0.1;
		initializeParameters();
	}

	public BollingerBandsStrategy(int period, double numStdDev, boolean useSqueeze, double squeezeThreshold) {
		super("布林带策略");
		this.period = period;
		this.numStdDev = numStdDev;
		this.useSqueeze = useSqueeze;
		this.squeezeThreshold = squeezeThreshold;
		initializeParameters();
	}

	private void initializeParameters() {
		setParameter("period", period);
		setParameter("numStdDev", numStdDev);
		setParameter("useSqueeze", useSqueeze);
		setParameter("squeezeThreshold", squeezeThreshold);
	}

	@Override
	protected void init() {
		priceHistoryMap = new HashMap<>();
		bandwidthHistoryMap = new HashMap<>();

		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("布林带策略初始化: 周期=%d, 标准差=%.1f, 收缩检测=%s", period, numStdDev, useSqueeze));
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		String symbol = bar.getSymbol();
		double currentPrice = bar.getClose();

		updatePriceHistory(symbol, currentPrice);

		if (!hasEnoughData(symbol)) {
			return;
		}

		// 计算布林带
		BollingerBandsResult bbResult = calculateBollingerBands(symbol);
		if (bbResult == null) {
			return;
		}

		// 检测布林带收缩
		boolean isSqueeze = useSqueeze ? detectSqueeze(symbol, bbResult.bandwidth) : false;

		boolean hasPosition = portfolio != null && portfolio.hasPosition(symbol);
		SignalEvent signal = generateSignal(bar, symbol, bbResult, isSqueeze, hasPosition);

		if (signal != null) {
			signals.add(signal);
			TradingLogger.logSignal(getName(), symbol, signal.getSignalType(), signal.getStrength(),
					String.format("中轨=%.2f, 上轨=%.2f, 下轨=%.2f, 带宽=%.4f%s", bbResult.middle, bbResult.upper,
							bbResult.lower, bbResult.bandwidth, isSqueeze ? " [收缩]" : ""));
		}
	}

	private SignalEvent generateSignal(BarEvent bar, String symbol, BollingerBandsResult bbResult, boolean isSqueeze,
			boolean hasPosition) {
		double currentPrice = bar.getClose();

		if (!hasPosition) {
			// 买入信号：价格触及下轨（均值回归）或突破上轨（突破）
			if (currentPrice <= bbResult.lower && !isSqueeze) {
				// 均值回归买入
				double strength = calculateRegressionStrength(bbResult, currentPrice);
				return new SignalEvent(bar.getTimestamp(), symbol, "BUY", strength, getName());
			} else if (currentPrice >= bbResult.upper && isSqueeze) {
				// 突破买入
				double strength = calculateBreakoutStrength(bbResult, currentPrice);
				return new SignalEvent(bar.getTimestamp(), symbol, "BUY", strength, getName());
			}
		} else {
			// 卖出信号：价格触及中轨或上轨
			if (currentPrice >= bbResult.middle) {
				double strength = calculateExitStrength(bbResult, currentPrice);
				return new SignalEvent(bar.getTimestamp(), symbol, "SELL", strength, getName());
			}
		}

		return null;
	}

	private double calculateRegressionStrength(BollingerBandsResult bbResult, double currentPrice) {
		double distanceFromLower = (bbResult.middle - currentPrice) / (bbResult.middle - bbResult.lower);
		return Math.min(distanceFromLower * 2, 1.0);
	}

	private double calculateBreakoutStrength(BollingerBandsResult bbResult, double currentPrice) {
		double distanceFromUpper = (currentPrice - bbResult.upper) / bbResult.upper;
		return Math.min(distanceFromUpper * 10, 1.0);
	}

	private double calculateExitStrength(BollingerBandsResult bbResult, double currentPrice) {
		double distanceFromMiddle = Math.abs(currentPrice - bbResult.middle) / bbResult.middle;
		return Math.min(distanceFromMiddle * 5, 1.0);
	}

	private BollingerBandsResult calculateBollingerBands(String symbol) {
		List<Double> prices = getPriceHistory(symbol, period);
		if (prices.size() < period) {
			return null;
		}

		// 计算中轨（移动平均）
		double middle = calculateSMA(prices);

		// 计算标准差
		double stdDev = calculateStandardDeviation(prices, middle);

		// 计算上下轨
		double upper = middle + (stdDev * numStdDev);
		double lower = middle - (stdDev * numStdDev);

		// 计算带宽
		double bandwidth = (upper - lower) / middle;

		return new BollingerBandsResult(upper, middle, lower, bandwidth);
	}

	private double calculateSMA(List<Double> prices) {
		double sum = 0.0;
		for (double price : prices) {
			sum += price;
		}
		return sum / prices.size();
	}

	private double calculateStandardDeviation(List<Double> prices, double mean) {
		double sum = 0.0;
		for (double price : prices) {
			sum += Math.pow(price - mean, 2);
		}
		return Math.sqrt(sum / prices.size());
	}

	private boolean detectSqueeze(String symbol, double currentBandwidth) {
		List<Double> bandwidthHistory = bandwidthHistoryMap.get(symbol);
		if (bandwidthHistory == null) {
			bandwidthHistory = new ArrayList<>();
			bandwidthHistoryMap.put(symbol, bandwidthHistory);
		}

		bandwidthHistory.add(currentBandwidth);

		// 保持历史数据大小
		if (bandwidthHistory.size() > 100) {
			bandwidthHistory = bandwidthHistory.subList(bandwidthHistory.size() - 100, bandwidthHistory.size());
			bandwidthHistoryMap.put(symbol, bandwidthHistory);
		}

		if (bandwidthHistory.size() < 20) {
			return false;
		}

		// 查找最近20个周期内的最小带宽
		double minBandwidth = Double.MAX_VALUE;
		for (int i = bandwidthHistory.size() - 20; i < bandwidthHistory.size(); i++) {
			if (bandwidthHistory.get(i) < minBandwidth) {
				minBandwidth = bandwidthHistory.get(i);
			}
		}

		// 如果当前带宽接近最小值，认为是收缩
		return currentBandwidth <= minBandwidth * (1 + squeezeThreshold);
	}

	// 工具方法
	private void updatePriceHistory(String symbol, double price) {
		List<Double> history = priceHistoryMap.get(symbol);
		if (history == null) {
			history = new ArrayList<>();
			priceHistoryMap.put(symbol, history);
		}
		history.add(price);

		int maxSize = period + 10;
		if (history.size() > maxSize) {
			history = history.subList(history.size() - maxSize, history.size());
			priceHistoryMap.put(symbol, history);
		}
	}

	private List<Double> getPriceHistory(String symbol, int count) {
		List<Double> history = priceHistoryMap.get(symbol);
		if (history == null || history.size() < count) {
			return new ArrayList<>();
		}
		return new ArrayList<>(history.subList(history.size() - count, history.size()));
	}

	private boolean hasEnoughData(String symbol) {
		List<Double> prices = priceHistoryMap.get(symbol);
		return prices != null && prices.size() >= period;
	}

	// 布林带结果类
	private static class BollingerBandsResult {
		final double upper;
		final double middle;
		final double lower;
		final double bandwidth;

		BollingerBandsResult(double upper, double middle, double lower, double bandwidth) {
			this.upper = upper;
			this.middle = middle;
			this.lower = lower;
			this.bandwidth = bandwidth;
		}
	}

	// Getter和Setter
	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
		setParameter("period", period);
	}

	public double getNumStdDev() {
		return numStdDev;
	}

	public void setNumStdDev(double numStdDev) {
		this.numStdDev = numStdDev;
		setParameter("numStdDev", numStdDev);
	}
}