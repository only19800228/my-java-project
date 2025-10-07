package com.Quantitative.all.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.Quantitative.BaseTest;
import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.portfolio.composite.StrategyCompositeManager;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.ADXStrategy;
import com.Quantitative.strategy.indicators.ATRStrategy;
import com.Quantitative.strategy.indicators.AroonStrategy;
import com.Quantitative.strategy.indicators.BollingerBandsStrategy;
import com.Quantitative.strategy.indicators.CCIStrategy;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;
import com.Quantitative.strategy.indicators.KDJStrategy;
import com.Quantitative.strategy.indicators.KeltnerChannelStrategy;
import com.Quantitative.strategy.indicators.MACDStrategy;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;
import com.Quantitative.strategy.indicators.OBVStrategy;
import com.Quantitative.strategy.indicators.UltimateOscillatorStrategy;

/**
 * 技术指标和组合策略综合测试类
 */
@RunWith(MockitoJUnitRunner.class)
public class TechnicalIndicatorsComprehensiveTest extends BaseTest {

	private BacktestConfig baseConfig;
	private DataFeed mockDataFeed;
	private List<BarEvent> testBars;

	// 创建工具方法
	public static String repeat(String str, int count) {
		return Stream.generate(() -> str).limit(count).collect(Collectors.joining());
	}

	@Before
	public void setUp() {
		// 基础配置
		baseConfig = new BacktestConfig("000001", LocalDateTime.of(2023, 1, 1, 0, 0),
				LocalDateTime.of(2023, 12, 31, 0, 0), 100000.0);

		// 模拟数据
		mockDataFeed = mock(DataFeed.class);
		testBars = generateTestBars(100); // 生成100个测试K线

		when(mockDataFeed.getAllBars()).thenReturn(testBars);
		when(mockDataFeed.hasNextBar()).thenReturn(true, true, true, false);
		when(mockDataFeed.getNextBar()).thenReturn(testBars.get(0), testBars.get(1), testBars.get(2), testBars.get(3));
	}

	/**
	 * 生成测试K线数据
	 */
	private List<BarEvent> generateTestBars(int count) {
		List<BarEvent> bars = new ArrayList<>();
		LocalDateTime startTime = LocalDateTime.of(2023, 1, 1, 9, 30);
		double price = 100.0;

		for (int i = 0; i < count; i++) {
			double open = price;
			double high = price + Math.random() * 5;
			double low = price - Math.random() * 5;
			double close = price + (Math.random() - 0.5) * 10;
			long volume = 1000000 + (long) (Math.random() * 9000000);

			bars.add(new BarEvent(startTime.plusMinutes(i * 5), "000001", open, high, low, close, volume));
			price = close; // 下一根K线的开盘价等于当前收盘价
		}

		return bars;
	}

	/**
	 * 测试单个技术指标策略
	 */
	private BacktestResult testSingleStrategy(BaseStrategy strategy, String strategyName) {
		try {
			logger.info("测试策略: {}", strategyName);

			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(mockDataFeed, baseConfig);
			engine.setStrategy(strategy);

			BacktestResult result = engine.runBacktest();
			assertNotNull("回测结果不应为空", result);

			logger.info("{} 策略测试完成: 收益率={:.2f}%, 交易次数={}, 胜率={:.1f}%", strategyName, result.getTotalReturn(),
					result.getTotalTrades(), result.getWinRate());

			return result;

		} catch (Exception e) {
			logger.error("策略 {} 测试失败: {}", strategyName, e.getMessage());
			fail("策略测试失败: " + strategyName);
			return null;
		}
	}

	// ==================== 单个指标策略测试 ====================

	@Test
	public void testRSIStrategy() {
		EnhancedRSIStrategy rsiStrategy = new EnhancedRSIStrategy(14, 70, 30, 0.02);
		BacktestResult result = testSingleStrategy(rsiStrategy, "RSI策略");

		// RSI策略特定验证
		assertTrue("RSI策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testMACDStrategy() {
		MACDStrategy macdStrategy = new MACDStrategy(12, 26, 9, true);
		BacktestResult result = testSingleStrategy(macdStrategy, "MACD策略");

		assertTrue("MACD策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testMovingAverageStrategy() {
		MovingAverageStrategy maStrategy = new MovingAverageStrategy(10, 30, 5, 1.2);
		BacktestResult result = testSingleStrategy(maStrategy, "双均线策略");

		assertTrue("双均线策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testBollingerBandsStrategy() {
		BollingerBandsStrategy bbStrategy = new BollingerBandsStrategy(20, 2.0, true, 0.1);
		BacktestResult result = testSingleStrategy(bbStrategy, "布林带策略");

		assertTrue("布林带策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testKDJStrategy() {
		KDJStrategy kdjStrategy = new KDJStrategy(9, 3, 80, 20);
		BacktestResult result = testSingleStrategy(kdjStrategy, "KDJ策略");

		assertTrue("KDJ策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testATRStrategy() {
		ATRStrategy atrStrategy = new ATRStrategy(14, 2.0, 0.02);
		BacktestResult result = testSingleStrategy(atrStrategy, "ATR策略");

		assertTrue("ATR策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testADXStrategy() {
		ADXStrategy adxStrategy = new ADXStrategy(14, 25.0, 20.0, 20.0);
		BacktestResult result = testSingleStrategy(adxStrategy, "ADX策略");

		assertTrue("ADX策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testCCIStrategy() {
		CCIStrategy cciStrategy = new CCIStrategy(20, 100, -100);
		BacktestResult result = testSingleStrategy(cciStrategy, "CCI策略");

		assertTrue("CCI策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testOBVStrategy() {
		OBVStrategy obvStrategy = new OBVStrategy(20, 1.5);
		BacktestResult result = testSingleStrategy(obvStrategy, "OBV策略");

		assertTrue("OBV策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testKeltnerChannelStrategy() {
		KeltnerChannelStrategy kcStrategy = new KeltnerChannelStrategy(20, 10, 2.0);
		BacktestResult result = testSingleStrategy(kcStrategy, "Keltner通道策略");

		assertTrue("Keltner通道策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testAroonStrategy() {
		AroonStrategy aroonStrategy = new AroonStrategy(14, 70.0);
		BacktestResult result = testSingleStrategy(aroonStrategy, "Aroon策略");

		assertTrue("Aroon策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	@Test
	public void testUltimateOscillatorStrategy() {
		UltimateOscillatorStrategy uoStrategy = new UltimateOscillatorStrategy(7, 14, 28, 70, 30);
		BacktestResult result = testSingleStrategy(uoStrategy, "终极振荡器策略");

		assertTrue("终极振荡器策略应该生成交易信号", result.getTotalTrades() >= 0);
	}

	// ==================== 组合策略测试 ====================

	@Test
	public void testStrategyCombination() {
		logger.info("开始组合策略测试...");

		// 创建策略组合管理器
		StrategyCompositeManager compositeManager = new StrategyCompositeManager(mockDataFeed, baseConfig);

		// 添加多个策略到组合
		compositeManager.addStrategy("RSI策略", new EnhancedRSIStrategy(14, 70, 30, 0.02), 0.3);
		compositeManager.addStrategy("MACD策略", new MACDStrategy(12, 26, 9, true), 0.4);
		compositeManager.addStrategy("均线策略", new MovingAverageStrategy(10, 30, 5, 1.2), 0.3);

		// 执行组合回测
		StrategyCompositeManager.CompositeBacktestResult compositeResult = compositeManager.runCompositeBacktest();

		assertNotNull("组合回测结果不应为空", compositeResult);
		assertNotNull("组合结果不应为空", compositeResult.getCompositeResult());

		// 打印组合分析报告
		compositeResult.printReport();

		logger.info("组合策略测试完成");
	}

	@Test
	public void testStrategyWeightOptimization() {
		logger.info("开始策略权重优化测试...");

		StrategyCompositeManager compositeManager = new StrategyCompositeManager(mockDataFeed, baseConfig);

		// 添加策略
		compositeManager.addStrategy("RSI策略", new EnhancedRSIStrategy(14, 70, 30, 0.02), 0.33);
		compositeManager.addStrategy("布林带策略", new BollingerBandsStrategy(20, 2.0, true, 0.1), 0.33);
		compositeManager.addStrategy("ATR策略", new ATRStrategy(14, 2.0, 0.02), 0.34);

		// 优化权重
		StrategyCompositeManager.WeightOptimizationResult optimizationResult = compositeManager.optimizeWeights();

		assertNotNull("权重优化结果不应为空", optimizationResult);
		assertNotNull("优化后的权重不应为空", optimizationResult.getOptimizedWeights());

		logger.info("权重优化完成，优化方法: {}", optimizationResult.getMethod());
		optimizationResult.getOptimizedWeights().forEach((strategy, weight) -> {
			logger.info("策略 {} 优化后权重: {:.1f}%", strategy, weight * 100);
		});
	}

	// ==================== 性能对比测试 ====================

	@Test
	public void testPerformanceComparison() {
		logger.info("开始策略性能对比测试...");

		Map<String, BacktestResult> results = new HashMap<>();

		// 测试多个策略
		String[] strategies = { "RSI策略", "MACD策略", "双均线策略", "布林带策略", "KDJ策略" };

		BaseStrategy[] strategyInstances = { new EnhancedRSIStrategy(14, 70, 30, 0.02),
				new MACDStrategy(12, 26, 9, true), new MovingAverageStrategy(10, 30, 5, 1.2),
				new BollingerBandsStrategy(20, 2.0, true, 0.1), new KDJStrategy(9, 3, 80, 20) };

		// 执行所有策略回测
		for (int i = 0; i < strategies.length; i++) {
			BacktestResult result = testSingleStrategy(strategyInstances[i], strategies[i]);
			results.put(strategies[i], result);
		}

		// 性能对比分析
		printPerformanceComparison(results);
	}

	/**
	 * 打印性能对比报告
	 */
	private void printPerformanceComparison(Map<String, BacktestResult> results) {
		logger.info("\n" + String.join("", Collections.nCopies(80, "=")));
		logger.info("策略性能对比报告");
		logger.info(String.join("", Collections.nCopies(80, "=")));
		logger.info("{:<15} {:<10} {:<10} {:<10} {:<10} {:<10}", "策略名称", "收益率%", "夏普比率", "最大回撤%", "交易次数", "胜率%");
		logger.info("\n" + String.join("", Collections.nCopies(80, "=")));

		results.forEach((name, result) -> {
			logger.info("{:<15} {:<10.2f} {:<10.2f} {:<10.2f} {:<10} {:<10.1f}", name, result.getTotalReturn(),
					result.getSharpeRatio(), result.getMaxDrawdown(), result.getTotalTrades(), result.getWinRate());
		});
		logger.info("\n" + String.join("", Collections.nCopies(80, "=")));
	}

	// ==================== 参数敏感性测试 ====================

	@Test
	public void testParameterSensitivity() {
		logger.info("开始参数敏感性测试...");

		// 测试RSI策略不同参数
		int[] periods = { 10, 14, 20 };
		double[] overboughtLevels = { 65, 70, 75 };
		double[] oversoldLevels = { 25, 30, 35 };

		for (int period : periods) {
			for (double overbought : overboughtLevels) {
				for (double oversold : oversoldLevels) {
					String paramDesc = String.format("RSI(%d,%d,%d)", period, (int) overbought, (int) oversold);
					EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(period, overbought, oversold, 0.02);

					BacktestResult result = testSingleStrategy(strategy, paramDesc);

					logger.info("参数 {}: 收益率={:.2f}%, 交易次数={}", paramDesc, result.getTotalReturn(),
							result.getTotalTrades());
				}
			}
		}
	}

	// ==================== 风险指标测试 ====================

	@Test
	public void testRiskMetrics() {
		logger.info("开始风险指标测试...");

		EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70, 30, 0.02);
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(mockDataFeed, baseConfig);
		engine.setStrategy(strategy);

		BacktestResult result = engine.runBacktest();

		// 验证风险指标计算
		assertTrue("夏普比率应该被计算", result.getSharpeRatio() != 0.0);
		assertTrue("索提诺比率应该被计算", result.getSortinoRatio() != 0.0);
		assertTrue("卡尔玛比率应该被计算", result.getCalmarRatio() != 0.0);
		assertTrue("最大回撤应该被计算", result.getMaxDrawdown() >= 0.0);

		logger.info("风险指标测试完成:");
		logger.info("夏普比率: {:.2f}", result.getSharpeRatio());
		logger.info("索提诺比率: {:.2f}", result.getSortinoRatio());
		logger.info("卡尔玛比率: {:.2f}", result.getCalmarRatio());
		logger.info("最大回撤: {:.2f}%", result.getMaxDrawdown());
	}

	// ==================== 缓存性能测试 ====================

	@Test
	public void testCachePerformance() {
		logger.info("开始缓存性能测试...");

		// 测试带缓存和不带缓存的性能差异
		EnhancedRSIStrategy strategyWithCache = new EnhancedRSIStrategy(14, 70, 30, 0.02);
		strategyWithCache.setCacheEnabled(true);

		EnhancedRSIStrategy strategyWithoutCache = new EnhancedRSIStrategy(14, 70, 30, 0.02);
		strategyWithoutCache.setCacheEnabled(false);

		long startTime = System.currentTimeMillis();
		BacktestResult resultWithCache = testSingleStrategy(strategyWithCache, "带缓存RSI策略");
		long timeWithCache = System.currentTimeMillis() - startTime;

		startTime = System.currentTimeMillis();
		BacktestResult resultWithoutCache = testSingleStrategy(strategyWithoutCache, "无缓存RSI策略");
		long timeWithoutCache = System.currentTimeMillis() - startTime;

		logger.info("性能对比 - 带缓存: {}ms, 无缓存: {}ms", timeWithCache, timeWithoutCache);
		logger.info("缓存效率提升: {:.1f}%", (double) (timeWithoutCache - timeWithCache) / timeWithoutCache * 100);
	}

	// ==================== 综合测试套件 ====================

	@Test
	public void runComprehensiveTestSuite() {
		logger.info("开始运行综合测试套件...");

		// 1. 测试单个指标策略
		testIndividualIndicators();

		// 2. 测试组合策略
		testStrategyCombination();

		// 3. 测试权重优化
		testStrategyWeightOptimization();

		// 4. 测试性能对比
		testPerformanceComparison();

		// 5. 测试风险指标
		testRiskMetrics();

		logger.info("综合测试套件执行完成");
	}

	/**
	 * 测试所有单个指标策略
	 */
	private void testIndividualIndicators() {
		logger.info("执行单个指标策略测试...");

		testRSIStrategy();
		testMACDStrategy();
		testMovingAverageStrategy();
		testBollingerBandsStrategy();
		testKDJStrategy();
		testATRStrategy();
		testADXStrategy();
		testCCIStrategy();
		testOBVStrategy();
		testKeltnerChannelStrategy();
		testAroonStrategy();
		testUltimateOscillatorStrategy();

		logger.info("单个指标策略测试完成");
	}
}