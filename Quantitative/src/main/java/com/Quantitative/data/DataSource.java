package com.Quantitative.data;

import java.time.LocalDateTime;
import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.interfaces.TradingComponent;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * 统一数据源接口 - 支持多种数据获取方式
 */
public interface DataSource extends TradingComponent {

	/**
	 * 加载历史数据
	 */
	List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end);

	/**
	 * 获取数据信息
	 */
	DataInfo getDataInfo();

	/**
	 * 检查数据源连接状态
	 */
	boolean isConnected();

	/**
	 * 获取数据源类型
	 */
	String getDataSourceType();

	/**
	 * 获取可用标的列表
	 */
	List<String> getAvailableSymbols();

	/**
	 * 数据质量验证
	 */
	DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end);
}