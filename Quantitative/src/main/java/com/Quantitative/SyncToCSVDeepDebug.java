package com.Quantitative;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.Quantitative.data.DataSourceManager;

public class SyncToCSVDeepDebug {

	public static void debugSyncToCSVDetails() {
		System.out.println("=== 深入调试 syncToCSV ===");

		DataSourceManager dataManager = new DataSourceManager();
		dataManager.initialize();

		String symbol = "601398";
		LocalDateTime startDate = LocalDateTime.of(2014, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2014, 1, 10, 0, 0);

		try {
			// 方法1：监控数据加载过程
			System.out.println("🔍 监控数据加载...");
			monitorDataLoading(dataManager, symbol, startDate, endDate);

			// 方法2：检查数据转换逻辑
			System.out.println("🔍 检查数据转换...");
			checkDataTransformation(dataManager, symbol, startDate, endDate);

		} catch (Exception e) {
			System.out.println("深度调试失败: " + e.getMessage());
			e.printStackTrace();
		} finally {
			dataManager.shutdown();
		}
	}

	private static void monitorDataLoading(DataSourceManager manager, String symbol, LocalDateTime start,
			LocalDateTime end) {
		try {
			// 获取内部数据源
			java.lang.reflect.Field dataSourcesField = manager.getClass().getDeclaredField("dataSources");
			dataSourcesField.setAccessible(true);
			Map<String, Object> dataSources = (Map<String, Object>) dataSourcesField.get(manager);

			Object akShareSource = dataSources.get("AKSHARE");
			if (akShareSource != null) {
				System.out.println("AKShare数据源: " + akShareSource.getClass().getName());

				// 尝试调用loadHistoricalData方法
				java.lang.reflect.Method loadMethod = findMethod(akShareSource, "loadHistoricalData");
				if (loadMethod != null) {
					System.out.println("调用loadHistoricalData...");
					Object result = loadMethod.invoke(akShareSource, symbol, start, end);
					System.out.println("加载结果: " + result);
				}
			}

		} catch (Exception e) {
			System.out.println("数据加载监控失败: " + e.getMessage());
		}
	}

	private static void checkDataTransformation(DataSourceManager manager, String symbol, LocalDateTime start,
			LocalDateTime end) {
		try {
			// 查找syncToCSV方法的具体实现
			java.lang.reflect.Method syncMethod = manager.getClass().getDeclaredMethod("syncToCSV", String.class,
					LocalDateTime.class, LocalDateTime.class);

			System.out.println("syncToCSV方法: " + syncMethod);

			// 在调用前后添加监控
			System.out.println("调用syncToCSV前的数据状态:");
			checkCSVFileBeforeSync(symbol);

			// 调用syncToCSV
			syncMethod.invoke(manager, symbol, start, end);

			System.out.println("调用syncToCSV后的数据状态:");
			checkCSVFileAfterSync(symbol);

		} catch (Exception e) {
			System.out.println("数据转换检查失败: " + e.getMessage());
		}
	}

	private static void checkCSVFileBeforeSync(String symbol) {
		File csvFile = new File("data/csv/" + symbol + ".csv");
		if (csvFile.exists()) {
			System.out.println("CSV文件已存在: " + csvFile.getAbsolutePath());
			try {
				List<String> lines = java.nio.file.Files.readAllLines(csvFile.toPath());
				System.out.println("文件行数: " + lines.size());
				if (!lines.isEmpty()) {
					System.out.println("首行内容: " + lines.get(0));
					if (lines.size() > 1) {
						System.out.println("数据行: " + lines.get(1));
					}
				}
			} catch (Exception e) {
				System.out.println("读取CSV文件失败: " + e.getMessage());
			}
		} else {
			System.out.println("CSV文件不存在");
		}
	}

	private static void checkCSVFileAfterSync(String symbol) {
		File csvFile = new File("data/csv/" + symbol + ".csv");
		if (csvFile.exists()) {
			try {
				List<String> lines = java.nio.file.Files.readAllLines(csvFile.toPath());
				System.out.println("同步后文件行数: " + lines.size());
				if (lines.size() > 1) {
					System.out.println("同步后数据样本:");
					for (int i = 1; i < Math.min(4, lines.size()); i++) {
						System.out.println("  " + lines.get(i));
					}
				}
			} catch (Exception e) {
				System.out.println("读取同步后CSV失败: " + e.getMessage());
			}
		}
	}

	private static java.lang.reflect.Method findMethod(Object obj, String methodName) {
		for (java.lang.reflect.Method method : obj.getClass().getMethods()) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}
}