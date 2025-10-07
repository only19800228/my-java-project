package com.Quantitative.strategy.composite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 多时间框架组合策略 - 日线+小时线双确认 原理：大周期定方向，小周期找入场点，提高胜率
 */
public class MultiTimeframeComposite extends BaseStrategy {
	private EnhancedRSIStrategy dailyRsi;
	private MovingAverageStrategy dailyMA;
	private EnhancedRSIStrategy hourlyRsi;
	private MovingAverageStrategy hourlyMA;

	// 时间框架状态
	private Map<String, String> trendDirection; // 日线趋势方向

	public MultiTimeframeComposite() {
		super("多时间框架组合策略");
		initializeStrategies();
		setDefaultParameters();
	}

	private void initializeStrategies() {
		// 日线策略 (大周期)
		this.dailyRsi = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		this.dailyMA = new MovingAverageStrategy(20, 50, 0, 0);

		// 小时线策略 (小周期)
		this.hourlyRsi = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
		this.hourlyMA = new MovingAverageStrategy(10, 20, 0, 0);

		this.trendDirection = new HashMap<>();
	}

	private void setDefaultParameters() {
		setParameter("dailyWeight", 0.6);
		setParameter("hourlyWeight", 0.4);
		setParameter("minTrendConfidence", 0.7);
		setParameter("useTrendFilter", true);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0, "多时间框架组合策略初始化: 日线定方向+小时线找点位");

		// 初始化策略
		if (dataFeed != null) {
			dailyRsi.setDataFeed(dataFeed);
			dailyMA.setDataFeed(dataFeed);
			hourlyRsi.setDataFeed(dataFeed);
			hourlyMA.setDataFeed(dataFeed);
		}

		dailyRsi.initialize();
		dailyMA.initialize();
		hourlyRsi.initialize();
		hourlyMA.initialize();
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			String symbol = bar.getSymbol();

			// 更新趋势方向 (模拟多时间框架数据)
			updateTrendDirection(symbol, bar);

			// 获取当前趋势
			String trend = trendDirection.get(symbol);
			if (trend == null) {
				return;
			}

			// 获取小时线信号
			List<SignalEvent> hourlySignals = hourlyRsi.onBar(bar);
			if (hourlySignals.isEmpty()) {
				return;
			}

			// 检查信号与趋势的一致性
			SignalEvent hourlySignal = hourlySignals.get(0);
			boolean isAlignedWithTrend = isSignalAlignedWithTrend(hourlySignal, trend);

			if (isAlignedWithTrend) {
				// 趋势一致，增强信号
				SignalEvent enhancedSignal = enhanceSignalWithTrend(hourlySignal, trend);
				signals.add(enhancedSignal);

				TradingLogger.logSignal(getName(), symbol, enhancedSignal.getSignalType(), enhancedSignal.getStrength(),
						String.format("趋势=%s, 多时间框架确认", trend));
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 更新趋势方向 (模拟多时间框架分析)
	 */
	private void updateTrendDirection(String symbol, BarEvent bar) {
		// 模拟日线趋势判断
		List<SignalEvent> dailySignals = dailyMA.onBar(bar);
		if (!dailySignals.isEmpty()) {
			SignalEvent dailySignal = dailySignals.get(0);
			if ("BUY".equals(dailySignal.getSignalType())) {
				trendDirection.put(symbol, "UPTREND");
			} else if ("SELL".equals(dailySignal.getSignalType())) {
				trendDirection.put(symbol, "DOWNTREND");
			}
		}

		// 如果没有MA信号，使用RSI判断
		if (!trendDirection.containsKey(symbol)) {
			Double dailyRsiValue = dailyRsi.getLastRSI();
			if (dailyRsiValue != null) {
				if (dailyRsiValue > 50) {
					trendDirection.put(symbol, "UPTREND");
				} else {
					trendDirection.put(symbol, "DOWNTREND");
				}
			}
		}
	}

	/**
	 * 检查信号与趋势是否一致
	 */
	private boolean isSignalAlignedWithTrend(SignalEvent signal, String trend) {
		if ("UPTREND".equals(trend)) {
			return "BUY".equals(signal.getSignalType());
		} else if ("DOWNTREND".equals(trend)) {
			return "SELL".equals(signal.getSignalType());
		}
		return false;
	}

	/**
	 * 根据趋势增强信号
	 */
	private SignalEvent enhanceSignalWithTrend(SignalEvent originalSignal, String trend) {
		double enhancedStrength = originalSignal.getStrength() * 1.3; // 增强30%
		enhancedStrength = Math.min(enhancedStrength, 1.0);

		return new SignalEvent(originalSignal.getTimestamp(), originalSignal.getSymbol(),
				originalSignal.getSignalType(), enhancedStrength, "MultiTimeframeComposite", "多时间框架确认: " + trend);
	}

	@Override
	public void reset() {
		super.reset();
		if (dailyRsi != null)
			dailyRsi.reset();
		if (dailyMA != null)
			dailyMA.reset();
		if (hourlyRsi != null)
			hourlyRsi.reset();
		if (hourlyMA != null)
			hourlyMA.reset();
		trendDirection.clear();
	}
}