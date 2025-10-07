package com.Quantitative.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 风险配置类
 */
public class RiskConfig {
	private final Map<String, Object> config;

	public RiskConfig() {
		this.config = new HashMap<>();
		setDefaultConfig();
	}

	private void setDefaultConfig() {
		config.put("maxPositionRatio", 0.1);
		config.put("maxDrawdownLimit", 0.2);
		config.put("dailyLossLimit", 0.05);
		config.put("maxConsecutiveLosses", 5);
		config.put("maxTotalRisk", 0.3);
		config.put("enableRealTimeMonitoring", true);
		config.put("alertThreshold", 0.8);
	}

	public double getMaxPositionRatio() {
		return (Double) config.get("maxPositionRatio");
	}

	public void setMaxPositionRatio(double ratio) {
		config.put("maxPositionRatio", ratio);
	}

	public double getMaxDrawdownLimit() {
		return (Double) config.get("maxDrawdownLimit");
	}

	public void setMaxDrawdownLimit(double limit) {
		config.put("maxDrawdownLimit", limit);
	}

	public Map<String, Object> getAllConfig() {
		return new HashMap<>(config);
	}

	public void updateConfig(Map<String, Object> newConfig) {
		config.putAll(newConfig);
	}
}