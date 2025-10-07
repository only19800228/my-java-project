package com.Quantitative.backtest;

import java.time.LocalDateTime;

import com.Quantitative.common.monitor.UnifiedMonitorManager;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

//在测试或主程序中使用
public class MonitoringExample {

	protected static AKShareDataFeed dataFeed;
	protected static BacktestConfig config;

	// 测试连接
	boolean connected = dataFeed.testConnection();
	// assertTrue("数据源连接失败", connected);

	public static void main(String[] args) {
		// 基础配置
		config = new BacktestConfig("000001", LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 6, 30, 0, 0),
				100000.0);

		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70, 30, 0.02);
		// 启用监控
		UnifiedMonitorManager monitorManager = UnifiedMonitorManager.getInstance();
		monitorManager.setEnabled(true);

		// 运行回测（自动集成监控）
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
		engine.setStrategy(strategy);
		BacktestResult result = engine.runBacktest(); // 自动监控

	}
}