// 修复后的测试类
package com.Quantitative.demo;

import java.time.LocalDateTime;

import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.portfolio.composite.StrategyCompositeManager;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 策略组合管理测试
 */
public class StrategyCompositeTest {

	public static void main(String[] args) {
		System.out.println("🎯 策略组合管理测试开始\n");

		try {
			// 1. 创建基础组件
			AKShareDataFeed dataFeed = new AKShareDataFeed();

			BacktestConfig baseConfig = new BacktestConfig();
			baseConfig.setSymbol("000001");
			baseConfig.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
			baseConfig.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
			baseConfig.setInitialCapital(100000.0);

			// 2. 创建策略组合管理器
			StrategyCompositeManager compositeManager = new StrategyCompositeManager(dataFeed, baseConfig);

			// 3. 添加多个策略到组合
			EnhancedRSIStrategy rsi1 = new EnhancedRSIStrategy(14, 70, 30, 0.02);
			EnhancedRSIStrategy rsi2 = new EnhancedRSIStrategy(9, 75, 25, 0.015);
			EnhancedRSIStrategy rsi3 = new EnhancedRSIStrategy(21, 65, 35, 0.025);

			compositeManager.addStrategy("RSI-14-70-30", rsi1, 0.4); // 40%权重
			compositeManager.addStrategy("RSI-9-75-25", rsi2, 0.3); // 30%权重
			compositeManager.addStrategy("RSI-21-65-35", rsi3, 0.3); // 30%权重

			// 4. 配置组合参数
			StrategyCompositeManager.CompositeConfig compConfig = new StrategyCompositeManager.CompositeConfig();
			compConfig.setWeightOptimizationMethod(
					StrategyCompositeManager.CompositeConfig.WeightOptimizationMethod.RISK_PARITY);
			compositeManager.setCompositeConfig(compConfig);

			// 5. 执行组合回测
			StrategyCompositeManager.CompositeBacktestResult result = compositeManager.runCompositeBacktest();
			result.printReport();

			// 6. 优化权重（可选）
			System.out.println("\n尝试优化策略权重...");
			StrategyCompositeManager.WeightOptimizationResult weightResult = compositeManager.optimizeWeights();

			// 7. 使用优化后的权重重新回测
			System.out.println("\n使用优化权重重新回测...");
			StrategyCompositeManager.CompositeBacktestResult optimizedResult = compositeManager.runCompositeBacktest();
			optimizedResult.printReport();

		} catch (Exception e) {
			System.err.println("❌ 策略组合测试失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n🎯 策略组合管理测试结束");
	}
}