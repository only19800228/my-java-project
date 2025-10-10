package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 优化版移动平均线策略 - 添加止损和仓位管理
 */
public class OptimizedMovingAverageStrategy extends BaseStrategy {
	private int shortWindow;
	private int longWindow;
	private double stopLossPercent; // 止损百分比
	private double positionSize; // 仓位大小
	private List<Double> priceHistory;

	// 策略状态
	private double shortMA;
	private double longMA;
	private int position; // 1: 多头, -1: 空头, 0: 空仓
	private double entryPrice; // 入场价格
	private double stopLossPrice; // 止损价格

	public OptimizedMovingAverageStrategy() {
		super("OptimizedMovingAverage");
		this.shortWindow = 5;
		this.longWindow = 20;
		this.stopLossPercent = 0.03; // 3%止损
		this.positionSize = 0.05; // 5%仓位
		this.priceHistory = new ArrayList<>();
		this.position = 0;
		setParameter("description", "优化版双移动平均线策略");
	}

	public OptimizedMovingAverageStrategy(int shortWindow, int longWindow, double stopLossPercent,
			double positionSize) {
		super("OptimizedMovingAverage");
		this.shortWindow = shortWindow;
		this.longWindow = longWindow;
		this.stopLossPercent = stopLossPercent;
		this.positionSize = positionSize;
		this.priceHistory = new ArrayList<>();
		this.position = 0;
		setParameter("description", String.format("优化版MA策略(短%d/长%d, 止损%.1f%%, 仓位%.1f%%)", shortWindow, longWindow,
				stopLossPercent * 100, positionSize * 100));
	}

	@Override
	protected void init() {
		TradingLogger.debug("OptimizedMovingAverageStrategy", "初始化策略: 短周期=%d, 长周期=%d, 止损=%.1f%%, 仓位=%.1f%%",
				shortWindow, longWindow, stopLossPercent * 100, positionSize * 100);
		priceHistory.clear();
		position = 0;
		shortMA = 0;
		longMA = 0;
		entryPrice = 0;
		stopLossPrice = 0;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 添加价格到历史数据
			priceHistory.add(bar.getClose());

			// 确保有足够的数据计算移动平均线
			if (priceHistory.size() < longWindow) {
				if (debugMode) {
					TradingLogger.debug("OptimizedMovingAverageStrategy", "数据不足，需要 %d 条，当前 %d 条", longWindow,
							priceHistory.size());
				}
				return;
			}

			// 计算移动平均线
			calculateMovingAverages();

			// 检查止损
			SignalEvent stopLossSignal = checkStopLoss(bar);
			if (stopLossSignal != null) {
				signals.add(stopLossSignal);
				return;
			}

			// 生成交易信号
			SignalEvent signal = generateSignal(bar);
			if (signal != null) {
				signals.add(signal);
			}

		} catch (Exception e) {
			TradingLogger.logSystemError("OptimizedMovingAverageStrategy", "calculateSignals", e);
		}
	}

	/**
	 * 检查止损条件
	 */
	private SignalEvent checkStopLoss(BarEvent bar) {
		if (position == 1 && entryPrice > 0) {
			double currentPrice = bar.getClose();
			double drawdown = (entryPrice - currentPrice) / entryPrice;

			// 触发止损
			if (drawdown >= stopLossPercent) {
				position = 0;
				String reason = String.format("止损触发: 入场%.2f, 当前%.2f, 回撤%.1f%%", entryPrice, currentPrice,
						drawdown * 100);

				TradingLogger.debug("OptimizedMovingAverageStrategy", reason);

				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.9, // 高强度止损信号
						this.getName(), reason);
			}
		}
		return null;
	}

	/**
	 * 计算移动平均线
	 */
	private void calculateMovingAverages() {
		// 计算短期移动平均线
		double shortSum = 0;
		for (int i = priceHistory.size() - shortWindow; i < priceHistory.size(); i++) {
			shortSum += priceHistory.get(i);
		}
		shortMA = shortSum / shortWindow;

		// 计算长期移动平均线
		double longSum = 0;
		for (int i = priceHistory.size() - longWindow; i < priceHistory.size(); i++) {
			longSum += priceHistory.get(i);
		}
		longMA = longSum / longWindow;

		if (debugMode) {
			TradingLogger.debug("OptimizedMovingAverageStrategy", "MA计算: 短MA=%.4f, 长MA=%.4f", shortMA, longMA);
		}
	}

	/**
	 * 生成交易信号
	 */
	private SignalEvent generateSignal(BarEvent bar) {
		int newPosition = position;
		String signalType = "HOLD";
		double strength = 0.0;
		String comment = "";

		// 金叉信号：短线上穿长线，买入
		if (shortMA > longMA && position <= 0) {
			newPosition = 1;
			signalType = "BUY";
			strength = Math.min((shortMA - longMA) / longMA, 1.0);
			entryPrice = bar.getClose();
			stopLossPrice = entryPrice * (1 - stopLossPercent);
			comment = String.format("移动平均线金叉: 短MA(%.4f) > 长MA(%.4f), 止损价: %.2f", shortMA, longMA, stopLossPrice);

			if (debugMode) {
				TradingLogger.debug("OptimizedMovingAverageStrategy", "生成买入信号: 价格=%.4f, 强度=%.2f, 止损=%.2f",
						bar.getClose(), strength, stopLossPrice);
			}
		}
		// 死叉信号：短线下穿长线，卖出
		else if (shortMA < longMA && position >= 0 && position == 1) {
			newPosition = 0;
			signalType = "SELL";
			strength = Math.min((longMA - shortMA) / longMA, 1.0);
			comment = String.format("移动平均线死叉: 短MA(%.4f) < 长MA(%.4f)", shortMA, longMA);

			if (debugMode) {
				TradingLogger.debug("OptimizedMovingAverageStrategy", "生成卖出信号: 价格=%.4f, 强度=%.2f", bar.getClose(),
						strength);
			}
		}

		// 如果仓位发生变化，生成信号事件
		if (newPosition != position) {
			position = newPosition;
			if (position == 0) {
				entryPrice = 0;
				stopLossPrice = 0;
			}
			return createSignalEvent(bar, signalType, strength, comment);
		}

		return null;
	}

	/**
	 * 创建信号事件
	 */
	private SignalEvent createSignalEvent(BarEvent bar, String signalType, double strength, String comment) {
		try {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), signalType, strength, this.getName(), comment);
		} catch (Exception e) {
			TradingLogger.logSystemError("OptimizedMovingAverageStrategy", "createSignalEvent", e);
			return null;
		}
	}

	// Getter 方法
	public int getShortWindow() {
		return shortWindow;
	}

	public int getLongWindow() {
		return longWindow;
	}

	public double getStopLossPercent() {
		return stopLossPercent;
	}

	public double getPositionSize() {
		return positionSize;
	}

	public double getShortMA() {
		return shortMA;
	}

	public double getLongMA() {
		return longMA;
	}

	public int getPosition() {
		return position;
	}

	public double getEntryPrice() {
		return entryPrice;
	}

	public double getStopLossPrice() {
		return stopLossPrice;
	}

	@Override
	public String toString() {
		return String.format("OptimizedMovingAverageStrategy[短%d/长%d, 止损%.1f%%, 仓位%.1f%%, 当前仓位=%d]", shortWindow,
				longWindow, stopLossPercent * 100, positionSize * 100, position);
	}
}