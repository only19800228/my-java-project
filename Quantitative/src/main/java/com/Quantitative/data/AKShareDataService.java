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

import org.json.JSONArray;
import org.json.JSONObject;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.validation.DataQualityReport;
import com.Quantitative.data.validation.DataValidator;

/**
 * AKShare数据服务 - 适配新的Python TuShare服务
 */
public class AKShareDataService {
	// 新的Python服务地址
	private static final String DEFAULT_BASE_URL = "http://192.168.28.128:8888/api";
	private String baseUrl;
	private int connectTimeout = 10000;
	private int readTimeout = 30000;
	private int maxRetries = 3;
	private boolean debugMode = false;

	// 缓存配置
	private Map<String, CacheEntry> dataCache;
	private long cacheExpiryMillis = 5 * 60 * 1000;

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

	// ==================== 主要数据获取方法 - 适配新API ====================

	/**
	 * 获取股票历史数据 - 适配新API
	 */
	public List<BarEvent> getStockHistory(String symbol, LocalDateTime start, LocalDateTime end, String period,
			String adjust) {
		String cacheKey = generateCacheKey("stock_history", symbol, start, end, period, adjust);

		// 检查缓存
		List<BarEvent> cachedData = getFromCache(cacheKey);
		if (cachedData != null) {
			if (debugMode) {
				System.out.printf("[TuShare] 使用缓存数据: %s (%d条)%n", symbol, cachedData.size());
			}
			return cachedData;
		}

		long startTime = System.currentTimeMillis();

		try {
			// 构建新API请求
			JSONObject request = new JSONObject();
			request.put("symbol", normalizeSymbol(symbol));
			request.put("start_date", start.format(DateTimeFormatter.ofPattern("yyyyMMdd"))); // 新API使用yyyyMMdd格式
			request.put("end_date", end.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			request.put("period", period);
			request.put("adjust", adjust != null ? adjust : "qfq");

			if (debugMode) {
				System.out.printf("[TuShare] 请求数据: %s %s~%s %s%n", symbol,
						start.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
						end.format(DateTimeFormatter.ofPattern("yyyyMMdd")), period);
			}

			// 发送请求到新服务端点
			String response = sendPostRequest("/stock/history", request.toString());

			if (response != null) {
				List<BarEvent> historicalData = parseNewStockHistoryResponse(response, symbol);

				// 缓存数据
				putToCache(cacheKey, historicalData);

				long responseTime = System.currentTimeMillis() - startTime;
				totalResponseTime += responseTime;

				if (debugMode) {
					System.out.printf("[TuShare] 数据获取成功: %s %d条, 耗时: %dms%n", symbol, historicalData.size(),
							responseTime);
				}

				return historicalData;
			}

		} catch (Exception e) {
			failedRequests++;
			System.err.printf("[TuShare] 获取股票历史数据失败: %s - %s%n", symbol, e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		return Collections.emptyList();
	}

	/**
	 * 获取股票实时数据 - 适配新API
	 */
	public Map<String, Object> getStockRealtime(String symbol) {
		String cacheKey = generateCacheKey("stock_realtime", symbol);

		try {
			JSONObject request = new JSONObject();
			request.put("symbol", normalizeSymbol(symbol));

			String response = sendPostRequest("/stock/realtime", request.toString());

			if (response != null) {
				return parseNewRealtimeResponse(response, symbol);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 获取股票实时数据失败: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyMap();
	}

	/**
	 * 获取股票列表 - 适配新API
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
			String response = sendGetRequest("/stock/list");

			if (response != null) {
				List<String> stockList = parseNewStockListResponse(response);

				// 缓存股票列表
				putToCache(cacheKey, stockList, 30 * 60 * 1000);

				return stockList;
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 获取股票列表失败: %s%n", e.getMessage());
		}

		return getDefaultStockList();
	}

	/**
	 * 获取指数数据 - 适配新API
	 */
	public List<BarEvent> getIndexHistory(String symbol, LocalDateTime start, LocalDateTime end, String period) {
		try {
			JSONObject request = new JSONObject();
			request.put("symbol", symbol);
			request.put("start_date", start.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			request.put("end_date", end.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

			String response = sendPostRequest("/index/history", request.toString());

			if (response != null) {
				return parseNewStockHistoryResponse(response, symbol);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 获取指数数据失败: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyList();
	}

	// ==================== 新API响应解析方法 ====================

	/**
	 * 解析新API的股票历史数据响应
	 */
	private List<BarEvent> parseNewStockHistoryResponse(String response, String symbol) {
		List<BarEvent> bars = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONArray data = jsonResponse.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONObject barData = data.getJSONObject(i);

					try {
						BarEvent bar = parseNewBarData(barData, symbol);
						if (bar != null) {
							bars.add(bar);
						}
					} catch (Exception e) {
						System.err.printf("[TuShare] 解析Bar数据失败: %s%n", e.getMessage());
					}
				}

				if (debugMode) {
					System.out.printf("[TuShare] 解析 %s 数据: %d 条记录%n", symbol, bars.size());
				}

			} else {
				String errorMsg = jsonResponse.optString("message", "未知错误");
				System.err.printf("[TuShare] API返回错误: %s%n", errorMsg);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 解析响应数据失败: %s%n", e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		return bars;
	}

	/**
	 * 解析新API的单个Bar数据
	 */
	private BarEvent parseNewBarData(JSONObject barData, String symbol) {
		try {
			// 新API的字段名
			String dateStr = barData.getString("trade_date");
			double open = barData.getDouble("open");
			double high = barData.getDouble("high");
			double low = barData.getDouble("low");
			double close = barData.getDouble("close");
			long volume = barData.getLong("volume");
			double amount = barData.getDouble("amount");

			// 解析日期 - 新API使用yyyyMMdd格式
			LocalDateTime timestamp = parseNewDateTime(dateStr);

			if (timestamp == null) {
				System.err.printf("[TuShare] 日期解析失败: %s%n", dateStr);
				return null;
			}

			return new BarEvent(timestamp, symbol, open, high, low, close, volume, amount);

		} catch (Exception e) {
			System.err.printf("[TuShare] 解析Bar数据异常: %s%n", e.getMessage());
			return null;
		}
	}

	/**
	 * 解析新API的实时数据响应
	 */
	private Map<String, Object> parseNewRealtimeResponse(String response, String symbol) {
		Map<String, Object> realtimeData = new HashMap<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONObject data = jsonResponse.getJSONObject("data");

				realtimeData.put("symbol", symbol);
				realtimeData.put("name", data.optString("name", ""));
				realtimeData.put("current", data.optDouble("price", 0.0));
				realtimeData.put("change", data.optDouble("change", 0.0));
				realtimeData.put("changePercent", data.optDouble("change_percent", 0.0));
				realtimeData.put("volume", data.optLong("volume", 0));
				realtimeData.put("turnover", data.optDouble("amount", 0.0));
				realtimeData.put("high", data.optDouble("high", 0.0));
				realtimeData.put("low", data.optDouble("low", 0.0));
				realtimeData.put("open", data.optDouble("open", 0.0));
				realtimeData.put("preClose", data.optDouble("pre_close", 0.0));
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 解析实时数据失败: %s%n", e.getMessage());
		}

		return realtimeData;
	}

	/**
	 * 解析新API的股票列表响应
	 */
	private List<String> parseNewStockListResponse(String response) {
		List<String> stockList = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONArray data = jsonResponse.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONObject stock = data.getJSONObject(i);
					String code = stock.optString("code", "");
					String name = stock.optString("name", "");

					if (!code.isEmpty() && !name.isEmpty()) {
						stockList.add(code + " - " + name);
					}
				}
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 解析股票列表失败: %s%n", e.getMessage());
		}

		return stockList;
	}

	// ==================== 工具方法 ====================

	/**
	 * 测试连接 - 适配新API
	 */
	public boolean testConnection() {
		try {
			JSONObject testRequest = new JSONObject();
			testRequest.put("symbol", "000001.SZ");
			testRequest.put("start_date", "20240101");
			testRequest.put("end_date", "20240110");

			String response = sendPostRequest("/test", testRequest.toString());

			if (response != null) {
				JSONObject jsonResponse = new JSONObject(response);
				boolean success = jsonResponse.getBoolean("success");

				if (debugMode) {
					System.out.printf("[TuShare] 连接测试: %s%n", success ? "成功" : "失败");
				}

				return success;
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 连接测试失败: %s%n", e.getMessage());
		}

		return false;
	}

	/**
	 * 规范化股票代码 - 适配TuShare格式
	 */
	private String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.trim().isEmpty()) {
			return symbol;
		}

		String normalized = symbol.trim().toUpperCase();

		// TuShare股票代码格式: 000001.SZ, 600000.SH
		if (normalized.matches("\\d{6}")) {
			if (normalized.startsWith("6") || normalized.startsWith("9")) {
				return normalized + ".SH";
			} else if (normalized.startsWith("0") || normalized.startsWith("2") || normalized.startsWith("3")) {
				return normalized + ".SZ";
			}
		}

		// 如果已经包含后缀，直接返回
		if (normalized.matches("\\d{6}\\.(SH|SZ)")) {
			return normalized;
		}

		// 如果包含名称，提取代码部分
		if (normalized.contains("-")) {
			String[] parts = normalized.split("-");
			if (parts.length > 0) {
				return normalizeSymbol(parts[0].trim());
			}
		}

		return normalized;
	}

	/**
	 * 解析日期时间 - 适配新API格式 (yyyyMMdd)
	 */
	private LocalDateTime parseNewDateTime(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		}

		try {
			// 新API使用yyyyMMdd格式
			if (dateStr.length() == 8) {
				String formattedDate = dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-"
						+ dateStr.substring(6, 8);
				return LocalDateTime.parse(formattedDate + "T00:00:00");
			} else {
				// 尝试其他格式
				return parseDateTime(dateStr);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] 日期解析失败: %s%n", dateStr);
		}

		return null;
	}

	// ==================== HTTP请求方法（需要更新端点）====================

	/**
	 * 发送POST请求 - 更新端点
	 */
	private String sendPostRequest(String endpoint, String jsonData) {
		return sendRequest(endpoint, "POST", jsonData);
	}

	/**
	 * 发送GET请求 - 更新端点
	 */
	private String sendGetRequest(String endpoint) {
		return sendRequest(endpoint, "GET", null);
	}

	/**
	 * 发送HTTP请求（带重试机制）
	 */
	private String sendRequest(String endpoint, String method, String jsonData) {
		int retries = 0;
		if (debugMode) {
			System.out.println("[TuShare] 发送HTTP请求到: " + baseUrl + endpoint);
		}

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

				} else {
					System.err.printf("[TuShare] HTTP错误: %d %s%n", responseCode, endpoint);
					if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
						System.err.printf("[TuShare] 端点不存在: %s%n", endpoint);
					}
				}

			} catch (Exception e) {
				retries++;
				if (retries <= maxRetries) {
					System.err.printf("[TuShare] 请求失败，第%d次重试: %s%n", retries, e.getMessage());
					try {
						Thread.sleep(1000 * retries);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				} else {
					System.err.printf("[TuShare] 请求最终失败: %s - %s%n", endpoint, e.getMessage());
					failedRequests++;
				}
			}
		}

		return null;
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

	// ==================== 其他方法保持不变 ====================

	// 保留原有的parseDateTime方法用于兼容
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

	// 缓存管理、备用数据、统计信息等方法保持不变
	// ...

	/**
	 * 获取默认股票列表
	 */
	private List<String> getDefaultStockList() {
		List<String> defaultStocks = Arrays.asList("000001.SZ - 平安银行", "000002.SZ - 万科A", "000858.SZ - 五粮液",
				"600519.SH - 贵州茅台", "601318.SH - 中国平安", "600036.SH - 招商银行");

		System.out.println("⚠ 使用默认股票列表");
		return new ArrayList<>(defaultStocks);
	}

	// 其他方法保持不变...
	// getValidatedStockHistory, getDataQualityReport, 配置方法等

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

	// 缓存条目类保持不变
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