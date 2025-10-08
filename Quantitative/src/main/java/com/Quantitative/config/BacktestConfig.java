package com.Quantitative.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 回测配置类 - 添加向后兼容的构造函数
 */
public class BacktestConfig {
	private String symbol;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private double initialCapital = 100000.0;
	private boolean debugMode = false;
	private int maxBars = 0;
	private Map<String, Object> riskParams = new HashMap<>();

	// 新增字段：数据源偏好
	private boolean preferLocalData = true; // 默认本地优先
	private String dataDirectory = "data/csv";
	private boolean slowMode = false;

	// 默认构造函数
	public BacktestConfig() {
		// 默认初始化
	}

	// 向后兼容的构造函数
	public BacktestConfig(String symbol, LocalDateTime startDate, LocalDateTime endDate, double initialCapital) {
		this.symbol = symbol;
		this.startDate = startDate;
		this.endDate = endDate;
		this.initialCapital = initialCapital;
	}

	// 完整的构造函数
	public BacktestConfig(String symbol, LocalDateTime startDate, LocalDateTime endDate, double initialCapital,
			boolean debugMode, int maxBars) {
		this.symbol = symbol;
		this.startDate = startDate;
		this.endDate = endDate;
		this.initialCapital = initialCapital;
		this.debugMode = debugMode;
		this.maxBars = maxBars;
	}

	// Getter 和 Setter 方法
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDateTime string) {
		this.startDate = string;
	}

	public LocalDateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDateTime string) {
		this.endDate = string;
	}

	public double getInitialCapital() {
		return initialCapital;
	}

	public void setInitialCapital(double initialCapital) {
		this.initialCapital = initialCapital;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public int getMaxBars() {
		return maxBars;
	}

	public void setMaxBars(int maxBars) {
		this.maxBars = maxBars;
	}

	public Map<String, Object> getRiskParams() {
		return riskParams;
	}

	public void setRiskParams(Map<String, Object> riskParams) {
		this.riskParams = riskParams;
	}

	// 新增的 Getter 和 Setter
	public boolean isPreferLocalData() {
		return preferLocalData;
	}

	public void setPreferLocalData(boolean preferLocalData) {
		this.preferLocalData = preferLocalData;
	}

	public String getDataDirectory() {
		return dataDirectory;
	}

	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	public boolean isSlowMode() {
		return slowMode;
	}

	public void setSlowMode(boolean slowMode) {
		this.slowMode = slowMode;
	}

	/**
	 * 便捷方法：设置风险参数
	 */
	public void addRiskParam(String key, Object value) {
		this.riskParams.put(key, value);
	}

	/**
	 * 便捷方法：获取风险参数
	 */
	public Object getRiskParam(String key) {
		return this.riskParams.get(key);
	}

	/**
	 * 便捷方法：获取风险参数（带默认值）
	 */
	public Object getRiskParam(String key, Object defaultValue) {
		return this.riskParams.getOrDefault(key, defaultValue);
	}

	@Override
	public String toString() {
		return String.format("BacktestConfig{symbol='%s', period=%s to %s, capital=%.2f, localFirst=%s}", symbol,
				startDate.toLocalDate(), endDate.toLocalDate(), initialCapital, preferLocalData);
	}

}