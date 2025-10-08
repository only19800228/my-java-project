package com.Quantitative;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.data.csv.CSVDataSource;

public class DirectDataSourceTest {

	public static void testDirectDataSources() {
		System.out.println("=== 直接数据源测试 ===");

		// 测试1: 直接使用AKShareDataFeed
		System.out.println("\n1. 直接AKShareDataFeed测试:");
		testAKShareDataFeedDirectly();

		// 测试2: 直接CSV写入测试
		System.out.println("\n2. 直接CSV写入测试:");
		testCSVWriteDirectly();
	}

	private static void testAKShareDataFeedDirectly() {
		try {
			AKShareDataFeed dataFeed = new AKShareDataFeed();
			dataFeed.loadHistoricalData("601398", LocalDateTime.of(2023, 1, 1, 0, 0),
					LocalDateTime.of(2023, 6, 10, 0, 0));
			List<BarEvent> bars = dataFeed.getAllBars();

			System.out.println("AKShareDataFeed直接加载:");
			System.out.println("记录数: " + bars.size());
			if (!bars.isEmpty()) {
				BarEvent firstBar = bars.get(0);
				System.out.printf("首条数据: %s C=%.2f%n", firstBar.getTimestamp(), firstBar.getClose());
			}

		} catch (Exception e) {
			System.out.println("直接AKShare测试失败: " + e.getMessage());
		}
	}

	private static void testCSVWriteDirectly() {
		try {
			CSVDataSource csvDataSource = new CSVDataSource();

			// 创建测试数据（正确价格）
			List<BarEvent> testBars = Arrays.asList(
					new BarEvent(LocalDateTime.of(2014, 1, 2, 0, 0), "601398", 3.60, 3.70, 3.40, 3.60, 403770, 0),
					new BarEvent(LocalDateTime.of(2014, 1, 3, 0, 0), "601398", 3.50, 3.60, 3.30, 3.40, 435310, 0));

			System.out.println("测试数据:");
			for (BarEvent bar : testBars) {
				System.out.printf("  %s: %.2f%n", bar.getTimestamp().toLocalDate(), bar.getClose());
			}

			// 尝试直接保存
			java.lang.reflect.Method saveMethod = csvDataSource.getClass().getDeclaredMethod("saveToCSV", String.class,
					List.class);
			if (saveMethod != null) {
				saveMethod.invoke(csvDataSource, "601398", testBars);
				System.out.println("直接保存完成");
			}

		} catch (Exception e) {
			System.out.println("直接CSV写入测试失败: " + e.getMessage());
		}
	}
}