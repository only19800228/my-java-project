package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * OBV能量潮策略 - 量价分析，检测资金流向
 */
public class OBVStrategy extends BaseStrategy {
	private int obvPeriod;
	private double volumeThreshold;
	private boolean usePriceConfirmation;
	private int maPeriod;

	private List<Double> obvValues = new ArrayList<>();
	private List<Double> closePrices = new ArrayList<>();
	private List<Long> volumes = new ArrayList<>();
	private Double lastOBV;
	private Double lastOBVMA;
	private Double lastClose;
	private Long lastVolume;

	public OBVStrategy() {
		super("OBV能量潮策略");
		setDefaultParameters();
	}

	public OBVStrategy(int maPeriod, double volumeThreshold) {
		super("OBV能量潮策略");
		this.maPeriod = maPeriod;
		this.volumeThreshold = volumeThreshold;
		this.obvPeriod = 20; // 默认OBV观察周期
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		setParameter("obvPeriod", obvPeriod);
		setParameter("volumeThreshold", volumeThreshold);
		setParameter("usePriceConfirmation", true);
		setParameter("maPeriod", maPeriod);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("OBV策略初始化: MA周期=%d, 成交量阈值=%.1f", maPeriod, volumeThreshold));

		obvValues.clear();
		closePrices.clear();
		volumes.clear();
		lastOBV = null;
		lastOBVMA = null;
		lastClose = null;
		lastVolume = null;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 更新数据
			updateData(bar.getClose(), bar.getVolume());

			// 计算OBV
			double currentOBV = calculateOBV(bar);
			obvValues.add(currentOBV);

			// 限制数据大小
			if (obvValues.size() > obvPeriod * 3) {
				obvValues.remove(0);
				closePrices.remove(0);
				volumes.remove(0);
			}

			lastOBV = currentOBV;
			lastClose = bar.getClose();
			lastVolume = bar.getVolume();

			// 计算OBV移动平均
			if (obvValues.size() >= maPeriod) {
				lastOBVMA = calculateOBVMA();
			}

			// 生成交易信号
			SignalEvent signal = generateOBVSignal(bar);
			if (signal != null) {
				signals.add(signal);

				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(),
						String.format("OBV=%.0f", currentOBV));
			}

			if (debugMode) {
				TradingLogger.debug(getName(), "%s OBV=%.0f, 价格=%.2f, 成交量=%,d", bar.getTimestamp().toLocalDate(),
						currentOBV, bar.getClose(), bar.getVolume());
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 计算OBV
	 */
	private double calculateOBV(BarEvent bar) {
		if (lastClose == null) {
			return bar.getVolume(); // 第一天的OBV等于当日成交量
		}

		double currentOBV = lastOBV != null ? lastOBV : 0;

		if (bar.getClose() > lastClose) {
			// 价格上涨，OBV增加
			currentOBV += bar.getVolume();
		} else if (bar.getClose() < lastClose) {
			// 价格下跌，OBV减少
			currentOBV -= bar.getVolume();
		}
		// 价格持平，OBV不变

		return currentOBV;
	}

	/**
	 * 计算OBV移动平均
	 */
	private double calculateOBVMA() {
		if (obvValues.size() < maPeriod) {
			return 0.0;
		}

		double sum = 0.0;
		for (int i = obvValues.size() - maPeriod; i < obvValues.size(); i++) {
			sum += obvValues.get(i);
		}

		return sum / maPeriod;
	}

	/**
	 * 生成OBV交易信号
	 */
	private SignalEvent generateOBVSignal(BarEvent bar) {
		if (lastOBV == null || lastOBVMA == null) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());

		// OBV突破信号
		SignalEvent breakoutSignal = generateBreakoutSignal(bar, hasPosition);
		if (breakoutSignal != null) {
			return breakoutSignal;
		}

		// 量价背离信号
		SignalEvent divergenceSignal = generateDivergenceSignal(bar, hasPosition);
		if (divergenceSignal != null) {
			return divergenceSignal;
		}

		// OBV趋势信号
		SignalEvent trendSignal = generateTrendSignal(bar, hasPosition);
		if (trendSignal != null) {
			return trendSignal;
		}

		return null;
	}

	/**
	 * 生成突破信号
	 */
	private SignalEvent generateBreakoutSignal(BarEvent bar, boolean hasPosition) {
		// OBV上穿均线且放量
		if (!hasPosition && lastOBV > lastOBVMA && isVolumeSurge(bar)) {
			if (!usePriceConfirmation || bar.getClose() > getRecentPriceMA()) {
				double strength = calculateBreakoutStrength(bar);
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "OBV突破");
			}
		}

		// OBV下穿均线且放量
		if (hasPosition && lastOBV < lastOBVMA && isVolumeSurge(bar)) {
			double strength = calculateBreakoutStrength(bar);
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", strength, "OBV下破");
		}

		return null;
	}

	/**
	 * 生成背离信号
	 */
	private SignalEvent generateDivergenceSignal(BarEvent bar, boolean hasPosition) {
		// 需要更多历史数据检测背离
		boolean bullishDivergence = checkBullishDivergence();
		boolean bearishDivergence = checkBearishDivergence();

		if (!hasPosition && bullishDivergence) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.7, "OBV底背离");
		}

		if (hasPosition && bearishDivergence) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.7, "OBV顶背离");
		}

		return null;
	}

	/**
	 * 生成趋势信号
	 */
	private SignalEvent generateTrendSignal(BarEvent bar, boolean hasPosition) {
		// OBV持续上升趋势
		if (!hasPosition && isOBVUptrend() && isPriceUptrend()) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.5, "OBV上升趋势");
		}

		// OBV持续下降趋势
		if (hasPosition && isOBVDowntrend() && isPriceDowntrend()) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.5, "OBV下降趋势");
		}

		return null;
	}

	/**
	 * 检查成交量激增
	 */
	private boolean isVolumeSurge(BarEvent bar) {
		if (volumes.size() < 10)
			return true;

		double avgVolume = volumes.stream().mapToLong(Long::longValue).average().orElse(bar.getVolume());

		return bar.getVolume() > avgVolume * volumeThreshold;
	}

	/**
	 * 计算突破强度
	 */
	private double calculateBreakoutStrength(BarEvent bar) {
		double obvStrength = Math.min(Math.abs(lastOBV - lastOBVMA) / Math.abs(lastOBVMA) * 2, 1.0);
		double volumeStrength = isVolumeSurge(bar) ? 0.3 : 0.1;

		return Math.min(obvStrength + volumeStrength, 1.0);
	}

	/**
	 * 检查看涨背离（价格新低但OBV没有新低）
	 */
	private boolean checkBullishDivergence() {
		// 需要价格和OBV的历史极值点
		// 简化实现
		return false;
	}

	/**
	 * 检查看跌背离（价格新高但OBV没有新高）
	 */
	private boolean checkBearishDivergence() {
		// 需要价格和OBV的历史极值点
		// 简化实现
		return false;
	}

	/**
	 * 检查OBV上升趋势
	 */
	private boolean isOBVUptrend() {
		if (obvValues.size() < 5)
			return false;

		// 检查最近5个OBV值是否递增
		for (int i = obvValues.size() - 5; i < obvValues.size() - 1; i++) {
			if (obvValues.get(i) >= obvValues.get(i + 1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查OBV下降趋势
	 */
	private boolean isOBVDowntrend() {
		if (obvValues.size() < 5)
			return false;

		// 检查最近5个OBV值是否递减
		for (int i = obvValues.size() - 5; i < obvValues.size() - 1; i++) {
			if (obvValues.get(i) <= obvValues.get(i + 1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查价格上升趋势
	 */
	private boolean isPriceUptrend() {
		// 简化实现
		return lastClose != null && closePrices.size() >= 2 && lastClose > closePrices.get(closePrices.size() - 2);
	}

	/**
	 * 检查价格下降趋势
	 */
	private boolean isPriceDowntrend() {
		// 简化实现
		return lastClose != null && closePrices.size() >= 2 && lastClose < closePrices.get(closePrices.size() - 2);
	}

	/**
	 * 获取近期价格均线
	 */
	private double getRecentPriceMA() {
		if (closePrices.size() < 5)
			return lastClose != null ? lastClose : 0;

		double sum = 0.0;
		int count = Math.min(5, closePrices.size());
		for (int i = closePrices.size() - count; i < closePrices.size(); i++) {
			sum += closePrices.get(i);
		}
		return sum / count;
	}

	private void updateData(double close, long volume) {
		closePrices.add(close);
		volumes.add(volume);
	}

	@Override
	public void reset() {
		super.reset();
		obvValues.clear();
		closePrices.clear();
		volumes.clear();
		lastOBV = null;
		lastOBVMA = null;
		lastClose = null;
		lastVolume = null;
	}

	// Getter和Setter方法
	public int getObvPeriod() {
		return obvPeriod;
	}

	public void setObvPeriod(int obvPeriod) {
		this.obvPeriod = obvPeriod;
		setParameter("obvPeriod", obvPeriod);
	}

	public double getVolumeThreshold() {
		return volumeThreshold;
	}

	public void setVolumeThreshold(double volumeThreshold) {
		this.volumeThreshold = volumeThreshold;
		setParameter("volumeThreshold", volumeThreshold);
	}

	public Double getLastOBV() {
		return lastOBV;
	}

	public Double getLastOBVMA() {
		return lastOBVMA;
	}
}