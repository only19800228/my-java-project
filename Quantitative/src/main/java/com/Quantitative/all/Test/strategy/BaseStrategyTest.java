package com.Quantitative.all.Test.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 策略测试基类 - 提供通用的测试工具方法
 */
public abstract class BaseStrategyTest {

	protected AKShareDataFeed dataFeed;
	protected BacktestConfig config;

	@Before
	public void setUp() {
		// 初始化数据源
		dataFeed = new AKShareDataFeed();
		dataFeed.setDebugMode(true);
		dataFeed.setParameter("timeframe", "daily");
		dataFeed.setParameter("adjust", "qfq");

		// 测试连接
		boolean connected = dataFeed.testConnection();
		assertTrue("数据源连接失败", connected);

		// 基础配置
		config = new BacktestConfig("601398", LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 6, 30, 0, 0),
				100000.0);
		config.setDebugMode(false);
	}

	/**
	 * 执行策略回测测试
	 */
	protected BacktestResult runStrategyTest(BaseStrategy strategy) {
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
		engine.setStrategy(strategy);

		BacktestResult result = engine.runBacktest();
		assertNotNull("回测结果不应为空", result);

		return result;
	}

	/**
	 * 验证回测结果的基本合理性
	 */
	protected void validateBasicResults(BacktestResult result) {
		assertNotNull("回测结果不应为空", result);
		assertEquals("初始资金应该匹配", 100000.0, result.getInitialCapital(), 0.001);
		assertTrue("最终资金应该合理", result.getFinalCapital() >= 0);
		assertTrue("总交易次数应该合理", result.getTotalTrades() >= 0);
	}

	/**
	 * 加载测试数据
	 */
	protected List<BarEvent> loadTestData() {
		dataFeed.loadHistoricalData(config.getSymbol(), config.getStartDate(), config.getEndDate());
		return dataFeed.getAllBars();
	}
}