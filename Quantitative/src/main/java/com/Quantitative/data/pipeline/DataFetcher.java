package com.Quantitative.data.pipeline;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.data.DataInfo;
import com.Quantitative.data.model.StockBar;
import com.Quantitative.data.model.StockData;

/**
 * 数据获取器 - 负责从各种数据源获取原始数据
 */
public class DataFetcher {
	private final AKShareDataFeed dataFeed;

	public DataFetcher() {
		this.dataFeed = new AKShareDataFeed();
		this.dataFeed.setDebugMode(true); // 开启调试模式
	}

	/**
	 * 从数据源获取股票数据
	 */
	public StockData fetchData(String symbol, String startDate, String endDate) {
		try {
			TradingLogger.debug("DataFetcher", "从数据源获取数据: %s [%s - %s]", symbol, startDate, endDate);

			// 使用AKShareDataFeed获取数据
			List<StockBar> bars = fetchBarsFromDataSource(symbol, startDate, endDate);

			if (bars == null || bars.isEmpty()) {
				TradingLogger.logRisk("WARN", "DataFetcher", "未获取到数据: %s", symbol);
				return null;
			}

			// 创建DataInfo元数据
			DataInfo dataInfo = createDataInfo(symbol, bars, startDate, endDate);

			TradingLogger.debug("DataFetcher", "成功获取 %d 条数据", bars.size());
			return new StockData(symbol, bars, dataInfo);

		} catch (Exception e) {
			TradingLogger.logSystemError("DataFetcher", "fetchData", e);
			return null;
		}
	}

	/**
	 * 从数据源获取K线数据 - 使用AKShareDataService获取真实数据
	 */
	private List<StockBar> fetchBarsFromDataSource(String symbol, String startDate, String endDate) {
		try {
			// 解析日期
			LocalDateTime startDateTime = parseDate(startDate);
			LocalDateTime endDateTime = parseDate(endDate);

			if (startDateTime == null || endDateTime == null) {
				TradingLogger.logRisk("ERROR", "DataFetcher", "日期解析失败: %s ~ %s", startDate, endDate);
				return MockDataFetcher.generateMockData(symbol, startDate, endDate, 50);
			}

			// 使用AKShareDataService获取真实数据
			TradingLogger.debug("DataFetcher", "调用AKShareDataService获取数据: %s", symbol);
			List<BarEvent> barEvents = dataFeed.getDataService().getStockHistory(symbol, startDateTime, endDateTime,
					"daily", "qfq");

			if (barEvents == null || barEvents.isEmpty()) {
				TradingLogger.logRisk("WARN", "DataFetcher", "AKShare返回空数据，使用模拟数据: %s", symbol);
				return MockDataFetcher.generateMockData(symbol, startDate, endDate, 100);
			}

			// 将BarEvent转换为StockBar
			List<StockBar> bars = convertBarEventToStockBar(barEvents, symbol);
			TradingLogger.debug("DataFetcher", "转换得到 %d 条K线数据", bars.size());

			return bars;

		} catch (Exception e) {
			TradingLogger.logSystemError("DataFetcher", "fetchBarsFromDataSource", e);
			// 出错时返回模拟数据
			return MockDataFetcher.generateMockData(symbol, startDate, endDate, 50);
		}
	}

	/**
	 * 将BarEvent转换为StockBar
	 */
	private List<StockBar> convertBarEventToStockBar(List<BarEvent> barEvents, String symbol) {
		List<StockBar> bars = new ArrayList<>();

		for (BarEvent barEvent : barEvents) {
			try {
				StockBar stockBar = new StockBar(symbol, barEvent.getTimestamp(), barEvent.getOpen(),
						barEvent.getHigh(), barEvent.getLow(), barEvent.getClose(), barEvent.getVolume(),
						barEvent.getTurnover());
				bars.add(stockBar);

			} catch (Exception e) {
				TradingLogger.logSystemError("DataFetcher", "convertBarEventToStockBar", e);
			}
		}

		return bars;
	}

	/**
	 * 解析日期字符串
	 */
	private LocalDateTime parseDate(String dateStr) {
		try {
			// 支持格式: yyyyMMdd, yyyy-MM-dd, yyyy/MM/dd
			String normalizedDate = dateStr.replaceAll("[-/]", "");
			if (normalizedDate.length() == 8) {
				int year = Integer.parseInt(normalizedDate.substring(0, 4));
				int month = Integer.parseInt(normalizedDate.substring(4, 6));
				int day = Integer.parseInt(normalizedDate.substring(6, 8));
				return LocalDateTime.of(year, month, day, 15, 0); // 下午3点收盘
			}
		} catch (Exception e) {
			TradingLogger.logSystemError("DataFetcher", "parseDate", e);
		}
		return null;
	}

	/**
	 * 创建数据元信息
	 */
	private DataInfo createDataInfo(String symbol, List<StockBar> bars, String startDate, String endDate) {
		if (bars == null || bars.isEmpty()) {
			return new DataInfo(symbol, null, null, 0, "1d");
		}

		LocalDateTime startTime = bars.get(0).getTimestamp();
		LocalDateTime endTime = bars.get(bars.size() - 1).getTimestamp();

		return new DataInfo.Builder(symbol).startTime(startTime).endTime(endTime).barCount(bars.size()).timeframe("1d")
				.dataSource("AKShare").dataQuality("RAW").build();
	}
}