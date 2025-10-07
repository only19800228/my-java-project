package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * Aroon指标策略 - 趋势强度和方向检测
 */
public class AroonStrategy extends BaseStrategy {
	private int period;
	private double strengthThreshold;
	private double crossoverThreshold;

	private List<Double> highPrices = new ArrayList<>();
	private List<Double> lowPrices = new ArrayList<>();
	private Double lastAroonUp;
	private Double lastAroonDown;
	private Double lastAroonOscillator;

	public AroonStrategy() {
		super("Aroon指标策略");
		setDefaultParameters();
	}

	public AroonStrategy(int period, double strengthThreshold) {
		super("Aroon指标策略");
		this.period = period;
		this.strengthThreshold = strengthThreshold;
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		setParameter("period", period);
		setParameter("strengthThreshold", strengthThreshold);
		setParameter("crossoverThreshold", 0.0);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("Aroon策略初始化: 周期=%d, 强度阈值=%.1f", period, strengthThreshold));

		highPrices.clear();
		lowPrices.clear();
		lastAroonUp = null;
		lastAroonDown = null;
		lastAroonOscillator = null;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 更新价格数据
			updatePriceData(bar.getHigh(), bar.getLow());

			// 限制数据大小
			if (highPrices.size() > period * 2) {
				highPrices.remove(0);
				lowPrices.remove(0);
			}

			// 检查是否有足够数据
			if (!hasEnoughData()) {
				return;
			}

			// 计算Aroon指标
			AroonResult aroon = calculateAroon();
			if (aroon != null) {
				lastAroonUp = aroon.aroonUp;
				lastAroonDown = aroon.aroonDown;
				lastAroonOscillator = aroon.aroonOscillator;
			}

			// 生成交易信号
			SignalEvent signal = generateAroonSignal(bar, aroon);
			if (signal != null) {
				signals.add(signal);

				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(),
						String.format("AroonUp=%.1f, AroonDown=%.1f, Oscillator=%.1f", aroon.aroonUp, aroon.aroonDown,
								aroon.aroonOscillator));
			}

			if (debugMode && aroon != null) {
				TradingLogger.debug(getName(), "%s AroonUp=%.1f, AroonDown=%.1f", bar.getTimestamp().toLocalDate(),
						aroon.aroonUp, aroon.aroonDown);
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * Aroon计算结果
	 */
	public static class AroonResult {
		public final double aroonUp;
		public final double aroonDown;
		public final double aroonOscillator;

		public AroonResult(double aroonUp, double aroonDown) {
			this.aroonUp = aroonUp;
			this.aroonDown = aroonDown;
			this.aroonOscillator = aroonUp - aroonDown;
		}
	}

	/**
	 * 计算Aroon指标
	 */
	private AroonResult calculateAroon() {
		if (highPrices.size() < period) {
			return null;
		}

		try {
			// 计算最高价和最低价的位置
			int periodsSinceHighestHigh = findPeriodsSinceHighestHigh();
			int periodsSinceLowestLow = findPeriodsSinceLowestLow();

			// 计算Aroon Up和Aroon Down
			double aroonUp = ((period - periodsSinceHighestHigh) / (double) period) * 100;
			double aroonDown = ((period - periodsSinceLowestLow) / (double) period) * 100;

			return new AroonResult(aroonUp, aroonDown);

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateAroon", e);
			return null;
		}
	}

	/**
	 * 找到距离最高价有多少个周期
	 */
	private int findPeriodsSinceHighestHigh() {
		List<Double> recentHighs = highPrices.subList(highPrices.size() - period, highPrices.size());
		double highestHigh = recentHighs.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

		for (int i = recentHighs.size() - 1; i >= 0; i--) {
			if (recentHighs.get(i) == highestHigh) {
				return recentHighs.size() - 1 - i;
			}
		}
		return period - 1;
	}

	/**
	 * 找到距离最低价有多少个周期
	 */
	private int findPeriodsSinceLowestLow() {
		List<Double> recentLows = lowPrices.subList(lowPrices.size() - period, lowPrices.size());
		double lowestLow = recentLows.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

		for (int i = recentLows.size() - 1; i >= 0; i--) {
			if (recentLows.get(i) == lowestLow) {
				return recentLows.size() - 1 - i;
			}
		}
		return period - 1;
	}

	/**
	 * 生成Aroon交易信号
	 */
	private SignalEvent generateAroonSignal(BarEvent bar, AroonResult aroon) {
		if (aroon == null) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());

		// 强势上升趋势信号
		if (!hasPosition && aroon.aroonUp > strengthThreshold && aroon.aroonDown < 50) {
			double strength = calculateTrendStrength(aroon, true);
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "Aroon强势上升");
		}

		// 强势下降趋势信号
		if (hasPosition && aroon.aroonDown > strengthThreshold && aroon.aroonUp < 50) {
			double strength = calculateTrendStrength(aroon, false);
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", strength, "Aroon强势下降");
		}

		// 金叉信号（Aroon Up上穿Aroon Down）
		if (!hasPosition && isGoldenCross(aroon)) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.6, "Aroon金叉");
		}

		// 死叉信号（Aroon Down上穿Aroon Up）
		if (hasPosition && isDeathCross(aroon)) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.6, "Aroon死叉");
		}

		// 振荡器信号
		SignalEvent oscillatorSignal = generateOscillatorSignal(bar, aroon, hasPosition);
		if (oscillatorSignal != null) {
			return oscillatorSignal;
		}

		return null;
	}

	/**
	 * 检查金叉
	 */
	private boolean isGoldenCross(AroonResult currentAroon) {
		if (lastAroonUp == null || lastAroonDown == null) {
			return false;
		}
		return lastAroonUp <= lastAroonDown && currentAroon.aroonUp > currentAroon.aroonDown;
	}

	/**
	 * 检查死叉
	 */
	private boolean isDeathCross(AroonResult currentAroon) {
		if (lastAroonUp == null || lastAroonDown == null) {
			return false;
		}
		return lastAroonUp >= lastAroonDown && currentAroon.aroonUp < currentAroon.aroonDown;
	}

	/**
	 * 生成振荡器信号
	 */
	private SignalEvent generateOscillatorSignal(BarEvent bar, AroonResult aroon, boolean hasPosition) {
		// 振荡器从负转正
		if (!hasPosition && lastAroonOscillator != null && lastAroonOscillator < 0 && aroon.aroonOscillator > 0) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.5, "Aroon振荡器转正");
		}

		// 振荡器从正转负
		if (hasPosition && lastAroonOscillator != null && lastAroonOscillator > 0 && aroon.aroonOscillator < 0) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.5, "Aroon振荡器转负");
		}

		return null;
	}

	/**
	 * 计算趋势强度
	 */
	private double calculateTrendStrength(AroonResult aroon, boolean isUpTrend) {
		if (isUpTrend) {
			double upStrength = aroon.aroonUp / 100.0;
			double downWeakness = 1.0 - (aroon.aroonDown / 100.0);
			return Math.min((upStrength * 0.7 + downWeakness * 0.3), 1.0);
		} else {
			double downStrength = aroon.aroonDown / 100.0;
			double upWeakness = 1.0 - (aroon.aroonUp / 100.0);
			return Math.min((downStrength * 0.7 + upWeakness * 0.3), 1.0);
		}
	}

	private void updatePriceData(double high, double low) {
		highPrices.add(high);
		lowPrices.add(low);
	}

	private boolean hasEnoughData() {
		return highPrices.size() >= period;
	}

	@Override
	public void reset() {
		super.reset();
		highPrices.clear();
		lowPrices.clear();
		lastAroonUp = null;
		lastAroonDown = null;
		lastAroonOscillator = null;
	}

	// Getter和Setter方法
	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
		setParameter("period", period);
	}

	public double getStrengthThreshold() {
		return strengthThreshold;
	}

	public void setStrengthThreshold(double strengthThreshold) {
		this.strengthThreshold = strengthThreshold;
		setParameter("strengthThreshold", strengthThreshold);
	}

	public Double getLastAroonUp() {
		return lastAroonUp;
	}

	public Double getLastAroonDown() {
		return lastAroonDown;
	}

	public Double getLastAroonOscillator() {
		return lastAroonOscillator;
	}
}