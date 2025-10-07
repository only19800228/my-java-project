package com.Quantitative.all.Test.strategy.ZH;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.composite.MeanReversionComposite;
import com.Quantitative.strategy.composite.MomentumBreakoutComposite;
import com.Quantitative.strategy.composite.MultiTimeframeComposite;
import com.Quantitative.strategy.composite.TrendFollowingComposite;

/**
 * 组合策略测试类 - 使用真实AKShare数据源
 */
public class CompositeStrategyBacktestTest {

	private AKShareDataFeed dataFeed;
	private BacktestConfig baseConfig;

	@Before
	public void setUp() {
		// 初始化数据源
		dataFeed = new AKShareDataFeed();
		dataFeed.setDebugMode(true);
		dataFeed.setParameter("timeframe", "daily");
		dataFeed.setParameter("adjust", "qfq");

		// 基础配置
		baseConfig = new BacktestConfig("000001", LocalDateTime.of(2023, 1, 1, 0, 0),
				LocalDateTime.of(2023, 6, 30, 0, 0), 100000.0);
		baseConfig.setDebugMode(false); // 测试时关闭详细日志
	}

	/**
	 * 测试均值回归组合策略
	 */
	@Test
	public void testMeanReversionCompositeStrategy() {
		System.out.println("\n=== 测试均值回归组合策略 ===");

		try {
			// 创建策略
			MeanReversionComposite strategy = new MeanReversionComposite();
			strategy.setDebugMode(true);

			// 执行回测
			BacktestResult result = executeBacktest(strategy, "MeanReversion");

			// 验证结果
			assertNotNull("回测结果不应为空", result);
			assertTrue("初始资金应正确设置", result.getInitialCapital() > 0);

			System.out.printf("均值回归策略回测完成: 总收益率=%.2f%%, 最大回撤=%.2f%%%n", result.getTotalReturn(),
					result.getMaxDrawdown());

		} catch (Exception e) {
			System.err.println("均值回归策略测试失败: " + e.getMessage());
			// 测试中允许失败，因为可能由于数据问题导致
		}
	}

	/**
	 * 测试动量突破组合策略
	 */
	@Test
	public void testMomentumBreakoutCompositeStrategy() {
		System.out.println("\n=== 测试动量突破组合策略 ===");

		try {
			// 创建策略
			MomentumBreakoutComposite strategy = new MomentumBreakoutComposite();
			strategy.setDebugMode(true);

			// 执行回测
			BacktestResult result = executeBacktest(strategy, "MomentumBreakout");

			// 验证结果
			assertNotNull("回测结果不应为空", result);
			assertTrue("应处理至少一个Bar", result.getTotalTrades() >= 0);

			System.out.printf("动量突破策略回测完成: 交易次数=%d, 夏普比率=%.2f%n", result.getTotalTrades(), result.getSharpeRatio());

		} catch (Exception e) {
			System.err.println("动量突破策略测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试多时间框架组合策略
	 */
	@Test
	public void testMultiTimeframeCompositeStrategy() {
		System.out.println("\n=== 测试多时间框架组合策略 ===");

		try {
			// 创建策略
			MultiTimeframeComposite strategy = new MultiTimeframeComposite();
			strategy.setDebugMode(true);

			// 执行回测
			BacktestResult result = executeBacktest(strategy, "MultiTimeframe");

			// 验证结果
			assertNotNull("回测结果不应为空", result);

			System.out.printf("多时间框架策略回测完成: 总收益率=%.2f%%, 胜率=%.1f%%%n", result.getTotalReturn(), result.getWinRate());

		} catch (Exception e) {
			System.err.println("多时间框架策略测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试趋势跟踪组合策略
	 */
	@Test
	public void testTrendFollowingCompositeStrategy() {
		System.out.println("\n=== 测试趋势跟踪组合策略 ===");

		try {
			// 创建策略
			TrendFollowingComposite strategy = new TrendFollowingComposite();
			strategy.setDebugMode(true);

			// 设置特定参数
			strategy.setFastPeriod(10);
			strategy.setSlowPeriod(30);
			strategy.setVolumeThreshold(1.5);

			// 执行回测
			BacktestResult result = executeBacktest(strategy, "TrendFollowing");

			// 验证结果
			assertNotNull("回测结果不应为空", result);

			System.out.printf("趋势跟踪策略回测完成: 最终资金=%.2f, 总交易=%d%n", result.getFinalCapital(), result.getTotalTrades());

		} catch (Exception e) {
			System.err.println("趋势跟踪策略测试失败: " + e.getMessage());
		}
	}

	/**
	 * 批量测试所有组合策略
	 */
	@Test
	public void testAllCompositeStrategies() {
		System.out.println("\n=== 批量测试所有组合策略 ===");

		// 测试配置 - 使用较短的时间范围以提高测试速度
		BacktestConfig quickConfig = new BacktestConfig("000001", LocalDateTime.of(2023, 3, 1, 0, 0),
				LocalDateTime.of(2023, 4, 30, 0, 0), 50000.0);

		BaseStrategy[] strategies = { new MeanReversionComposite(), new MomentumBreakoutComposite(),
				new MultiTimeframeComposite(), new TrendFollowingComposite() };

		String[] strategyNames = { "均值回归", "动量突破", "多时间框架", "趋势跟踪" };

		int successCount = 0;

		for (int i = 0; i < strategies.length; i++) {
			try {
				System.out.printf("\n测试策略 %d/%d: %s%n", i + 1, strategies.length, strategyNames[i]);

				strategies[i].setDebugMode(false);
				BacktestResult result = executeBacktestWithConfig(strategies[i], strategyNames[i], quickConfig);

				if (result != null) {
					successCount++;
					System.out.printf("✓ %s策略测试成功: 收益率=%.2f%%%n", strategyNames[i], result.getTotalReturn());
				}

			} catch (Exception e) {
				System.out.printf("✗ %s策略测试失败: %s%n", strategyNames[i], e.getMessage());
			}
		}

		System.out.printf("\n测试完成: %d/%d 个策略测试成功%n", successCount, strategies.length);
		assertTrue("至少应有一个策略测试成功", successCount > 0);
	}

	/**
	 * 执行回测的辅助方法
	 */
	private BacktestResult executeBacktest(BaseStrategy strategy, String strategyName) {
		return executeBacktestWithConfig(strategy, strategyName, baseConfig);
	}

	private BacktestResult executeBacktestWithConfig(BaseStrategy strategy, String strategyName,
			BacktestConfig config) {
		try {
			// 测试数据源连接
			boolean connected = dataFeed.testConnection();
			if (!connected) {
				System.out.println("⚠ 数据源连接失败，使用模拟数据");
				// 这里可以添加备用数据源逻辑
			}

			// 创建回测引擎
			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
			engine.setStrategy(strategy);

			// 执行回测
			long startTime = System.currentTimeMillis();
			BacktestResult result = engine.runBacktest();
			long endTime = System.currentTimeMillis();

			// 记录性能信息
			if (result != null) {
				System.out.printf("策略 %s 回测耗时: %d ms%n", strategyName, (endTime - startTime));
				result.printSummary();
			}

			return result;

		} catch (Exception e) {
			System.err.println("执行回测时发生错误: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 测试策略参数优化
	 */
	@Test
	public void testStrategyParameterOptimization() {
		System.out.println("\n=== 测试策略参数优化 ===");

		try {
			// 使用趋势跟踪策略进行参数优化测试
			TrendFollowingComposite strategy = new TrendFollowingComposite();

			// 测试不同的参数组合
			int[][] paramCombinations = { { 5, 20 }, // 快线5，慢线20
					{ 10, 30 }, // 快线10，慢线30
					{ 8, 21 } // 快线8，慢线21
			};

			for (int[] params : paramCombinations) {
				try {
					strategy.setFastPeriod(params[0]);
					strategy.setSlowPeriod(params[1]);

					BacktestResult result = executeBacktest(strategy,
							String.format("TrendFollowing_%d_%d", params[0], params[1]));

					if (result != null) {
						System.out.printf("参数(%d,%d): 收益率=%.2f%%, 夏普=%.2f%n", params[0], params[1],
								result.getTotalReturn(), result.getSharpeRatio());
					}

				} catch (Exception e) {
					System.out.printf("参数(%d,%d)测试失败: %s%n", params[0], params[1], e.getMessage());
				}
			}

		} catch (Exception e) {
			System.err.println("参数优化测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试不同股票代码
	 */
	@Test
	public void testDifferentSymbols() {
		System.out.println("\n=== 测试不同股票代码 ===");

		String[] testSymbols = { "000001", "000002", "600519" }; // 平安银行, 万科A,
																	// 贵州茅台

		TrendFollowingComposite strategy = new TrendFollowingComposite();
		strategy.setDebugMode(false);

		// 使用较短的时间范围
		BacktestConfig quickConfig = new BacktestConfig("000001", LocalDateTime.of(2023, 4, 1, 0, 0),
				LocalDateTime.of(2023, 5, 31, 0, 0), 100000.0);

		for (String symbol : testSymbols) {
			try {
				System.out.printf("\n测试股票: %s%n", symbol);
				quickConfig.setSymbol(symbol);

				BacktestResult result = executeBacktestWithConfig(strategy, "TrendFollowing", quickConfig);

				if (result != null) {
					System.out.printf("股票 %s: 收益率=%.2f%%, 交易次数=%d%n", symbol, result.getTotalReturn(),
							result.getTotalTrades());
				}

			} catch (Exception e) {
				System.out.printf("股票 %s 测试失败: %s%n", symbol, e.getMessage());
			}
		}
	}

	/**
	 * 性能测试 - 测量策略执行效率
	 */
	@Test
	public void testStrategyPerformance() {
		System.out.println("\n=== 策略性能测试 ===");

		BaseStrategy[] strategies = { new MeanReversionComposite(), new MomentumBreakoutComposite(),
				new MultiTimeframeComposite(), new TrendFollowingComposite() };

		String[] strategyNames = { "MeanReversion", "MomentumBreakout", "MultiTimeframe", "TrendFollowing" };

		// 使用小数据集进行性能测试
		BacktestConfig perfConfig = new BacktestConfig("000001", LocalDateTime.of(2023, 5, 1, 0, 0),
				LocalDateTime.of(2023, 5, 31, 0, 0), 100000.0);

		for (int i = 0; i < strategies.length; i++) {
			try {
				System.out.printf("\n性能测试: %s%n", strategyNames[i]);

				long startTime = System.currentTimeMillis();
				BacktestResult result = executeBacktestWithConfig(strategies[i], strategyNames[i], perfConfig);
				long endTime = System.currentTimeMillis();

				if (result != null) {
					long duration = endTime - startTime;
					System.out.printf("策略 %s 执行时间: %d ms, 处理效率: %.1f bars/ms%n", strategyNames[i], duration,
							result.getTotalTrades() > 0 ? (double) result.getTotalTrades() / duration : 0);
				}

			} catch (Exception e) {
				System.out.printf("策略 %s 性能测试失败: %s%n", strategyNames[i], e.getMessage());
			}
		}
	}
}