package com.Quantitative.all.demo;

import java.time.LocalDateTime;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.csv.CSVDataSource;
import com.Quantitative.strategy.indicators.RSIStrategy;

/**
 * 测试修复后的RSI策略
 */
public class TestFixedRSIStrategy {

	public static void main(String[] args) {
		System.out.println("=== 测试修复后的RSI策略 ===\n");

		try {
			// 使用CSV数据源
			CSVDataSource csvDataSource = new CSVDataSource();
			csvDataSource.initialize();

			// 配置回测
			BacktestConfig config = new BacktestConfig();
			config.setSymbol("000001");
			config.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
			config.setEndDate(LocalDateTime.of(2023, 6, 30, 0, 0));
			config.setInitialCapital(100000.0);
			config.setDebugMode(true); // 开启调试模式

			// 创建修复后的RSI策略
			RSIStrategy strategy = new RSIStrategy(14, 70, 30, 0.02);
			strategy.setDebugMode(true);

			// 创建回测引擎
			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(csvDataSource, config);
			engine.setStrategy(strategy);

			System.out.println("🚀 开始回测...");
			BacktestResult result = engine.runBacktest();

			// 显示结果
			System.out.println("\n📊 回测结果:");
			result.calculateAdvancedMetrics();
			result.printSummary();

			// 特别关注数据使用情况
			System.out.println("\n📈 策略数据使用统计:");
			System.out.println("所需最小数据条数: " + (14 + 1));
			System.out.println("总交易次数: " + result.getTotalTrades());
			System.out.println("胜率: " + String.format("%.1f%%", result.getWinRate()));

			csvDataSource.shutdown();

		} catch (Exception e) {
			System.err.println("❌ 测试失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n=== 测试完成 ===");
	}
}