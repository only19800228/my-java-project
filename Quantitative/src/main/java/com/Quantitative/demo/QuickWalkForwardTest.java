// 新增：快速Walk-Forward测试
package com.Quantitative.demo;

import java.time.LocalDateTime;

import com.Quantitative.backtest.advanced.WalkForwardAnalyzer;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 快速Walk-Forward测试（使用固定参数）
 */
public class QuickWalkForwardTest {

	public static void main(String[] args) {
		System.out.println("⚡ 快速Walk-Forward测试开始\n");

		try {
			// 使用更小的数据范围和固定参数加快测试
			AKShareDataFeed dataFeed = new AKShareDataFeed();
			EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();

			WalkForwardAnalyzer.WalkForwardConfig wfConfig = new WalkForwardAnalyzer.WalkForwardConfig();
			wfConfig.setSymbol("000001");
			wfConfig.setStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
			wfConfig.setEndDate(LocalDateTime.of(2022, 12, 31, 0, 0));
			wfConfig.setTrainingMonths(6); // 6个月训练
			wfConfig.setTestingMonths(3); // 3个月测试
			wfConfig.setRollingStepMonths(3); // 3个月滚动
			wfConfig.setOptimizationMethod(WalkForwardAnalyzer.WalkForwardConfig.OptimizationMethod.FIXED_PARAMS);

			WalkForwardAnalyzer analyzer = new WalkForwardAnalyzer(dataFeed, wfConfig);
			analyzer.setStrategy(strategy);

			WalkForwardAnalyzer.WalkForwardResult result = analyzer.analyze();
			result.printReport();

		} catch (Exception e) {
			System.err.println("❌ 快速测试失败: " + e.getMessage());
			// 继续执行，不中断
		}
	}
}