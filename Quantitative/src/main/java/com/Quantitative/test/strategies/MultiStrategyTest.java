package com.Quantitative.test.strategies;

import java.time.LocalDateTime;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.composite.TrendFollowingComposite;

/**
 * 多策略测试
 */
public class MultiStrategyTest {
	public static void main(String[] args) {
		System.out.println("=== 多策略测试开始 ===\n");

		// 测试趋势跟踪策略
		testTrendFollowingStrategy();

		System.out.println("\n=== 多策略测试结束 ===");
	}

	private static void testTrendFollowingStrategy() {
		System.out.println("测试趋势跟踪策略...");

		BacktestConfig config = new BacktestConfig();
		config.setSymbol("000001");
		config.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
		config.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
		config.setInitialCapital(100000.0);

		AKShareDataFeed dataFeed = new AKShareDataFeed();
		TrendFollowingComposite strategy = new TrendFollowingComposite(5, 20, 5, 1.2);

		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
		engine.setStrategy(strategy);

		BacktestResult result = engine.runBacktest();
		result.printSummary();
	}
}
