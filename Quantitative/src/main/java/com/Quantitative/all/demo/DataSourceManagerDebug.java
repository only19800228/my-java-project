package com.Quantitative.all.demo;

import java.time.LocalDateTime;

import com.Quantitative.data.DataSourceManager;

public class DataSourceManagerDebug {

	public static void debugSyncToCSV() {
		System.out.println("=== 调试 syncToCSV 方法 ===");

		DataSourceManager dataManager = new DataSourceManager();
		dataManager.initialize();

		// 在同步前后添加调试信息
		System.out.println("🔍 开始同步数据到CSV...");

		// 监控数据转换过程
		String symbol = "601398";
		LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2023, 1, 10, 0, 0);

		try {
			// 方法1：通过反射获取内部数据
			debugDataSourceManagerInternals(dataManager, symbol, startDate, endDate);

			// 方法2：直接调用并监控
			dataManager.syncToCSV(symbol, startDate, endDate);

		} catch (Exception e) {
			System.out.println("❌ syncToCSV 调试失败: " + e.getMessage());
			e.printStackTrace();
		}

		dataManager.shutdown();
	}

	private static void debugDataSourceManagerInternals(DataSourceManager manager, String symbol, LocalDateTime start,
			LocalDateTime end) {
		try {
			// 获取内部字段
			java.lang.reflect.Field[] fields = manager.getClass().getDeclaredFields();
			System.out.println("DataSourceManager 内部字段:");
			for (java.lang.reflect.Field field : fields) {
				field.setAccessible(true);
				String fieldName = field.getName();
				Object value = field.get(manager);
				System.out.printf("  %s: %s%n", fieldName, value != null ? value.getClass().getSimpleName() : "null");

				// 特别关注数据相关的字段
				if (fieldName.toLowerCase().contains("data") || fieldName.toLowerCase().contains("feed")
						|| fieldName.toLowerCase().contains("source")) {
					System.out.printf("    → 详细: %s%n", value);
				}
			}
		} catch (Exception e) {
			System.out.println("反射调试失败: " + e.getMessage());
		}
	}
}