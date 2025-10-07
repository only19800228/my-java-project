// 新增：统一配置管理类
package com.Quantitative.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 统一系统配置管理
 */
public class SystemConfig {
	private static final String DEFAULT_CONFIG = "application.properties";
	private static final Map<String, String> configCache = new HashMap<>();
	private static Properties properties = new Properties();
	private static boolean initialized = false;

	static {
		loadDefaultConfig();
	}

	/**
	 * 加载默认配置
	 */
	private static void loadDefaultConfig() {
		try {
			// 这里可以从文件加载，现在先设置默认值
			setDefaultProperties();
			initialized = true;
			System.out.println("✅ 系统配置初始化完成");
		} catch (Exception e) {
			System.err.println("❌ 加载配置文件失败: " + e.getMessage());
			setDefaultProperties();
		}
	}

	/**
	 * 设置默认属性值
	 */
	private static void setDefaultProperties() {
		// 数据配置
		properties.setProperty("datafeed.akshare.baseurl", "http://127.0.0.1:8888/api/akshare");
		properties.setProperty("datafeed.akshare.timeout", "30000");
		properties.setProperty("datafeed.cache.enabled", "true");

		// 交易配置
		properties.setProperty("trading.commission.rate", "0.0003");
		properties.setProperty("trading.min.trade.quantity", "100");
		properties.setProperty("trading.slippage.rate", "0.001");

		// 风险配置
		properties.setProperty("risk.max.position.ratio", "0.1");
		properties.setProperty("risk.max.drawdown.limit", "0.2");
		properties.setProperty("risk.daily.loss.limit", "0.05");

		// 性能配置
		properties.setProperty("performance.monitor.enabled", "true");
		properties.setProperty("indicator.cache.size", "2000");

		// 日志配置
		properties.setProperty("log.level", "INFO");
		properties.setProperty("log.trading.enabled", "true");
	}

	/**
	 * 获取字符串配置值
	 */
	public static String getString(String key) {
		return getString(key, null);
	}

	public static String getString(String key, String defaultValue) {
		// 检查缓存
		String cachedValue = configCache.get(key);
		if (cachedValue != null) {
			return cachedValue;
		}

		// 从系统属性获取（优先级最高）
		String systemValue = System.getProperty(key);
		if (systemValue != null) {
			configCache.put(key, systemValue);
			return systemValue;
		}

		// 从配置文件获取
		String configValue = properties.getProperty(key, defaultValue);
		configCache.put(key, configValue);

		return configValue;
	}

	/**
	 * 获取整数配置值
	 */
	public static int getInt(String key) {
		return getInt(key, 0);
	}

	public static int getInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			System.err.println("❌ 配置值解析失败: " + key + "=" + getString(key) + ", 使用默认值: " + defaultValue);
			return defaultValue;
		}
	}

	/**
	 * 获取浮点数配置值
	 */
	public static double getDouble(String key) {
		return getDouble(key, 0.0);
	}

	public static double getDouble(String key, double defaultValue) {
		try {
			return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			System.err.println("❌ 配置值解析失败: " + key + "=" + getString(key) + ", 使用默认值: " + defaultValue);
			return defaultValue;
		}
	}

	/**
	 * 获取布尔配置值
	 */
	public static boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		String value = getString(key, String.valueOf(defaultValue));
		return Boolean.parseBoolean(value);
	}

	/**
	 * 重新加载配置
	 */
	public static void reload() {
		configCache.clear();
		properties.clear();
		loadDefaultConfig();
		System.out.println("✅ 系统配置已重新加载");
	}

	/**
	 * 设置配置值（用于测试）
	 */
	public static void setProperty(String key, String value) {
		properties.setProperty(key, value);
		configCache.put(key, value);
	}

	/**
	 * 获取所有配置（用于诊断）
	 */
	public static Properties getAllProperties() {
		return new Properties(properties);
	}

	public static boolean isInitialized() {
		return initialized;
	}
}