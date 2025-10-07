package com.Quantitative.all.demo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.adaptive.AdaptiveDataFeed;
import com.Quantitative.portfolio.RiskManager;
import com.Quantitative.strategy.indicators.RSIStrategy;

/**
 * RSI策略真实数据测试类 - 使用自适应数据源 自动在网络和本地数据源之间切换，提高数据获取的可靠性
 */
public class RSIStrategyRealTestAdaptive {

	public static void main(String[] args) {
		System.out.println("=== RSI策略真实数据测试开始（自适应数据源）===\n");

		try {
			// 执行完整测试流程
			testRSIStrategyWithAdaptiveData();

		} catch (Exception e) {
			System.err.println("测试执行失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n=== RSI策略真实数据测试结束 ===");
	}

	/**
	 * RSI策略自适应数据源测试
	 */
	public static void testRSIStrategyWithAdaptiveData() {
		System.out.println("步骤1: 创建测试配置");
		BacktestConfig config = createTestConfig();

		System.out.println("步骤2: 创建自适应数据源");
		AdaptiveDataFeed dataFeed = createAdaptiveDataFeed();

		System.out.println("步骤3: 创建RSI策略");
		RSIStrategy strategy = createRSIStrategy();

		System.out.println("步骤4: 创建风险管理");
		RiskManager riskManager = createRiskManager();

		System.out.println("步骤5: 创建回测引擎");
		EventDrivenBacktestEngine engine = createBacktestEngine(dataFeed, config);

		System.out.println("步骤6: 设置组件");
		setupComponents(engine, strategy, riskManager);

		System.out.println("步骤7: 执行回测");
		BacktestResult result = executeBacktest(engine);

		System.out.println("步骤8: 分析结果");
		analyzeResults(result, strategy);

		System.out.println("步骤9: 生成报告");
		generateReport(result, strategy);
	}

	/**
	 * 步骤1: 创建测试配置
	 */
	private static BacktestConfig createTestConfig() {
		BacktestConfig config = new BacktestConfig();

		// 设置基本参数
		config.setSymbol("601398"); // 平安银行
		config.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
		config.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
		config.setInitialCapital(100000.0); // 10万初始资金
		config.setDebugMode(true); // 开启调试模式
		config.setMaxBars(0); // 无限制Bar数量

		// 设置数据源偏好（回测时优先使用本地数据）
		config.setPreferLocalData(true);

		// 设置风险参数
		Map<String, Object> riskParams = new HashMap<>();
		riskParams.put("maxPositionRatio", 0.1); // 单品种最大仓位10%
		riskParams.put("maxDrawdownLimit", 0.15); // 最大回撤15%
		riskParams.put("dailyLossLimit", 0.03); // 单日亏损3%
		riskParams.put("maxConsecutiveLosses", 5); // 最大连续亏损5次
		config.setRiskParams(riskParams);

		System.out.println("✓ 测试配置创建完成");
		System.out.println("  标的: " + config.getSymbol());
		System.out.println("  时间: " + config.getStartDate().toLocalDate() + " 到 " + config.getEndDate().toLocalDate());
		System.out.println("  资金: " + config.getInitialCapital());
		System.out.println("  数据源模式: " + (config.isPreferLocalData() ? "本地优先" : "网络优先"));

		return config;
	}

	/**
	 * 步骤2: 创建自适应数据源
	 */
	private static AdaptiveDataFeed createAdaptiveDataFeed() {
		// 创建自适应数据源
		AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();

		// 初始化数据源
		dataFeed.initialize();

		// 配置数据源参数
		Map<String, Object> feedConfig = new HashMap<>();
		feedConfig.put("cacheEnabled", true);
		feedConfig.put("autoSave", true);
		feedConfig.put("timeframe", "daily"); // 日线数据
		feedConfig.put("adjust", "qfq"); // 前复权
		dataFeed.configure(feedConfig);

		// 设置数据源优先级（回测时优先使用本地数据）
		dataFeed.setDataSourcePriority(Arrays.asList("LOCAL", "NETWORK"));

		// 测试连接
		if (dataFeed.isConnected()) {
			System.out.println("✓ 自适应数据源连接成功");
			System.out.println("  数据源状态: " + dataFeed.getStatus());
			System.out.println("  可用标的: " + dataFeed.getAvailableSymbols().size() + " 个");
		} else {
			System.out.println("⚠ 数据源连接失败，将使用备用数据");
		}

		return dataFeed;
	}

	/**
	 * 步骤3: 创建RSI策略
	 */
	private static RSIStrategy createRSIStrategy() {
		// 创建RSI策略实例
		RSIStrategy strategy = new RSIStrategy(14, 70, 30, 0.02);

		// 配置策略参数
		strategy.setDebugMode(true);
		strategy.setUseCache(true);
		strategy.setSignalThreshold(3.0); // RSI信号阈值

		System.out.println("✓ RSI策略创建完成");
		System.out.println("  周期: " + strategy.getRsiPeriod());
		System.out.println("  超买: " + strategy.getOverbought());
		System.out.println("  超卖: " + strategy.getOversold());
		System.out.println("  仓位: " + (strategy.getPositionSizeRatio() * 100) + "%");

		return strategy;
	}

	/**
	 * 步骤4: 创建风险管理
	 */
	private static RiskManager createRiskManager() {
		RiskManager riskManager = new RiskManager();

		System.out.println("✓ 风险管理器创建完成");

		return riskManager;
	}

	/**
	 * 步骤5: 创建回测引擎
	 */
	private static EventDrivenBacktestEngine createBacktestEngine(AdaptiveDataFeed dataFeed, BacktestConfig config) {
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);

		System.out.println("✓ 回测引擎创建完成");

		return engine;
	}

	/**
	 * 步骤6: 设置组件
	 */
	private static void setupComponents(EventDrivenBacktestEngine engine, RSIStrategy strategy,
			RiskManager riskManager) {
		engine.setStrategy(strategy);
		engine.setRiskManager(riskManager);

		System.out.println("✓ 组件设置完成");
	}

	/**
	 * 步骤7: 执行回测
	 */
	private static BacktestResult executeBacktest(EventDrivenBacktestEngine engine) {
		System.out.println("\n开始执行回测...");

		// 显示数据源信息
		AdaptiveDataFeed dataFeed = (AdaptiveDataFeed) engine.getDataFeed();
		System.out.println("数据源状态: " + dataFeed.getStatus());

		long startTime = System.currentTimeMillis();

		BacktestResult result = engine.runBacktest();

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		System.out.printf("✓ 回测执行完成，耗时: %.2f 秒%n", duration / 1000.0);

		return result;
	}

	/**
	 * 步骤8: 分析结果
	 */
	private static void analyzeResults(BacktestResult result, RSIStrategy strategy) {
		System.out.println("\n=== 回测结果分析 ===");

		// 基础收益分析
		double totalReturn = result.getTotalReturn();
		double annualReturn = result.getAnnualReturn();
		double maxDrawdown = result.getMaxDrawdown();

		System.out.printf("总收益率: %.2f%%%n", totalReturn);
		System.out.printf("年化收益率: %.2f%%%n", annualReturn);
		System.out.printf("最大回撤: %.2f%%%n", maxDrawdown);

		// 交易统计
		int totalTrades = result.getTotalTrades();
		double winRate = result.getWinRate();
		double profitFactor = result.getProfitFactor();

		System.out.printf("总交易次数: %d%n", totalTrades);
		System.out.printf("胜率: %.1f%%%n", winRate);
		System.out.printf("盈亏比: %.2f%n", profitFactor);

		// 风险评估
		double sharpeRatio = result.getSharpeRatio();
		double sortinoRatio = result.getSortinoRatio();

		System.out.printf("夏普比率: %.2f%n", sharpeRatio);
		System.out.printf("索提诺比率: %.2f%n", sortinoRatio);

		// 策略表现评级
		rateStrategyPerformance(result);
	}

	/**
	 * 步骤9: 生成报告
	 */
	private static void generateReport(BacktestResult result, RSIStrategy strategy) {
		System.out.println("\n=== 详细报告 ===");

		// 打印详细结果摘要
		result.printSummary();

		// 可选：打印交易历史
		if (result.getTotalTrades() > 0 && result.getTotalTrades() <= 50) {
			System.out.println("\n显示交易历史（最多50条）:");
			result.printTradeHistory();
		}

		// 生成建议
		generateRecommendations(result, strategy);
	}

	/**
	 * 策略表现评级
	 */
	private static void rateStrategyPerformance(BacktestResult result) {
		double totalReturn = result.getTotalReturn();
		double maxDrawdown = result.getMaxDrawdown();
		double sharpeRatio = result.getSharpeRatio();
		double winRate = result.getWinRate();

		int score = 0;

		if (totalReturn > 20)
			score += 3;
		else if (totalReturn > 10)
			score += 2;
		else if (totalReturn > 0)
			score += 1;

		if (maxDrawdown < 10)
			score += 3;
		else if (maxDrawdown < 15)
			score += 2;
		else if (maxDrawdown < 20)
			score += 1;

		if (sharpeRatio > 1.5)
			score += 2;
		else if (sharpeRatio > 1.0)
			score += 1;

		if (winRate > 60)
			score += 2;
		else if (winRate > 50)
			score += 1;

		String rating;
		if (score >= 8)
			rating = "优秀";
		else if (score >= 6)
			rating = "良好";
		else if (score >= 4)
			rating = "一般";
		else
			rating = "需要优化";

		System.out.printf("策略评级: %s (%d/10)%n", rating, score);
	}

	/**
	 * 生成优化建议
	 */
	private static void generateRecommendations(BacktestResult result, RSIStrategy strategy) {
		System.out.println("\n=== 优化建议 ===");

		double totalReturn = result.getTotalReturn();
		double winRate = result.getWinRate();
		int totalTrades = result.getTotalTrades();

		if (totalTrades == 0) {
			System.out.println("❌ 问题: 策略没有生成任何交易");
			System.out.println("💡 建议: 检查RSI参数是否过于严格，尝试放宽超买超卖阈值");
			return;
		}

		if (totalReturn < 0) {
			System.out.println("❌ 问题: 策略总体亏损");
			System.out.println("💡 建议: 考虑调整RSI周期或添加趋势过滤");
		}

		if (winRate < 40) {
			System.out.println("❌ 问题: 胜率偏低");
			System.out.println("💡 建议: 提高信号阈值，减少无效交易");
		}

		if (totalTrades < 10) {
			System.out.println("⚠ 注意: 交易次数较少");
			System.out.println("💡 建议: 延长回测时间或选择波动性更大的标的");
		}

		if (totalTrades > 100) {
			System.out.println("⚠ 注意: 交易频率较高");
			System.out.println("💡 建议: 考虑增加交易成本的影响分析");
		}

		// RSI特定建议
		System.out.println("\nRSI策略特定建议:");
		System.out.println("1. 尝试不同的RSI周期: 6, 9, 14, 21");
		System.out.println("2. 调整超买超卖阈值: (65,35), (70,30), (75,25)");
		System.out.println("3. 考虑添加移动平均线过滤趋势");
		System.out.println("4. 测试不同仓位的风险收益比");
	}

	/**
	 * 数据同步功能 - 新增方法
	 */
	public static void syncDataForBacktest(String symbol, LocalDateTime start, LocalDateTime end) {
		System.out.println("\n=== 数据同步 ===");

		AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();
		dataFeed.initialize();

		System.out.printf("同步数据: %s (%s 到 %s)%n", symbol, start.toLocalDate(), end.toLocalDate());

		try {
			dataFeed.syncToLocal(symbol, start, end);
			System.out.println("✓ 数据同步完成");
		} catch (Exception e) {
			System.err.println("❌ 数据同步失败: " + e.getMessage());
		} finally {
			dataFeed.shutdown();
		}
	}

	/**
	 * 批量数据同步 - 新增方法
	 */
	public static void batchSyncData(String[] symbols, LocalDateTime start, LocalDateTime end) {
		System.out.println("\n=== 批量数据同步 ===");

		AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();
		dataFeed.initialize();

		System.out.printf("批量同步 %d 个标的: %s 到 %s%n", symbols.length, start.toLocalDate(), end.toLocalDate());

		dataFeed.batchSyncToLocal(Arrays.asList(symbols), start, end);
		dataFeed.shutdown();
	}

	/**
	 * 多参数测试方法（使用自适应数据源）
	 */
	public static void testMultipleParametersWithAdaptiveData() {
		System.out.println("\n=== RSI多参数测试（自适应数据源）===");

		// 测试不同的RSI参数组合
		int[] periods = { 9, 14, 21 };
		double[] overboughtLevels = { 70, 75, 80 };
		double[] oversoldLevels = { 20, 25, 30 };

		for (int period : periods) {
			for (double overbought : overboughtLevels) {
				for (double oversold : oversoldLevels) {
					testSpecificParametersWithAdaptiveData(period, overbought, oversold);
				}
			}
		}
	}

	/**
	 * 测试特定参数组合（使用自适应数据源）
	 */
	private static void testSpecificParametersWithAdaptiveData(int period, double overbought, double oversold) {
		try {
			RSIStrategy strategy = new RSIStrategy(period, overbought, oversold, 0.02);
			AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();
			dataFeed.initialize();
			dataFeed.setDataSourcePriority(Arrays.asList("LOCAL", "NETWORK"));

			BacktestConfig config = createTestConfig();

			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
			engine.setStrategy(strategy);

			BacktestResult result = engine.runBacktest();

			System.out.printf("RSI(%d, %.0f, %.0f) -> 收益率: %.2f%%, 胜率: %.1f%%, 交易: %d%n", period, overbought, oversold,
					result.getTotalReturn(), result.getWinRate(), result.getTotalTrades());

			dataFeed.shutdown();

		} catch (Exception e) {
			System.err.printf("参数测试失败: RSI(%d, %.0f, %.0f) - %s%n", period, overbought, oversold, e.getMessage());
		}
	}

	/**
	 * 网络优先测试 - 新增方法
	 */
	public static void testWithNetworkPriority() {
		System.out.println("\n=== 网络优先模式测试 ===");

		BacktestConfig config = createTestConfig();
		config.setPreferLocalData(false); // 网络优先

		AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();
		dataFeed.initialize();
		dataFeed.setDataSourcePriority(Arrays.asList("NETWORK", "LOCAL")); // 网络优先

		RSIStrategy strategy = createRSIStrategy();
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
		engine.setStrategy(strategy);

		BacktestResult result = engine.runBacktest();
		analyzeResults(result, strategy);

		dataFeed.shutdown();
	}

	/**
	 * 演示自适应数据源的优势 - 新增方法
	 */
	public static void demonstrateAdaptiveDataSource() {
		System.out.println("\n=== 自适应数据源演示 ===");

		// 1. 创建自适应数据源
		AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();
		dataFeed.initialize();

		// 2. 测试本地优先
		System.out.println("1. 本地优先模式:");
		dataFeed.setDataSourcePriority(Arrays.asList("LOCAL", "NETWORK"));
		testDataLoading(dataFeed, "000001");

		// 3. 测试网络优先
		System.out.println("\n2. 网络优先模式:");
		dataFeed.setDataSourcePriority(Arrays.asList("NETWORK", "LOCAL"));
		testDataLoading(dataFeed, "000002");

		// 4. 测试数据同步
		System.out.println("\n3. 数据同步功能:");
		dataFeed.syncToLocal("000001", LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 6, 30, 0, 0));

		dataFeed.shutdown();
	}

	private static void testDataLoading(AdaptiveDataFeed dataFeed, String symbol) {
		try {
			List<BarEvent> data = dataFeed.loadHistoricalData(symbol, LocalDateTime.of(2023, 1, 1, 0, 0),
					LocalDateTime.of(2023, 3, 31, 0, 0));
			System.out.printf("  加载 %s: %d 条数据%n", symbol, data.size());
		} catch (Exception e) {
			System.out.printf("  加载 %s 失败: %s%n", symbol, e.getMessage());
		}
	}
}

/*
 * 使用示例
 * 
 * // 1. 基本回测测试 RSIStrategyRealTest.testRSIStrategyWithAdaptiveData();
 * 
 * // 2. 数据同步（首次使用或更新数据） RSIStrategyRealTest.syncDataForBacktest("601398",
 * LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 12, 31, 0, 0));
 * 
 * // 3. 多参数优化测试 RSIStrategyRealTest.testMultipleParametersWithAdaptiveData();
 * 
 * // 4. 网络优先测试（获取最新数据） RSIStrategyRealTest.testWithNetworkPriority();
 * 
 * // 5. 演示自适应数据源功能 RSIStrategyRealTest.demonstrateAdaptiveDataSource();
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
