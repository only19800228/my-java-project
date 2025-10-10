package com.Quantitative.config.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.Quantitative.common.utils.TradingLogger;

/**
 * 动态配置管理器 - 完全重写的安全版本
 */
public class DynamicConfigManager {
	private static volatile DynamicConfigManager INSTANCE;

	final Map<String, Object> configStore;
	final Map<String, CopyOnWriteArrayList<Consumer<Object>>> configListeners;
	final Map<String, Object> configBackup;
	private volatile boolean initialized = false;

	// 私有构造函数
	private DynamicConfigManager() {
		// 使用更安全的方式初始化
		this.configStore = new ConcurrentHashMap<>(16);
		this.configListeners = new ConcurrentHashMap<>(8);
		this.configBackup = new ConcurrentHashMap<>(8);
		initialize();
	}

	// 双重检查锁定的单例模式
	public static DynamicConfigManager getInstance() {
		if (INSTANCE == null) {
			synchronized (DynamicConfigManager.class) {
				if (INSTANCE == null) {
					INSTANCE = new DynamicConfigManager();
				}
			}
		}
		return INSTANCE;
	}

	private void initialize() {
		if (initialized) {
			return;
		}

		try {
			TradingLogger.debug("ConfigManager", "开始初始化配置管理器...");

			// 使用绝对安全的方式设置默认配置
			initializeDefaultConfigSafely();

			initialized = true;
			TradingLogger.debug("ConfigManager", "配置管理器初始化完成");

		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "初始化失败", e);
			// 即使失败也要设置紧急默认值
			setEmergencyDefaults();
			initialized = true;
		}
	}

	private void initializeDefaultConfigSafely() {
		// 先清空
		configStore.clear();

		// 一个一个地设置，避免批量操作的复杂性
		safePut("data.preferLocal", Boolean.TRUE);
		safePut("trading.commissionRate", Double.valueOf(0.0003));
		safePut("risk.maxPositionRatio", Double.valueOf(0.1));
		safePut("performance.cacheEnabled", Boolean.TRUE);
		safePut("datafeed.timeout", Integer.valueOf(30000));
		safePut("logging.level", "INFO");
		safePut("backtest.initialCapital", Double.valueOf(100000.0));
		safePut("risk.maxDrawdownLimit", Double.valueOf(0.2));
		safePut("risk.dailyLossLimit", Double.valueOf(0.05));

		TradingLogger.debug("ConfigManager", "安全默认配置设置完成，共 {} 项", configStore.size());
	}

	private void safePut(String key, Object value) {
		try {
			if (key != null && value != null) {
				configStore.put(key, value);
			}
		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "安全设置配置失败: " + key, e);
		}
	}

	private void setEmergencyDefaults() {
		TradingLogger.debug("ConfigManager", "设置紧急默认配置");
		// 只设置最关键的几个配置
		safePut("performance.cacheEnabled", Boolean.TRUE);
		safePut("data.preferLocal", Boolean.TRUE);
	}

	/**
	 * 超安全的配置设置方法
	 */
	public <T> boolean setConfig(String key, T value) {
		return setConfigInternal(key, value, true);
	}

	public <T> boolean setConfig(String key, T value, boolean validate) {
		return setConfigInternal(key, value, validate);
	}

	private <T> boolean setConfigInternal(String key, T value, boolean validate) {
		// 确保已初始化
		if (!initialized) {
			initialize();
		}

		// 极端严格的空值检查
		if (key == null) {
			TradingLogger.logRisk("ERROR", "Config", "配置键为null");
			return false;
		}

		String trimmedKey = key.trim();
		if (trimmedKey.isEmpty()) {
			TradingLogger.logRisk("ERROR", "Config", "配置键为空字符串");
			return false;
		}

		// 验证配置
		if (validate && !validateConfigInternal(trimmedKey, value)) {
			return false;
		}

		try {
			// 备份旧值
			Object oldValue = configStore.get(trimmedKey);
			if (oldValue != null) {
				configBackup.put(trimmedKey, oldValue);
			}

			// 核心操作：安全地设置值
			if (value != null) {
				// 这是可能出问题的地方，让我们用最安全的方式
				safeMapPut(configStore, trimmedKey, value);
			} else {
				configStore.remove(trimmedKey);
			}

			// 通知监听器
			safeNotifyConfigChange(trimmedKey, value);

			TradingLogger.debug("ConfigManager", "配置更新成功: {} = {}", trimmedKey, value);
			return true;

		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "设置配置失败: " + trimmedKey, e);
			return false;
		}
	}

	/**
	 * 安全的Map.put操作
	 */
	private <K, V> void safeMapPut(Map<K, V> map, K key, V value) {
		if (map == null || key == null || value == null) {
			throw new IllegalArgumentException("Map.put参数不能为null");
		}
		map.put(key, value);
	}

	/**
	 * 安全的配置验证
	 */
	// 在 DynamicConfigManager 中修复 validateConfigInternal 方法
	private <T> boolean validateConfigInternal(String key, T value) {
		if (value == null) {
			TradingLogger.debug("ConfigManager", "配置值不能为null: {}", key);
			return false;
		}

		try {
			// 根据key进行特定验证
			switch (key) {
			case "risk.maxPositionRatio":
			case "risk.maxDrawdownLimit":
			case "risk.dailyLossLimit":
			case "trading.commissionRate":
				return validatePercentage(value);

			case "datafeed.timeout":
			case "backtest.initialCapital":
				return validatePositiveNumber(value);

			case "performance.cacheEnabled":
			case "data.preferLocal":
				return value instanceof Boolean;

			case "logging.level":
				return validateLogLevel(value);

			default:
				// 对于未知配置项，也进行基本验证
				return validateUnknownConfig(value);
			}
		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "配置验证异常: " + key, e);
			return false;
		}
	}

	// 新增：未知配置项的基本验证
	private <T> boolean validateUnknownConfig(T value) {
		// 对于未知配置项，只进行基本类型检查
		if (value instanceof String || value instanceof Number || value instanceof Boolean
				|| value instanceof java.util.List || value instanceof java.util.Map) {
			return true;
		}
		TradingLogger.debug("ConfigManager", "未知配置类型不支持: {}", value.getClass().getSimpleName());
		return false;
	}

	// 修复百分比验证方法
	private <T> boolean validatePercentage(T value) {
		if (value instanceof Number) {
			double doubleValue = ((Number) value).doubleValue();
			boolean isValid = doubleValue >= 0 && doubleValue <= 1;
			if (!isValid) {
				TradingLogger.debug("ConfigManager", "百分比配置验证失败: {} 不在 [0,1] 范围内", doubleValue);
			}
			return isValid;
		}
		TradingLogger.debug("ConfigManager", "百分比配置类型错误: {}", value.getClass().getSimpleName());
		return false;
	}

	// 修复正数验证方法
	private <T> boolean validatePositiveNumber(T value) {
		if (value instanceof Number) {
			double doubleValue = ((Number) value).doubleValue();
			boolean isValid = doubleValue > 0;
			if (!isValid) {
				TradingLogger.debug("ConfigManager", "正数配置验证失败: {} <= 0", doubleValue);
			}
			return isValid;
		}
		TradingLogger.debug("ConfigManager", "正数配置类型错误: {}", value.getClass().getSimpleName());
		return false;
	}

	private <T> boolean validateLogLevel(T value) {
		if (value instanceof String) {
			String level = ((String) value).toUpperCase();
			return level.equals("DEBUG") || level.equals("INFO") || level.equals("WARN") || level.equals("ERROR");
		}
		return false;
	}

	/**
	 * 安全的配置变更通知
	 */
	private <T> void safeNotifyConfigChange(String key, T newValue) {
		try {
			CopyOnWriteArrayList<Consumer<Object>> listeners = configListeners.get(key);
			if (listeners != null && !listeners.isEmpty()) {
				for (Consumer<Object> listener : listeners) {
					try {
						listener.accept(newValue);
					} catch (Exception e) {
						TradingLogger.logSystemError("ConfigManager", "配置监听器执行失败", e);
					}
				}
			}
		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "通知配置变更失败", e);
		}
	}

	/**
	 * 获取配置值 - 超安全版本
	 */
	@SuppressWarnings("unchecked")
	public <T> T getConfig(String key, T defaultValue) {
		if (!initialized) {
			initialize();
		}

		if (key == null) {
			return defaultValue;
		}

		String trimmedKey = key.trim();
		if (trimmedKey.isEmpty()) {
			return defaultValue;
		}

		try {
			Object value = configStore.get(trimmedKey);
			if (value == null) {
				return defaultValue;
			}

			// 尝试类型转换
			return (T) value;

		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "获取配置失败: " + key, e);
			return defaultValue;
		}
	}

	public <T> T getConfig(String key) {
		return getConfig(key, null);
	}

	/**
	 * 添加配置监听器
	 */
	public <T> void addConfigListener(String key, Consumer<T> listener) {
		if (key == null || listener == null) {
			return;
		}

		String trimmedKey = key.trim();
		if (trimmedKey.isEmpty()) {
			return;
		}

		try {
			configListeners.computeIfAbsent(trimmedKey, k -> new CopyOnWriteArrayList<>())
					.add((Consumer<Object>) listener);
		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "添加配置监听器失败", e);
		}
	}

	/**
	 * 回滚配置
	 */
	public boolean rollbackConfig(String key) {
		if (key == null) {
			return false;
		}

		String trimmedKey = key.trim();
		if (!configBackup.containsKey(trimmedKey)) {
			return false;
		}

		try {
			Object oldValue = configBackup.get(trimmedKey);
			safeMapPut(configStore, trimmedKey, oldValue);
			configBackup.remove(trimmedKey);
			safeNotifyConfigChange(trimmedKey, oldValue);
			return true;
		} catch (Exception e) {
			TradingLogger.logSystemError("ConfigManager", "配置回滚失败", e);
			return false;
		}
	}

	// 其他方法...
	public Map<String, Object> exportAllConfigs() {
		return new ConcurrentHashMap<>(configStore);
	}

	public boolean isInitialized() {
		return initialized;
	}
}