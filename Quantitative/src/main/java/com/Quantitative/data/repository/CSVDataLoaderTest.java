package com.Quantitative.data.repository;

import java.util.List;

import com.Quantitative.data.model.StockBar;
import com.Quantitative.data.model.StockData;

/**
 * @author 测试数据加载
 *
 */
public class CSVDataLoaderTest {
	public static void main(String[] args) {
		System.out.println("=== CSV数据加载测试 ===");

		// 创建数据加载器
		CSVDataLoader loader = new CSVDataLoader();

		// 检查可用股票
		System.out.println("\n1. 检查可用股票数据:");
		List<String> availableSymbols = loader.getAvailableSymbols();
		System.out.println("可用股票: " + availableSymbols);

		if (availableSymbols.isEmpty()) {
			System.out.println("❌ 没有找到CSV数据文件，请先运行数据流水线");
			return;
		}

		// 测试加载第一个股票
		String testSymbol = availableSymbols.get(0);
		System.out.println("\n2. 测试加载股票: " + testSymbol);

		StockData stockData = loader.loadStockData(testSymbol);

		if (stockData == null || stockData.isEmpty()) {
			System.out.println("❌ 数据加载失败");
			return;
		}

		// 显示加载结果
		System.out.println("✅ 数据加载成功!");
		System.out.println("股票代码: " + stockData.getSymbol());
		System.out.println("数据条数: " + stockData.size());
		System.out.println("时间范围: " + stockData.getBars().get(0).getTimestamp().toLocalDate() + " ~ "
				+ stockData.getBars().get(stockData.size() - 1).getTimestamp().toLocalDate());

		// 显示前5条数据样本
		System.out.println("\n3. 数据样本 (前5条):");
		System.out.println("Date\t\tOpen\tHigh\tLow\tClose\tVolume");
		System.out.println("------------------------------------------------------------");

		for (int i = 0; i < Math.min(5, stockData.size()); i++) {
			StockBar bar = stockData.getBar(i);
			System.out.printf("%s\t%.2f\t%.2f\t%.2f\t%.2f\t%,d\n",
					bar.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")),
					bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
		}

		// 显示数据统计
		System.out.println("\n4. 数据统计:");
		displayDataStatistics(stockData);

		// 验证数据完整性
		System.out.println("\n5. 数据完整性验证:");
		validateDataIntegrity(stockData);

		System.out.println("\n🎉 CSV数据加载测试完成!");
	}

	/**
	 * 显示数据统计信息
	 */
	private static void displayDataStatistics(StockData stockData) {
		if (stockData == null || stockData.isEmpty())
			return;

		List<StockBar> bars = stockData.getBars();

		// 价格统计
		double maxPrice = bars.stream().mapToDouble(StockBar::getHigh).max().orElse(0);
		double minPrice = bars.stream().mapToDouble(StockBar::getLow).min().orElse(0);
		double avgPrice = bars.stream().mapToDouble(StockBar::getClose).average().orElse(0);

		// 成交量统计
		long totalVolume = bars.stream().mapToLong(StockBar::getVolume).sum();
		long avgVolume = totalVolume / bars.size();

		System.out.printf("价格范围: %.2f - %.2f (平均: %.2f)\n", minPrice, maxPrice, avgPrice);
		System.out.printf("总成交量: %,d (平均: %,d/日)\n", totalVolume, avgVolume);
		System.out.printf("数据质量: %s\n", stockData.getDataInfo().getDataQuality());
		System.out.printf("数据来源: %s\n", stockData.getDataInfo().getDataSource());
	}

	/**
	 * 验证数据完整性
	 */
	private static void validateDataIntegrity(StockData stockData) {
		if (stockData == null || stockData.isEmpty()) {
			System.out.println("❌ 数据为空");
			return;
		}

		List<StockBar> bars = stockData.getBars();
		int validCount = 0;
		int invalidCount = 0;

		for (StockBar bar : bars) {
			if (bar.getOpen() > 0 && bar.getHigh() > 0 && bar.getLow() > 0 && bar.getClose() > 0 && bar.getVolume() >= 0
					&& bar.getTimestamp() != null) {
				validCount++;
			} else {
				invalidCount++;
			}
		}

		System.out.printf("有效数据: %d 条\n", validCount);
		System.out.printf("无效数据: %d 条\n", invalidCount);
		System.out.printf("数据完整率: %.1f%%\n", (validCount * 100.0 / bars.size()));

		if (invalidCount == 0) {
			System.out.println("✅ 数据完整性验证通过");
		} else {
			System.out.println("⚠ 数据完整性警告: 发现无效数据");
		}
	}
}