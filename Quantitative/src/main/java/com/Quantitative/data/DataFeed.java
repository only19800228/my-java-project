package com.Quantitative.data;

import java.time.LocalDateTime;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.interfaces.TradingComponent;
import com.Quantitative.data.validation.DataQualityReport;
import com.Quantitative.data.validation.DataValidator;

/**
 * 数据馈送接口 - 统一的数据获取接口
 */
public interface DataFeed extends TradingComponent {

	/**
	 * 加载历史数据
	 */
	List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end);

	/**
	 * 获取下一个Bar数据
	 */
	BarEvent getNextBar();

	/**
	 * 检查是否还有下一个Bar
	 */
	boolean hasNextBar();

	/**
	 * 重置数据指针
	 */
	void reset();

	/**
	 * 获取所有已加载的数据
	 */
	List<BarEvent> getAllBars();

	/**
	 * 获取数据信息
	 */
	DataInfo getDataInfo();

	/**
	 * 设置数据参数
	 */
	void setParameter(String key, Object value);

	/**
	 * 获取数据参数
	 */
	Object getParameter(String key);

	/**
	 * 获取可用标的列表
	 */
	List<String> getAvailableSymbols();

	/**
	 * 检查数据连接状态
	 */
	boolean isConnected();

	/**
	 * 获取数据质量报告（新增）
	 */
	default DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		// 默认实现，子类可以重写
		List<BarEvent> bars = loadHistoricalData(symbol, start, end);
		DataQualityReport report = new DataQualityReport(symbol, start, end);

		for (BarEvent bar : bars) {
			DataValidator.ValidationResult result = DataValidator.validateBar(bar);
			report.addValidationResult(result);
		}

		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(bars);
		report.setSeriesValidation(seriesResult);

		return report;
	}

	// List<DataInfo> fetchData(String symbol, String startDate, String
	// endDate);
}