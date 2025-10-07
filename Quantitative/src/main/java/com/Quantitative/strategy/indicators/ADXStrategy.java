package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * ADX平均趋向指数策略 - 趋势强度测量
 * 
 * ADX测量趋势强度而不指示方向 +DI测量上升趋势强度，-DI测量下降趋势强度 高ADX值表示强趋势，低ADX值表示弱趋势或盘整
 */
public class ADXStrategy extends BaseStrategy {
	// ADX参数
	private int adxPeriod;
	private double adxThreshold;
	private double diPlusThreshold;
	private double diMinusThreshold;

	// 状态跟踪
	private List<Double> highPrices = new ArrayList<>();
	private List<Double> lowPrices = new ArrayList<>();
	private List<Double> closePrices = new ArrayList<>();
	private List<Double> adxValues = new ArrayList<>();
	private List<Double> diPlusValues = new ArrayList<>();
	private List<Double> diMinusValues = new ArrayList<>();

	private Double lastADX;
	private Double lastDIPlus;
	private Double lastDIMinus;

	// 信号配置
	private boolean useDICross;
	private boolean useTrendStrength;
	private double weakTrendThreshold;
	private double strongTrendThreshold;

	public ADXStrategy() {
		super("ADX趋势强度策略");
		setDefaultParameters();
	}

	public ADXStrategy(int period, double adxThreshold, double diPlusThreshold, double diMinusThreshold) {
		super("ADX趋势强度策略");
		this.adxPeriod = period;
		this.adxThreshold = adxThreshold;
		this.diPlusThreshold = diPlusThreshold;
		this.diMinusThreshold = diMinusThreshold;
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		// 默认参数
		this.adxPeriod = 14;
		this.adxThreshold = 25.0; // ADX > 25表示趋势明显
		this.diPlusThreshold = 20.0;
		this.diMinusThreshold = 20.0;
		this.useDICross = true;
		this.useTrendStrength = true;
		this.weakTrendThreshold = 20.0;
		this.strongTrendThreshold = 40.0;

		setParameter("adxPeriod", adxPeriod);
		setParameter("adxThreshold", adxThreshold);
		setParameter("diPlusThreshold", diPlusThreshold);
		setParameter("diMinusThreshold", diMinusThreshold);
		setParameter("useDICross", useDICross);
		setParameter("useTrendStrength", useTrendStrength);
		setParameter("weakTrendThreshold", weakTrendThreshold);
		setParameter("strongTrendThreshold", strongTrendThreshold);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("ADX策略初始化: 周期=%d, ADX阈值=%.1f, +DI阈值=%.1f, -DI阈值=%.1f", adxPeriod, adxThreshold,
						diPlusThreshold, diMinusThreshold));

		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		adxValues.clear();
		diPlusValues.clear();
		diMinusValues.clear();
		lastADX = null;
		lastDIPlus = null;
		lastDIMinus = null;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 更新价格数据
			updatePriceData(bar.getHigh(), bar.getLow(), bar.getClose());

			// 检查是否有足够数据
			if (!hasEnoughData()) {
				return;
			}

			// 计算ADX指标
			ADXResult adxResult = calculateADX();
			if (adxResult == null) {
				return;
			}

			// 更新历史值
			adxValues.add(adxResult.adx);
			diPlusValues.add(adxResult.diPlus);
			diMinusValues.add(adxResult.diMinus);

			lastADX = adxResult.adx;
			lastDIPlus = adxResult.diPlus;
			lastDIMinus = adxResult.diMinus;

			// 限制数据大小
			if (adxValues.size() > adxPeriod * 3) {
				adxValues.remove(0);
				diPlusValues.remove(0);
				diMinusValues.remove(0);
			}

			// 生成交易信号
			SignalEvent signal = generateADXSignal(bar, adxResult);
			if (signal != null) {
				signals.add(signal);
				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(), String
						.format("ADX=%.1f, +DI=%.1f, -DI=%.1f", adxResult.adx, adxResult.diPlus, adxResult.diMinus));
			}

			if (debugMode) {
				TradingLogger.debug(getName(), "%s ADX=%.1f, +DI=%.1f, -DI=%.1f, 趋势强度=%.1f",
						bar.getTimestamp().toLocalDate(), adxResult.adx, adxResult.diPlus, adxResult.diMinus,
						adxResult.trendStrength);
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * ADX计算结果
	 */
	public static class ADXResult {
		public final double adx;
		public final double diPlus;
		public final double diMinus;
		public final double trendStrength;
		public final String trendType;

		public ADXResult(double adx, double diPlus, double diMinus) {
			this.adx = adx;
			this.diPlus = diPlus;
			this.diMinus = diMinus;
			this.trendStrength = adx;

			if (diPlus > diMinus) {
				this.trendType = "UPTREND";
			} else if (diMinus > diPlus) {
				this.trendType = "DOWNTREND";
			} else {
				this.trendType = "SIDEWAYS";
			}
		}
	}

	/**
	 * 计算ADX指标
	 */
	private ADXResult calculateADX() {
		int requiredBars = adxPeriod * 2; // ADX需要更多数据
		if (highPrices.size() < requiredBars) {
			return null;
		}

		try {
			// 1. 计算方向运动(Directional Movement)
			List<Double> plusDM = new ArrayList<>();
			List<Double> minusDM = new ArrayList<>();
			List<Double> trueRanges = new ArrayList<>();

			for (int i = 1; i < highPrices.size(); i++) {
				double upMove = highPrices.get(i) - highPrices.get(i - 1);
				double downMove = lowPrices.get(i - 1) - lowPrices.get(i);

				// 计算+DM和-DM
				if (upMove > downMove && upMove > 0) {
					plusDM.add(upMove);
					minusDM.add(0.0);
				} else if (downMove > upMove && downMove > 0) {
					plusDM.add(0.0);
					minusDM.add(downMove);
				} else {
					plusDM.add(0.0);
					minusDM.add(0.0);
				}

				// 计算真实波幅
				double trueRange = calculateTrueRange(i);
				trueRanges.add(trueRange);
			}

			// 2. 计算平滑值（使用Wilder平滑）
			double smoothedPlusDM = calculateSmoothedValue(plusDM, adxPeriod, trueRanges.size() - adxPeriod);
			double smoothedMinusDM = calculateSmoothedValue(minusDM, adxPeriod, trueRanges.size() - adxPeriod);
			double smoothedTR = calculateSmoothedValue(trueRanges, adxPeriod, trueRanges.size() - adxPeriod);

			// 3. 计算方向指标(DI)
			double diPlus = (smoothedPlusDM / smoothedTR) * 100;
			double diMinus = (smoothedMinusDM / smoothedTR) * 100;

			// 4. 计算方向指数(DX)
			double dx = 0.0;
			if (diPlus + diMinus > 0) {
				dx = (Math.abs(diPlus - diMinus) / (diPlus + diMinus)) * 100;
			}

			// 5. 计算ADX（DX的平滑值）
			double adx = calculateSmoothedADX(dx);

			return new ADXResult(adx, diPlus, diMinus);

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateADX", e);
			return null;
		}
	}

	/**
	 * 计算真实波幅
	 */
	private double calculateTrueRange(int index) {
		double high = highPrices.get(index);
		double low = lowPrices.get(index);
		double prevClose = closePrices.get(index - 1);

		double tr1 = high - low;
		double tr2 = Math.abs(high - prevClose);
		double tr3 = Math.abs(low - prevClose);

		return Math.max(tr1, Math.max(tr2, tr3));
	}

	/**
	 * 计算平滑值（Wilder平滑方法）
	 */
	private double calculateSmoothedValue(List<Double> values, int period, int startIndex) {
		if (values.size() < startIndex + period) {
			return 0.0;
		}

		// 第一个值是前period个值的简单平均
		if (adxValues.isEmpty()) {
			double sum = 0.0;
			for (int i = startIndex; i < startIndex + period; i++) {
				sum += values.get(i);
			}
			return sum / period;
		}

		// 后续值使用Wilder平滑：前值 * (period-1) + 当前值) / period
		double previousValue = getPreviousSmoothedValue(values);
		double currentValue = values.get(values.size() - 1);

		return (previousValue * (period - 1) + currentValue) / period;
	}

	/**
	 * 获取前一个平滑值
	 */
	private double getPreviousSmoothedValue(List<Double> values) {
		// 简化实现，实际应该维护平滑值历史
		if (values.size() < 2)
			return values.get(0);
		return values.get(values.size() - 2);
	}

	/**
	 * 计算ADX（DX的平滑值）
	 */
	private double calculateSmoothedADX(double currentDX) {
		if (adxValues.isEmpty()) {
			// 第一个ADX值是前period个DX值的平均
			return currentDX; // 简化实现
		}

		// Wilder平滑：ADX = (前ADX * (period-1) + 当前DX) / period
		double previousADX = adxValues.get(adxValues.size() - 1);
		return (previousADX * (adxPeriod - 1) + currentDX) / adxPeriod;
	}

	/**
	 * 生成ADX交易信号
	 */
	private SignalEvent generateADXSignal(BarEvent bar, ADXResult adx) {
		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());

		// 强趋势信号
		if (adx.adx > adxThreshold) {
			// +DI > -DI 表示上升趋势
			if (adx.diPlus > adx.diMinus && adx.diPlus > diPlusThreshold) {
				if (!hasPosition) {
					double strength = calculateTrendStrength(adx);
					return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "ADX上升趋势");
				}
			}
			// -DI > +DI 表示下降趋势
			else if (adx.diMinus > adx.diPlus && adx.diMinus > diMinusThreshold) {
				if (hasPosition) {
					double strength = calculateTrendStrength(adx);
					return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", strength, "ADX下降趋势");
				}
			}
		}

		// DI交叉信号
		if (useDICross) {
			SignalEvent crossSignal = generateDICrossSignal(bar, adx, hasPosition);
			if (crossSignal != null) {
				return crossSignal;
			}
		}

		// 趋势减弱平仓
		if (hasPosition && adx.adx < weakTrendThreshold) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.3, "趋势减弱");
		}

		return null;
	}

	/**
	 * 生成DI交叉信号
	 */
	private SignalEvent generateDICrossSignal(BarEvent bar, ADXResult adx, boolean hasPosition) {
		if (diPlusValues.size() < 2 || diMinusValues.size() < 2) {
			return null;
		}

		double previousDIPlus = diPlusValues.get(diPlusValues.size() - 2);
		double previousDIMinus = diMinusValues.get(diMinusValues.size() - 2);

		// +DI上穿-DI（金叉）
		if (!hasPosition && previousDIPlus <= previousDIMinus && adx.diPlus > adx.diMinus) {
			double strength = calculateCrossStrength(adx);
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "DI金叉");
		}

		// +DI下穿-DI（死叉）
		if (hasPosition && previousDIPlus >= previousDIMinus && adx.diPlus < adx.diMinus) {
			double strength = calculateCrossStrength(adx);
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", strength, "DI死叉");
		}

		return null;
	}

	/**
	 * 计算趋势强度
	 */
	private double calculateTrendStrength(ADXResult adx) {
		double baseStrength = Math.min(adx.adx / 100.0, 0.8);

		// DI差异增强强度
		double diDifference = Math.abs(adx.diPlus - adx.diMinus) / (adx.diPlus + adx.diMinus);
		double diBonus = Math.min(diDifference * 0.3, 0.2);

		return Math.min(baseStrength + diBonus, 1.0);
	}

	/**
	 * 计算交叉强度
	 */
	private double calculateCrossStrength(ADXResult adx) {
		double diSpread = Math.abs(adx.diPlus - adx.diMinus);
		double adxContribution = Math.min(adx.adx / 50.0, 0.5);

		return Math.min((diSpread * 2.0) + adxContribution, 1.0);
	}

	/**
	 * 获取趋势类型描述
	 */
	public String getTrendDescription(ADXResult adx) {
		if (adx.adx < weakTrendThreshold) {
			return "无趋势";
		} else if (adx.adx < strongTrendThreshold) {
			return "弱趋势";
		} else {
			return "强趋势";
		}
	}

	/**
	 * 判断是否处于强趋势中
	 */
	public boolean isStrongTrend(ADXResult adx) {
		return adx.adx >= strongTrendThreshold;
	}

	/**
	 * 判断趋势方向
	 */
	public String getTrendDirection(ADXResult adx) {
		if (adx.diPlus > adx.diMinus) {
			return "上升";
		} else if (adx.diMinus > adx.diPlus) {
			return "下降";
		} else {
			return "盘整";
		}
	}

	private void updatePriceData(double high, double low, double close) {
		highPrices.add(high);
		lowPrices.add(low);
		closePrices.add(close);

		// 限制数据大小
		int maxSize = adxPeriod * 4;
		if (highPrices.size() > maxSize) {
			highPrices.remove(0);
			lowPrices.remove(0);
			closePrices.remove(0);
		}
	}

	private boolean hasEnoughData() {
		return highPrices.size() >= adxPeriod * 2;
	}

	@Override
	public void reset() {
		super.reset();
		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		adxValues.clear();
		diPlusValues.clear();
		diMinusValues.clear();
		lastADX = null;
		lastDIPlus = null;
		lastDIMinus = null;
	}

	// ==================== Getter和Setter方法 ====================

	public int getAdxPeriod() {
		return adxPeriod;
	}

	public void setAdxPeriod(int adxPeriod) {
		this.adxPeriod = adxPeriod;
		setParameter("adxPeriod", adxPeriod);
	}

	public double getAdxThreshold() {
		return adxThreshold;
	}

	public void setAdxThreshold(double adxThreshold) {
		this.adxThreshold = adxThreshold;
		setParameter("adxThreshold", adxThreshold);
	}

	public double getDiPlusThreshold() {
		return diPlusThreshold;
	}

	public void setDiPlusThreshold(double diPlusThreshold) {
		this.diPlusThreshold = diPlusThreshold;
		setParameter("diPlusThreshold", diPlusThreshold);
	}

	public double getDiMinusThreshold() {
		return diMinusThreshold;
	}

	public void setDiMinusThreshold(double diMinusThreshold) {
		this.diMinusThreshold = diMinusThreshold;
		setParameter("diMinusThreshold", diMinusThreshold);
	}

	public Double getLastADX() {
		return lastADX;
	}

	public Double getLastDIPlus() {
		return lastDIPlus;
	}

	public Double getLastDIMinus() {
		return lastDIMinus;
	}

	/**
	 * 获取ADX指标描述
	 */
	public String getADXDescription() {
		return String.format("ADX(%d) - 平均趋向指数，趋势强度测量", adxPeriod);
	}
}