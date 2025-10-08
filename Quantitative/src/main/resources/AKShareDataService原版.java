package com.Quantitative.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.validation.DataQualityReport;
import com.Quantitative.data.validation.DataValidator;

/**
 * AKShare数据服务 - 使用HTTP API获取真实股票数据 支持A股、港股、美股、期货、期权等多种数据
 */
public class AKShareDataService {
	// API配置
	private static final String DEFAULT_BASE_URL = "http://192.168.28.128:8888/api";
	// private static final String DEFAULT_BASE_URL =
	// "http://192.168.10.7:8888/api/akshare";
	// private static final String DEFAULT_BASE_URL =
	// "http://192.168.10.7:8888/api/efinance";
	private String baseUrl;
	private int connectTimeout = 10000; // 10秒连接超时
	private int readTimeout = 30000; // 30秒读取超时
	private int maxRetries = 3; // 最大重试次数
	private boolean debugMode = false;

	// 缓存配置
	private Map<String, CacheEntry> dataCache;
	private long cacheExpiryMillis = 5 * 60 * 1000; // 5分钟缓存

	// 统计信息
	private int totalRequests = 0;
	private int failedRequests = 0;
	private long totalResponseTime = 0;

	public AKShareDataService() {
		this(DEFAULT_BASE_URL);
	}

	public AKShareDataService(String baseUrl) {
		this.baseUrl = baseUrl;
		this.dataCache = new HashMap<>();
		testConnection();
	}

	// ==================== 主要数据获取方法 ====================

	/**
	 * 获取股票历史数据
	 */
	public List<BarEvent> getStockHistory(String symbol, LocalDateTime start, LocalDateTime end, String period,
			String adjust) {
		String cacheKey = generateCacheKey("stock_history", symbol, start, end, period, adjust);

		// 检查缓存
		List<BarEvent> cachedData = getFromCache(cacheKey);
		if (cachedData != null) {
			if (debugMode) {
				System.out.printf("[AKShare] 使用缓存数据: %s (%d条)%n", symbol, cachedData.size());
			}
			return cachedData;
		}

		long startTime = System.currentTimeMillis();

		try {
			// 构建请求
			JSONObject request = new JSONObject();
			request.put("symbol", normalizeSymbol(symbol));
			request.put("start_date", start.format(DateTimeFormatter.ISO_LOCAL_DATE));
			request.put("end_date", end.format(DateTimeFormatter.ISO_LOCAL_DATE));
			request.put("period", period);
			request.put("adjust", adjust != null ? adjust : "qfq");

			if (debugMode) {
				System.out.printf("[AKShare] 请求数据: %s %s~%s %s%n", symbol, start.toLocalDate(), end.toLocalDate(),
						period);
			}

			// 发送请求
			String response = sendPostRequest("/stock_zh_a_hist", request.toString());

			if (response != null) {
				List<BarEvent> historicalData = parseStockHistoryResponse(response, symbol);

				// 缓存数据
				putToCache(cacheKey, historicalData);

				long responseTime = System.currentTimeMillis() - startTime;
				totalResponseTime += responseTime;

				if (debugMode) {
					System.out.printf("[AKShare] 数据获取成功: %s %d条, 耗时: %dms%n", symbol, historicalData.size(),
							responseTime);
				}

				return historicalData;
			}

		} catch (Exception e) {
			failedRequests++;
			System.err.printf("[AKShare] 获取股票历史数据失败: %s - %s%n", symbol, e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		// 如果获取失败，返回空列表或备用数据
		return Collections.emptyList();
	}

	/**
	 * 获取股票实时数据
	 */
	public Map<String, Object> getStockRealtime(String symbol) {
		String cacheKey = generateCacheKey("stock_realtime", symbol);

		try {
			JSONObject request = new JSONObject();
			request.put("symbol", normalizeSymbol(symbol));

			String response = sendPostRequest("/stock_zh_a_spot", request.toString());

			if (response != null) {
				return parseRealtimeResponse(response, symbol);
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 获取股票实时数据失败: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyMap();
	}

	/**
	 * 获取股票列表
	 */
	public List<String> getStockList() {
		String cacheKey = "stock_list";

		// 检查缓存
		@SuppressWarnings("unchecked")
		List<String> cachedList = (List<String>) getFromCache(cacheKey);
		if (cachedList != null) {
			return cachedList;
		}

		try {
			String response = sendGetRequest("/stock_list");

			if (response != null) {
				List<String> stockList = parseStockListResponse(response);

				// 缓存股票列表（缓存时间稍长）
				putToCache(cacheKey, stockList, 30 * 60 * 1000); // 30分钟

				return stockList;
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 获取股票列表失败: %s%n", e.getMessage());
		}

		// 返回默认股票列表
		return getDefaultStockList();
	}

	/**
	 * 获取指数数据
	 */
	public List<BarEvent> getIndexHistory(String symbol, LocalDateTime start, LocalDateTime end, String period) {
		try {
			JSONObject request = new JSONObject();
			request.put("symbol", symbol);
			request.put("start_date", start.format(DateTimeFormatter.ISO_LOCAL_DATE));
			request.put("end_date", end.format(DateTimeFormatter.ISO_LOCAL_DATE));
			request.put("period", period);

			String response = sendPostRequest("/index_zh_a_hist", request.toString());

			if (response != null) {
				return parseStockHistoryResponse(response, symbol);
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 获取指数数据失败: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyList();
	}

	/**
	 * 获取板块数据
	 */
	public List<Map<String, Object>> getSectorData() {
		try {
			String response = sendGetRequest("/sector_data");

			if (response != null) {
				return parseSectorResponse(response);
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 获取板块数据失败: %s%n", e.getMessage());
		}

		return Collections.emptyList();
	}

	// ==================== HTTP请求方法 ====================

	/**
	 * 发送POST请求
	 */
	private String sendPostRequest(String endpoint, String jsonData) {
		return sendRequest(endpoint, "POST", jsonData);
	}

	/**
	 * 发送GET请求
	 */
	private String sendGetRequest(String endpoint) {
		return sendRequest(endpoint, "GET", null);
	}

	/**
	 * 发送HTTP请求（带重试机制）
	 */
	private String sendRequest(String endpoint, String method, String jsonData) {
		int retries = 0;
		System.out.println("发送HTTP请求");
		while (retries <= maxRetries) {
			try {
				URL url = new URL(baseUrl + endpoint);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();

				connection.setRequestMethod(method);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("User-Agent", "TradingSystemTool/1.0");

				connection.setConnectTimeout(connectTimeout);
				connection.setReadTimeout(readTimeout);
				connection.setDoOutput("POST".equals(method));

				// 发送请求数据（POST请求）
				if ("POST".equals(method) && jsonData != null) {
					try (OutputStream os = connection.getOutputStream()) {
						byte[] input = jsonData.getBytes("utf-8");
						os.write(input, 0, input.length);
					}
				}

				// 获取响应
				int responseCode = connection.getResponseCode();

				if (responseCode == HttpURLConnection.HTTP_OK) {
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
					StringBuilder response = new StringBuilder();
					String inputLine;

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();

					totalRequests++;
					return response.toString();

				} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
					System.err.printf("[AKShare] 端点不存在: %s%n", endpoint);
					break;
				} else {
					System.err.printf("[AKShare] HTTP错误: %d %s%n", responseCode, endpoint);
				}

			} catch (Exception e) {
				retries++;
				if (retries <= maxRetries) {
					System.err.printf("[AKShare] 请求失败，第%d次重试: %s%n", retries, e.getMessage());
					try {
						Thread.sleep(1000 * retries); // 指数退避
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				} else {
					System.err.printf("[AKShare] 请求最终失败: %s - %s%n", endpoint, e.getMessage());
					failedRequests++;
				}
			}
		}

		return null;
	}

	// ==================== 响应解析方法 ====================

	/**
	 * 解析股票历史数据响应
	 */
	private List<BarEvent> parseStockHistoryResponse(String response, String symbol) {
		List<BarEvent> bars = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONArray data = jsonResponse.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONObject barData = data.getJSONObject(i);

					try {
						BarEvent bar = parseBarData(barData, symbol);
						if (bar != null) {
							bars.add(bar);
						}
					} catch (Exception e) {
						System.err.printf("[AKShare] 解析Bar数据失败: %s%n", e.getMessage());
					}
				}

				if (debugMode) {
					System.out.printf("[AKShare] 解析 %s 数据: %d 条记录%n", symbol, bars.size());
				}

			} else {
				String errorMsg = jsonResponse.optString("message", "未知错误");
				System.err.printf("[AKShare] API返回错误: %s%n", errorMsg);
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 解析响应数据失败: %s%n", e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		return bars;
	}

	/**
	 * 解析单个Bar数据
	 */
	private BarEvent parseBarData(JSONObject barData, String symbol) {
		try {
			// 处理不同数据源的字段名差异
			String dateStr = getStringValue(barData, "日期", "date", "trade_date");
			double open = getDoubleValue(barData, "开盘", "open");
			double high = getDoubleValue(barData, "最高", "high");
			double low = getDoubleValue(barData, "最低", "low");
			double close = getDoubleValue(barData, "收盘", "close");
			long volume = getLongValue(barData, "成交量", "volume", "vol");
			double turnover = getDoubleValue(barData, "成交额", "amount", "turnover");

			// 解析日期
			LocalDateTime timestamp = parseDateTime(dateStr);

			if (timestamp == null) {
				System.err.printf("[AKShare] 日期解析失败: %s%n", dateStr);
				return null;
			}

			return new BarEvent(timestamp, symbol, open, high, low, close, volume, turnover);

		} catch (Exception e) {
			System.err.printf("[AKShare] 解析Bar数据异常: %s%n", e.getMessage());
			return null;
		}
	}

	/**
	 * 解析实时数据响应
	 */
	private Map<String, Object> parseRealtimeResponse(String response, String symbol) {
		Map<String, Object> realtimeData = new HashMap<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONArray data = jsonResponse.getJSONArray("data");

				if (data.length() > 0) {
					JSONObject stockData = data.getJSONObject(0);

					realtimeData.put("symbol", symbol);
					realtimeData.put("name", stockData.optString("名称", ""));
					realtimeData.put("current", stockData.optDouble("最新价", 0.0));
					realtimeData.put("change", stockData.optDouble("涨跌额", 0.0));
					realtimeData.put("changePercent", stockData.optDouble("涨跌幅", 0.0));
					realtimeData.put("volume", stockData.optLong("成交量", 0));
					realtimeData.put("turnover", stockData.optDouble("成交额", 0.0));
					realtimeData.put("high", stockData.optDouble("最高", 0.0));
					realtimeData.put("low", stockData.optDouble("最低", 0.0));
					realtimeData.put("open", stockData.optDouble("今开", 0.0));
					realtimeData.put("preClose", stockData.optDouble("昨收", 0.0));
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 解析实时数据失败: %s%n", e.getMessage());
		}

		return realtimeData;
	}

	/**
	 * 解析股票列表响应
	 */
	private List<String> parseStockListResponse(String response) {
		List<String> stockList = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONArray data = jsonResponse.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONObject stock = data.getJSONObject(i);
					String code = stock.optString("代码", "");
					String name = stock.optString("名称", "");

					if (!code.isEmpty() && !name.isEmpty()) {
						stockList.add(code + " - " + name);
					}
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 解析股票列表失败: %s%n", e.getMessage());
		}

		return stockList;
	}

	/**
	 * 解析板块数据响应
	 */
	private List<Map<String, Object>> parseSectorResponse(String response) {
		List<Map<String, Object>> sectorList = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONArray data = jsonResponse.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONObject sector = data.getJSONObject(i);
					Map<String, Object> sectorData = new HashMap<>();

					sectorData.put("name", sector.optString("板块名称", ""));
					sectorData.put("changePercent", sector.optDouble("涨跌幅", 0.0));
					sectorData.put("leaderStock", sector.optString("领涨股票", ""));
					sectorData.put("leaderChangePercent", sector.optDouble("领涨股涨跌幅", 0.0));

					sectorList.add(sectorData);
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 解析板块数据失败: %s%n", e.getMessage());
		}

		return sectorList;
	}

	// ==================== 工具方法 ====================

	/**
	 * 测试连接
	 */
	public boolean testConnection() {
		try {
			JSONObject testRequest = new JSONObject();
			testRequest.put("symbol", "000001");
			testRequest.put("start_date", "2024-01-01");
			testRequest.put("end_date", "2024-01-10");
			testRequest.put("period", "daily");

			String response = sendPostRequest("/test", testRequest.toString());

			if (response != null) {
				JSONObject jsonResponse = new JSONObject(response);
				boolean success = jsonResponse.optBoolean("success", false);

				if (debugMode) {
					System.out.printf("[AKShare] 连接测试: %s%n", success ? "成功" : "失败");
				}

				return success;
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 连接测试失败: %s%n", e.getMessage());
		}

		return false;
	}

	/**
	 * 规范化股票代码
	 */
	private String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.trim().isEmpty()) {
			return symbol;
		}

		// 移除空格和特殊字符
		String normalized = symbol.trim().toUpperCase();

		// 如果是A股代码，确保格式正确
		if (normalized.matches("\\d{6}")) {
			return normalized;
		}

		// 如果包含名称，只提取代码部分
		if (normalized.contains("-")) {
			String[] parts = normalized.split("-");
			if (parts.length > 0) {
				return parts[0].trim();
			}
		}

		return normalized;
	}

	/**
	 * 解析日期时间
	 */
	private LocalDateTime parseDateTime(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		}

		try {
			// 尝试多种日期格式
			DateTimeFormatter[] formatters = { DateTimeFormatter.ofPattern("yyyy-MM-dd"),
					DateTimeFormatter.ofPattern("yyyy/MM/dd"), DateTimeFormatter.ofPattern("yyyyMMdd"),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
					DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss") };

			for (DateTimeFormatter formatter : formatters) {
				try {
					if (dateStr.length() <= 10) {
						return LocalDateTime.parse(dateStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
					} else {
						return LocalDateTime.parse(dateStr, formatter);
					}
				} catch (Exception e) {
					// 尝试下一个格式
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] 日期解析失败: %s%n", dateStr);
		}

		return null;
	}

	/**
	 * 获取字符串值（支持多个字段名）
	 */
	private String getStringValue(JSONObject obj, String... keys) {
		for (String key : keys) {
			if (obj.has(key)) {
				return obj.getString(key);
			}
		}
		return "";
	}

	/**
	 * 获取double值（支持多个字段名）
	 */
	private double getDoubleValue(JSONObject obj, String... keys) {
		for (String key : keys) {
			if (obj.has(key)) {
				return obj.getDouble(key);
			}
		}
		return 0.0;
	}

	/**
	 * 获取long值（支持多个字段名）
	 */
	private long getLongValue(JSONObject obj, String... keys) {
		for (String key : keys) {
			if (obj.has(key)) {
				return obj.getLong(key);
			}
		}
		return 0L;
	}

	// ==================== 缓存管理 ====================

	/**
	 * 从缓存获取数据
	 */
	@SuppressWarnings("unchecked")
	private <T> T getFromCache(String key) {
		CacheEntry entry = dataCache.get(key);
		if (entry != null && !entry.isExpired()) {
			return (T) entry.getData();
		}

		// 移除过期缓存
		if (entry != null && entry.isExpired()) {
			dataCache.remove(key);
		}

		return null;
	}

	/**
	 * 存入缓存
	 */
	private void putToCache(String key, Object data) {
		putToCache(key, data, cacheExpiryMillis);
	}

	private void putToCache(String key, Object data, long expiryMillis) {
		dataCache.put(key, new CacheEntry(data, expiryMillis));

		// 简单的缓存清理（当缓存太大时）
		if (dataCache.size() > 100) {
			cleanupExpiredCache();
		}
	}

	/**
	 * 清理过期缓存
	 */
	private void cleanupExpiredCache() {
		Iterator<Map.Entry<String, CacheEntry>> it = dataCache.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, CacheEntry> entry = it.next();
			if (entry.getValue().isExpired()) {
				it.remove();
			}
		}
	}

	/**
	 * 生成缓存键
	 */
	private String generateCacheKey(String type, Object... params) {
		StringBuilder key = new StringBuilder(type);
		for (Object param : params) {
			if (param != null) {
				key.append("_").append(param.toString());
			}
		}
		return key.toString();
	}

	// ==================== 备用数据方法 ====================

	/**
	 * 获取默认股票列表
	 */
	private List<String> getDefaultStockList() {
		List<String> defaultStocks = Arrays.asList("000001 - 平安银行", "000002 - 万科A", "000858 - 五粮液", "600519 - 贵州茅台",
				"601318 - 中国平安", "600036 - 招商银行", "000333 - 美的集团", "000651 - 格力电器", "600276 - 恒瑞医药", "601888 - 中国国旅");

		System.out.println("? 使用默认股票列表");
		return new ArrayList<>(defaultStocks);
	}

	/**
	 * 生成备用数据
	 */
	public List<BarEvent> generateFallbackData(String symbol, LocalDateTime start, LocalDateTime end) {
		System.out.println("? 生成备用数据...");
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
			double turnover = price * volume;

			BarEvent bar = new BarEvent(currentTime, symbol, open, high, low, price, volume, turnover);
			fallbackData.add(bar);

			currentTime = currentTime.plusDays(step);
		}

		return fallbackData;
	}

	// ==================== 统计和状态方法 ====================

	/**
	 * 获取服务统计信息
	 */
	// 修复前的代码（第750行附近）
	public Map<String, Object> getServiceStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalRequests", totalRequests);
		stats.put("failedRequests", failedRequests);
		stats.put("successRate",
				totalRequests > 0 ? (double) (totalRequests - failedRequests) / totalRequests * 100 : 0.0);
		stats.put("avgResponseTime", totalRequests > 0 ? totalResponseTime / totalRequests : 0);
		stats.put("cacheSize", dataCache.size());
		stats.put("baseUrl", baseUrl);

		// 修复：直接调用方法，不需要从 Map 获取
		boolean connected = testConnection();
		stats.put("connected", connected);

		return stats;
	}

	/**
	 * 打印服务状态
	 */
	public void printServiceStatus() {
		Map<String, Object> stats = getServiceStatistics();
		System.out.println("\n=== AKShare数据服务状态 ===");
		System.out.printf("总请求数: %d%n", stats.get("totalRequests"));
		System.out.printf("失败请求: %d%n", stats.get("failedRequests"));
		System.out.printf("成功率: %.1f%%%n", stats.get("successRate"));
		System.out.printf("平均响应时间: %dms%n", stats.get("avgResponseTime"));
		System.out.printf("缓存大小: %d%n", stats.get("cacheSize"));
		// System.out.printf("连接状态: %s%n", stats.get("connected") ? "正常" :
		// "异常");
		System.out.printf("连接状态: %s%n", Boolean.TRUE.equals(stats.get("connected")) ? "正常" : "异常");
		System.out.printf("服务地址: %s%n", stats.get("baseUrl"));
	}

	// ======================辅助方法==============

	// ----验证数据是否合理
	// 在 AKShareDataService 类中添加数据验证方法

	/**
	 * 增强的数据获取方法 - 带验证
	 */
	public List<BarEvent> getValidatedStockHistory(String symbol, LocalDateTime start, LocalDateTime end, String period,
			String adjust) {
		List<BarEvent> rawData = getStockHistory(symbol, start, end, period, adjust);

		if (rawData.isEmpty()) {
			return rawData;
		}

		List<BarEvent> validatedData = new ArrayList<>();
		int repairedCount = 0;
		int invalidCount = 0;

		for (BarEvent bar : rawData) {
			DataValidator.ValidationResult result = DataValidator.validateBar(bar);

			if (result.isValid()) {
				validatedData.add(bar);

				// 记录警告
				if (result.hasWarnings() && debugMode) {
					System.out.printf("[数据验证] %s 警告: %s%n", bar.getTimestamp().toLocalDate(),
							String.join("; ", result.getWarnings()));
				}
			} else {
				// 尝试修复数据
				BarEvent repairedBar = DataValidator.repairBarData(bar);
				DataValidator.ValidationResult repairResult = DataValidator.validateBar(repairedBar);

				if (repairResult.isValid()) {
					validatedData.add(repairedBar);
					repairedCount++;

					if (debugMode) {
						System.out.printf("[数据修复] %s 修复成功: %s -> %s%n", bar.getTimestamp().toLocalDate(),
								bar.getClose(), repairedBar.getClose());
					}
				} else {
					invalidCount++;
					if (debugMode) {
						System.out.printf("[数据验证] %s 数据无效被丢弃: %s%n", bar.getTimestamp().toLocalDate(),
								String.join("; ", result.getErrors()));
					}
				}
			}
		}

		// 验证整个序列的连续性
		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(validatedData);
		if (seriesResult.hasWarnings() && debugMode) {
			System.out.printf("[序列验证] %s 序列警告: %s%n", symbol, String.join("; ", seriesResult.getWarnings()));
		}

		if (debugMode) {
			System.out.printf("[数据验证] %s: 原始%d条, 有效%d条, 修复%d条, 丢弃%d条%n", symbol, rawData.size(), validatedData.size(),
					repairedCount, invalidCount);
		}

		return validatedData;
	}

	/**
	 * 获取数据质量报告
	 */
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		List<BarEvent> data = getStockHistory(symbol, start, end, "daily", "qfq");

		DataQualityReport report = new DataQualityReport(symbol, start, end);

		for (BarEvent bar : data) {
			DataValidator.ValidationResult result = DataValidator.validateBar(bar);
			report.addValidationResult(result);
		}

		// 序列验证
		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(data);
		report.setSeriesValidation(seriesResult);

		return report;
	}

	// ==================== 配置方法 ====================

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public void setCacheExpiryMillis(long cacheExpiryMillis) {
		this.cacheExpiryMillis = cacheExpiryMillis;
	}

	public void clearCache() {
		dataCache.clear();
		System.out.println("? AKShare数据缓存已清空");
	}

	// ==================== 缓存条目类 ====================

	/**
	 * 缓存条目类
	 */
	private static class CacheEntry {
		private final Object data;
		private final long expiryTime;

		public CacheEntry(Object data, long expiryMillis) {
			this.data = data;
			this.expiryTime = System.currentTimeMillis() + expiryMillis;
		}

		public Object getData() {
			return data;
		}

		public boolean isExpired() {
			return System.currentTimeMillis() > expiryTime;
		}
	}

}