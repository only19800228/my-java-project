package com.Quantitative.all.Test.DataFeed;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.strategy.indicators.KDJStrategy;

/**
 * KDJ随机指标策略测试
 */
public class KDJStrategyTest extends BaseStrategyTest {

	@Test
	public void testKDJStrategyBasic() {
		System.out.println("=== KDJ策略基础测试 ===");

		KDJStrategy strategy = new KDJStrategy(9, 3, 80, 20);
		strategy.setDebugMode(true);

		BacktestResult result = runStrategyTest(strategy);
		validateBasicResults(result);

		result.printSummary();
		System.out.println("KDJ策略测试完成");
	}

	@Test
	public void testKDJStrategyParameters() {
		System.out.println("=== KDJ策略参数测试 ===");

		// 测试不同的K、D周期组合
		int[][] parameters = { { 9, 3 }, // 标准参数
				{ 14, 6 }, // 较长周期
				{ 5, 3 } // 较短周期
		};

		for (int[] param : parameters) {
			KDJStrategy strategy = new KDJStrategy(param[0], param[1], 80, 20);

			BacktestResult result = runStrategyTest(strategy);
			validateBasicResults(result);

			System.out.printf("KDJ(%d,%d) 测试: 收益率=%.2f%%, 交易次数=%d%n", param[0], param[1], result.getTotalReturn(),
					result.getTotalTrades());
		}
	}
}