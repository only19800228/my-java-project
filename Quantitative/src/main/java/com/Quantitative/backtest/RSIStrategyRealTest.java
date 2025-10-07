package com.Quantitative.backtest;

import java.time.LocalDateTime;

import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * RSI策略真实数据测试 - 完整示例
 */
public class RSIStrategyRealTest {

	public static void main(String[] args) {
		System.out.println("=== RSI策略真实数据测试开始 ===\n");

		try {
			// 步骤1: 创建测试配置
			System.out.println("步骤1: 创建测试配置");
			BacktestConfig config = new BacktestConfig("000001", LocalDateTime.of(2023, 1, 1, 0, 0),
					LocalDateTime.of(2023, 12, 31, 0, 0), 100000.0);
			System.out.println("? 测试配置创建完成");
			System.out.println("  标的: " + config.getSymbol());
			System.out.println("  时间: " + config.getStartDate() + " 到 " + config.getEndDate());
			System.out.println("  资金: " + config.getInitialCapital());

			// 步骤2: 创建数据源
			System.out.println("\n步骤2: 创建数据源");
			AKShareDataFeed dataFeed = new AKShareDataFeed();
			dataFeed.setDebugMode(true);
			dataFeed.setParameter("timeframe", "daily");
			dataFeed.setParameter("adjust", "qfq");

			// 测试连接
			boolean connected = dataFeed.testConnection();
			if (!connected) {
				System.out.println("? 警告: 数据源连接失败，使用模拟数据");
			} else {
				System.out.println("? 数据源连接成功");
			}

			// 步骤3: 创建RSI策略
			System.out.println("\n步骤3: 创建RSI策略");
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02);
			strategy.setDebugMode(true);
			System.out.println("? RSI策略创建完成");
			System.out.println("  周期: " + strategy.getRsiPeriod());
			System.out.println("  超买: " + strategy.getOverbought());
			System.out.println("  超卖: " + strategy.getOversold());
			System.out.println("  仓位: " + (strategy.getPositionSizeRatio() * 100) + "%");

			// 步骤4: 创建回测引擎
			System.out.println("\n步骤4: 创建回测引擎");
			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
			engine.setStrategy(strategy);
			System.out.println("? 回测引擎创建完成");

			// 步骤5: 执行回测
			System.out.println("\n步骤5: 执行回测");
			long startTime = System.currentTimeMillis();
			BacktestResult result = engine.runBacktest();
			long endTime = System.currentTimeMillis();

			System.out.println("? 回测执行完成，耗时: " + (endTime - startTime) / 1000.0 + " 秒");

			// 步骤6: 分析结果
			System.out.println("\n步骤6: 分析结果");
			result.calculateAdvancedMetrics();
			result.printSummary();

			// 步骤7: 生成详细报告
			System.out.println("\n步骤7: 生成详细报告");
			if (result.getTotalTrades() > 0) {
				result.printTradeHistory();
			}

			// 性能分析
			System.out.println("\n=== 性能分析 ===");
			System.out.println("总交易次数: " + result.getTotalTrades());
			System.out.println("胜率: " + String.format("%.1f%%", result.getWinRate()));
			System.out.println("盈亏比: " + String.format("%.2f", result.getProfitFactor()));
			System.out.println("最大回撤: " + String.format("%.2f%%", result.getMaxDrawdown()));

			// 策略评级
			System.out.println("\n=== 策略评级 ===");
			double rating = calculateStrategyRating(result);
			System.out.println("策略评级: " + getRatingDescription(rating));

		} catch (Exception e) {
			System.err.println("测试失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n=== RSI策略真实数据测试结束 ===");
	}

	/**
	 * 计算策略评级 (0-10分)
	 */
	private static double calculateStrategyRating(BacktestResult result) {
		double rating = 5.0; // 基础分

		// 收益率加分
		if (result.getTotalReturn() > 0) {
			rating += Math.min(result.getTotalReturn() / 10, 3);
		} else {
			rating -= Math.min(Math.abs(result.getTotalReturn()) / 5, 3);
		}

		// 胜率加分
		rating += Math.min(result.getWinRate() / 20, 2);

		// 盈亏比加分
		if (result.getProfitFactor() > 1) {
			rating += Math.min(result.getProfitFactor() - 1, 2);
		}

		// 回撤扣分
		if (result.getMaxDrawdown() > 10) {
			rating -= Math.min((result.getMaxDrawdown() - 10) / 5, 2);
		}

		return Math.max(0, Math.min(10, rating));
	}

	/**
	 * 获取评级描述
	 */
	private static String getRatingDescription(double rating) {
		if (rating >= 9)
			return "优秀 (10/10)";
		if (rating >= 8)
			return "很好 (9/10)";
		if (rating >= 7)
			return "良好 (8/10)";
		if (rating >= 6)
			return "一般 (7/10)";
		if (rating >= 5)
			return "需要优化 (6/10)";
		return "需要大幅优化 (" + (int) rating + "/10)";
	}
}