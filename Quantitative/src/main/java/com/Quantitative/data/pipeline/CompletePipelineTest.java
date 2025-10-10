package com.Quantitative.data.pipeline;

import com.Quantitative.data.model.StockBar;
import com.Quantitative.data.model.StockData;
import com.Quantitative.data.repository.CSVDataLoader;

/**
 * @author 数据获取加工 保存 提取 完整流水线
 *
 */
public class CompletePipelineTest {
	public static void main(String[] args) {
		System.out.println("=== 完整数据流水线测试 ===");

		String testSymbol = "000002";
		String startDate = "20240101";
		String endDate = "20240131";

		// 步骤1: 运行数据流水线
		System.out.println("\n1. 运行数据流水线...");
		DataPipeline pipeline = new DataPipeline();
		boolean processSuccess = pipeline.processStockData(testSymbol, startDate, endDate);

		if (!processSuccess) {
			System.out.println("❌ 数据处理失败");
			return;
		}
		System.out.println("✅ 数据处理完成");

		// 步骤2: 测试数据加载
		System.out.println("\n2. 测试数据加载...");
		CSVDataLoader loader = new CSVDataLoader();
		StockData loadedData = loader.loadStockData(testSymbol);

		if (loadedData == null || loadedData.isEmpty()) {
			System.out.println("❌ 数据加载失败");
			return;
		}
		System.out.println("✅ 数据加载成功");

		// 步骤3: 验证数据一致性
		System.out.println("\n3. 数据验证:");
		System.out.println("股票代码: " + loadedData.getSymbol());
		System.out.println("数据条数: " + loadedData.size());
		System.out.println("时间范围: " + loadedData.getBars().get(0).getTimestamp().toLocalDate() + " ~ "
				+ loadedData.getBars().get(loadedData.size() - 1).getTimestamp().toLocalDate());

		// 显示第一条和最后一条数据
		if (loadedData.size() >= 2) {
			StockBar firstBar = loadedData.getBar(0);
			StockBar lastBar = loadedData.getBar(loadedData.size() - 1);

			System.out.println("\n第一条数据:");
			System.out.printf("  日期: %s, 收盘价: %.2f, 成交量: %,d\n", firstBar.getTimestamp().toLocalDate(),
					firstBar.getClose(), firstBar.getVolume());

			System.out.println("最后一条数据:");
			System.out.printf("  日期: %s, 收盘价: %.2f, 成交量: %,d\n", lastBar.getTimestamp().toLocalDate(),
					lastBar.getClose(), lastBar.getVolume());
		}

		System.out.println("\n🎉 完整数据流水线测试成功!");
		System.out.println("数据流水线: 数据获取 → 处理 → 保存 → 加载 全部正常!");
	}
}