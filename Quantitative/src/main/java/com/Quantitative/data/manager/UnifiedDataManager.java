package com.Quantitative.data.manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.Quantitative.common.cache.UnifiedCacheManager;
import com.Quantitative.common.monitor.MonitorUtils;
import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.data.DataInfo;
import com.Quantitative.data.DataSource;
import com.Quantitative.data.validation.DataQualityReport;
import com.Quantitative.data.validation.DataValidator;

/**
 * 统一数据管理器 - 修复版本
 */
public class UnifiedDataManager {
	private static final UnifiedDataManager INSTANCE = new UnifiedDataManager();

	private final Map<String, DataFeed> dataFeeds = new ConcurrentHashMap<>();
	private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
	private final UnifiedCacheManager cacheManager = UnifiedCacheManager.getInstance();
	private DataFeed defaultDataFeed;

	private UnifiedDataManager() {
		initializeDefaultFeeds();
	}

	public static UnifiedDataManager getInstance() {
		return INSTANCE;
	}

	private void initializeDefaultFeeds() {
		try {
			// 这里可以初始化AKShare数据源等
			TradingLogger.debug("DataManager", "初始化数据管理器");
		} catch (Exception e) {
			TradingLogger.logSystemError("DataManager", "初始化失败", e);
		}
	}

	/**
	 * 注册数据源
	 */
	public void registerDataSource(String name, DataSource dataSource) {
		dataSources.put(name, dataSource);
		TradingLogger.debug("DataManager", "注册数据源: {}", name);
	}

	/**
	 * 注册数据馈送
	 */
	public void registerDataFeed(String name, DataFeed dataFeed) {
		dataFeeds.put(name, dataFeed);
		if (defaultDataFeed == null) {
			defaultDataFeed = dataFeed;
		}
		TradingLogger.debug("DataManager", "注册数据馈送: {}", name);
	}

	/**
	 * 获取历史数据（带缓存和监控）
	 */
	public List<BarEvent> getHistoricalData(String symbol, LocalDateTime start, LocalDateTime end, String timeframe) {
		return MonitorUtils.monitor("DataManager", "getHistoricalData", () -> {
			String cacheKey = String.format("hist_%s_%s_%s_%s", symbol, start.toLocalDate(), end.toLocalDate(),
					timeframe);

			// 尝试从缓存获取
			return cacheManager.getCached("historical_data", cacheKey, () -> {
				if (defaultDataFeed == null) {
					throw new IllegalStateException("没有可用的数据馈送");
				}

				List<BarEvent> bars = defaultDataFeed.loadHistoricalData(symbol, start, end);

				// 数据质量检查
				if (bars != null && !bars.isEmpty()) {
					DataQualityReport qualityReport = createDataQualityReport(symbol, start, end, bars);
					if (qualityReport.getDataQualityScore() < 80) {
						TradingLogger.logRisk("WARN", "DataQuality", "数据质量较低: {} - 评分: {:.1f}", symbol,
								qualityReport.getDataQualityScore());
					}

					TradingLogger.debug("DataManager", "加载数据: {} bars, 质量: {:.1f}", bars.size(),
							qualityReport.getDataQualityScore());
				} else {
					TradingLogger.logRisk("WARN", "DataQuality", "未获取到数据: {}", symbol);
				}

				return bars;
			});
		});
	}

	/**
	 * 创建数据质量报告
	 */
	private DataQualityReport createDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end,
			List<BarEvent> bars) {
		DataQualityReport report = new DataQualityReport(symbol, start, end);

		for (BarEvent bar : bars) {
			DataValidator.ValidationResult result = DataValidator.validateBar(bar);
			report.addValidationResult(result);
		}

		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(bars);
		report.setSeriesValidation(seriesResult);

		return report;
	}

	/**
	 * 获取数据信息
	 */
	public DataInfo getDataInfo(String symbol, LocalDateTime start, LocalDateTime end) {
		List<BarEvent> bars = getHistoricalData(symbol, start, end, "1d");
		return new DataInfo.Builder(symbol).startTime(start).endTime(end).barCount(bars != null ? bars.size() : 0)
				.timeframe("1d").dataQuality(bars == null || bars.isEmpty() ? "EMPTY" : "COMPLETE").build();
	}

	/**
	 * 检查数据可用性
	 */
	public boolean isDataAvailable(String symbol, LocalDateTime start, LocalDateTime end) {
		try {
			DataInfo info = getDataInfo(symbol, start, end);
			return !info.isEmpty() && info.getBarCount() > 0;
		} catch (Exception e) {
			TradingLogger.debug("DataManager", "数据可用性检查失败: {} - {}", symbol, e.getMessage());
			return false;
		}
	}

	/**
	 * 获取数据质量报告
	 */
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		List<BarEvent> bars = getHistoricalData(symbol, start, end, "1d");

		if (bars == null || bars.isEmpty()) {
			DataQualityReport emptyReport = new DataQualityReport(symbol, start, end);
			TradingLogger.debug("DataManager", "无数据可生成质量报告: {}", symbol);
			return emptyReport;
		}

		return createDataQualityReport(symbol, start, end, bars);
	}

	/**
	 * 清理数据缓存
	 */
	public void clearCache() {
		cacheManager.cleanupAll();
		TradingLogger.debug("DataManager", "清理数据缓存");
	}

	// Getter方法
	public DataFeed getDefaultDataFeed() {
		return defaultDataFeed;
	}

	public void setDefaultDataFeed(DataFeed dataFeed) {
		this.defaultDataFeed = dataFeed;
	}
}