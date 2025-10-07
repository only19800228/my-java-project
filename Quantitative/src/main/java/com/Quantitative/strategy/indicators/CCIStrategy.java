package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * CCI商品通道指标策略 - 检测超买超卖和趋势变化
 */
public class CCIStrategy extends BaseStrategy {
	private int cciPeriod;
	private double overbought;
	private double oversold;
	private double exitThreshold;
	private boolean useDivergence;

	private List<Double> typicalPrices = new ArrayList<>();
	private Double lastCCI;
	private Double lastTypicalPrice;

	public CCIStrategy() {
		super("CCI商品通道策略");
		setDefaultParameters();
	}

	public CCIStrategy(int period, double overbought, double oversold) {
		super("CCI商品通道策略");
		this.cciPeriod = period;
		this.overbought = overbought;
		this.oversold = oversold;
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		setParameter("cciPeriod", cciPeriod);
		setParameter("overbought", overbought);
		setParameter("oversold", oversold);
		setParameter("exitThreshold", 0.0);
		setParameter("useDivergence", false);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("CCI策略初始化: 周期=%d, 超买=%.0f, 超卖=%.0f", cciPeriod, overbought, oversold));

		typicalPrices.clear();
		lastCCI = null;
		lastTypicalPrice = null;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 计算典型价格
			double typicalPrice = (bar.getHigh() + bar.getLow() + bar.getClose()) / 3.0;
			typicalPrices.add(typicalPrice);

			// 限制数据大小
			if (typicalPrices.size() > cciPeriod * 2) {
				typicalPrices.remove(0);
			}

			// 计算CCI
			if (typicalPrices.size() >= cciPeriod) {
				lastCCI = calculateCCI();
				lastTypicalPrice = typicalPrice;
			}

			// 生成交易信号
			SignalEvent signal = generateCCISignal(bar);
			if (signal != null) {
				signals.add(signal);

				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(),
						String.format("CCI=%.1f", lastCCI));
			}

			if (debugMode && lastCCI != null) {
				TradingLogger.debug(getName(), "%s CCI=%.1f, 价格=%.2f", bar.getTimestamp().toLocalDate(), lastCCI,
						bar.getClose());
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 计算CCI指标
	 */
	private double calculateCCI() {
		if (typicalPrices.size() < cciPeriod) {
			return 0.0;
		}

		// 获取最近period个典型价格
		List<Double> recentPrices = typicalPrices.subList(typicalPrices.size() - cciPeriod, typicalPrices.size());

		// 计算简单移动平均
		double sma = recentPrices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

		// 计算平均偏差
		double meanDeviation = recentPrices.stream().mapToDouble(price -> Math.abs(price - sma)).average().orElse(0.0);

		// 计算CCI
		double currentTypicalPrice = typicalPrices.get(typicalPrices.size() - 1);
		if (meanDeviation == 0) {
			return 0.0;
		}

		return (currentTypicalPrice - sma) / (0.015 * meanDeviation);
	}

	/**
	 * 生成CCI交易信号
	 */
	private SignalEvent generateCCISignal(BarEvent bar) {
		if (lastCCI == null) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());

		// 超卖区域买入信号
		if (!hasPosition && lastCCI < oversold) {
			// 确保CCI开始回升
			if (isCCIReversingUp()) {
				double strength = calculateBuyStrength();
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, getName());
			}
		}

		// 超买区域卖出信号
		if (hasPosition && lastCCI > overbought) {
			// 确保CCI开始回落
			if (isCCIReversingDown()) {
				double strength = calculateSellStrength();
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", strength, getName());
			}
		}

		// 零轴穿越信号
		SignalEvent zeroCrossSignal = checkZeroCross(bar, hasPosition);
		if (zeroCrossSignal != null) {
			return zeroCrossSignal;
		}

		return null;
	}

	/**
	 * 检查CCI是否开始回升
	 */
	private boolean isCCIReversingUp() {
		// 需要历史CCI数据，简化实现
		return true;
	}

	/**
	 * 检查CCI是否开始回落
	 */
	private boolean isCCIReversingDown() {
		// 需要历史CCI数据，简化实现
		return true;
	}

	/**
	 * 检查零轴穿越
	 */
	private SignalEvent checkZeroCross(BarEvent bar, boolean hasPosition) {
		// 这里需要维护CCI历史数据
		// 简化实现：零轴上方做多，零轴下方做空
		if (!hasPosition && lastCCI > 0 && lastCCI < 100) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.4, "CCI零轴上穿");
		}

		if (hasPosition && lastCCI < 0 && lastCCI > -100) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.4, "CCI零轴下穿");
		}

		return null;
	}

	/**
	 * 计算买入信号强度
	 */
	private double calculateBuyStrength() {
		if (lastCCI == null)
			return 0.3;

		double distanceFromOversold = Math.max(0, oversold - lastCCI);
		double maxDistance = Math.abs(oversold);
		double baseStrength = distanceFromOversold / maxDistance;

		return Math.min(baseStrength * 0.8 + 0.2, 1.0);
	}

	/**
	 * 计算卖出信号强度
	 */
	private double calculateSellStrength() {
		if (lastCCI == null)
			return 0.3;

		double distanceFromOverbought = Math.max(0, lastCCI - overbought);
		double maxDistance = Math.abs(overbought);
		double baseStrength = distanceFromOverbought / maxDistance;

		return Math.min(baseStrength * 0.8 + 0.2, 1.0);
	}

	/**
	 * 检测背离信号
	 */
	private boolean checkBullishDivergence() {
		// 价格创新低但CCI没有创新低 - 看涨背离
		// 需要价格和CCI的历史数据
		return false;
	}

	private boolean checkBearishDivergence() {
		// 价格创新高但CCI没有创新高 - 看跌背离
		// 需要价格和CCI的历史数据
		return false;
	}

	@Override
	public void reset() {
		super.reset();
		typicalPrices.clear();
		lastCCI = null;
		lastTypicalPrice = null;
	}

	// Getter和Setter方法
	public int getCciPeriod() {
		return cciPeriod;
	}

	public void setCciPeriod(int cciPeriod) {
		this.cciPeriod = cciPeriod;
		setParameter("cciPeriod", cciPeriod);
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

	public Double getLastCCI() {
		return lastCCI;
	}
}