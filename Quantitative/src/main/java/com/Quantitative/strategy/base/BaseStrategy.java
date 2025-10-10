package com.Quantitative.strategy.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.cache.UnifiedCacheManager;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.core.interfaces.TradingComponent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.portfolio.Portfolio;

/**
 * 优化后的策略基类 - 使用统一缓存
 */
public abstract class BaseStrategy implements TradingComponent {
	protected String name;
	protected DataFeed dataFeed;
	protected Portfolio portfolio;
	protected Map<String, Object> parameters;
	protected List<BarEvent> historicalData;
	protected BarEvent currentBar;

	// 使用统一缓存管理器
	protected final UnifiedCacheManager cacheManager = UnifiedCacheManager.getInstance();
	protected boolean cacheEnabled = true;
	protected boolean initialized = false;
	protected boolean debugMode = false;
	protected String status = "CREATED";

	public BaseStrategy(String name) {
		this.name = name;
		this.parameters = new HashMap<>();
		this.historicalData = new ArrayList<>();
	}

	// ==================== TradingComponent 接口实现 ====================

	@Override
	public void initialize() {
		long startTime = System.nanoTime();

		try {
			System.out.println("初始化策略: " + name);
			preloadData();
			init();
			initialized = true;
			status = "INITIALIZED";

			long duration = System.nanoTime() - startTime;
			System.out.printf("策略初始化完成，耗时: %.3fms%n", duration / 1_000_000.0);

		} catch (Exception e) {
			status = "INIT_FAILED";
			throw new RuntimeException("策略初始化失败: " + name, e);
		}
	}

	@Override
	public void configure(Map<String, Object> config) {
		if (config != null) {
			this.parameters.putAll(config);

			// 应用常用配置
			if (config.containsKey("debugMode")) {
				this.debugMode = (Boolean) config.get("debugMode");
			}
			if (config.containsKey("cacheEnabled")) {
				this.cacheEnabled = (Boolean) config.get("cacheEnabled");
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		System.out.println("重置策略: " + name);
		this.historicalData.clear();
		this.currentBar = null;
		this.initialized = false;
		this.status = "RESET";

		// 清理策略相关缓存
		cacheManager.getOrCreateRegion("strategies", 1000, 30 * 60 * 1000L).clear();
	}

	@Override
	public void shutdown() {
		System.out.println("关闭策略: " + name);
		this.status = "SHUTDOWN";
	}

	// ==================== 策略核心方法 ====================

	/**
	 * 处理K线数据的主方法
	 */
	public List<SignalEvent> onBar(BarEvent bar) {
		this.currentBar = bar;
		List<SignalEvent> signals = new ArrayList<>();
		calculateSignals(bar, signals);
		return signals;
	}

	/**
	 * 抽象方法 - 由子类实现具体的信号生成逻辑
	 */
	protected abstract void calculateSignals(BarEvent bar, List<SignalEvent> signals);

	/**
	 * 抽象初始化方法 - 由子类实现
	 */
	protected abstract void init();

	// ==================== 数据管理方法 ====================

	public void setParameter(String key, Object value) {
		parameters.put(key, value);
	}

	public Object getParameter(String key) {
		return parameters.get(key);
	}

	public Map<String, Object> getParameters() {
		return new HashMap<>(parameters);
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
		setParameter("cacheEnabled", cacheEnabled);
	}

	public DataFeed getDataFeed() {
		return this.dataFeed;
	}

	public Portfolio getPortfolio() {
		return this.portfolio;
	}

	public void setDataFeed(DataFeed dataFeed) {
		this.dataFeed = dataFeed;
	}

	public void setPortfolio(Portfolio portfolio) {
		this.portfolio = portfolio;
	}

	// ==================== 工具方法 ====================

	/**
	 * 数据预加载
	 */
	protected void preloadData() {
		if (dataFeed == null) {
			System.out.println("[数据预加载] 数据源未设置");
			return;
		}

		// 避免重复加载
		if (historicalData != null && !historicalData.isEmpty()) {
			if (debugMode) {
				System.out.printf("[数据预加载] 已有数据: %d 条，跳过重复加载%n", historicalData.size());
			}
			return;
		}

		List<BarEvent> allBars = dataFeed.getAllBars();
		if (allBars != null && !allBars.isEmpty()) {
			this.historicalData = new ArrayList<>(allBars);
			System.out.printf("[数据预加载] 加载数据: %d 条%n", historicalData.size());
		} else {
			System.out.println("[数据预加载] 警告：未获取到任何数据！");
			this.historicalData = new ArrayList<>();
		}
	}

	/**
	 * 带缓存的指标计算
	 */
	protected Double calculateWithCache(String indicatorName, List<Double> prices, int period, Object... params) {
		if (!cacheEnabled) {
			return calculateIndicator(indicatorName, prices, period, params);
		}

		// 生成缓存键
		String cacheKey = generateCacheKey(indicatorName, prices, period, params);

		return cacheManager.getCached("indicators", cacheKey, () -> {
			long startTime = System.nanoTime();
			Double result = calculateIndicator(indicatorName, prices, period, params);
			long duration = System.nanoTime() - startTime;

			if (debugMode && result != null && duration > 1_000_000) {
				System.out.printf("[策略缓存] %s(%d) 计算耗时: %.3fms%n", indicatorName, period, duration / 1_000_000.0);
			}

			return result;
		});
	}

	/**
	 * 生成缓存键
	 */
	protected String generateCacheKey(String indicatorName, List<Double> prices, int period, Object... params) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(indicatorName).append("_").append(period);

		// 使用价格数据的哈希（优化性能，只取关键点）
		if (prices != null && !prices.isEmpty()) {
			int sampleSize = Math.min(10, prices.size());
			for (int i = prices.size() - sampleSize; i < prices.size(); i++) {
				keyBuilder.append("_").append(String.format("%.6f", prices.get(i)));
			}
		}

		// 添加参数
		for (Object param : params) {
			keyBuilder.append("_").append(param);
		}

		return keyBuilder.toString();
	}

	/**
	 * 指标计算实现 - 由子类重写
	 */
	protected Double calculateIndicator(String indicatorName, List<Double> prices, int period, Object... params) {
		// 默认实现，子类应该重写这个方法
		switch (indicatorName.toUpperCase()) {
		case "RSI":
			return calculateRSI(prices, period);
		// 可以添加其他默认指标实现
		default:
			return null;
		}
	}

	/**
	 * RSI计算实现
	 */
	protected Double calculateRSI(List<Double> prices, int period) {
		if (prices.size() < period + 1) {
			return null;
		}

		double totalGain = 0.0;
		double totalLoss = 0.0;

		for (int i = 1; i <= period; i++) {
			double change = prices.get(i) - prices.get(i - 1);
			if (change > 0) {
				totalGain += change;
			} else {
				totalLoss -= change;
			}
		}

		double avgGain = totalGain / period;
		double avgLoss = totalLoss / period;

		if (avgLoss == 0) {
			return 100.0;
		}

		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	// ==================== 状态方法 ====================

	public Map<String, Object> getStrategyStatus() {
		Map<String, Object> status = new HashMap<>();
		status.put("name", name);
		status.put("initialized", initialized);
		status.put("debugMode", debugMode);
		status.put("cacheEnabled", cacheEnabled);
		status.put("parameters", new HashMap<>(parameters));
		status.put("historicalDataSize", historicalData.size());

		// 添加缓存统计
		UnifiedCacheManager.CacheStats cacheStats = cacheManager.getRegion("indicators").getStats();
		status.put("cacheStats", cacheStats.toString());

		return status;
	}

	public boolean isInitialized() {
		return initialized;
	}

}