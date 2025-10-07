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
 * 双均线策略 - 趋势跟踪
 */
public class MovingAverageStrategy extends BaseStrategy {
	private int fastPeriod;
	private int slowPeriod;
	private int volumePeriod;
	private double volumeThreshold;

	private Map<String, List<Double>> priceHistoryMap = new HashMap<>(); // 直接初始化
	private Map<String, List<Long>> volumeHistoryMap = new HashMap<>(); // 直接初始化

	public MovingAverageStrategy() {
		super("双均线策略");
		this.fastPeriod = 10;
		this.slowPeriod = 30;
		this.volumePeriod = 5;
		this.volumeThreshold = 1.0;
		initializeParameters();
	}

	public MovingAverageStrategy(int fastPeriod, int slowPeriod, int volumePeriod, double volumeThreshold) {
		super("双均线策略");
		this.fastPeriod = fastPeriod;
		this.slowPeriod = slowPeriod;
		this.volumePeriod = volumePeriod;
		this.volumeThreshold = volumeThreshold;
		initializeParameters();
	}

	private void initializeParameters() {
		setParameter("fastPeriod", fastPeriod);
		setParameter("slowPeriod", slowPeriod);
		setParameter("volumePeriod", volumePeriod);
		setParameter("volumeThreshold", volumeThreshold);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("双均线策略初始化: 快线=%d, 慢线=%d, 成交量周期=%d", fastPeriod, slowPeriod, volumePeriod));
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		String symbol = bar.getSymbol();
		double currentPrice = bar.getClose();
		long currentVolume = bar.getVolume();

		// 更新价格和成交量历史
		updatePriceHistory(symbol, currentPrice);
		updateVolumeHistory(symbol, currentVolume);

		// 检查数据是否足够
		if (!hasEnoughData(symbol)) {
			return;
		}

		// 计算技术指标
		Double fastMA = calculateMA(symbol, fastPeriod);
		Double slowMA = calculateMA(symbol, slowPeriod);
		Double volumeMA = calculateVolumeMA(symbol, volumePeriod);
		boolean volumeConfirmed = isVolumeConfirmed(currentVolume, volumeMA);

		if (fastMA == null || slowMA == null || volumeMA == null) {
			return;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(symbol);

		// 生成交易信号
		SignalEvent signal = generateSignal(bar, symbol, fastMA, slowMA, volumeConfirmed, hasPosition);
		if (signal != null) {
			signals.add(signal);
			TradingLogger.logSignal(getName(), symbol, signal.getSignalType(), signal.getStrength(),
					String.format("快线=%.2f, 慢线=%.2f", fastMA, slowMA));
		}
	}

	private SignalEvent generateSignal(BarEvent bar, String symbol, double fastMA, double slowMA,
			boolean volumeConfirmed, boolean hasPosition) {
		// 添加调试日志
		TradingLogger.debug(getName(), symbol,
				String.format("快线=%.2f, 慢线=%.2f, 成交量确认=%s, 已有仓位=%s", fastMA, slowMA, volumeConfirmed, hasPosition));
		// 金叉：快线上穿慢线
		boolean goldenCross = isGoldenCross(symbol, fastMA, slowMA);
		// 死叉：快线下穿慢线
		boolean deathCross = isDeathCross(symbol, fastMA, slowMA);

		if (!hasPosition && goldenCross && volumeConfirmed) {
			// 买入信号：金叉且成交量确认
			double strength = calculateSignalStrength(fastMA, slowMA, bar.getClose());
			return new SignalEvent(bar.getTimestamp(), symbol, "BUY", strength, getName());
		} else if (hasPosition && deathCross) {
			// 卖出信号：死叉
			double strength = calculateSignalStrength(fastMA, slowMA, bar.getClose());
			return new SignalEvent(bar.getTimestamp(), symbol, "SELL", strength, getName());
		}

		return null;
	}

	private boolean isGoldenCross(String symbol, double fastMA, double slowMA) {
		List<Double> fastHistory = getPriceHistory(symbol, fastPeriod + 1);
		List<Double> slowHistory = getPriceHistory(symbol, slowPeriod + 1);

		if (fastHistory.size() < 2 || slowHistory.size() < 2) {
			return false;
		}

		double prevFastMA = calculateMA(fastHistory.subList(0, fastHistory.size() - 1), fastPeriod);
		double prevSlowMA = calculateMA(slowHistory.subList(0, slowHistory.size() - 1), slowPeriod);

		return prevFastMA <= prevSlowMA && fastMA > slowMA;
	}

	private boolean isDeathCross(String symbol, double fastMA, double slowMA) {
		List<Double> fastHistory = getPriceHistory(symbol, fastPeriod + 1);
		List<Double> slowHistory = getPriceHistory(symbol, slowPeriod + 1);

		if (fastHistory.size() < 2 || slowHistory.size() < 2) {
			return false;
		}

		double prevFastMA = calculateMA(fastHistory.subList(0, fastHistory.size() - 1), fastPeriod);
		double prevSlowMA = calculateMA(slowHistory.subList(0, slowHistory.size() - 1), slowPeriod);

		return prevFastMA >= prevSlowMA && fastMA < slowMA;
	}

	private boolean isVolumeConfirmed(long currentVolume, Double volumeMA) {
		return volumeMA != null && currentVolume > volumeMA * volumeThreshold;
	}

	private double calculateSignalStrength(double fastMA, double slowMA, double currentPrice) {
		double maDiff = Math.abs(fastMA - slowMA) / slowMA;
		double priceToMA = Math.abs(currentPrice - fastMA) / fastMA;

		double strength = Math.min(maDiff * 10 + priceToMA * 5, 1.0);
		return Math.max(strength, 0.3); // 最小强度0.3
	}

	// 工具方法
	private void updatePriceHistory(String symbol, double price) {
		List<Double> history = priceHistoryMap.get(symbol);
		if (history == null) {
			history = new ArrayList<>();
			priceHistoryMap.put(symbol, history);
		}
		history.add(price);

		// 限制历史数据大小
		int maxSize = Math.max(fastPeriod, slowPeriod) + 10;
		if (history.size() > maxSize) {
			history = history.subList(history.size() - maxSize, history.size());
			priceHistoryMap.put(symbol, history);
		}
	}

	private void updateVolumeHistory(String symbol, long volume) {
		List<Long> history = volumeHistoryMap.get(symbol);
		if (history == null) {
			history = new ArrayList<>();
			volumeHistoryMap.put(symbol, history);
		}
		history.add(volume);

		// 限制历史数据大小
		int maxSize = volumePeriod + 10;
		if (history.size() > maxSize) {
			history = history.subList(history.size() - maxSize, history.size());
			volumeHistoryMap.put(symbol, history);
		}
	}

	private Double calculateMA(String symbol, int period) {
		List<Double> prices = getPriceHistory(symbol, period);
		return calculateMA(prices, period);
	}

	private Double calculateMA(List<Double> prices, int period) {
		if (prices.size() < period) {
			return null;
		}

		double sum = 0.0;
		for (int i = prices.size() - period; i < prices.size(); i++) {
			sum += prices.get(i);
		}
		return sum / period;
	}

	private Double calculateVolumeMA(String symbol, int period) {
		List<Long> volumes = volumeHistoryMap.get(symbol);
		if (volumes == null || volumes.size() < period) {
			return null;
		}

		long sum = 0;
		for (int i = volumes.size() - period; i < volumes.size(); i++) {
			sum += volumes.get(i);
		}
		return (double) sum / period;
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
		List<Long> volumes = volumeHistoryMap.get(symbol);

		return prices != null && prices.size() >= Math.max(fastPeriod, slowPeriod) && volumes != null
				&& volumes.size() >= volumePeriod;
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
}