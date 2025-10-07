package com.Quantitative.all.demo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataSourceManager;
import com.Quantitative.data.csv.CSVDataSource;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * CSV文件创建和使用演示,同步数据到CSV文件
 */
public class CSVFileCreationDemo {

	public static void main(String[] args) {
		System.out.println("=== CSV文件创建演示 ===\n");

		try {
			// 步骤1: 创建数据源管理器
			DataSourceManager dataManager = new DataSourceManager();
			dataManager.initialize();

			// 步骤2: 检查数据目录
			CSVDataSource csvDataSource = dataManager.getCSVDataSource();
			if (csvDataSource != null) {
				// System.out.println("? CSV数据目录: " +
				// csvDataSource.getDataDirectory());
				System.out.println("? CSV数据源连接状态: " + csvDataSource.isConnected());
			}

			// 步骤3: 同步数据到CSV（这会创建文件）
			System.out.println("\n步骤3: 同步数据到CSV文件");
			List<String> symbols = Arrays.asList("000001", "000002", "600519", "601985", "600660", "601288", "01658");

			for (String symbol : symbols) {
				System.out.println("\n正在同步: " + symbol);
				dataManager.syncToCSV(symbol, LocalDateTime.of(2014, 1, 1, 0, 0), LocalDateTime.of(2024, 12, 31, 0, 0));
			}

			// 步骤4: 列出生成的CSV文件
			System.out.println("\n步骤4: 检查生成的CSV文件");
			List<String> availableSymbols = csvDataSource.getAvailableSymbols();
			System.out.println("? 可用的CSV文件: " + availableSymbols);

			// 步骤5: 从CSV文件加载数据测试
			System.out.println("\n步骤5: 从CSV文件加载数据测试");
			for (String symbol : availableSymbols) {
				List<BarEvent> data = csvDataSource.loadHistoricalData(symbol, LocalDateTime.of(2023, 1, 1, 0, 0),
						LocalDateTime.of(2023, 3, 31, 0, 0));
				System.out.println("? " + symbol + ".csv: " + data.size() + "条记录");

				// 显示前几条数据
				if (!data.isEmpty()) {
					System.out.println("  示例数据:");
					for (int i = 0; i < Math.min(3, data.size()); i++) {
						BarEvent bar = data.get(i);
						System.out.printf("    %s: O%.2f H%.2f L%.2f C%.2f V%d%n", bar.getTimestamp().toLocalDate(),
								bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
					}
				}
			}

			// 步骤6: 数据质量检查
			System.out.println("\n步骤6: CSV数据质量检查");
			for (String symbol : availableSymbols) {
				DataQualityReport report = csvDataSource.getDataQualityReport(symbol,
						LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 3, 31, 0, 0));
				System.out.println("? " + symbol + " 质量评分: " + String.format("%.1f/100", report.getDataQualityScore()));
			}

			dataManager.shutdown();

		} catch (Exception e) {
			System.err.println("演示失败: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n=== 演示结束 ===");
	}
}