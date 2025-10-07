package com.Quantitative.data.adaptive;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataInfo;
import com.Quantitative.data.DataSource;
import com.Quantitative.data.adapter.AKShareDataSourceAdapter;
import com.Quantitative.data.csv.CSVDataSource;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * 智能自适应数据源 - 自动在网络和本地数据源之间切换
 */
public class AdaptiveDataSource implements DataSource {

	private final AKShareDataSourceAdapter networkDataSource;
	private final CSVDataSource localDataSource;
	private String status = "CREATED";
	private Map<String, Object> config;

	// 数据源优先级配置
	private List<DataSource> dataSourcePriority;

	// 缓存管理
	private Map<String, List<BarEvent>> dataCache;
	private boolean cacheEnabled = true;

	public AdaptiveDataSource() {
		this.networkDataSource = new AKShareDataSourceAdapter();
		this.localDataSource = new CSVDataSource();

		// 配置CSV数据源的日期格式
		Map<String, Object> csvConfig = new HashMap<>();
		csvConfig.put("dateFormat", "yyyy-MM-dd");
		this.localDataSource.configure(csvConfig);

		this.dataCache = new HashMap<>();
		initializeDataSourcePriority();
	}

	public AdaptiveDataSource(String csvDataDirectory) {
		this.networkDataSource = new AKShareDataSourceAdapter();
		this.localDataSource = new CSVDataSource(csvDataDirectory);
		this.dataCache = new HashMap<>();
		initializeDataSourcePriority();
	}

	private void initializeDataSourcePriority() {
		dataSourcePriority = new ArrayList<>();
		// 默认优先级：网络 -> 本地
		dataSourcePriority.add(networkDataSource);
		dataSourcePriority.add(localDataSource);
	}

	@Override
	public void initialize() {
		System.out.println("? 初始化自适应数据源...");

		// 初始化所有数据源
		for (DataSource dataSource : dataSourcePriority) {
			try {
				dataSource.initialize();
				System.out.println("? 初始化数据源: " + dataSource.getName());
			} catch (Exception e) {
				System.err.println("? 数据源初始化失败: " + dataSource.getName() + " - " + e.getMessage());
			}
		}

		this.status = "INITIALIZED";
		System.out.println("? 自适应数据源初始化完成");
	}

	@Override
	public void configure(Map<String, Object> config) {
		this.config = config;

		if (config != null) {
			// 配置缓存
			if (config.containsKey("cacheEnabled")) {
				this.cacheEnabled = (Boolean) config.get("cacheEnabled");
			}

			// 配置数据源优先级
			if (config.containsKey("dataSourcePriority")) {
				@SuppressWarnings("unchecked")
				List<String> priority = (List<String>) config.get("dataSourcePriority");
				setDataSourcePriority(priority);
			}
		}

		// 传递配置给子数据源
		if (networkDataSource != null) {
			networkDataSource.configure(config);
		}
		if (localDataSource != null) {
			localDataSource.configure(config);
		}
	}

	/**
	 * 设置数据源优先级
	 */
	public void setDataSourcePriority(List<String> priority) {
		dataSourcePriority.clear();

		for (String sourceName : priority) {
			switch (sourceName.toUpperCase()) {
			case "NETWORK":
			case "AKSHARE":
				dataSourcePriority.add(networkDataSource);
				break;
			case "LOCAL":
			case "CSV":
				dataSourcePriority.add(localDataSource);
				break;
			}
		}

		System.out.println("? 设置数据源优先级: " + priority);
	}

	@Override
	public List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end) {
		// 检查缓存
		String cacheKey = generateCacheKey(symbol, start, end);
		if (cacheEnabled && dataCache.containsKey(cacheKey)) {
			System.out.println("? 从缓存加载数据: " + symbol);
			return new ArrayList<>(dataCache.get(cacheKey));
		}

		List<BarEvent> result = new ArrayList<>();
		String usedDataSource = null;

		// 按优先级尝试各个数据源
		for (DataSource dataSource : dataSourcePriority) {
			try {
				System.out.printf("? 尝试从 %s 加载数据: %s%n", dataSource.getDataSourceType(), symbol);

				List<BarEvent> data = dataSource.loadHistoricalData(symbol, start, end);

				if (!data.isEmpty()) {
					result = data;
					usedDataSource = dataSource.getDataSourceType();

					// 如果从网络加载成功，自动保存到本地
					if (dataSource == networkDataSource && localDataSource != null) {
						try {
							localDataSource.saveToCSV(symbol, data);
							System.out.println("? 自动保存数据到本地CSV: " + symbol);
						} catch (Exception e) {
							System.err.println("? 保存到CSV失败: " + e.getMessage());
						}
					}

					break;
				}
			} catch (Exception e) {
				System.err.printf("? 数据源 %s 加载失败: %s%n", dataSource.getDataSourceType(), e.getMessage());
				// 继续尝试下一个数据源
			}
		}

		if (!result.isEmpty()) {
			// 缓存数据
			if (cacheEnabled) {
				dataCache.put(cacheKey, new ArrayList<>(result));
			}

			System.out.printf("? 成功从 %s 加载数据: %s, %d条记录%n", usedDataSource, symbol, result.size());
		} else {
			System.out.println("? 所有数据源均无数据: " + symbol);
		}

		return result;
	}

	/**
	 * 强制从特定数据源加载数据
	 */
	public List<BarEvent> loadHistoricalDataFromSource(String sourceType, String symbol, LocalDateTime start,
			LocalDateTime end) {
		DataSource dataSource = getDataSourceByType(sourceType);
		if (dataSource != null) {
			return dataSource.loadHistoricalData(symbol, start, end);
		}
		return new ArrayList<>();
	}

	/**
	 * 同步数据到本地
	 */
	public void syncToLocal(String symbol, LocalDateTime start, LocalDateTime end) {
		try {
			System.out.printf("? 同步数据到本地: %s %s 到 %s%n", symbol, start, end);

			List<BarEvent> data = networkDataSource.loadHistoricalData(symbol, start, end);
			if (!data.isEmpty()) {
				localDataSource.saveToCSV(symbol, data);
				System.out.println("? 数据同步完成: " + symbol + " -> " + data.size() + "条记录");

				// 更新缓存
				if (cacheEnabled) {
					String cacheKey = generateCacheKey(symbol, start, end);
					dataCache.put(cacheKey, new ArrayList<>(data));
				}
			} else {
				System.out.println("? 无数据可同步");
			}
		} catch (Exception e) {
			System.err.println("? 数据同步失败: " + e.getMessage());
		}
	}

	/**
	 * 批量同步多个标的
	 */
	public void batchSyncToLocal(List<String> symbols, LocalDateTime start, LocalDateTime end) {
		System.out.printf("? 开始批量同步 %d 个标的的数据%n", symbols.size());

		int successCount = 0;
		for (String symbol : symbols) {
			try {
				syncToLocal(symbol, start, end);
				successCount++;

				// 添加延迟，避免请求过于频繁
				Thread.sleep(100);
			} catch (Exception e) {
				System.err.println("? 同步失败: " + symbol + " - " + e.getMessage());
			}
		}

		System.out.printf("? 批量同步完成: %d/%d 成功%n", successCount, symbols.size());
	}

	private DataSource getDataSourceByType(String sourceType) {
		switch (sourceType.toUpperCase()) {
		case "NETWORK":
		case "AKSHARE":
			return networkDataSource;
		case "LOCAL":
		case "CSV":
			return localDataSource;
		default:
			return null;
		}
	}

	private String generateCacheKey(String symbol, LocalDateTime start, LocalDateTime end) {
		return String.format("%s_%s_%s", symbol, start != null ? start.toString() : "null",
				end != null ? end.toString() : "null");
	}

	@Override
	public String getName() {
		return "AdaptiveDataSource";
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		dataCache.clear();
		if (networkDataSource != null)
			networkDataSource.reset();
		if (localDataSource != null)
			localDataSource.reset();
		this.status = "RESET";
	}

	@Override
	public void shutdown() {
		dataCache.clear();
		if (networkDataSource != null)
			networkDataSource.shutdown();
		if (localDataSource != null)
			localDataSource.shutdown();
		this.status = "SHUTDOWN";
	}

	@Override
	public DataInfo getDataInfo() {
		return new DataInfo("AdaptiveSource", null, null, dataCache.size(), "adaptive");
	}

	@Override
	public boolean isConnected() {
		// 只要有一个数据源可用就返回true
		return networkDataSource.isConnected() || localDataSource.isConnected();
	}

	@Override
	public String getDataSourceType() {
		return "ADAPTIVE_SOURCE";
	}

	@Override
	public List<String> getAvailableSymbols() {
		// 合并所有数据源的可用标的
		List<String> allSymbols = new ArrayList<>();

		// 优先使用本地数据源的标的（通常更可靠）
		if (localDataSource != null) {
			allSymbols.addAll(localDataSource.getAvailableSymbols());
		}

		// 添加网络数据源的标的（去重）
		if (networkDataSource != null) {
			for (String symbol : networkDataSource.getAvailableSymbols()) {
				if (!allSymbols.contains(symbol)) {
					allSymbols.add(symbol);
				}
			}
		}

		return allSymbols;
	}

	@Override
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		// 使用实际加载数据的数据源生成报告
		List<BarEvent> data = loadHistoricalData(symbol, start, end);
		DataQualityReport report = new DataQualityReport(symbol, start, end);

		// 这里可以调用数据验证器来生成详细的质量报告
		// DataValidator.validateSeries(data, report);

		return report;
	}

	// Getter方法
	public AKShareDataSourceAdapter getNetworkDataSource() {
		return networkDataSource;
	}

	public CSVDataSource getLocalDataSource() {
		return localDataSource;
	}

	public boolean isCacheEnabled() {
		return cacheEnabled;
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}
}