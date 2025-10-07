package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * Keltner Channel策略 - 基于ATR的通道突破策略 中轨：EMA 上轨：EMA + ATR * multiplier 下轨：EMA -
 * ATR * multiplier
 */
public class KeltnerChannelStrategy extends BaseStrategy {
	private int emaPeriod;
	private int atrPeriod;
	private double atrMultiplier;
	private boolean useSqueeze;
	private double squeezeThreshold;

	private List<Double> highPrices = new ArrayList<>();
	private List<Double> lowPrices = new ArrayList<>();
	private List<Double> closePrices = new ArrayList<>();
	private List<Double> trueRanges = new ArrayList<>();

	private Double upperBand;
	private Double middleBand;
	private Double lowerBand;
	private Double currentATR;
	private Double lastClose;

	public KeltnerChannelStrategy() {
		super("Keltner Channel策略");
		setDefaultParameters();
	}

	public KeltnerChannelStrategy(int emaPeriod, int atrPeriod, double atrMultiplier) {
		super("Keltner Channel策略");
		this.emaPeriod = emaPeriod;
		this.atrPeriod = atrPeriod;
		this.atrMultiplier = atrMultiplier;
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		setParameter("emaPeriod", emaPeriod);
		setParameter("atrPeriod", atrPeriod);
		setParameter("atrMultiplier", atrMultiplier);
		setParameter("useSqueeze", true);
		setParameter("squeezeThreshold", 0.1);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("KC策略初始化: EMA周期=%d, ATR周期=%d, 乘数=%.1f", emaPeriod, atrPeriod, atrMultiplier));

		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		trueRanges.clear();
		upperBand = null;
		middleBand = null;
		lowerBand = null;
		currentATR = null;
		lastClose = null;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 更新价格数据
			updatePriceData(bar.getHigh(), bar.getLow(), bar.getClose());

			// 计算真实波幅
			double trueRange = calculateTrueRange(bar);
			trueRanges.add(trueRange);

			// 限制数据大小
			if (trueRanges.size() > atrPeriod * 2) {
				trueRanges.remove(0);
			}
			if (highPrices.size() > emaPeriod * 2) {
				highPrices.remove(0);
				lowPrices.remove(0);
				closePrices.remove(0);
			}

			// 检查是否有足够数据
			if (!hasEnoughData()) {
				return;
			}

			// 计算KC指标
			calculateKeltnerChannel();

			// 生成交易信号
			SignalEvent signal = generateKCSignal(bar);
			if (signal != null) {
				signals.add(signal);
				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(),
						getChannelInfo());
			}

			if (debugMode && upperBand != null) {
				TradingLogger.debug(getName(), "%s 上轨=%.2f, 中轨=%.2f, 下轨=%.2f, ATR=%.3f",
						bar.getTimestamp().toLocalDate(), upperBand, middleBand, lowerBand, currentATR);
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 计算Keltner Channel
	 */
	private void calculateKeltnerChannel() {
		// 计算EMA中轨
		middleBand = calculateEMA(closePrices, emaPeriod);

		// 计算ATR
		currentATR = calculateATR();

		if (middleBand != null && currentATR != null) {
			upperBand = middleBand + (currentATR * atrMultiplier);
			lowerBand = middleBand - (currentATR * atrMultiplier);
		}
	}

	/**
	 * 计算指数移动平均
	 */
	private Double calculateEMA(List<Double> prices, int period) {
		if (prices.size() < period) {
			return null;
		}

		double multiplier = 2.0 / (period + 1.0);
		double ema = prices.get(0);

		for (int i = 1; i < prices.size(); i++) {
			ema = (prices.get(i) - ema) * multiplier + ema;
		}

		return ema;
	}

	/**
	 * 计算ATR
	 */
	private Double calculateATR() {
		if (trueRanges.size() < atrPeriod) {
			return null;
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
	 * 生成KC交易信号
	 */
	private SignalEvent generateKCSignal(BarEvent bar) {
		if (upperBand == null || middleBand == null || lowerBand == null) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());
		double currentPrice = bar.getClose();
		double channelWidth = (upperBand - lowerBand) / middleBand;

		// 检查通道收缩（波动率压缩）
		boolean isSqueeze = useSqueeze ? detectSqueeze(channelWidth) : false;

		if (!hasPosition) {
			// 买入信号：价格突破上轨，特别是从收缩状态突破
			if (currentPrice > upperBand) {
				double strength = calculateBreakoutStrength(currentPrice, true);
				if (isSqueeze) {
					strength = Math.min(strength + 0.2, 1.0); // 收缩突破更强
				}
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "KC上轨突破");
			}

			// 均值回归：价格触及下轨
			if (currentPrice < lowerBand && !isSqueeze) {
				double strength = calculateRegressionStrength(currentPrice);
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "KC下轨回归");
			}
		} else {
			// 卖出信号：价格跌破下轨或触及中轨止盈
			if (currentPrice < lowerBand) {
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.7, "KC下轨突破");
			}

			// 止盈：价格回到中轨
			if (currentPrice <= middleBand) {
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.5, "KC中轨止盈");
			}
		}

		return null;
	}

	/**
	 * 检测通道收缩（低波动率）
	 */
	private boolean detectSqueeze(double currentWidth) {
		// 需要历史宽度数据，简化实现
		// 实际应该比较当前宽度与历史平均宽度
		return currentWidth < squeezeThreshold;
	}

	/**
	 * 计算突破强度
	 */
	private double calculateBreakoutStrength(double currentPrice, boolean isUpper) {
		if (middleBand == null)
			return 0.5;

		double distance = Math.abs(currentPrice - middleBand) / middleBand;
		return Math.min(distance * 10, 1.0);
	}

	/**
	 * 计算均值回归强度
	 */
	private double calculateRegressionStrength(double currentPrice) {
		if (lowerBand == null || middleBand == null)
			return 0.5;

		double distanceFromLower = (middleBand - currentPrice) / (middleBand - lowerBand);
		return Math.min(distanceFromLower * 2, 1.0);
	}

	private void updatePriceData(double high, double low, double close) {
		highPrices.add(high);
		lowPrices.add(low);
		closePrices.add(close);
		lastClose = close;
	}

	private boolean hasEnoughData() {
		return closePrices.size() >= Math.max(emaPeriod, atrPeriod);
	}

	private String getChannelInfo() {
		return String.format("上轨=%.2f, 中轨=%.2f, 下轨=%.2f", upperBand, middleBand, lowerBand);
	}

	@Override
	public void reset() {
		super.reset();
		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		trueRanges.clear();
		upperBand = null;
		middleBand = null;
		lowerBand = null;
		currentATR = null;
		lastClose = null;
	}

	// Getter和Setter方法
	public int getEmaPeriod() {
		return emaPeriod;
	}

	public void setEmaPeriod(int emaPeriod) {
		this.emaPeriod = emaPeriod;
		setParameter("emaPeriod", emaPeriod);
	}

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

	public Double getUpperBand() {
		return upperBand;
	}

	public Double getMiddleBand() {
		return middleBand;
	}

	public Double getLowerBand() {
		return lowerBand;
	}
}