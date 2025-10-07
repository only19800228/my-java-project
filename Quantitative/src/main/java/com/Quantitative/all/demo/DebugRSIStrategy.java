package com.Quantitative.all.demo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.portfolio.composite.StrategyCompositeManager;
import com.Quantitative.portfolio.composite.StrategyCompositeManager.CompositeBacktestResult;
import com.Quantitative.portfolio.composite.StrategyCompositeManager.WeightOptimizationResult;
import com.Quantitative.strategy.composite.MeanReversionComposite;
import com.Quantitative.strategy.composite.MomentumBreakoutComposite;
import com.Quantitative.strategy.composite.MultiTimeframeComposite;
import com.Quantitative.strategy.composite.TrendFollowingComposite;

/**
 * 详细调试RSI策略
 */
public class DebugRSIStrategy {

	public static void main(String[] args) {
		System.out.println("=== 详细调试RSI策略 ===\n");
		System.out.println("步骤1: 创建测试配置");
		BacktestConfig config = createTestConfig();

		System.out.println("步骤2: 创建数据源");
		AKShareDataFeed dataFeed = createDataFeed();

		// 创建组合策略管理器
		StrategyCompositeManager compositeManager = new StrategyCompositeManager(dataFeed, config);

		// 添加各种组合策略
		compositeManager.addStrategy("趋势跟踪", new TrendFollowingComposite(), 0.3);
		compositeManager.addStrategy("均值回归", new MeanReversionComposite(), 0.25);
		compositeManager.addStrategy("动量突破", new MomentumBreakoutComposite(), 0.25);
		compositeManager.addStrategy("多时间框架", new MultiTimeframeComposite(), 0.2);

		// 执行组合回测
		CompositeBacktestResult result = compositeManager.runCompositeBacktest();

		// 优化权重
		WeightOptimizationResult optimized = compositeManager.optimizeWeights();
	}

	/**
	 * 创建测试配置
	 */
	private static BacktestConfig createTestConfig() {
		System.out.println("? 创建回测配置...");

		BacktestConfig config = new BacktestConfig();

		// 设置回测参数
		config.setSymbol("000001"); // 测试用平安银行
		config.setStartDate(LocalDateTime.now().minusMonths(6));
		config.setEndDate(LocalDateTime.now());
		config.setInitialCapital(100000.0);
		config.setDebugMode(true);
		config.setMaxBars(1000);
		config.setPreferLocalData(true);
		config.setSlowMode(false);

		// 设置风险参数
		config.addRiskParam("commissionRate", 0.0003); // 万三手续费
		config.addRiskParam("slippage", 0.001); // 0.1%滑点
		config.addRiskParam("maxPositionRatio", 0.1); // 单票最大仓位10%
		config.addRiskParam("stopLoss", 0.05); // 5%止损
		config.addRiskParam("takeProfit", 0.15); // 15%止盈

		System.out.println("? 测试配置创建完成: " + config.toString());
		return config;
	}

	/**
	 * 创建数据源
	 */
	private static AKShareDataFeed createDataFeed() {
		System.out.println("? 创建AKShare数据源...");

		AKShareDataFeed dataFeed = new AKShareDataFeed();

		// 配置数据源参数
		Map<String, Object> feedConfig = new HashMap<>();
		feedConfig.put("timeframe", "daily");
		feedConfig.put("adjust", "qfq"); // 前复权
		feedConfig.put("maxRetry", 3);
		feedConfig.put("timeout", 30000);

		dataFeed.configure(feedConfig);
		dataFeed.setDebugMode(true); // 开启调试模式

		// 测试连接
		boolean connected = dataFeed.testConnection();
		System.out.println("? 数据源连接测试: " + (connected ? "成功" : "失败"));

		// 初始化数据源
		dataFeed.initialize();

		System.out.println("? 数据源创建完成，状态: " + dataFeed.getStatus());

		return dataFeed;
	}
}