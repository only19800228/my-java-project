package com.Quantitative.data.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataInfo;
import com.Quantitative.data.model.StockBar;
import com.Quantitative.data.model.StockData;
import com.Quantitative.data.validation.DataValidator;

/**
 * 数据处理器 - 负责数据清洗、验证和加工
 */
public class DataProcessor {
	private final DataValidator dataValidator;

	public DataProcessor() {
		this.dataValidator = new DataValidator();
	}

	/**
	 * 处理原始数据
	 */
	public StockData process(StockData rawData) {
		if (rawData == null || rawData.isEmpty()) {
			TradingLogger.logRisk("WARN", "DataProcessor", "输入数据为空");
			return null;
		}

		try {
			TradingLogger.debug("DataProcessor", "开始数据处理: %s", rawData.getSymbol());

			// 1. 数据清洗
			TradingLogger.debug("DataProcessor", "阶段1: 数据清洗");
			List<StockBar> cleanedBars = cleanData(rawData.getBars());
			TradingLogger.debug("DataProcessor", "数据清洗完成: %d/%d 条有效数据", cleanedBars.size(), rawData.size());

			// 2. 数据验证
			TradingLogger.debug("DataProcessor", "阶段2: 数据验证");
			List<StockBar> validatedBars = validateData(cleanedBars);
			TradingLogger.debug("DataProcessor", "数据验证完成: %d/%d 条验证通过", validatedBars.size(), cleanedBars.size());

			// 3. 数据加工
			TradingLogger.debug("DataProcessor", "阶段3: 数据加工");
			List<StockBar> processedBars = enhanceData(validatedBars);
			TradingLogger.debug("DataProcessor", "数据加工完成");

			// 4. 创建处理后的DataInfo
			DataInfo processedInfo = createProcessedDataInfo(rawData.getDataInfo(), validatedBars.size());

			return new StockData(rawData.getSymbol(), processedBars, processedInfo);

		} catch (Exception e) {
			TradingLogger.logSystemError("DataProcessor", "process", e);
			return null;
		}
	}

	/**
	 * 数据清洗：去除无效数据
	 */
	private List<StockBar> cleanData(List<StockBar> rawBars) {
		return rawBars.stream().filter(bar -> bar != null).filter(bar -> bar.getTimestamp() != null)
				.filter(bar -> bar.getOpen() > 0 && bar.getHigh() > 0 && bar.getLow() > 0 && bar.getClose() > 0)
				.filter(bar -> bar.getVolume() >= 0).filter(bar -> bar.getHigh() >= bar.getLow())
				.filter(bar -> bar.getHigh() >= bar.getOpen() && bar.getHigh() >= bar.getClose())
				.filter(bar -> bar.getLow() <= bar.getOpen() && bar.getLow() <= bar.getClose())
				.collect(Collectors.toList());
	}

	/**
	 * 数据验证 - 使用适配器将StockBar转换为BarEvent进行验证
	 */
	private List<StockBar> validateData(List<StockBar> cleanedBars) {
		return cleanedBars.stream().filter(bar -> {
			// 将StockBar转换为BarEvent进行验证
			BarEvent barEvent = convertToBarEvent(bar);
			DataValidator.ValidationResult result = DataValidator.validateBar(barEvent);
			return result.isValid();
		}).collect(Collectors.toList());
	}

	/**
	 * 将StockBar转换为BarEvent
	 */
	private BarEvent convertToBarEvent(StockBar stockBar) {
		return new BarEvent(stockBar.getTimestamp(), stockBar.getSymbol(), stockBar.getOpen(), stockBar.getHigh(),
				stockBar.getLow(), stockBar.getClose(), stockBar.getVolume(), stockBar.getTurnover());
	}

	/**
	 * 数据增强：添加计算字段
	 */
	private List<StockBar> enhanceData(List<StockBar> validatedBars) {
		// 这里可以添加技术指标计算
		// 目前直接返回原始数据，你可以后续扩展
		return new ArrayList<>(validatedBars);
	}

	/**
	 * 创建处理后的数据元信息
	 */
	private DataInfo createProcessedDataInfo(DataInfo originalInfo, int processedBarCount) {
		return new DataInfo.Builder(originalInfo.getSymbol()).startTime(originalInfo.getStartTime())
				.endTime(originalInfo.getEndTime()).barCount(processedBarCount).timeframe(originalInfo.getTimeframe())
				.dataSource(originalInfo.getDataSource()).dataQuality("PROCESSED").build();
	}
}