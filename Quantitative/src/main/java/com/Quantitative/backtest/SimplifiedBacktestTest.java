package com.Quantitative.backtest;

import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.data.model.StockBar;
import com.Quantitative.data.model.StockData;
import com.Quantitative.data.repository.CSVDataLoader;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 简化版回测测试 - 直接使用策略和事件处理
 */
public class SimplifiedBacktestTest {
	public static void main(String[] args) {
		System.out.println("=== 简化版回测测试 ===");

		try {
			// 1. 加载数据
			System.out.println("\n1. 加载数据...");
			CSVDataLoader loader = new CSVDataLoader();
			StockData stockData = loader.loadStockData("000001");

			if (stockData == null) {
				System.out.println("❌ 数据加载失败");
				return;
			}
			System.out.println("✅ 数据加载成功: " + stockData.size() + " 条记录");

			// 2. 创建策略
			System.out.println("\n2. 创建策略...");
			MovingAverageStrategy strategy = new MovingAverageStrategy(5, 20);
			strategy.setDebugMode(true);
			strategy.initialize();

			// 3. 模拟回测循环
			System.out.println("\n3. 运行回测循环...");
			runSimplifiedBacktest(strategy, stockData);

			System.out.println("\n🎉 简化版回测测试完成!");

		} catch (Exception e) {
			System.err.println("回测测试失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 运行简化版回测
	 */
	private static void runSimplifiedBacktest(MovingAverageStrategy strategy, StockData stockData) {
		int totalSignals = 0;
		double initialCapital = 100000.0;
		double capital = initialCapital;
		int position = 0; // 0: 空仓, 1: 多头
		double entryPrice = 0.0;
		int shares = 0;

		System.out.println("日期\t\t价格\t短MA\t长MA\t信号\t操作\t仓位\t资金");
		System.out.println("----------------------------------------------------------------------------");

		for (int i = 0; i < stockData.size(); i++) {
			StockBar stockBar = stockData.getBar(i);

			// 创建BarEvent
			BarEvent barEvent = new BarEvent(stockBar.getTimestamp(), stockBar.getSymbol(), stockBar.getOpen(),
					stockBar.getHigh(), stockBar.getLow(), stockBar.getClose(), stockBar.getVolume(),
					stockBar.getTurnover());

			// 处理Bar数据
			List<SignalEvent> signals = strategy.onBar(barEvent);

			// 执行交易逻辑
			String operation = "持有";
			if (!signals.isEmpty()) {
				SignalEvent signal = signals.get(0);
				totalSignals++;

				if (signal.isBuySignal() && position == 0) {
					// 买入
					position = 1;
					entryPrice = stockBar.getClose();
					shares = (int) (capital * 0.1 / entryPrice); // 10%仓位
					double cost = shares * entryPrice * 1.0003; // 包含手续费
					capital -= cost;
					operation = "买入 " + shares + "股";
				} else if (signal.isSellSignal() && position == 1) {
					// 卖出
					position = 0;
					double revenue = shares * stockBar.getClose() * 0.9997; // 扣除手续费
					capital += revenue;
					double profit = revenue - (shares * entryPrice * 1.0003);
					operation = String.format("卖出 盈利:%.2f", profit);
					shares = 0;
				}
			}

			// 更新市值
			double totalValue = capital;
			if (position == 1) {
				totalValue += shares * stockBar.getClose();
			}

			// 显示结果（只显示有信号或关键点）
			if (!signals.isEmpty() || i % 5 == 0 || i == stockData.size() - 1) {
				String signalInfo = signals.isEmpty() ? "HOLD" : signals.get(0).getSignalType();
				System.out.printf("%s\t%.2f\t%.2f\t%.2f\t%s\t%s\t%s\t%,.2f\n",
						stockBar.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")),
						stockBar.getClose(), strategy.getShortMA(), strategy.getLongMA(), signalInfo, operation,
						position == 1 ? "多头" : "空仓", totalValue);
			}
		}

		// 显示最终结果
		double totalReturn = (capital - initialCapital) / initialCapital * 100;
		System.out.println("\n=== 最终结果 ===");
		System.out.printf("初始资金: %,.2f\n", initialCapital);
		System.out.printf("最终资金: %,.2f\n", capital);
		System.out.printf("总收益率: %.2f%%\n", totalReturn);
		System.out.printf("总交易信号: %d\n", totalSignals);
		System.out.printf("最终仓位: %s\n", position == 1 ? "多头" : "空仓");
	}
}