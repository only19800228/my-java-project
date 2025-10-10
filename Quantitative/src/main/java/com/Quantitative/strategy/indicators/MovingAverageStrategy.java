package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 移动平均线策略
 */
public class MovingAverageStrategy extends BaseStrategy {
	private int shortWindow;
	private int longWindow;
	private List<Double> priceHistory;

	// 策略状态
	private double shortMA;
	private double longMA;
	private int position; // 1: 多头, -1: 空头, 0: 空仓

	public MovingAverageStrategy() {
		super("MovingAverage"); // 使用正确的构造函数
		this.shortWindow = 5;
		this.longWindow = 20;
		this.priceHistory = new ArrayList<>();
		this.position = 0;
		setParameter("description", "双移动平均线交叉策略");
	}

	public MovingAverageStrategy(int shortWindow, int longWindow) {
		super("MovingAverage");
		this.shortWindow = shortWindow;
		this.longWindow = longWindow;
		this.priceHistory = new ArrayList<>();
		this.position = 0;
		setParameter("description", "双移动平均线交叉策略 - 短周期:" + shortWindow + ", 长周期:" + longWindow);
	}

	@Override
	protected void init() {
		TradingLogger.debug("MovingAverageStrategy", "初始化策略: 短周期=%d, 长周期=%d", shortWindow, longWindow);
		priceHistory.clear();
		position = 0;
		shortMA = 0;
		longMA = 0;
	}

	/**
	 * 计算交易信号 - 实现BaseStrategy的抽象方法
	 */
	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 添加价格到历史数据
			priceHistory.add(bar.getClose());

			// 确保有足够的数据计算移动平均线
			if (priceHistory.size() < longWindow) {
				if (debugMode) {
					TradingLogger.debug("MovingAverageStrategy", "数据不足，需要 %d 条，当前 %d 条", longWindow,
							priceHistory.size());
				}
				return;
			}

			// 计算移动平均线
			calculateMovingAverages();

			// 生成交易信号
			SignalEvent signal = generateSignal(bar);
			if (signal != null) {
				signals.add(signal);
			}

		} catch (Exception e) {
			TradingLogger.logSystemError("MovingAverageStrategy", "calculateSignals", e);
		}
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
			TradingLogger.debug("MovingAverageStrategy", "MA计算: 短MA=%.4f, 长MA=%.4f", shortMA, longMA);
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
			strength = Math.min((shortMA - longMA) / longMA, 1.0); // 限制强度在0-1之间
			comment = String.format("移动平均线金叉: 短MA(%.4f) > 长MA(%.4f)", shortMA, longMA);
			if (debugMode) {
				TradingLogger.debug("MovingAverageStrategy", "生成买入信号: 价格=%.4f, 强度=%.2f", bar.getClose(), strength);
			}
		}
		// 死叉信号：短线下穿长线，卖出
		else if (shortMA < longMA && position >= 0) {
			newPosition = -1;
			signalType = "SELL";
			strength = Math.min((longMA - shortMA) / longMA, 1.0); // 限制强度在0-1之间
			comment = String.format("移动平均线死叉: 短MA(%.4f) < 长MA(%.4f)", shortMA, longMA);
			if (debugMode) {
				TradingLogger.debug("MovingAverageStrategy", "生成卖出信号: 价格=%.4f, 强度=%.2f", bar.getClose(), strength);
			}
		}

		// 如果仓位发生变化，生成信号事件
		if (newPosition != position) {
			position = newPosition;
			return createSignalEvent(bar, signalType, strength, comment);
		}

		return null;
	}

	/**
	 * 创建信号事件 - 使用正确的SignalEvent构造函数
	 */
	private SignalEvent createSignalEvent(BarEvent bar, String signalType, double strength, String comment) {
		try {
			// 使用正确的SignalEvent构造函数
			return new SignalEvent(bar.getTimestamp(), // LocalDateTime
														// timestamp
					bar.getSymbol(), // String symbol
					signalType, // String signalType
					strength, // double strength (0-1)
					this.getName(), // String strategyName
					comment // String comment
			);

		} catch (Exception e) {
			TradingLogger.logSystemError("MovingAverageStrategy", "createSignalEvent", e);
			return null;
		}
	}

	// Getter 和 Setter 方法
	public int getShortWindow() {
		return shortWindow;
	}

	public void setShortWindow(int shortWindow) {
		this.shortWindow = shortWindow;
		setParameter("shortWindow", shortWindow);
	}

	public int getLongWindow() {
		return longWindow;
	}

	public void setLongWindow(int longWindow) {
		this.longWindow = longWindow;
		setParameter("longWindow", longWindow);
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

	@Override
	public String toString() {
		return String.format("MovingAverageStrategy[短周期=%d, 长周期=%d, 当前仓位=%d]", shortWindow, longWindow, position);
	}
}