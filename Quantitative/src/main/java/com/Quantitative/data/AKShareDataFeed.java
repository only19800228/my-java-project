package com.Quantitative.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.Quantitative.common.monitor.MonitorUtils;
import com.Quantitative.common.utils.PerformanceMonitor;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * AKShare数据馈送实现
 */
public class AKShareDataFeed implements DataFeed {
	private List<BarEvent> bars;
	private int currentIndex;
	private String currentSymbol;
	private AKShareDataService dataService;
	private Map<String, Object> parameters;
	private String status = "CREATED";
	private PerformanceMonitor performanceMonitor;
	private boolean debugMode = false; // 添加调试模式字段

	public AKShareDataFeed() {
		this.bars = new ArrayList<>();
		this.currentIndex = 0;
		this.parameters = new HashMap<>();
		this.dataService = new AKShareDataService();
		this.performanceMonitor = PerformanceMonitor.getInstance();

		initializeDefaultParameters();
	}

	private void initializeDefaultParameters() {
		parameters.put("timeframe", "daily");
		parameters.put("adjust", "qfq");
		parameters.put("maxRetry", 3);
		parameters.put("timeout", 30000);
	}

	// 添加缺失的方法
	public void setDebugMode(boolean debug) {
		this.debugMode = debug;
		if (debug) {
			System.out.println("? AKShareDataFeed 调试模式已开启");
		}
	}

	public boolean testConnection() {
		try {
			boolean connected = dataService.testConnection();
			// dataService.setApiToken("cf717df1f1a23819051ffec86c681a0dac5a88a836d3ddc4c2661199");
			// // 替换为你的真实Token

			if (debugMode) {
				System.out.println("? AKShare数据服务连接测试: " + (connected ? "成功" : "失败"));
			}
			return connected;
		} catch (Exception e) {
			if (debugMode) {
				System.err.println("? AKShare数据服务连接测试异常: " + e.getMessage());
			}
			return false;
		}
	}

	@Override
	public void initialize() {
		System.out.println("初始化AKShare数据馈送...");
		this.status = "INITIALIZED";
		System.out.println("? AKShare数据馈送初始化完成");
	}

	// 数据验证是否合理
	// 在 AKShareDataFeed 类中增强数据加载方法

	@Override
	public List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end) {
		return MonitorUtils.monitor("DataFeed", "loadHistoricalData", () -> {
			this.currentSymbol = symbol;
			this.bars.clear();
			this.currentIndex = 0;

			String timeframe = (String) parameters.get("timeframe");
			String adjust = (String) parameters.get("adjust");

			List<BarEvent> historicalData = dataService.getValidatedStockHistory(symbol, start, end, timeframe, adjust);

			if (historicalData.isEmpty()) {
				historicalData = generateFallbackData(symbol, start, end);
			}

			this.bars.addAll(historicalData);
			this.status = "DATA_LOADED";

			return new ArrayList<>(historicalData);
		});
	}

	@Override
	public BarEvent getNextBar() {
		return MonitorUtils.monitor("DataFeed", "getNextBar", () -> {
			if (currentIndex < bars.size()) {
				return bars.get(currentIndex++);
			}
			return null;
		});
	}

	/**
	 * 获取数据质量报告
	 */
	@Override
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		if (dataService == null) {
			throw new IllegalStateException("数据服务未初始化");
		}
		return dataService.getDataQualityReport(symbol, start, end);
	}

	/**
	 * 获取所有已加载的数据
	 */
	@Override
	public List<BarEvent> getAllBars() {
		return new ArrayList<>(bars);

	}

	// 其他现有方法保持不变...
	@Override
	public void configure(Map<String, Object> config) {
		if (config != null) {
			parameters.putAll(config);
		}
	}

	@Override
	public String getName() {
		return "AKShareDataFeed";
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		this.currentIndex = 0;
		System.out.println("? 数据馈送指针已重置");
	}

	@Override
	public void shutdown() {
		this.bars.clear();
		this.currentIndex = 0;
		this.status = "SHUTDOWN";
		System.out.println("? AKShare数据馈送已关闭");
	}

	@Override
	public boolean hasNextBar() {
		return currentIndex < bars.size();
	}

	@Override
	public DataInfo getDataInfo() {
		if (bars.isEmpty()) {
			return new DataInfo(currentSymbol, null, null, 0, (String) parameters.get("timeframe"));
		}

		LocalDateTime start = bars.get(0).getTimestamp();
		LocalDateTime end = bars.get(bars.size() - 1).getTimestamp();
		return new DataInfo(currentSymbol, start, end, bars.size(), (String) parameters.get("timeframe"));
	}

	@Override
	public void setParameter(String key, Object value) {
		parameters.put(key, value);
	}

	@Override
	public Object getParameter(String key) {
		return parameters.get(key);
	}

	@Override
	public List<String> getAvailableSymbols() {
		return dataService.getStockList();
	}

	@Override
	public boolean isConnected() {
		return dataService.testConnection();
	}

	/**
	 * 备用数据生成
	 */
	private List<BarEvent> generateFallbackData(String symbol, LocalDateTime start, LocalDateTime end) {
		System.err.println("? 使用备用数据生成...");
		List<BarEvent> fallbackData = new ArrayList<>();
		Random random = new Random(symbol.hashCode());

		double basePrice = 10.0 + random.nextDouble() * 90;
		LocalDateTime currentTime = start;
		int count = 100;
		long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end);
		long step = Math.max(1, daysBetween / count);

		double price = basePrice;

		for (int i = 0; i < count; i++) {
			double change = (random.nextGaussian() * 0.02);
			price = price * (1 + change);
			price = Math.max(0.01, price);

			double open = price * (1 + (random.nextDouble() - 0.5) * 0.01);
			double high = Math.max(open, price) * (1 + random.nextDouble() * 0.02);
			double low = Math.min(open, price) * (1 - random.nextDouble() * 0.02);
			long volume = (long) (1000000 + random.nextDouble() * 9000000);

			BarEvent bar = new BarEvent(currentTime, symbol, open, high, low, price, volume);
			fallbackData.add(bar);

			currentTime = currentTime.plusDays(step);
		}

		return fallbackData;
	}

	/**
	 * 获取数据统计
	 */
	public Map<String, Object> getDataStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalBars", bars.size());
		stats.put("currentIndex", currentIndex);
		stats.put("symbol", currentSymbol);
		stats.put("parameters", new HashMap<>(parameters));
		stats.put("dataServiceStatus", dataService.testConnection() ? "CONNECTED" : "DISCONNECTED");
		stats.put("debugMode", debugMode);
		return stats;
	}

	/**
	 * 获取数据服务实例（用于高级数据操作）
	 */
	public AKShareDataService getDataService() {
		return this.dataService;
	}

	/**
	 * 设置数据服务（用于测试或自定义实现）
	 */
	public void setDataService(AKShareDataService dataService) {
		this.dataService = dataService;
	}

	/**
	 * 获取带验证的历史数据
	 */
	public List<BarEvent> getValidatedStockHistory(String symbol, LocalDateTime start, LocalDateTime end) {
		if (dataService == null) {
			throw new IllegalStateException("数据服务未初始化");
		}

		String timeframe = (String) parameters.get("timeframe");
		String adjust = (String) parameters.get("adjust");

		return dataService.getValidatedStockHistory(symbol, start, end, timeframe, adjust);
	}

	/**
	 * 静态工厂方法创建数据源实例
	 */
	public static AKShareDataFeed createDataFeed() {
		return createDataFeed(new HashMap<>());
	}

	/**
	 * 静态工厂方法创建数据源实例（带配置）
	 */
	public static AKShareDataFeed createDataFeed(Map<String, Object> config) {
		AKShareDataFeed dataFeed = new AKShareDataFeed();
		if (config != null && !config.isEmpty()) {
			dataFeed.configure(config);
		}
		dataFeed.initialize();
		return dataFeed;
	}

	/**
	 * 创建带调试模式的数据源
	 */
	public static AKShareDataFeed createDataFeed(boolean debugMode) {
		AKShareDataFeed dataFeed = new AKShareDataFeed();
		dataFeed.setDebugMode(debugMode);
		dataFeed.initialize();
		return dataFeed;
	}

}