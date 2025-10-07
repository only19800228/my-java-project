// 新增：Walk-Forward测试类
package com.Quantitative.demo;

import java.time.LocalDateTime;

import com.Quantitative.backtest.advanced.WalkForwardAnalyzer;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * Walk-Forward分析测试
 */
public class WalkForwardTest {

	public static void main(String[] args) {
		System.out.println("🎯 Walk-Forward分析测试开始\n");

		try {
			// 1. 创建数据源
			AKShareDataFeed dataFeed = new AKShareDataFeed();
			dataFeed.setDebugMode(false);

			// 2. 创建策略
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();
			strategy.setDebugMode(false);

			// 3. 配置Walk-Forward分析
			WalkForwardAnalyzer.WalkForwardConfig wfConfig = new WalkForwardAnalyzer.WalkForwardConfig();
			wfConfig.setSymbol("000001");
			wfConfig.setStartDate(LocalDateTime.of(2018, 1, 1, 0, 0));
			wfConfig.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
			wfConfig.setTrainingMonths(12); // 1年训练
			wfConfig.setTestingMonths(3); // 3个月测试
			wfConfig.setRollingStepMonths(3); // 每3个月滚动一次
			wfConfig.setOptimizationMethod(WalkForwardAnalyzer.WalkForwardConfig.OptimizationMethod.GRID_SEARCH);

			// 4. 创建分析器
			WalkForwardAnalyzer analyzer = new WalkForwardAnalyzer(dataFeed, wfConfig);
			analyzer.setStrategy(strategy);

			// 5. 执行分析
			WalkForwardAnalyzer.WalkForwardResult result = analyzer.analyze();

			// 6. 显示结果
			result.printReport();

			// 7. 保存结果（可选）
			saveResults(result);

		} catch (Exception e) {
			System.err.println("❌ Walk-Forward测试失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n🎯 Walk-Forward分析测试结束");
	}

	private static void saveResults(WalkForwardAnalyzer.WalkForwardResult result) {
		// 这里可以添加结果保存逻辑
		System.out.println("\n💾 结果保存功能待实现...");
	}
}