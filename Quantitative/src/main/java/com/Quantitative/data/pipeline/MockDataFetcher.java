package com.Quantitative.data.pipeline;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.data.model.StockBar;

/**
 * 模拟数据获取器 - 用于测试数据流水线---模拟数据
 */
public class MockDataFetcher {
	private static final Random random = new Random();

	/**
	 * 生成模拟股票数据
	 */
	public static List<StockBar> generateMockData(String symbol, String startDate, String endDate, int count) {
		List<StockBar> bars = new ArrayList<>();

		// 解析开始日期
		LocalDateTime currentTime = parseDate(startDate);
		LocalDateTime endTime = parseDate(endDate);

		// 基础价格
		double basePrice = 10.0 + random.nextDouble() * 90.0; // 10-100之间的随机价格

		for (int i = 0; i < count; i++) {
			if (currentTime.isAfter(endTime)) {
				break;
			}

			// 生成价格数据
			double open = basePrice * (0.95 + random.nextDouble() * 0.1);
			double close = open * (0.98 + random.nextDouble() * 0.04);
			double high = Math.max(open, close) * (1.0 + random.nextDouble() * 0.03);
			double low = Math.min(open, close) * (0.97 - random.nextDouble() * 0.02);
			long volume = 1000000 + random.nextInt(9000000);
			double turnover = (open + close) / 2 * volume;

			StockBar bar = new StockBar(symbol, currentTime, open, high, low, close, volume, turnover);
			bars.add(bar);

			// 移动到下一个交易日
			currentTime = currentTime.plusDays(1);

			// 更新基础价格
			basePrice = close;
		}

		TradingLogger.debug("MockDataFetcher", "生成 %d 条模拟数据: %s", bars.size(), symbol);
		return bars;
	}

	private static LocalDateTime parseDate(String dateStr) {
		// 简单解析日期，格式: yyyyMMdd
		int year = Integer.parseInt(dateStr.substring(0, 4));
		int month = Integer.parseInt(dateStr.substring(4, 6));
		int day = Integer.parseInt(dateStr.substring(6, 8));
		return LocalDateTime.of(year, month, day, 15, 0); // 下午3点收盘
	}
}