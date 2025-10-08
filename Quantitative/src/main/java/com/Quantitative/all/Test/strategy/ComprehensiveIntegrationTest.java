package com.Quantitative.all.Test.strategy;

import java.time.LocalDateTime;

import org.junit.Test;

import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.backtest.SignalProcessingDebugger;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.data.DataFeed;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

public class ComprehensiveIntegrationTest {

	@Test
	public void testSignalDebug() {
		System.out.println("=== 信号处理调试测试 ===");

		// 1. 创建回测引擎（使用你的现有代码）
		BacktestConfig config = new BacktestConfig();
		config.setSymbol("601398");
		config.setStartDate(LocalDateTime.of(2014, 1, 1, 0, 0));
		config.setEndDate(LocalDateTime.of(2014, 6, 30, 0, 0));

		config.setInitialCapital(100000);

		DataFeed dataFeed = new AKShareDataFeed();
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);

		// 2. 设置策略
		BaseStrategy strategy = new EnhancedRSIStrategy(); // 你的RSI策略
		engine.setStrategy(strategy);

		// 3. 运行回测初始化（加载数据、初始化组件）
		engine.runBacktest(); // 这会执行到事件循环前停止

		// 4. 创建调试器并运行调试
		SignalProcessingDebugger debugger = new SignalProcessingDebugger(engine);
		debugger.debugSignalProcessing();

		// 或者快速调试
		// debugger.quickDebug();
	}
}