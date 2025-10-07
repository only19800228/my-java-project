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
 * MACD策略 - 动量指标
 */
public class MACDStrategy extends BaseStrategy {
	private int fastPeriod;
	private int slowPeriod;
	private int signalPeriod;
	private boolean useZeroCross;

	private Map<String, List<Double>> priceHistoryMap;
	private Map<String, Double> lastMACDMap;
	private Map<String, Double> lastSignalMap;

	public MACDStrategy() {
		super("MACD策略");
		this.fastPeriod = 12;
		this.slowPeriod = 26;
		this.signalPeriod = 9;
		this.useZeroCross = false;
		initializeParameters();
	}

	public MACDStrategy(int fastPeriod, int slowPeriod, int signalPeriod, boolean useZeroCross) {
		super("MACD策略");
		this.fastPeriod = fastPeriod;
		this.slowPeriod = slowPeriod;
		this.signalPeriod = signalPeriod;
		this.useZeroCross = useZeroCross;
		initializeParameters();
	}

	private void initializeParameters() {
		setParameter("fastPeriod", fastPeriod);
		setParameter("slowPeriod", slowPeriod);
		setParameter("signalPeriod", signalPeriod);
		setParameter("useZeroCross", useZeroCross);
	}

	@Override
	protected void init() {
		priceHistoryMap = new HashMap<>();
		lastMACDMap = new HashMap<>();
		lastSignalMap = new HashMap<>();

		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("MACD策略初始化: 快线=%d, 慢线=%d, 信号线=%d", fastPeriod, slowPeriod, signalPeriod));
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		String symbol = bar.getSymbol();
		double currentPrice = bar.getClose();

		updatePriceHistory(symbol, currentPrice);

		if (!hasEnoughData(symbol)) {
			return;
		}

		// 计算MACD
		MACDResult macdResult = calculateMACD(symbol);
		if (macdResult == null) {
			return;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(symbol);
		SignalEvent signal = generateSignal(bar, symbol, macdResult, hasPosition);

		if (signal != null) {
			signals.add(signal);
			TradingLogger.logSignal(getName(), symbol, signal.getSignalType(), signal.getStrength(),
					String.format("MACD=%.4f, Signal=%.4f, Histogram=%.4f", macdResult.macd, macdResult.signal,
							macdResult.histogram));
		}

		// 更新历史值
		lastMACDMap.put(symbol, macdResult.macd);
		lastSignalMap.put(symbol, macdResult.signal);
	}

	private SignalEvent generateSignal(BarEvent bar, String symbol, MACDResult macdResult, boolean hasPosition) {
		Double lastMACD = lastMACDMap.get(symbol);
		Double lastSignal = lastSignalMap.get(symbol);

		if (lastMACD == null || lastSignal == null) {
			return null;
		}

		boolean macdCrossAboveSignal = lastMACD <= lastSignal && macdResult.macd > macdResult.signal;
		boolean macdCrossBelowSignal = lastMACD >= lastSignal && macdResult.macd < macdResult.signal;
		boolean macdCrossAboveZero = lastMACD <= 0 && macdResult.macd > 0;
		boolean macdCrossBelowZero = lastMACD >= 0 && macdResult.macd < 0;

		if (!hasPosition) {
			// 买入信号
			if ((useZeroCross && macdCrossAboveZero) || (!useZeroCross && macdCrossAboveSignal)) {
				double strength = calculateMACDStrength(macdResult, true);
				return new SignalEvent(bar.getTimestamp(), symbol, "BUY", strength, getName());
			}
		} else {
			// 卖出信号
			if ((useZeroCross && macdCrossBelowZero) || (!useZeroCross && macdCrossBelowSignal)) {
				double strength = calculateMACDStrength(macdResult, false);
				return new SignalEvent(bar.getTimestamp(), symbol, "SELL", strength, getName());
			}
		}

		return null;
	}

	private double calculateMACDStrength(MACDResult macdResult, boolean isBuy) {
		double histogramStrength = Math.abs(macdResult.histogram) / (Math.abs(macdResult.macd) + 0.0001);
		double strength = Math.min(histogramStrength * 5, 1.0);
		return Math.max(strength, 0.3);
	}

	private MACDResult calculateMACD(String symbol) {
		List<Double> prices = getPriceHistory(symbol, slowPeriod + signalPeriod);
		if (prices.size() < slowPeriod + signalPeriod) {
			return null;
		}

		// 计算EMA
		double fastEMA = calculateEMA(prices, fastPeriod);
		double slowEMA = calculateEMA(prices, slowPeriod);
		double macd = fastEMA - slowEMA;

		// 计算信号线（MACD的EMA）
		List<Double> macdHistory = getMACDHistory(symbol);
		macdHistory.add(macd);
		double signal = calculateEMAForList(macdHistory, signalPeriod);

		double histogram = macd - signal;

		return new MACDResult(macd, signal, histogram);
	}

	private double calculateEMA(List<Double> prices, int period) {
		if (prices.isEmpty())
			return 0.0;

		double multiplier = 2.0 / (period + 1.0);
		double ema = prices.get(0);

		for (int i = 1; i < prices.size(); i++) {
			ema = (prices.get(i) - ema) * multiplier + ema;
		}

		return ema;
	}

	private double calculateEMAForList(List<Double> values, int period) {
		if (values.isEmpty())
			return 0.0;
		if (values.size() == 1)
			return values.get(0);

		double multiplier = 2.0 / (period + 1.0);
		double ema = values.get(0);

		for (int i = 1; i < values.size(); i++) {
			ema = (values.get(i) - ema) * multiplier + ema;
		}

		return ema;
	}

	private List<Double> getMACDHistory(String symbol) {
		// 简化实现，实际应该维护MACD历史
		List<Double> prices = getPriceHistory(symbol, slowPeriod + signalPeriod);
		List<Double> macdHistory = new ArrayList<>();

		for (int i = 0; i <= prices.size() - slowPeriod; i++) {
			List<Double> subPrices = prices.subList(i, i + slowPeriod);
			double fastEMA = calculateEMA(subPrices, fastPeriod);
			double slowEMA = calculateEMA(subPrices, slowPeriod);
			macdHistory.add(fastEMA - slowEMA);
		}

		return macdHistory;
	}

	// 工具方法
	private void updatePriceHistory(String symbol, double price) {
		List<Double> history = priceHistoryMap.get(symbol);
		if (history == null) {
			history = new ArrayList<>();
			priceHistoryMap.put(symbol, history);
		}
		history.add(price);

		int maxSize = slowPeriod + signalPeriod + 10;
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
		return prices != null && prices.size() >= slowPeriod + signalPeriod;
	}

	// MACD结果类
	private static class MACDResult {
		final double macd;
		final double signal;
		final double histogram;

		MACDResult(double macd, double signal, double histogram) {
			this.macd = macd;
			this.signal = signal;
			this.histogram = histogram;
		}
	}

	// Getter和Setter
	public int getFastPeriod() {
		return fastPeriod;
	}

	public void setFastPeriod(int fastPeriod) {
		this.fastPeriod = fastPeriod;
		setParameter("fastPeriod", fastPeriod);
	}

	public int getSlowPeriod() {
		return slowPeriod;
	}

	public void setSlowPeriod(int slowPeriod) {
		this.slowPeriod = slowPeriod;
		setParameter("slowPeriod", slowPeriod);
	}

	public int getSignalPeriod() {
		return signalPeriod;
	}

	public void setSignalPeriod(int signalPeriod) {
		this.signalPeriod = signalPeriod;
		setParameter("signalPeriod", signalPeriod);
	}
}