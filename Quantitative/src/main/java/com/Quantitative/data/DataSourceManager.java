package com.Quantitative.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.adapter.AKShareDataSourceAdapter;
import com.Quantitative.data.csv.CSVDataSource;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * 数据源管理器 - 统一管理多个数据源
 */
public class DataSourceManager implements DataSource {

	private Map<String, DataSource> dataSources;
	private String defaultDataSource;
	private String status = "CREATED";

	public DataSourceManager() {
		this.dataSources = new HashMap<>();
		initializeDefaultDataSources();
	}

	private void initializeDefaultDataSources() {
		// 添加CSV数据源
		CSVDataSource csvDataSource = new CSVDataSource();
		addDataSource("CSV", csvDataSource);

		// 添加AKShare数据源（使用适配器）
		AKShareDataSourceAdapter akShareDataSource = new AKShareDataSourceAdapter();
		addDataSource("AKSHARE", akShareDataSource);

		// 设置默认数据源
		this.defaultDataSource = "AKSHARE";
	}

	/**
	 * 添加数据源
	 */
	public void addDataSource(String name, DataSource dataSource) {
		dataSources.put(name.toUpperCase(), dataSource);
		System.out.println("? 添加数据源: " + name);
	}

	/**
	 * 获取特定数据源
	 */
	public DataSource getDataSource(String name) {
		return dataSources.get(name.toUpperCase());
	}

	/**
	 * 获取AKShare数据源适配器
	 */
	public AKShareDataSourceAdapter getAKShareDataSource() {
		return (AKShareDataSourceAdapter) dataSources.get("AKSHARE");
	}

	/**
	 * 获取CSV数据源
	 */
	public CSVDataSource getCSVDataSource() {
		return (CSVDataSource) dataSources.get("CSV");
	}

	/**
	 * 移除数据源
	 */
	public void removeDataSource(String name) {
		dataSources.remove(name.toUpperCase());
		System.out.println("? 移除数据源: " + name);
	}

	/**
	 * 设置默认数据源
	 */
	public void setDefaultDataSource(String name) {
		if (dataSources.containsKey(name.toUpperCase())) {
			this.defaultDataSource = name.toUpperCase();
			System.out.println("? 设置默认数据源: " + name);
		} else {
			System.out.println("? 数据源不存在: " + name);
		}
	}

	/**
	 * 从指定数据源加载数据
	 */
	public List<BarEvent> loadHistoricalData(String sourceName, String symbol, LocalDateTime start, LocalDateTime end) {
		DataSource dataSource = dataSources.get(sourceName.toUpperCase());
		if (dataSource == null) {
			System.out.println("? 数据源不存在: " + sourceName);
			return new ArrayList<>();
		}

		return dataSource.loadHistoricalData(symbol, start, end);
	}

	/**
	 * 自动选择数据源加载数据（默认→备用）
	 */
	@Override
	public List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end) {
		// 首先尝试默认数据源
		List<BarEvent> data = loadHistoricalData(defaultDataSource, symbol, start, end);

		// 如果默认数据源失败，尝试其他数据源
		if (data.isEmpty()) {
			System.out.println("? 默认数据源无数据，尝试备用数据源");

			for (String sourceName : dataSources.keySet()) {
				if (!sourceName.equals(defaultDataSource)) {
					data = loadHistoricalData(sourceName, symbol, start, end);
					if (!data.isEmpty()) {
						System.out.println("? 从备用数据源获取数据: " + sourceName);
						break;
					}
				}
			}
		}

		if (data.isEmpty()) {
			System.out.println("? 所有数据源均无数据: " + symbol);
		}

		return data;
	}

	@Override
	public void initialize() {
		for (DataSource dataSource : dataSources.values()) {
			dataSource.initialize();
		}
		this.status = "INITIALIZED";
		System.out.println("? 数据源管理器初始化完成");
	}

	@Override
	public void configure(Map<String, Object> config) {
		// 配置各个数据源
	}

	@Override
	public String getName() {
		return "DataSourceManager";
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		for (DataSource dataSource : dataSources.values()) {
			dataSource.reset();
		}
		this.status = "RESET";
	}

	@Override
	public void shutdown() {
		for (DataSource dataSource : dataSources.values()) {
			dataSource.shutdown();
		}
		this.status = "SHUTDOWN";
	}

	@Override
	public DataInfo getDataInfo() {
		// 返回综合数据信息
		return new DataInfo("MultiSource", null, null, 0, "mixed");
	}

	@Override
	public boolean isConnected() {
		for (DataSource dataSource : dataSources.values()) {
			if (dataSource.isConnected()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getDataSourceType() {
		return "MULTI_SOURCE";
	}

	@Override
	public List<String> getAvailableSymbols() {
		// 合并所有数据源的可用标的
		List<String> allSymbols = new ArrayList<>();
		for (DataSource dataSource : dataSources.values()) {
			allSymbols.addAll(dataSource.getAvailableSymbols());
		}
		return allSymbols;
	}

	@Override
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		// 使用默认数据源生成质量报告
		DataSource defaultSource = dataSources.get(defaultDataSource);
		if (defaultSource != null) {
			return defaultSource.getDataQualityReport(symbol, start, end);
		}
		return new DataQualityReport(symbol, start, end);
	}

	// Getter方法
	public Map<String, DataSource> getDataSources() {
		return new HashMap<>(dataSources);
	}

	public String getDefaultDataSource() {
		return defaultDataSource;
	}

	// 同步数据到CSV
	/**
	 * 同步数据到CSV
	 */
	public void syncToCSV(String symbol, LocalDateTime start, LocalDateTime end) {
		try {
			DataSource csvDataSource = dataSources.get("CSV");
			DataSource akShareDataSource = dataSources.get("AKSHARE");

			if (csvDataSource instanceof CSVDataSource && akShareDataSource != null) {
				// 从AKShare加载数据
				List<BarEvent> bars = akShareDataSource.loadHistoricalData(symbol, start, end);

				if (!bars.isEmpty()) {
					// 保存到CSV
					((CSVDataSource) csvDataSource).saveToCSV(symbol, bars);
					System.out.println("? 数据同步完成: " + symbol + " -> " + bars.size() + "条记录");
				} else {
					System.out.println("? 无数据可同步");
				}
			} else {
				System.out.println("? 数据源配置不完整，无法同步");
			}
		} catch (Exception e) {
			System.err.println("数据同步失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 同步多个标的到CSV
	 */
	public void syncMultipleToCSV(List<String> symbols, LocalDateTime start, LocalDateTime end) {
		for (String symbol : symbols) {
			System.out.println("同步标的: " + symbol);
			syncToCSV(symbol, start, end);
		}
	}

	/**
	 * 获取所有已加载的Bar数据（为回测引擎提供） 这个方法主要用于 EventDrivenBacktestEngine
	 */
	public List<BarEvent> getAllBars() {
		List<BarEvent> allBars = new ArrayList<>();

		// 从所有数据源收集数据
		for (DataSource dataSource : dataSources.values()) {
			// 如果数据源实现了 DataFeed 接口，可以获取所有bars
			if (dataSource instanceof DataFeed) {
				DataFeed dataFeed = (DataFeed) dataSource;
				allBars.addAll(dataFeed.getAllBars());
			}
		}

		return allBars;
	}

	/**
	 * 获取指定数据源的所有Bar数据
	 */
	public List<BarEvent> getAllBars(String sourceName) {
		DataSource dataSource = dataSources.get(sourceName.toUpperCase());
		if (dataSource instanceof DataFeed) {
			return ((DataFeed) dataSource).getAllBars();
		}
		return new ArrayList<>();
	}

}