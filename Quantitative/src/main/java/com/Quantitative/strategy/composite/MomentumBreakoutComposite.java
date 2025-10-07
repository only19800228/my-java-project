package com.Quantitative.strategy.composite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.ADXStrategy;
import com.Quantitative.strategy.indicators.ATRStrategy;
import com.Quantitative.strategy.indicators.OBVStrategy;

/**
 * 动量突破组合策略 - 量价齐升突破 原理：结合ADX趋势强度、ATR波动率和OBV量能确认突破有效性
 */
public class MomentumBreakoutComposite extends BaseStrategy {
	private ADXStrategy adxStrategy;
	private ATRStrategy atrStrategy;
	private OBVStrategy obvStrategy;

	// 突破参数
	private double breakoutMultiplier = 1.5;
	private double minTrendStrength = 20.0;
	private double volumeSurgeThreshold = 1.5;

	// 价格通道跟踪
	private Map<String, List<Double>> highPrices;
	private Map<String, List<Double>> lowPrices;

	public MomentumBreakoutComposite() {
		super("动量突破组合策略");
		initializeStrategies();
		setDefaultParameters();
	}

	private void initializeStrategies() {
		this.adxStrategy = new ADXStrategy(14, 20.0, 25.0, 25.0);
		this.atrStrategy = new ATRStrategy(14, 2.0, 0.03);
		this.obvStrategy = new OBVStrategy(20, 1.5);

		this.highPrices = new HashMap<>();
		this.lowPrices = new HashMap<>();
	}

	private void setDefaultParameters() {
		setParameter("breakoutMultiplier", breakoutMultiplier);
		setParameter("minTrendStrength", minTrendStrength);
		setParameter("volumeSurgeThreshold", volumeSurgeThreshold);
		setParameter("lookbackPeriod", 20);
		setParameter("useVolumeConfirmation", true);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0, "动量突破组合策略初始化: ADX+ATR+OBV量价突破确认");

		if (dataFeed != null) {
			adxStrategy.setDataFeed(dataFeed);
			atrStrategy.setDataFeed(dataFeed);
			obvStrategy.setDataFeed(dataFeed);
		}

		if (portfolio != null) {
			adxStrategy.setPortfolio(portfolio);
			atrStrategy.setPortfolio(portfolio);
			obvStrategy.setPortfolio(portfolio);
		}

		adxStrategy.initialize();
		atrStrategy.initialize();
		obvStrategy.initialize();
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			String symbol = bar.getSymbol();
			double currentHigh = bar.getHigh();
			double currentLow = bar.getLow();
			double currentClose = bar.getClose();
			long currentVolume = bar.getVolume();

			// 更新价格通道
			updatePriceChannels(symbol, currentHigh, currentLow);

			// 获取指标状态
			Double adx = adxStrategy.getLastADX();
			Double atr = atrStrategy.getCurrentATR();
			Double obv = obvStrategy.getLastOBV();
			Double obvMA = obvStrategy.getLastOBVMA();

			// 检查突破条件
			BreakoutSignal breakout = checkBreakoutConditions(bar, symbol, adx, atr, obv, obvMA);

			if (breakout != null) {
				signals.add(breakout.toSignalEvent());

				TradingLogger.logSignal(getName(), symbol, breakout.direction, breakout.strength,
						String.format("突破类型=%s, ADX=%.1f, ATR=%.3f", breakout.breakoutType, adx, atr));
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 检查突破条件
	 */
	private BreakoutSignal checkBreakoutConditions(BarEvent bar, String symbol, Double adx, Double atr, Double obv,
			Double obvMA) {
		if (adx == null || atr == null || obv == null) {
			return null;
		}

		// 趋势强度不足，不交易
		if (adx < minTrendStrength) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(symbol);
		double currentClose = bar.getClose();
		long currentVolume = bar.getVolume();

		// 计算价格通道
		PriceChannel channel = calculatePriceChannel(symbol);
		if (channel == null) {
			return null;
		}

		// 上突破检查
		if (!hasPosition && currentClose > channel.upperBand) {
			boolean volumeConfirmed = isVolumeSurge(currentVolume, symbol);
			boolean obvConfirmed = obv > obvMA;

			if (volumeConfirmed && obvConfirmed) {
				double strength = calculateBreakoutStrength(bar, channel, "UP");
				return new BreakoutSignal(bar, "BUY", strength, "上突破");
			}
		}

		// 下突破检查 (做空或止损)
		if (hasPosition && currentClose < channel.lowerBand) {
			double strength = calculateBreakoutStrength(bar, channel, "DOWN");
			return new BreakoutSignal(bar, "SELL", strength, "下突破止损");
		}

		return null;
	}

	/**
	 * 计算价格通道 (基于ATR)
	 */
	private PriceChannel calculatePriceChannel(String symbol) {
		List<Double> highs = highPrices.get(symbol);
		List<Double> lows = lowPrices.get(symbol);

		if (highs == null || lows == null || highs.size() < 20) {
			return null;
		}

		// 计算近期高点和低点
		double recentHigh = highs.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
		double recentLow = lows.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

		// 使用ATR计算通道宽度
		Double atr = atrStrategy.getCurrentATR();
		if (atr == null) {
			return null;
		}

		double upperBand = recentHigh + atr * breakoutMultiplier;
		double lowerBand = recentLow - atr * breakoutMultiplier;

		return new PriceChannel(upperBand, lowerBand, recentHigh, recentLow);
	}

	/**
	 * 检查成交量激增
	 */
	private boolean isVolumeSurge(long currentVolume, String symbol) {
		// 简化实现 - 实际应该计算平均成交量
		return currentVolume > 1000000; // 示例阈值
	}

	private double calculateBreakoutStrength(BarEvent bar, PriceChannel channel, String direction) {
		double baseStrength = 0.5;
		double distanceStrength = 0.0;

		if ("UP".equals(direction)) {
			distanceStrength = (bar.getClose() - channel.upperBand) / channel.upperBand;
		} else {
			distanceStrength = (channel.lowerBand - bar.getClose()) / channel.lowerBand;
		}

		// 突破距离越大，信号越强
		double distanceBonus = Math.min(distanceStrength * 10, 0.3);

		return Math.min(baseStrength + distanceBonus, 1.0);
	}

	private void updatePriceChannels(String symbol, double high, double low) {
		// 更新高点
		List<Double> highs = highPrices.get(symbol);
		if (highs == null) {
			highs = new ArrayList<>();
			highPrices.put(symbol, highs);
		}
		highs.add(high);

		// 更新低点
		List<Double> lows = lowPrices.get(symbol);
		if (lows == null) {
			lows = new ArrayList<>();
			lowPrices.put(symbol, lows);
		}
		lows.add(low);

		// 限制数据大小
		int maxSize = 50;
		if (highs.size() > maxSize) {
			highs.remove(0);
		}
		if (lows.size() > maxSize) {
			lows.remove(0);
		}
	}

	@Override
	public void reset() {
		super.reset();
		if (adxStrategy != null)
			adxStrategy.reset();
		if (atrStrategy != null)
			atrStrategy.reset();
		if (obvStrategy != null)
			obvStrategy.reset();
		highPrices.clear();
		lowPrices.clear();
	}

	/**
	 * 价格通道内部类
	 */
	private static class PriceChannel {
		final double upperBand;
		final double lowerBand;
		final double recentHigh;
		final double recentLow;

		PriceChannel(double upperBand, double lowerBand, double recentHigh, double recentLow) {
			this.upperBand = upperBand;
			this.lowerBand = lowerBand;
			this.recentHigh = recentHigh;
			this.recentLow = recentLow;
		}
	}

	/**
	 * 突破信号内部类
	 */
	private static class BreakoutSignal {
		final BarEvent bar;
		final String direction;
		final double strength;
		final String breakoutType;

		BreakoutSignal(BarEvent bar, String direction, double strength, String breakoutType) {
			this.bar = bar;
			this.direction = direction;
			this.strength = strength;
			this.breakoutType = breakoutType;
		}

		SignalEvent toSignalEvent() {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), direction, strength,
					"MomentumBreakoutComposite", breakoutType);
		}
	}
}