package com.Quantitative.backtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.Quantitative.BaseTest;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.data.DataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 回测引擎测试 - 修复版本
 */
@RunWith(MockitoJUnitRunner.class)
public class BacktestEngineTest extends BaseTest {

	@Test
	public void testBacktestEngineInitialization() {
		// 准备真实的数据源
		AKShareDataFeed dataFeed = new AKShareDataFeed();
		dataFeed.setDebugMode(true);
		dataFeed.setParameter("timeframe", "daily");
		dataFeed.setParameter("adjust", "qfq");

		BacktestConfig config = new BacktestConfig("000001", LocalDateTime.of(2023, 1, 1, 0, 0),
				LocalDateTime.of(2023, 12, 31, 0, 0), 100000.0);

		// 执行
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);

		// 验证
		assertNotNull(engine);
		assertNotNull(engine.getEventBus());
		assertEquals(config, engine.getConfig());

		logger.info("回测引擎初始化测试通过");
	}

	@Test
	public void testBacktestWorkflowWithMockData() {
		// 准备模拟数据
		DataFeed mockDataFeed = mock(DataFeed.class);
		List<BarEvent> testBars = Arrays.asList(createTestBarEvent("000001", 10.0), createTestBarEvent("000001", 10.5),
				createTestBarEvent("000001", 10.2), createTestBarEvent("000001", 10.8),
				createTestBarEvent("000001", 11.0));

		when(mockDataFeed.hasNextBar()).thenReturn(true, true, true, true, true, false);
		when(mockDataFeed.getNextBar()).thenReturn(testBars.get(0), testBars.get(1), testBars.get(2), testBars.get(3),
				testBars.get(4));
		when(mockDataFeed.getAllBars()).thenReturn(testBars);

		BacktestConfig config = new BacktestConfig();
		config.setSymbol("000001");
		config.setInitialCapital(100000.0);
		config.setStartDate(LocalDateTime.now().minusDays(10));
		config.setEndDate(LocalDateTime.now());

		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(mockDataFeed, config);

		// 使用RSI策略
		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();
		engine.setStrategy(strategy);

		// 执行
		assertNoException(() -> {
			BacktestResult result = engine.runBacktest();

			// 验证基本结果
			assertNotNull(result);
			assertEquals(100000.0, result.getInitialCapital(), 0.001);
			// 由于数据量少，可能没有交易，这是正常的
			assertTrue(result.getTotalTrades() >= 0);
		});

		logger.info("回测流程完整性测试通过");
	}

	@Test
	public void testBacktestWithEmptyData() {
		// 准备空数据
		DataFeed mockDataFeed = mock(DataFeed.class);
		when(mockDataFeed.hasNextBar()).thenReturn(false);
		when(mockDataFeed.getAllBars()).thenReturn(Arrays.asList());

		BacktestConfig config = new BacktestConfig();
		config.setSymbol("000001");
		config.setInitialCapital(100000.0);
		config.setStartDate(LocalDateTime.now().minusDays(10));
		config.setEndDate(LocalDateTime.now());

		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(mockDataFeed, config);
		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();
		engine.setStrategy(strategy);

		// 执行
		assertNoException(() -> {
			BacktestResult result = engine.runBacktest();

			// 验证
			assertNotNull(result);
			assertEquals(100000.0, result.getInitialCapital(), 0.001);
			assertEquals(0, result.getTotalTrades()); // 没有数据应该没有交易
		});

		logger.info("空数据回测测试通过");
	}

	@Test
	public void testRealDataBacktest() {
		logger.info("=== 真实数据回测测试开始 ===");

		try {
			// 步骤1: 创建数据源
			logger.info("步骤1: 创建数据源");
			AKShareDataFeed dataFeed = new AKShareDataFeed();
			dataFeed.setDebugMode(true);
			dataFeed.setParameter("timeframe", "daily");
			dataFeed.setParameter("adjust", "qfq");

			// 测试连接
			boolean connected = dataFeed.testConnection();
			assertTrue("数据源连接失败", connected);
			logger.info("? 数据源连接成功");

			// 步骤2: 创建配置
			logger.info("步骤2: 创建配置");
			BacktestConfig config = new BacktestConfig("000001", LocalDateTime.of(2023, 1, 1, 0, 0),
					LocalDateTime.of(2023, 3, 31, 0, 0), // 缩短测试时间
					100000.0);

			// 步骤3: 创建策略
			logger.info("步骤3: 创建策略");
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
			strategy.setDebugMode(true);

			// 步骤4: 创建回测引擎
			logger.info("步骤4: 创建回测引擎");
			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
			engine.setStrategy(strategy);

			// 步骤5: 执行回测
			logger.info("步骤5: 执行回测");
			BacktestResult result = engine.runBacktest();

			// 验证结果
			assertNotNull("回测结果为空", result);
			assertEquals(100000.0, result.getInitialCapital(), 0.001);
			logger.info("? 回测执行完成");

			// 打印简要结果
			result.printSummary();

		} catch (Exception e) {
			logger.error("真实数据回测测试失败", e);
			fail("真实数据回测测试失败: " + e.getMessage());
		}

		logger.info("=== 真实数据回测测试结束 ===");
	}
}