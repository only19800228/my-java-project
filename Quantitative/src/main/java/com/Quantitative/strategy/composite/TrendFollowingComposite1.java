package com.Quantitative.strategy.composite;

import java.util.List;
import java.util.Map;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.ADXStrategy;
import com.Quantitative.strategy.indicators.MACDStrategy;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 趋势跟踪组合策略 - 多时间框架趋势确认 原理：结合MACD、均线和ADX，只在强趋势行情中交易
 */
public class TrendFollowingComposite1 extends BaseStrategy {
	private MACDStrategy macdStrategy;
	private MovingAverageStrategy maStrategy;
	private ADXStrategy adxStrategy;

	// 策略权重
	private double macdWeight = 0.4;
	private double maWeight = 0.4;
	private double adxWeight = 0.2;

	// 状态跟踪
	private Map<String, Double> lastMacdHistogram;
	private Map<String, Boolean> trendConfirmed;

	public TrendFollowingComposite1() {
		super("趋势跟踪组合策略");
		initializeStrategies();
		setDefaultParameters();
	}

	private void initializeStrategies() {
		// MACD: 快速反应趋势变化
		this.macdStrategy = new MACDStrategy(12, 26, 9, true);

		// 双均线: 确认趋势方向
		this.maStrategy = new MovingAverageStrategy(10, 30, 5, 1.2);

		// ADX: 衡量趋势强度
		this.adxStrategy = new ADXStrategy(14, 25.0, 25.0, 25.0);
	}

	private void setDefaultParameters() {
		setParameter("macdWeight", macdWeight);
		setParameter("maWeight", maWeight);
		setParameter("adxWeight", adxWeight);
		setParameter("minTrendStrength", 25.0);
		setParameter("useMultiTimeframe", true);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0, "趋势跟踪组合策略初始化: MACD+均线+ADX多维度趋势确认");

		// 初始化子策略
		if (dataFeed != null) {
			macdStrategy.setDataFeed(dataFeed);
			maStrategy.setDataFeed(dataFeed);
			adxStrategy.setDataFeed(dataFeed);
		}

		if (portfolio != null) {
			macdStrategy.setPortfolio(portfolio);
			maStrategy.setPortfolio(portfolio);
			adxStrategy.setPortfolio(portfolio);
		}

		macdStrategy.initialize();
		maStrategy.initialize();
		adxStrategy.initialize();
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			String symbol = bar.getSymbol();

			// 获取各策略信号
			List<SignalEvent> macdSignals = macdStrategy.onBar(bar);
			List<SignalEvent> maSignals = maStrategy.onBar(bar);
			List<SignalEvent> adxSignals = adxStrategy.onBar(bar);

			// 计算综合信号强度
			double compositeScore = calculateCompositeScore(bar, symbol, macdSignals, maSignals, adxSignals);

			// 生成最终信号
			SignalEvent finalSignal = generateFinalSignal(bar, symbol, compositeScore);
			if (finalSignal != null) {
				signals.add(finalSignal);

				TradingLogger.logSignal(getName(), symbol, finalSignal.getSignalType(), finalSignal.getStrength(),
						String.format("综合评分=%.2f, MACD=%d, 均线=%d, ADX=%d", compositeScore, macdSignals.size(),
								maSignals.size(), adxSignals.size()));
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 计算综合评分 (0-1)
	 */
	private double calculateCompositeScore(BarEvent bar, String symbol, List<SignalEvent> macdSignals,
			List<SignalEvent> maSignals, List<SignalEvent> adxSignals) {
		double score = 0.0;
		int signalCount = 0;

		// MACD信号评分
		if (!macdSignals.isEmpty()) {
			double macdScore = macdSignals.stream().mapToDouble(SignalEvent::getStrength).average().orElse(0.0);
			score += macdScore * macdWeight;
			signalCount++;
		}

		// 均线信号评分
		if (!maSignals.isEmpty()) {
			double maScore = maSignals.stream().mapToDouble(SignalEvent::getStrength).average().orElse(0.0);
			score += maScore * maWeight;
			signalCount++;
		}

		// ADX趋势强度评分
		Double adxValue = adxStrategy.getLastADX();
		if (adxValue != null && adxValue > 25.0) {
			double adxScore = Math.min(adxValue / 100.0, 1.0);
			score += adxScore * adxWeight;
			signalCount++;
		}

		// 信号一致性加分
		if (signalCount >= 2) {
			score *= 1.2; // 多个信号确认，增加权重
		}

		return Math.min(score, 1.0);
	}

	private SignalEvent generateFinalSignal(BarEvent bar, String symbol, double compositeScore) {
		if (compositeScore < 0.6) {
			return null; // 信号强度不足
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(symbol);

		// 根据评分生成信号
		if (!hasPosition && compositeScore >= 0.7) {
			return new SignalEvent(bar.getTimestamp(), symbol, "BUY", compositeScore, "趋势跟踪买入");
		} else if (hasPosition && compositeScore < 0.4) {
			return new SignalEvent(bar.getTimestamp(), symbol, "SELL", 1.0 - compositeScore, "趋势减弱卖出");
		}

		return null;
	}

	@Override
	public void reset() {
		super.reset();
		if (macdStrategy != null)
			macdStrategy.reset();
		if (maStrategy != null)
			maStrategy.reset();
		if (adxStrategy != null)
			adxStrategy.reset();
	}
}