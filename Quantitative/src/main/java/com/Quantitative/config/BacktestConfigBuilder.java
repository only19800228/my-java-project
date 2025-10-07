package com.Quantitative.config;

import java.time.LocalDateTime;

/**
 * BacktestConfig 构建器 - 提供更灵活的配置方式
 */
public class BacktestConfigBuilder {
	private BacktestConfig config;

	public BacktestConfigBuilder() {
		this.config = new BacktestConfig();
	}

	public BacktestConfigBuilder(String symbol, LocalDateTime start, LocalDateTime end, double capital) {
		this.config = new BacktestConfig(symbol, start, end, capital);
	}

	public BacktestConfigBuilder setSymbol(String symbol) {
		config.setSymbol(symbol);
		return this;
	}

	public BacktestConfigBuilder setPeriod(LocalDateTime start, LocalDateTime end) {
		config.setStartDate(start);
		config.setEndDate(end);
		return this;
	}

	public BacktestConfigBuilder setCapital(double capital) {
		config.setInitialCapital(capital);
		return this;
	}

	public BacktestConfigBuilder setDebugMode(boolean debug) {
		config.setDebugMode(debug);
		return this;
	}

	public BacktestConfigBuilder setMaxBars(int maxBars) {
		config.setMaxBars(maxBars);
		return this;
	}

	public BacktestConfigBuilder setPreferLocalData(boolean preferLocal) {
		config.setPreferLocalData(preferLocal);
		return this;
	}

	public BacktestConfigBuilder setDataDirectory(String directory) {
		config.setDataDirectory(directory);
		return this;
	}

	public BacktestConfigBuilder setSlowMode(boolean slowMode) {
		config.setSlowMode(slowMode);
		return this;
	}

	public BacktestConfigBuilder addRiskParam(String key, Object value) {
		config.addRiskParam(key, value);
		return this;
	}

	public BacktestConfig build() {
		return config;
	}

	/**
	 * 创建默认配置
	 */
	public static BacktestConfig createDefault(String symbol, LocalDateTime start, LocalDateTime end) {
		return new BacktestConfigBuilder().setSymbol(symbol).setPeriod(start, end).setCapital(100000.0)
				.setPreferLocalData(true).build();
	}

	/**
	 * 创建网络优先配置
	 */
	public static BacktestConfig createNetworkFirst(String symbol, LocalDateTime start, LocalDateTime end) {
		return new BacktestConfigBuilder().setSymbol(symbol).setPeriod(start, end).setCapital(100000.0)
				.setPreferLocalData(false).build();
	}
}