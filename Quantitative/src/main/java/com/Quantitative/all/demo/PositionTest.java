package com.Quantitative.all.demo;

import com.Quantitative.portfolio.Position;

public class PositionTest {
	public static void main(String[] args) {
		System.out.println("=== Position 类完整测试 ===\n");

		// 测试1: 基础买入卖出
		testBasicOperations();

		// 测试2: 多次买入平均成本
		testAverageCost();

		// 测试3: 盈亏计算
		testProfitCalculation();

		// 测试4: 完整交易流程
		testCompleteTrading();

		System.out.println("\n✓ 所有测试通过");
	}

	/**
	 * 测试1: 基础买入卖出
	 */
	private static void testBasicOperations() {
		System.out.println("测试1: 基础买入卖出");
		Position position = new Position("000001");

		// 买入1000股 @10.0
		position.addBuy(1000, 10.0);
		position.updateMarketValue(10.0); // 更新市值
		System.out.printf("买入后: %s%n", position);
		assert position.getQuantity() == 1000 : "持仓数量错误";
		assert position.getAvgCost() == 10.0 : "平均成本错误";
		assert position.getUnrealizedPnl() == 0.0 : "未实现盈亏应该为0";

		// 卖出500股 @11.0
		position.addSell(500, 11.0);
		position.updateMarketValue(11.0); // 更新市值
		System.out.printf("卖出后: %s%n", position);
		assert position.getQuantity() == 500 : "卖出后持仓数量错误";
		assert position.getAvgCost() == 10.0 : "卖出后平均成本应该不变";

		// 检查盈亏计算
		double expectedMarketValue = 500 * 11.0; // 5500
		double expectedPnl = expectedMarketValue - (500 * 10.0); // 500
		double expectedPnlPercent = (expectedPnl / (500 * 10.0)) * 100; // 10%

		System.out.printf("预期市值: %.2f, 实际市值: %.2f%n", expectedMarketValue, position.getMarketValue());
		System.out.printf("预期盈亏: %.2f, 实际盈亏: %.2f%n", expectedPnl, position.getUnrealizedPnl());
		System.out.printf("预期盈亏%%: %.2f%%, 实际盈亏%%: %.2f%%%n", expectedPnlPercent, position.getUnrealizedPnlPercent());

		assert Math.abs(position.getMarketValue() - expectedMarketValue) < 0.01 : "市值计算错误";
		assert Math.abs(position.getUnrealizedPnl() - expectedPnl) < 0.01 : "盈亏计算错误";
		assert Math.abs(position.getUnrealizedPnlPercent() - expectedPnlPercent) < 0.01 : "盈亏百分比计算错误";

		System.out.println("✓ 基础操作测试通过\n");
	}

	/**
	 * 测试2: 多次买入平均成本计算
	 */
	private static void testAverageCost() {
		System.out.println("测试2: 多次买入平均成本");
		Position position = new Position("000002");

		// 第一次买入：1000股 @10.0
		position.addBuy(1000, 10.0);
		System.out.printf("第一次买入后: 数量=%,d, 成本=%.4f%n", position.getQuantity(), position.getAvgCost());

		// 第二次买入：500股 @12.0
		position.addBuy(500, 12.0);
		System.out.printf("第二次买入后: 数量=%,d, 成本=%.4f%n", position.getQuantity(), position.getAvgCost());

		// 计算预期平均成本
		double expectedAvgCost = (1000 * 10.0 + 500 * 12.0) / 1500; // 10.6667
		System.out.printf("预期平均成本: %.4f, 实际平均成本: %.4f%n", expectedAvgCost, position.getAvgCost());

		assert Math.abs(position.getAvgCost() - expectedAvgCost) < 0.001 : "平均成本计算错误";
		assert position.getQuantity() == 1500 : "总持仓数量错误";

		System.out.println("✓ 平均成本测试通过\n");
	}

	/**
	 * 测试3: 盈亏计算
	 */
	private static void testProfitCalculation() {
		System.out.println("测试3: 盈亏计算");
		Position position = new Position("000003");

		// 建立持仓
		position.addBuy(1000, 10.0);
		position.updateMarketValue(10.0);
		System.out.printf("建仓后盈亏: %.2f(%.2f%%)%n", position.getUnrealizedPnl(), position.getUnrealizedPnlPercent());

		// 价格上涨
		position.updateMarketValue(12.0);
		System.out.printf("价格上涨后: %s%n", position);

		double expectedPnl = 1000 * (12.0 - 10.0); // 2000
		double expectedPnlPercent = (2000.0 / (1000 * 10.0)) * 100; // 20%

		assert Math.abs(position.getUnrealizedPnl() - expectedPnl) < 0.01 : "盈利计算错误";
		assert Math.abs(position.getUnrealizedPnlPercent() - expectedPnlPercent) < 0.01 : "盈利百分比计算错误";

		// 价格下跌
		position.updateMarketValue(9.0);
		System.out.printf("价格下跌后: %s%n", position);

		expectedPnl = 1000 * (9.0 - 10.0); // -1000
		expectedPnlPercent = (-1000.0 / (1000 * 10.0)) * 100; // -10%

		assert Math.abs(position.getUnrealizedPnl() - expectedPnl) < 0.01 : "亏损计算错误";
		assert Math.abs(position.getUnrealizedPnlPercent() - expectedPnlPercent) < 0.01 : "亏损百分比计算错误";

		System.out.println("✓ 盈亏计算测试通过\n");
	}

	/**
	 * 测试4: 完整交易流程
	 */
	private static void testCompleteTrading() {
		System.out.println("测试4: 完整交易流程");
		Position position = new Position("000004");

		// 完整交易流程
		System.out.println("1. 第一次买入: 800股 @15.0");
		position.addBuy(800, 15.0);
		position.updateMarketValue(15.0);
		System.out.printf("   当前状态: %s%n", position);

		System.out.println("2. 第二次买入: 200股 @16.0");
		position.addBuy(200, 16.0);
		position.updateMarketValue(16.0);
		System.out.printf("   当前状态: %s%n", position);

		System.out.println("3. 价格涨到18.0");
		position.updateMarketValue(18.0);
		System.out.printf("   当前状态: %s%n", position);

		System.out.println("4. 卖出500股 @18.0");
		position.addSell(500, 18.0);
		position.updateMarketValue(18.0);
		System.out.printf("   当前状态: %s%n", position);

		System.out.println("5. 价格跌到17.0");
		position.updateMarketValue(17.0);
		System.out.printf("   最终状态: %s%n", position);

		// 验证最终状态
		assert position.getQuantity() == 500 : "最终持仓数量错误";

		// 平均成本应该是 (800*15.0 + 200*16.0) / 1000 = 15.2
		// 卖出500股后，剩下的500股平均成本还是15.2
		double expectedAvgCost = (800 * 15.0 + 200 * 16.0) / 1000;
		assert Math.abs(position.getAvgCost() - expectedAvgCost) < 0.001 : "最终平均成本错误";

		System.out.println("✓ 完整交易流程测试通过\n");
	}
}