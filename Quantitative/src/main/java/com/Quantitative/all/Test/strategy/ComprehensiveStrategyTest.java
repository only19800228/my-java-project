package com.Quantitative.all.Test.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.Quantitative.backtest.BacktestResult;
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
 * 综合策略比较测试
 */
public class ComprehensiveStrategyTest extends BaseStrategyTest {

	@Test
	public void testAllStrategiesComparison() {
		List<StrategyTestResult> results = new ArrayList<>();

		// 测试所有策略
		BaseStrategy[] strategies = { new ADXStrategy(14, 25.0, 20.0, 20.0), new AroonStrategy(14, 70.0),
				new ATRStrategy(14, 2.0, 0.02), new BollingerBandsStrategy(20, 2.0, true, 0.1),
				new CCIStrategy(14, 100, -100), new EnhancedRSIStrategy(14, 70.0, 30.0, 0.02),
				new KDJStrategy(9, 3, 80, 20), new KeltnerChannelStrategy(20, 10, 2.0),
				new MACDStrategy(12, 26, 9, false), new MovingAverageStrategy(10, 30, 5, 1.0), new OBVStrategy(20, 1.2),
				new UltimateOscillatorStrategy(7, 14, 28, 70.0, 30.0) };

		for (BaseStrategy strategy : strategies) {
			System.out.println("测试策略: " + strategy.getName());

			BacktestResult result = runStrategyTest(strategy);
			results.add(new StrategyTestResult(strategy.getName(), result));
		}

		// 打印比较结果
		printStrategyComparison(results);
	}

	private void printStrategyComparison(List<StrategyTestResult> results) {
		// System.out.println("\n" + "=".repeat(100));
		System.out.println("\n" + String.join("", Collections.nCopies(100, "=")));
		System.out.println("策略性能比较报告");
		// System.out.println("=".repeat(100));
		System.out.println(String.join("", Collections.nCopies(100, "=")));
		System.out.printf("%-20s | %8s | %8s | %8s | %8s | %8s%n", "策略名称", "收益率%", "夏普比率", "最大回撤%", "交易次数", "胜率%");
		// System.out.println("-".repeat(100));
		System.out.println(String.join("", Collections.nCopies(100, "-")));
		for (StrategyTestResult result : results) {
			System.out.printf("%-20s | %8.2f | %8.2f | %8.2f | %8d | %8.1f%n", result.strategyName, result.totalReturn,
					result.sharpeRatio, result.maxDrawdown, result.totalTrades, result.winRate);
		}
		// System.out.println("=".repeat(100));
		System.out.println(String.join("", Collections.nCopies(100, "=")));
	}

	private static class StrategyTestResult {
		String strategyName;
		double totalReturn;
		double sharpeRatio;
		double maxDrawdown;
		int totalTrades;
		double winRate;

		StrategyTestResult(String name, BacktestResult result) {
			this.strategyName = name;
			this.totalReturn = result.getTotalReturn();
			this.sharpeRatio = result.getSharpeRatio();
			this.maxDrawdown = result.getMaxDrawdown();
			this.totalTrades = result.getTotalTrades();
			this.winRate = result.getWinRate();
		}
	}
}