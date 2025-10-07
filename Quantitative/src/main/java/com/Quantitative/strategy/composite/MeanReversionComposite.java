package com.Quantitative.strategy.composite;

import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.BollingerBandsStrategy;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 均值回归组合策略 - RSI超买超卖 + 布林带
 */
public class MeanReversionComposite extends BaseStrategy {
	private EnhancedRSIStrategy rsiStrategy;
	private BollingerBandsStrategy bbStrategy;
	private double rsiOversold;
	private double rsiOverbought;

	public MeanReversionComposite() {
		super("均值回归组合策略");
		this.rsiStrategy = new EnhancedRSIStrategy(14, 70, 30, 0.02);
		this.bbStrategy = new BollingerBandsStrategy(20, 2.0, false, 0.1);
		this.rsiOversold = 30;
		this.rsiOverbought = 70;
		initializeParameters();
	}

	private void initializeParameters() {
		setParameter("rsiOversold", rsiOversold);
		setParameter("rsiOverbought", rsiOverbought);
	}

	@Override
	protected void init() {
		if (dataFeed != null) {
			rsiStrategy.setDataFeed(dataFeed);
			bbStrategy.setDataFeed(dataFeed);
		}
		if (portfolio != null) {
			rsiStrategy.setPortfolio(portfolio);
			bbStrategy.setPortfolio(portfolio);
		}
		rsiStrategy.initialize();
		bbStrategy.initialize();
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		String symbol = bar.getSymbol();
		double currentPrice = bar.getClose();

		// 获取RSI信号
		List<SignalEvent> rsiSignals = rsiStrategy.onBar(bar);

		// 获取布林带数据
		// 这里需要扩展BollingerBandsStrategy以提供布林带数据访问

		// 组合逻辑：RSI超卖 + 价格在布林带下轨附近
		for (SignalEvent rsiSignal : rsiSignals) {
			if (rsiSignal.isBuySignal() && isNearBollingerLower(symbol, currentPrice)) {
				signals.add(rsiSignal);
			}
		}
	}

	private boolean isNearBollingerLower(String symbol, double currentPrice) {
		// 这里需要访问布林带策略的内部状态
		// 简化实现：假设有方法可以获取布林带下轨
		return true; // 实际实现需要访问布林带数据
	}

	// 参数设置
	public void setRsiPeriod(int period) {
		rsiStrategy.setRsiPeriod(period);
	}

	public void setRsiLevels(double overbought, double oversold) {
		rsiStrategy.setOverbought(overbought);
		rsiStrategy.setOversold(oversold);
		this.rsiOverbought = overbought;
		this.rsiOversold = oversold;
	}
}