package com.Quantitative.data.pipeline;

import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.data.model.StockData;

/**
 * 数据流水线 - 协调数据获取、处理、保存的完整流程
 */
public class DataPipeline {
	private final DataFetcher dataFetcher;
	private final DataProcessor dataProcessor;
	private final DataSaver dataSaver;

	public DataPipeline() {
		this.dataFetcher = new DataFetcher();
		this.dataProcessor = new DataProcessor();
		this.dataSaver = new DataSaver();
	}

	/**
	 * 执行完整的数据处理流水线
	 */
	public boolean processStockData(String symbol, String startDate, String endDate) {
		try {
			TradingLogger.debug("DataPipeline", "开始处理股票数据: %s", symbol);

			// 1. 数据获取阶段
			TradingLogger.debug("DataPipeline", "阶段1: 从数据源获取数据");
			StockData rawData = dataFetcher.fetchData(symbol, startDate, endDate);
			if (rawData == null || rawData.isEmpty()) {
				TradingLogger.logSystemError("DataPipeline", "fetchData", new Exception("获取数据失败: " + symbol));
				return false;
			}
			TradingLogger.debug("DataPipeline", "获取到 %d 条原始数据", rawData.size());

			// 2. 数据处理阶段
			TradingLogger.debug("DataPipeline", "阶段2: 数据清洗和加工");
			StockData processedData = dataProcessor.process(rawData);
			if (processedData == null || processedData.isEmpty()) {
				TradingLogger.logSystemError("DataPipeline", "processData", new Exception("数据处理失败: " + symbol));
				return false;
			}
			TradingLogger.debug("DataPipeline", "处理完成 %d 条数据", processedData.size());

			// 3. 数据保存阶段
			TradingLogger.debug("DataPipeline", "阶段3: 保存处理后的数据");
			boolean saveResult = dataSaver.saveData(processedData);
			if (saveResult) {
				TradingLogger.debug("DataPipeline", "数据流水线完成: %s", symbol);
			} else {
				TradingLogger.logSystemError("DataPipeline", "saveData", new Exception("数据保存失败: " + symbol));
			}

			return saveResult;

		} catch (Exception e) {
			TradingLogger.logSystemError("DataPipeline", "processStockData", e);
			return false;
		}
	}

	/**
	 * 批量处理多个股票
	 */
	public void processMultipleStocks(List<String> symbols, String startDate, String endDate) {
		int successCount = 0;
		for (String symbol : symbols) {
			if (processStockData(symbol, startDate, endDate)) {
				successCount++;
			}
		}
		TradingLogger.debug("DataPipeline", "批量处理完成: %d/%d 成功", successCount, symbols.size());
	}
}