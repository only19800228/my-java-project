package com.Quantitative.all.Test.DataFeed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDateTime;

import org.junit.Before;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
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
		config = new BacktestConfig("000001", LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 6, 30, 0, 0),
				100000.0);
	}

	/**
	 * 执行策略回测测试
	 */
	protected BacktestResult runStrategyTest(BaseStrategy strategy) {
		try {
			// 创建回测引擎
			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
			engine.setStrategy(strategy);

			// 执行回测
			BacktestResult result = engine.runBacktest();

			// 验证基本结果
			assertNotNull("回测结果不应为空", result);
			assertEquals("初始资金应匹配", config.getInitialCapital(), result.getInitialCapital(), 0.001);

			return result;

		} catch (Exception e) {
			fail("策略回测失败: " + e.getMessage());
			return null;
		}
	}

	/**
	 * 验证回测结果的基本合理性
	 */
	protected void validateBasicResults(BacktestResult result) {
		assertNotNull("回测结果不应为空", result);
		assertTrue("最终资金应为非负数", result.getFinalCapital() >= 0);
		assertTrue("总交易次数应为非负数", result.getTotalTrades() >= 0);
		assertTrue("胜率应在0-100之间", result.getWinRate() >= 0 && result.getWinRate() <= 100);
	}
}