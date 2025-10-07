package com.Quantitative.strategy.composite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.BollingerBandsStrategy;
import com.Quantitative.strategy.indicators.CCIStrategy;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 均值回归组合策略 - 多指标超买超卖确认 原理：结合RSI、布林带、CCI在极端位置寻找反转机会
 */
public class MeanReversionComposite1 extends BaseStrategy {
	private EnhancedRSIStrategy rsiStrategy;
	private BollingerBandsStrategy bbStrategy;
	private CCIStrategy cciStrategy;

	// 超买超卖阈值
	private double rsiOversold = 30.0;
	private double rsiOverbought = 70.0;
	private double cciOversold = -100.0;
	private double cciOverbought = 100.0;

	// 价格历史用于波动率计算
	private Map<String, List<Double>> priceHistory;

	public MeanReversionComposite1() {
		super("均值回归组合策略");
		initializeStrategies();
		setDefaultParameters();
	}

	private void initializeStrategies() {
		// RSI: 动量超买超卖
		this.rsiStrategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);

		// 布林带: 价格位置判断
		this.bbStrategy = new BollingerBandsStrategy(20, 2.0, true, 0.1);

		// CCI: 商品通道超买超卖
		this.cciStrategy = new CCIStrategy(14, 100.0, -100.0);

		this.priceHistory = new HashMap<>();
	}

	private void setDefaultParameters() {
		setParameter("rsiOversold", rsiOversold);
		setParameter("rsiOverbought", rsiOverbought);
		setParameter("cciOversold", cciOversold);
		setParameter("cciOverbought", cciOverbought);
		setParameter("volatilityThreshold", 0.02);
		setParameter("positionSizeRatio", 0.03);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0, "均值回归组合策略初始化: RSI+布林带+CCI超买超卖确认");

		if (dataFeed != null) {
			rsiStrategy.setDataFeed(dataFeed);
			bbStrategy.setDataFeed(dataFeed);
			cciStrategy.setDataFeed(dataFeed);
		}

		if (portfolio != null) {
			rsiStrategy.setPortfolio(portfolio);
			bbStrategy.setPortfolio(portfolio);
			cciStrategy.setPortfolio(portfolio);
		}

		rsiStrategy.initialize();
		bbStrategy.initialize();
		cciStrategy.initialize();
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			String symbol = bar.getSymbol();
			double currentPrice = bar.getClose();

			// 更新价格历史
			updatePriceHistory(symbol, currentPrice);

			// 获取各指标状态
			Double rsi = rsiStrategy.getLastRSI();
			Double cci = cciStrategy.getLastCCI();
			boolean isBollingerSqueeze = isBollingerSqueeze(symbol);

			// 检查均值回归条件
			MeanReversionSignal signal = checkMeanReversionConditions(bar, symbol, rsi, cci, isBollingerSqueeze);

			if (signal != null) {
				signals.add(signal.toSignalEvent());

				TradingLogger.logSignal(getName(), symbol, signal.direction, signal.strength,
						String.format("RSI=%.1f, CCI=%.1f, 布林带收缩=%s", rsi, cci, isBollingerSqueeze));
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 检查均值回归条件
	 */
	private MeanReversionSignal checkMeanReversionConditions(BarEvent bar, String symbol, Double rsi, Double cci,
			boolean isSqueeze) {
		if (rsi == null || cci == null) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(symbol);
		double currentPrice = bar.getClose();

		// 超卖回归条件 (买入信号)
		if (!hasPosition) {
			boolean rsiOversold = rsi < this.rsiOversold;
			boolean cciOversold = cci < this.cciOversold;
			boolean priceExtreme = isPriceAtExtreme(symbol, currentPrice, "LOW");

			// 需要至少两个条件满足
			int buyConditions = 0;
			if (rsiOversold)
				buyConditions++;
			if (cciOversold)
				buyConditions++;
			if (priceExtreme)
				buyConditions++;

			if (buyConditions >= 2) {
				double strength = calculateBuyStrength(rsi, cci, isSqueeze);
				return new MeanReversionSignal(bar, "BUY", strength, "超卖回归买入");
			}
		}

		// 超买回归条件 (卖出信号)
		if (hasPosition) {
			boolean rsiOverbought = rsi > this.rsiOverbought;
			boolean cciOverbought = cci > this.cciOverbought;
			boolean priceExtreme = isPriceAtExtreme(symbol, currentPrice, "HIGH");

			// 需要至少两个条件满足
			int sellConditions = 0;
			if (rsiOverbought)
				sellConditions++;
			if (cciOverbought)
				sellConditions++;
			if (priceExtreme)
				sellConditions++;

			if (sellConditions >= 2) {
				double strength = calculateSellStrength(rsi, cci, isSqueeze);
				return new MeanReversionSignal(bar, "SELL", strength, "超买回归卖出");
			}
		}

		return null;
	}

	/**
	 * 判断价格是否处于极端位置
	 */
	private boolean isPriceAtExtreme(String symbol, double currentPrice, String extremeType) {
		List<Double> prices = priceHistory.get(symbol);
		if (prices == null || prices.size() < 20) {
			return false;
		}

		// 计算近期价格分位数
		int lookback = Math.min(20, prices.size());
		List<Double> recentPrices = prices.subList(prices.size() - lookback, prices.size());

		if ("LOW".equals(extremeType)) {
			double lowest = recentPrices.stream().mapToDouble(Double::doubleValue).min().orElse(currentPrice);
			return currentPrice <= lowest * 1.02; // 2%容差
		} else {
			double highest = recentPrices.stream().mapToDouble(Double::doubleValue).max().orElse(currentPrice);
			return currentPrice >= highest * 0.98; // 2%容差
		}
	}

	/**
	 * 判断布林带是否收缩
	 */
	private boolean isBollingerSqueeze(String symbol) {
		// 简化实现 - 实际应该调用布林带策略的方法
		return Math.random() < 0.3; // 30%概率收缩
	}

	private double calculateBuyStrength(double rsi, double cci, boolean isSqueeze) {
		double rsiStrength = (rsiOversold - rsi) / rsiOversold;
		double cciStrength = (cciOversold - cci) / Math.abs(cciOversold);
		double squeezeBonus = isSqueeze ? 0.2 : 0.0;

		return Math.min((rsiStrength + cciStrength) * 0.4 + squeezeBonus, 1.0);
	}

	private double calculateSellStrength(double rsi, double cci, boolean isSqueeze) {
		double rsiStrength = (rsi - rsiOverbought) / (100 - rsiOverbought);
		double cciStrength = (cci - cciOverbought) / (200 - cciOverbought);
		double squeezeBonus = isSqueeze ? 0.2 : 0.0;

		return Math.min((rsiStrength + cciStrength) * 0.4 + squeezeBonus, 1.0);
	}

	private void updatePriceHistory(String symbol, double price) {
		List<Double> history = priceHistory.get(symbol);
		if (history == null) {
			history = new ArrayList<>();
			priceHistory.put(symbol, history);
		}
		history.add(price);

		// 限制历史数据大小
		if (history.size() > 100) {
			history.remove(0);
		}
	}

	@Override
	public void reset() {
		super.reset();
		if (rsiStrategy != null)
			rsiStrategy.reset();
		if (bbStrategy != null)
			bbStrategy.reset();
		if (cciStrategy != null)
			cciStrategy.reset();
		priceHistory.clear();
	}

	/**
	 * 均值回归信号内部类
	 */
	private static class MeanReversionSignal {
		final BarEvent bar;
		final String direction;
		final double strength;
		final String reason;

		MeanReversionSignal(BarEvent bar, String direction, double strength, String reason) {
			this.bar = bar;
			this.direction = direction;
			this.strength = strength;
			this.reason = reason;
		}

		SignalEvent toSignalEvent() {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), direction, strength, "MeanReversionComposite",
					reason);
		}
	}
}