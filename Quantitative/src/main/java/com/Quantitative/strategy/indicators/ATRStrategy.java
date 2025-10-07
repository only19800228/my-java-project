package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * ATR平均真实波幅策略 - 基于波动率的止损和仓位管理
 */
public class ATRStrategy extends BaseStrategy {
	private int atrPeriod;
	private double atrMultiplier;
	private double volatilityThreshold;
	private boolean useATRStops;

	private List<Double> trueRanges = new ArrayList<>();
	private Double currentATR;
	private Double lastClose;

	public ATRStrategy() {
		super("ATR波动率策略");
		setDefaultParameters();
	}

	public ATRStrategy(int period, double multiplier, double volatilityThreshold) {
		super("ATR波动率策略");
		this.atrPeriod = period;
		this.atrMultiplier = multiplier;
		this.volatilityThreshold = volatilityThreshold;
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		setParameter("atrPeriod", atrPeriod);
		setParameter("atrMultiplier", atrMultiplier);
		setParameter("volatilityThreshold", volatilityThreshold);
		setParameter("useATRStops", true);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("ATR策略初始化: 周期=%d, 乘数=%.1f", atrPeriod, atrMultiplier));

		trueRanges.clear();
		currentATR = null;
		lastClose = null;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 计算真实波幅
			double trueRange = calculateTrueRange(bar);
			trueRanges.add(trueRange);

			// 限制数据大小
			if (trueRanges.size() > atrPeriod * 2) {
				trueRanges.remove(0);
			}

			// 计算ATR
			if (trueRanges.size() >= atrPeriod) {
				currentATR = calculateATR();
			}

			lastClose = bar.getClose();

			// 生成交易信号
			SignalEvent signal = generateATRSignal(bar);
			if (signal != null) {
				signals.add(signal);

				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(),
						String.format("ATR=%.3f", currentATR));
			}

			if (debugMode && currentATR != null) {
				TradingLogger.debug(getName(), "%s ATR=%.3f, 价格=%.2f", bar.getTimestamp().toLocalDate(), currentATR,
						bar.getClose());
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 计算真实波幅
	 */
	private double calculateTrueRange(BarEvent bar) {
		if (lastClose == null) {
			return bar.getHigh() - bar.getLow();
		}

		double tr1 = bar.getHigh() - bar.getLow();
		double tr2 = Math.abs(bar.getHigh() - lastClose);
		double tr3 = Math.abs(bar.getLow() - lastClose);

		return Math.max(tr1, Math.max(tr2, tr3));
	}

	/**
	 * 计算ATR
	 */
	private double calculateATR() {
		if (trueRanges.size() < atrPeriod) {
			return 0.0;
		}

		// 使用Wilder平滑方法
		double atr = 0.0;
		for (int i = trueRanges.size() - atrPeriod; i < trueRanges.size(); i++) {
			atr += trueRanges.get(i);
		}
		atr /= atrPeriod;

		return atr;
	}

	/**
	 * 生成ATR交易信号
	 */
	private SignalEvent generateATRSignal(BarEvent bar) {
		if (currentATR == null || currentATR == 0) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());
		double volatilityRatio = currentATR / bar.getClose();

		// 低波动率突破信号
		if (!hasPosition && volatilityRatio < volatilityThreshold) {
			// 检测价格突破
			if (isPriceBreakout(bar)) {
				double strength = calculateBreakoutStrength(bar);
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, getName());
			}
		}

		// ATR止损信号
		if (hasPosition && useATRStops) {
			SignalEvent stopSignal = checkATRStop(bar);
			if (stopSignal != null) {
				return stopSignal;
			}
		}

		return null;
	}

	/**
	 * 检测价格突破
	 */
	private boolean isPriceBreakout(BarEvent bar) {
		if (trueRanges.size() < atrPeriod * 2) {
			return false;
		}

		// 简单突破逻辑：价格突破近期高点
		double recentHigh = calculateRecentHigh();
		return bar.getClose() > recentHigh + currentATR * 0.5;
	}

	/**
	 * 计算近期高点
	 */
	private double calculateRecentHigh() {
		int lookback = Math.min(atrPeriod, trueRanges.size());
		double high = 0.0;

		// 这里需要访问历史高价数据，简化实现
		return high;
	}

	/**
	 * 计算突破强度
	 */
	private double calculateBreakoutStrength(BarEvent bar) {
		if (currentATR == null)
			return 0.3;

		double volatilityScore = 1.0 - (currentATR / bar.getClose());
		return Math.min(volatilityScore * 0.8 + 0.2, 1.0);
	}

	/**
	 * 检查ATR止损
	 */
	private SignalEvent checkATRStop(BarEvent bar) {
		// 这里应该基于持仓成本和ATR计算止损位
		// 简化实现：如果价格下跌超过2倍ATR，则止损
		if (portfolio != null) {
			Double entryPrice = getEntryPrice(bar.getSymbol());
			if (entryPrice != null) {
				double stopDistance = currentATR * atrMultiplier;
				if (bar.getClose() < entryPrice - stopDistance) {
					return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.8, "ATR止损");
				}
			}
		}
		return null;
	}

	/**
	 * 获取入场价格
	 */
	private Double getEntryPrice(String symbol) {
		if (portfolio != null) {
			Map<String, Object> positionInfo = portfolio.getPositionInfo(symbol);
			if (positionInfo != null) {
				return (Double) positionInfo.get("avgCost");
			}
		}
		return null;
	}

	/**
	 * 计算建议止损距离（基于ATR）
	 */
	public Double calculateStopDistance(String symbol, double currentPrice) {
		if (currentATR == null)
			return null;
		return currentATR * atrMultiplier;
	}

	/**
	 * 计算建议仓位大小（基于ATR）
	 */
	public Double calculatePositionSize(double capital, double currentPrice) {
		if (currentATR == null)
			return null;

		// 基于ATR的风险管理：每笔交易风险不超过资本的1%
		double riskPerTrade = capital * 0.01;
		double stopDistance = currentATR * atrMultiplier;
		double positionValue = riskPerTrade / (stopDistance / currentPrice);

		return Math.min(positionValue, capital * 0.1); // 不超过10%仓位
	}

	@Override
	public void reset() {
		super.reset();
		trueRanges.clear();
		currentATR = null;
		lastClose = null;
	}

	// Getter和Setter方法
	public int getAtrPeriod() {
		return atrPeriod;
	}

	public void setAtrPeriod(int atrPeriod) {
		this.atrPeriod = atrPeriod;
		setParameter("atrPeriod", atrPeriod);
	}

	public double getAtrMultiplier() {
		return atrMultiplier;
	}

	public void setAtrMultiplier(double atrMultiplier) {
		this.atrMultiplier = atrMultiplier;
		setParameter("atrMultiplier", atrMultiplier);
	}

	public Double getCurrentATR() {
		return currentATR;
	}
}