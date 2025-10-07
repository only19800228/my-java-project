package com.Quantitative.all.demo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.adaptive.AdaptiveDataFeed;

/**
 * 自适应数据源使用示例
 */
public class AdaptiveDataSourceDemo {

	public static void main(String[] args) {
		System.out.println("=== 自适应数据源演示 ===\n");

		// 创建自适应数据源
		AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();
		dataFeed.initialize();

		// 配置数据源优先级（本地优先）
		dataFeed.setDataSourcePriority(Arrays.asList("LOCAL", "NETWORK"));

		// 加载数据 - 会自动在本地和网络之间切换
		System.out.println("? 加载数据（本地优先）...");
		List<BarEvent> data = dataFeed.loadHistoricalData("000001", LocalDateTime.of(2023, 1, 1, 0, 0),
				LocalDateTime.of(2023, 3, 31, 0, 0));

		System.out.println("? 加载结果: " + data.size() + " 条数据");

		// 切换为网络优先
		dataFeed.setDataSourcePriority(Arrays.asList("NETWORK", "LOCAL"));

		// 批量同步数据到本地
		System.out.println("\n? 批量同步数据到本地...");
		dataFeed.batchSyncToLocal(Arrays.asList("000001", "000002", "000858"), LocalDateTime.of(2023, 1, 1, 0, 0),
				LocalDateTime.of(2023, 6, 30, 0, 0));

		// 在回测引擎中使用
		System.out.println("\n? 在回测引擎中使用...");
		runBacktestWithAdaptiveData(dataFeed);

		dataFeed.shutdown();
		System.out.println("=== 演示结束 ===");
	}

	private static void runBacktestWithAdaptiveData(AdaptiveDataFeed dataFeed) {
		// 加载数据
		dataFeed.loadHistoricalData("000001", LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 3, 31, 0, 0));

		// 现在可以正常使用 getAllBars() 了
		List<BarEvent> bars = dataFeed.getAllBars();
		System.out.println("? 回测数据: " + bars.size() + " 条");

		// 或者使用流式访问
		int count = 0;
		while (dataFeed.hasNextBar() && count < 5) {
			BarEvent bar = dataFeed.getNextBar();
			System.out.printf("  Bar %d: %s %.2f%n", ++count, bar.getTimestamp(), bar.getClose());
		}
	}
}