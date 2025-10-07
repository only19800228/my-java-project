package com.Quantitative.all.demo;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.csv.CSVDataSource;

/**
 * 保存股票数据到CSV文件 - 用于历史数据回测
 */
public class StockDataToCSVDemo {

	public static void main(String[] args) {
		System.out.println("=== 保存股票数据到CSV文件（用于回测） ===\n");

		// 使用标准数据目录 data/csv/
		CSVDataSource csvDataSource = new CSVDataSource(); // 使用默认目录 data/csv/
		csvDataSource.initialize();

		try {
			// 步骤1: 显示数据目录信息
			System.out.println("📁 数据目录: " + csvDataSource.getDataDirectory());
			System.out.println("📁 目录存在: " + new File(csvDataSource.getDataDirectory()).exists());

			// 步骤2: 创建真实的股票数据（模拟多个股票）
			System.out.println("\n步骤2: 创建股票数据");

			// 创建多个股票的模拟数据
			String[] stockCodes = { "000001", "000002", "600519", "601318", "600036" };

			for (String stockCode : stockCodes) {
				System.out.println("\n🔧 处理股票: " + stockCode);

				// 创建模拟数据（更真实的数据）
				List<BarEvent> stockData = createRealisticStockData(stockCode, 100); // 100个交易日

				// 保存到CSV
				csvDataSource.saveToCSV(stockCode, stockData);

				// 验证保存结果
				String filePath = csvDataSource.getFilePath(stockCode);
				File file = new File(filePath);
				System.out.println("✅ 保存完成: " + filePath);
				System.out.println("✅ 文件大小: " + file.length() + " 字节");

				// 立即加载验证
				List<BarEvent> loadedData = csvDataSource.loadHistoricalData(stockCode,
						LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 12, 31, 0, 0));
				System.out.println("✅ 加载验证: " + loadedData.size() + " 条记录");
			}

			// 步骤3: 显示所有保存的CSV文件
			System.out.println("\n步骤3: 所有CSV文件列表");
			displayAllCSVFiles(csvDataSource);

			// 步骤4: 测试数据加载功能
			System.out.println("\n步骤4: 测试数据加载功能");
			testDataLoading(csvDataSource);

		} catch (Exception e) {
			System.err.println("❌ 演示失败: " + e.getMessage());
			e.printStackTrace();
		} finally {
			csvDataSource.shutdown();
		}

		System.out.println("\n=== 演示完成 ===");
		System.out.println("💡 所有股票数据已保存到: " + csvDataSource.getDataDirectory());
		System.out.println("💡 你可以在回测中使用这些CSV文件加载历史数据");
	}

	/**
	 * 创建更真实的股票数据
	 */
	private static List<BarEvent> createRealisticStockData(String stockCode, int tradingDays) {
		List<BarEvent> stockData = new ArrayList<>();

		// 根据股票代码设置不同的初始价格
		double basePrice = getBasePriceByStockCode(stockCode);
		LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);

		double currentPrice = basePrice;
		double trend = 0.0; // 趋势因子

		for (int i = 0; i < tradingDays; i++) {
			LocalDateTime timestamp = startDate.plusDays(i);

			// 跳过周末（简单模拟）
			if (timestamp.getDayOfWeek().getValue() > 5) {
				continue;
			}

			// 模拟更真实的价格波动
			double dailyVolatility = 0.02 + Math.random() * 0.03; // 2%-5%的日波动率
			double randomChange = (Math.random() - 0.5) * 2 * dailyVolatility;

			// 添加趋势因子
			trend += (Math.random() - 0.5) * 0.01;
			trend = Math.max(-0.1, Math.min(0.1, trend)); // 限制趋势范围

			double totalChange = randomChange + trend;
			currentPrice = currentPrice * (1 + totalChange);
			currentPrice = Math.max(0.01, currentPrice); // 确保价格为正

			// 生成OHLC价格
			double open = currentPrice;
			double high = open * (1 + Math.random() * dailyVolatility);
			double low = open * (1 - Math.random() * dailyVolatility);
			double close = open * (1 + (Math.random() - 0.5) * dailyVolatility);

			// 确保价格关系正确
			high = Math.max(open, Math.max(close, high));
			low = Math.min(open, Math.min(close, low));

			// 生成成交量（与价格波动相关）
			long baseVolume = getBaseVolumeByStockCode(stockCode);
			double volumeMultiplier = 0.8 + Math.random() * 0.4; // 0.8-1.2倍
			long volume = (long) (baseVolume * volumeMultiplier);

			double turnover = close * volume;

			BarEvent bar = new BarEvent(timestamp, stockCode, open, high, low, close, volume, turnover);
			stockData.add(bar);
		}

		return stockData;
	}

	/**
	 * 根据股票代码获取基准价格
	 */
	private static double getBasePriceByStockCode(String stockCode) {
		switch (stockCode) {
		case "000001":
			return 15.0; // 平安银行
		case "000002":
			return 25.0; // 万科A
		case "600519":
			return 1800.0; // 贵州茅台
		case "601318":
			return 50.0; // 中国平安
		case "600036":
			return 35.0; // 招商银行
		default:
			return 10.0;
		}
	}

	/**
	 * 根据股票代码获取基准成交量
	 */
	private static long getBaseVolumeByStockCode(String stockCode) {
		switch (stockCode) {
		case "000001":
			return 50000000L; // 平安银行
		case "000002":
			return 30000000L; // 万科A
		case "600519":
			return 2000000L; // 贵州茅台
		case "601318":
			return 40000000L; // 中国平安
		case "600036":
			return 35000000L; // 招商银行
		default:
			return 10000000L;
		}
	}

	/**
	 * 显示所有CSV文件
	 */
	private static void displayAllCSVFiles(CSVDataSource csvDataSource) {
		List<String> files = csvDataSource.listAllFiles();

		if (files.isEmpty()) {
			System.out.println("❌ 没有找到CSV文件");
			return;
		}

		System.out.println("找到 " + files.size() + " 个CSV文件:");
		System.out.println("股票代码 | 文件大小 | 数据条数");
		System.out.println("--------|----------|----------");

		for (String filename : files) {
			String symbol = filename.replace(".csv", "");
			String filePath = csvDataSource.getFilePath(symbol);
			File file = new File(filePath);

			// 加载数据统计
			List<BarEvent> data = csvDataSource.loadHistoricalData(symbol, null, null);

			System.out.printf("%-8s | %6.1fKB | %8d条%n", symbol, file.length() / 1024.0, data.size());
		}
	}

	/**
	 * 测试数据加载功能
	 */
	private static void testDataLoading(CSVDataSource csvDataSource) {
		String testStock = "000001";

		System.out.println("🧪 测试加载股票: " + testStock);

		// 测试1: 加载全部数据
		List<BarEvent> allData = csvDataSource.loadHistoricalData(testStock, null, null);
		System.out.println("✅ 全部数据: " + allData.size() + " 条");

		if (!allData.isEmpty()) {
			// 显示数据范围
			LocalDateTime startDate = allData.get(0).getTimestamp();
			LocalDateTime endDate = allData.get(allData.size() - 1).getTimestamp();
			System.out.println("✅ 时间范围: " + startDate.toLocalDate() + " 到 " + endDate.toLocalDate());

			// 显示前3条数据
			System.out.println("✅ 数据样例:");
			for (int i = 0; i < Math.min(3, allData.size()); i++) {
				BarEvent bar = allData.get(i);
				System.out.printf("   %s: %.2f (V:%,d)%n", bar.getTimestamp().toLocalDate(), bar.getClose(),
						bar.getVolume());
			}
		}

		// 测试2: 按时间范围加载
		List<BarEvent> rangeData = csvDataSource.loadHistoricalData(testStock, LocalDateTime.of(2023, 3, 1, 0, 0),
				LocalDateTime.of(2023, 3, 31, 0, 0));
		System.out.println("✅ 3月份数据: " + rangeData.size() + " 条");
	}
}