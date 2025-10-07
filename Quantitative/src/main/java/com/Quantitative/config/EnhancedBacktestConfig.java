package com.Quantitative.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 完善的回测配置系统
 */
public class EnhancedBacktestConfig extends BacktestConfig {
	private Map<String, Object> performanceSettings;
	private Map<String, Object> riskSettings;
	private Map<String, Object> dataSettings;
	private boolean enablePerformanceMonitor;
	private boolean enableMemoryMonitor;
	private int maxMemoryUsageMB;

	public EnhancedBacktestConfig() {
		super();
		initializeEnhancedSettings();
	}

	public EnhancedBacktestConfig(String symbol, LocalDateTime startDate, LocalDateTime endDate,
			double initialCapital) {
		super(symbol, startDate, endDate, initialCapital);
		initializeEnhancedSettings();
	}

	private void initializeEnhancedSettings() {
		this.performanceSettings = new HashMap<>();
		this.riskSettings = new HashMap<>();
		this.dataSettings = new HashMap<>();

		// 默认性能设置
		performanceSettings.put("enableCaching", true);
		performanceSettings.put("cacheMaxSize", 10000);
		performanceSettings.put("cacheExpireMinutes", 30);
		performanceSettings.put("enableCompression", false);

		// 默认风险设置
		riskSettings.put("maxPositionRatio", 0.1);
		riskSettings.put("maxDrawdownLimit", 0.2);
		riskSettings.put("dailyLossLimit", 0.05);
		riskSettings.put("enableStopLoss", true);
		riskSettings.put("enableTakeProfit", true);

		// 默认数据设置
		dataSettings.put("preferLocalData", true);
		dataSettings.put("dataQualityCheck", true);
		dataSettings.put("autoRepairData", true);

		this.enablePerformanceMonitor = true;
		this.enableMemoryMonitor = true;
		this.maxMemoryUsageMB = 512;
	}

	// Getter和Setter方法
	public Map<String, Object> getPerformanceSettings() {
		return new HashMap<>(performanceSettings);
	}

	public void setPerformanceSetting(String key, Object value) {
		performanceSettings.put(key, value);
	}

	public Object getPerformanceSetting(String key) {
		return performanceSettings.get(key);
	}

	public Map<String, Object> getRiskSettings() {
		return new HashMap<>(riskSettings);
	}

	public void setRiskSetting(String key, Object value) {
		riskSettings.put(key, value);
	}

	public Object getRiskSetting(String key) {
		return riskSettings.get(key);
	}

	public Map<String, Object> getDataSettings() {
		return new HashMap<>(dataSettings);
	}

	public void setDataSetting(String key, Object value) {
		dataSettings.put(key, value);
	}

	public Object getDataSetting(String key) {
		return dataSettings.get(key);
	}

	public boolean isEnablePerformanceMonitor() {
		return enablePerformanceMonitor;
	}

	public void setEnablePerformanceMonitor(boolean enablePerformanceMonitor) {
		this.enablePerformanceMonitor = enablePerformanceMonitor;
	}

	public boolean isEnableMemoryMonitor() {
		return enableMemoryMonitor;
	}

	public void setEnableMemoryMonitor(boolean enableMemoryMonitor) {
		this.enableMemoryMonitor = enableMemoryMonitor;
	}

	public int getMaxMemoryUsageMB() {
		return maxMemoryUsageMB;
	}

	public void setMaxMemoryUsageMB(int maxMemoryUsageMB) {
		this.maxMemoryUsageMB = maxMemoryUsageMB;
	}

	/**
	 * 验证配置有效性
	 */
	public List<String> validate() {
		List<String> errors = new ArrayList<>();

		if (getSymbol() == null || getSymbol().trim().isEmpty()) {
			errors.add("标的代码不能为空");
		}

		if (getStartDate() == null || getEndDate() == null) {
			errors.add("时间范围不能为空");
		} else if (getStartDate().isAfter(getEndDate())) {
			errors.add("开始时间不能晚于结束时间");
		}

		if (getInitialCapital() <= 0) {
			errors.add("初始资金必须大于0");
		}

		// 验证风险设置
		double maxPositionRatio = (Double) riskSettings.get("maxPositionRatio");
		if (maxPositionRatio <= 0 || maxPositionRatio > 1) {
			errors.add("最大仓位比例必须在0到1之间");
		}

		return errors;
	}

	/**
	 * 导出配置为Map
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> configMap = new LinkedHashMap<>();

		// 基础配置
		configMap.put("symbol", getSymbol());
		configMap.put("startDate", getStartDate());
		configMap.put("endDate", getEndDate());
		configMap.put("initialCapital", getInitialCapital());
		configMap.put("debugMode", isDebugMode());

		// 增强配置
		configMap.put("performanceSettings", new HashMap<>(performanceSettings));
		configMap.put("riskSettings", new HashMap<>(riskSettings));
		configMap.put("dataSettings", new HashMap<>(dataSettings));
		configMap.put("enablePerformanceMonitor", enablePerformanceMonitor);
		configMap.put("enableMemoryMonitor", enableMemoryMonitor);
		configMap.put("maxMemoryUsageMB", maxMemoryUsageMB);

		return configMap;
	}

	@Override
	public String toString() {
		return String.format(
				"EnhancedBacktestConfig{symbol='%s', period=%s to %s, capital=%.2f, performanceMonitor=%s}",
				getSymbol(), getStartDate().toLocalDate(), getEndDate().toLocalDate(), getInitialCapital(),
				enablePerformanceMonitor);
	}
}