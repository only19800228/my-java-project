package com.Quantitative.all.demo;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.csv.CSVDataSource;

/**
 * 手动创建CSV文件演示------创建模拟文件
 */
public class ManualCSVCreationDemo {

	public static void main(String[] args) {
		System.out.println("=== 手动创建CSV文件演示 ===\n");

		try {
			// 步骤1: 创建自定义目录的数据源
			String customDataDir = "my_data/custom/";
			CSVDataSource csvDataSource = new CSVDataSource(customDataDir);
			csvDataSource.initialize();

			System.out.println("? 数据目录: " + csvDataSource.getDataDirectory());
			System.out.println("? 目录存在: " + new File(customDataDir).exists());

			// 步骤2: 创建模拟数据
			System.out.println("\n步骤2: 创建模拟数据");
			List<BarEvent> mockData = createMockData("TEST001", 50);
			System.out.println("? 生成模拟数据: " + mockData.size() + "条");

			// 步骤3: 保存到CSV
			System.out.println("\n步骤3: 保存到CSV文件");
			csvDataSource.saveToCSV("TEST001", mockData);

			// 步骤4: 验证文件创建
			System.out.println("\n步骤4: 验证文件创建");
			String filePath = csvDataSource.getFilePath("TEST001");
			File file = new File(filePath);
			System.out.println("? 文件路径: " + filePath);
			System.out.println("? 文件存在: " + file.exists());
			System.out.println("? 文件大小: " + (file.exists() ? file.length() + "字节" : "N/A"));

			// 步骤5: 从文件加载数据验证
			System.out.println("\n步骤5: 从文件加载验证");
			List<BarEvent> loadedData = csvDataSource.loadHistoricalData("TEST001", LocalDateTime.of(2023, 1, 1, 0, 0),
					LocalDateTime.of(2023, 12, 31, 0, 0));
			System.out.println("? 加载数据: " + loadedData.size() + "条");

			// 步骤6: 显示文件内容预览
			System.out.println("\n步骤6: 文件内容预览");
			if (!loadedData.isEmpty()) {
				System.out.println("前5条记录:");
				for (int i = 0; i < Math.min(5, loadedData.size()); i++) {
					BarEvent bar = loadedData.get(i);
					System.out.printf("  %d. %s: %.2f (V:%,d)%n", i + 1, bar.getTimestamp().toLocalDate(),
							bar.getClose(), bar.getVolume());
				}
			}

			// 步骤7: 清理测试文件
			System.out.println("\n步骤7: 清理测试文件");
			boolean deleted = csvDataSource.deleteFile("TEST001");
			System.out.println("? 文件删除: " + (deleted ? "成功" : "失败"));

			csvDataSource.shutdown();

		} catch (Exception e) {
			System.err.println("演示失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n=== 演示结束 ===");
	}

	/**
	 * 创建模拟数据
	 */
	private static List<BarEvent> createMockData(String symbol, int count) {
		List<BarEvent> mockData = new ArrayList<>();
		LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
		double price = 10.0;

		for (int i = 0; i < count; i++) {
			LocalDateTime timestamp = startDate.plusDays(i);

			// 模拟价格波动
			double change = (Math.random() - 0.5) * 0.5; // ±0.25
			price = price * (1 + change);
			price = Math.max(0.01, price); // 确保价格为正

			double open = price;
			double high = price * (1 + Math.random() * 0.02);
			double low = price * (1 - Math.random() * 0.02);
			double close = price * (1 + (Math.random() - 0.5) * 0.01);
			long volume = (long) (1000000 + Math.random() * 9000000);

			BarEvent bar = new BarEvent(timestamp, symbol, open, high, low, close, volume);
			mockData.add(bar);
		}

		return mockData;
	}
}