package com.Quantitative.all.demo;

import java.time.LocalDateTime;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.adaptive.AdaptiveDataFeed;

/**
 * 测试修复后的CSV解析
 */
public class FixedCSVParsingDemo {

	public static void main(String[] args) {
		System.out.println("=== 测试修复后的CSV解析 ===\n");

		// 创建自适应数据源
		AdaptiveDataFeed dataFeed = new AdaptiveDataFeed();
		dataFeed.initialize();

		// 设置为本地优先，测试CSV解析
		dataFeed.setDataSourcePriority(java.util.Arrays.asList("LOCAL", "NETWORK"));

		try {
			// 测试加载数据
			System.out.println("? 测试CSV数据加载...");
			List<BarEvent> data = dataFeed.loadHistoricalData("000001", LocalDateTime.of(2023, 1, 1, 0, 0),
					LocalDateTime.of(2023, 3, 31, 0, 0));

			System.out.printf("? CSV加载结果: %d 条数据%n", data.size());

			if (!data.isEmpty()) {
				// 显示前几条数据
				System.out.println("\n? 前5条数据:");
				for (int i = 0; i < Math.min(5, data.size()); i++) {
					BarEvent bar = data.get(i);
					System.out.printf("  %d: %s O=%.2f H=%.2f L=%.2f C=%.2f V=%,d%n", i + 1, bar.getTimestamp(),
							bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
				}
			}

		} catch (Exception e) {
			System.err.println("? 测试失败: " + e.getMessage());
			e.printStackTrace();
		} finally {
			dataFeed.shutdown();
		}

		System.out.println("=== 测试结束 ===");
	}
}