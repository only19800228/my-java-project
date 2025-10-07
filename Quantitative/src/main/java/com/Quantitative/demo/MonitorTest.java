
package com.Quantitative.demo;

import java.time.LocalDateTime;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.monitoring.ConsoleMonitor;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 实时监控测试
 */
public class MonitorTest {

	public static void main(String[] args) {
		System.out.println("📊 实时监控测试开始\n");

		try {
			// 1. 创建基础组件
			AKShareDataFeed dataFeed = new AKShareDataFeed();
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();

			BacktestConfig config = new BacktestConfig();
			config.setSymbol("000001");
			config.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
			config.setEndDate(LocalDateTime.of(2023, 3, 31, 0, 0)); // 缩短测试时间
			config.setInitialCapital(100000.0);
			config.setDebugMode(false);

			// 2. 创建回测引擎
			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
			engine.setStrategy(strategy);

			// 3. 创建监控面板
			ConsoleMonitor monitor = new ConsoleMonitor(engine);
			monitor.setUpdateInterval(1); // 每秒更新

			// 4. 启动监控
			monitor.startMonitoring();

			// 5. 执行回测（在另一个线程）
			Thread backtestThread = new Thread(() -> {
				try {
					Thread.sleep(1000); // 等待监控启动
					BacktestResult result = engine.runBacktest();

					// 模拟进度更新
					for (int i = 0; i <= 100; i += 10) {
						monitor.setProgress(i);
						Thread.sleep(500);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			backtestThread.start();

			// 6. 运行监控一段时间
			Thread.sleep(10000); // 运行10秒

			// 7. 停止监控
			monitor.stopMonitoring();

		} catch (Exception e) {
			System.err.println("❌ 监控测试失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n📊 实时监控测试结束");
	}
}