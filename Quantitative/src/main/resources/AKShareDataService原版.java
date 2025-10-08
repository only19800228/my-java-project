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
 * AKShare���ݷ��� - ʹ��HTTP API��ȡ��ʵ��Ʊ���� ֧��A�ɡ��۹ɡ����ɡ��ڻ�����Ȩ�ȶ�������
 */
public class AKShareDataService {
	// API����
	private static final String DEFAULT_BASE_URL = "http://192.168.28.128:8888/api";
	// private static final String DEFAULT_BASE_URL =
	// "http://192.168.10.7:8888/api/akshare";
	// private static final String DEFAULT_BASE_URL =
	// "http://192.168.10.7:8888/api/efinance";
	private String baseUrl;
	private int connectTimeout = 10000; // 10�����ӳ�ʱ
	private int readTimeout = 30000; // 30���ȡ��ʱ
	private int maxRetries = 3; // ������Դ���
	private boolean debugMode = false;

	// ��������
	private Map<String, CacheEntry> dataCache;
	private long cacheExpiryMillis = 5 * 60 * 1000; // 5���ӻ���

	// ͳ����Ϣ
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

	// ==================== ��Ҫ���ݻ�ȡ���� ====================

	/**
	 * ��ȡ��Ʊ��ʷ����
	 */
	public List<BarEvent> getStockHistory(String symbol, LocalDateTime start, LocalDateTime end, String period,
			String adjust) {
		String cacheKey = generateCacheKey("stock_history", symbol, start, end, period, adjust);

		// ��黺��
		List<BarEvent> cachedData = getFromCache(cacheKey);
		if (cachedData != null) {
			if (debugMode) {
				System.out.printf("[AKShare] ʹ�û�������: %s (%d��)%n", symbol, cachedData.size());
			}
			return cachedData;
		}

		long startTime = System.currentTimeMillis();

		try {
			// ��������
			JSONObject request = new JSONObject();
			request.put("symbol", normalizeSymbol(symbol));
			request.put("start_date", start.format(DateTimeFormatter.ISO_LOCAL_DATE));
			request.put("end_date", end.format(DateTimeFormatter.ISO_LOCAL_DATE));
			request.put("period", period);
			request.put("adjust", adjust != null ? adjust : "qfq");

			if (debugMode) {
				System.out.printf("[AKShare] ��������: %s %s~%s %s%n", symbol, start.toLocalDate(), end.toLocalDate(),
						period);
			}

			// ��������
			String response = sendPostRequest("/stock_zh_a_hist", request.toString());

			if (response != null) {
				List<BarEvent> historicalData = parseStockHistoryResponse(response, symbol);

				// ��������
				putToCache(cacheKey, historicalData);

				long responseTime = System.currentTimeMillis() - startTime;
				totalResponseTime += responseTime;

				if (debugMode) {
					System.out.printf("[AKShare] ���ݻ�ȡ�ɹ�: %s %d��, ��ʱ: %dms%n", symbol, historicalData.size(),
							responseTime);
				}

				return historicalData;
			}

		} catch (Exception e) {
			failedRequests++;
			System.err.printf("[AKShare] ��ȡ��Ʊ��ʷ����ʧ��: %s - %s%n", symbol, e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		// �����ȡʧ�ܣ����ؿ��б��������
		return Collections.emptyList();
	}

	/**
	 * ��ȡ��Ʊʵʱ����
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
			System.err.printf("[AKShare] ��ȡ��Ʊʵʱ����ʧ��: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyMap();
	}

	/**
	 * ��ȡ��Ʊ�б�
	 */
	public List<String> getStockList() {
		String cacheKey = "stock_list";

		// ��黺��
		@SuppressWarnings("unchecked")
		List<String> cachedList = (List<String>) getFromCache(cacheKey);
		if (cachedList != null) {
			return cachedList;
		}

		try {
			String response = sendGetRequest("/stock_list");

			if (response != null) {
				List<String> stockList = parseStockListResponse(response);

				// �����Ʊ�б�����ʱ���Գ���
				putToCache(cacheKey, stockList, 30 * 60 * 1000); // 30����

				return stockList;
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] ��ȡ��Ʊ�б�ʧ��: %s%n", e.getMessage());
		}

		// ����Ĭ�Ϲ�Ʊ�б�
		return getDefaultStockList();
	}

	/**
	 * ��ȡָ������
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
			System.err.printf("[AKShare] ��ȡָ������ʧ��: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyList();
	}

	/**
	 * ��ȡ�������
	 */
	public List<Map<String, Object>> getSectorData() {
		try {
			String response = sendGetRequest("/sector_data");

			if (response != null) {
				return parseSectorResponse(response);
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] ��ȡ�������ʧ��: %s%n", e.getMessage());
		}

		return Collections.emptyList();
	}

	// ==================== HTTP���󷽷� ====================

	/**
	 * ����POST����
	 */
	private String sendPostRequest(String endpoint, String jsonData) {
		return sendRequest(endpoint, "POST", jsonData);
	}

	/**
	 * ����GET����
	 */
	private String sendGetRequest(String endpoint) {
		return sendRequest(endpoint, "GET", null);
	}

	/**
	 * ����HTTP���󣨴����Ի��ƣ�
	 */
	private String sendRequest(String endpoint, String method, String jsonData) {
		int retries = 0;
		System.out.println("����HTTP����");
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

				// �����������ݣ�POST����
				if ("POST".equals(method) && jsonData != null) {
					try (OutputStream os = connection.getOutputStream()) {
						byte[] input = jsonData.getBytes("utf-8");
						os.write(input, 0, input.length);
					}
				}

				// ��ȡ��Ӧ
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
					System.err.printf("[AKShare] �˵㲻����: %s%n", endpoint);
					break;
				} else {
					System.err.printf("[AKShare] HTTP����: %d %s%n", responseCode, endpoint);
				}

			} catch (Exception e) {
				retries++;
				if (retries <= maxRetries) {
					System.err.printf("[AKShare] ����ʧ�ܣ���%d������: %s%n", retries, e.getMessage());
					try {
						Thread.sleep(1000 * retries); // ָ���˱�
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				} else {
					System.err.printf("[AKShare] ��������ʧ��: %s - %s%n", endpoint, e.getMessage());
					failedRequests++;
				}
			}
		}

		return null;
	}

	// ==================== ��Ӧ�������� ====================

	/**
	 * ������Ʊ��ʷ������Ӧ
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
						System.err.printf("[AKShare] ����Bar����ʧ��: %s%n", e.getMessage());
					}
				}

				if (debugMode) {
					System.out.printf("[AKShare] ���� %s ����: %d ����¼%n", symbol, bars.size());
				}

			} else {
				String errorMsg = jsonResponse.optString("message", "δ֪����");
				System.err.printf("[AKShare] API���ش���: %s%n", errorMsg);
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] ������Ӧ����ʧ��: %s%n", e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		return bars;
	}

	/**
	 * ��������Bar����
	 */
	private BarEvent parseBarData(JSONObject barData, String symbol) {
		try {
			// ����ͬ����Դ���ֶ�������
			String dateStr = getStringValue(barData, "����", "date", "trade_date");
			double open = getDoubleValue(barData, "����", "open");
			double high = getDoubleValue(barData, "���", "high");
			double low = getDoubleValue(barData, "���", "low");
			double close = getDoubleValue(barData, "����", "close");
			long volume = getLongValue(barData, "�ɽ���", "volume", "vol");
			double turnover = getDoubleValue(barData, "�ɽ���", "amount", "turnover");

			// ��������
			LocalDateTime timestamp = parseDateTime(dateStr);

			if (timestamp == null) {
				System.err.printf("[AKShare] ���ڽ���ʧ��: %s%n", dateStr);
				return null;
			}

			return new BarEvent(timestamp, symbol, open, high, low, close, volume, turnover);

		} catch (Exception e) {
			System.err.printf("[AKShare] ����Bar�����쳣: %s%n", e.getMessage());
			return null;
		}
	}

	/**
	 * ����ʵʱ������Ӧ
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
					realtimeData.put("name", stockData.optString("����", ""));
					realtimeData.put("current", stockData.optDouble("���¼�", 0.0));
					realtimeData.put("change", stockData.optDouble("�ǵ���", 0.0));
					realtimeData.put("changePercent", stockData.optDouble("�ǵ���", 0.0));
					realtimeData.put("volume", stockData.optLong("�ɽ���", 0));
					realtimeData.put("turnover", stockData.optDouble("�ɽ���", 0.0));
					realtimeData.put("high", stockData.optDouble("���", 0.0));
					realtimeData.put("low", stockData.optDouble("���", 0.0));
					realtimeData.put("open", stockData.optDouble("��", 0.0));
					realtimeData.put("preClose", stockData.optDouble("����", 0.0));
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] ����ʵʱ����ʧ��: %s%n", e.getMessage());
		}

		return realtimeData;
	}

	/**
	 * ������Ʊ�б���Ӧ
	 */
	private List<String> parseStockListResponse(String response) {
		List<String> stockList = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getBoolean("success")) {
				JSONArray data = jsonResponse.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONObject stock = data.getJSONObject(i);
					String code = stock.optString("����", "");
					String name = stock.optString("����", "");

					if (!code.isEmpty() && !name.isEmpty()) {
						stockList.add(code + " - " + name);
					}
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] ������Ʊ�б�ʧ��: %s%n", e.getMessage());
		}

		return stockList;
	}

	/**
	 * �������������Ӧ
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

					sectorData.put("name", sector.optString("�������", ""));
					sectorData.put("changePercent", sector.optDouble("�ǵ���", 0.0));
					sectorData.put("leaderStock", sector.optString("���ǹ�Ʊ", ""));
					sectorData.put("leaderChangePercent", sector.optDouble("���ǹ��ǵ���", 0.0));

					sectorList.add(sectorData);
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] �����������ʧ��: %s%n", e.getMessage());
		}

		return sectorList;
	}

	// ==================== ���߷��� ====================

	/**
	 * ��������
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
					System.out.printf("[AKShare] ���Ӳ���: %s%n", success ? "�ɹ�" : "ʧ��");
				}

				return success;
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] ���Ӳ���ʧ��: %s%n", e.getMessage());
		}

		return false;
	}

	/**
	 * �淶����Ʊ����
	 */
	private String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.trim().isEmpty()) {
			return symbol;
		}

		// �Ƴ��ո�������ַ�
		String normalized = symbol.trim().toUpperCase();

		// �����A�ɴ��룬ȷ����ʽ��ȷ
		if (normalized.matches("\\d{6}")) {
			return normalized;
		}

		// ����������ƣ�ֻ��ȡ���벿��
		if (normalized.contains("-")) {
			String[] parts = normalized.split("-");
			if (parts.length > 0) {
				return parts[0].trim();
			}
		}

		return normalized;
	}

	/**
	 * ��������ʱ��
	 */
	private LocalDateTime parseDateTime(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		}

		try {
			// ���Զ������ڸ�ʽ
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
					// ������һ����ʽ
				}
			}

		} catch (Exception e) {
			System.err.printf("[AKShare] ���ڽ���ʧ��: %s%n", dateStr);
		}

		return null;
	}

	/**
	 * ��ȡ�ַ���ֵ��֧�ֶ���ֶ�����
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
	 * ��ȡdoubleֵ��֧�ֶ���ֶ�����
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
	 * ��ȡlongֵ��֧�ֶ���ֶ�����
	 */
	private long getLongValue(JSONObject obj, String... keys) {
		for (String key : keys) {
			if (obj.has(key)) {
				return obj.getLong(key);
			}
		}
		return 0L;
	}

	// ==================== ������� ====================

	/**
	 * �ӻ����ȡ����
	 */
	@SuppressWarnings("unchecked")
	private <T> T getFromCache(String key) {
		CacheEntry entry = dataCache.get(key);
		if (entry != null && !entry.isExpired()) {
			return (T) entry.getData();
		}

		// �Ƴ����ڻ���
		if (entry != null && entry.isExpired()) {
			dataCache.remove(key);
		}

		return null;
	}

	/**
	 * ���뻺��
	 */
	private void putToCache(String key, Object data) {
		putToCache(key, data, cacheExpiryMillis);
	}

	private void putToCache(String key, Object data, long expiryMillis) {
		dataCache.put(key, new CacheEntry(data, expiryMillis));

		// �򵥵Ļ�������������̫��ʱ��
		if (dataCache.size() > 100) {
			cleanupExpiredCache();
		}
	}

	/**
	 * ������ڻ���
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
	 * ���ɻ����
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

	// ==================== �������ݷ��� ====================

	/**
	 * ��ȡĬ�Ϲ�Ʊ�б�
	 */
	private List<String> getDefaultStockList() {
		List<String> defaultStocks = Arrays.asList("000001 - ƽ������", "000002 - ���A", "000858 - ����Һ", "600519 - ����ę́",
				"601318 - �й�ƽ��", "600036 - ��������", "000333 - ���ļ���", "000651 - ��������", "600276 - ����ҽҩ", "601888 - �й�����");

		System.out.println("? ʹ��Ĭ�Ϲ�Ʊ�б�");
		return new ArrayList<>(defaultStocks);
	}

	/**
	 * ���ɱ�������
	 */
	public List<BarEvent> generateFallbackData(String symbol, LocalDateTime start, LocalDateTime end) {
		System.out.println("? ���ɱ�������...");
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

	// ==================== ͳ�ƺ�״̬���� ====================

	/**
	 * ��ȡ����ͳ����Ϣ
	 */
	// �޸�ǰ�Ĵ��루��750�и�����
	public Map<String, Object> getServiceStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalRequests", totalRequests);
		stats.put("failedRequests", failedRequests);
		stats.put("successRate",
				totalRequests > 0 ? (double) (totalRequests - failedRequests) / totalRequests * 100 : 0.0);
		stats.put("avgResponseTime", totalRequests > 0 ? totalResponseTime / totalRequests : 0);
		stats.put("cacheSize", dataCache.size());
		stats.put("baseUrl", baseUrl);

		// �޸���ֱ�ӵ��÷���������Ҫ�� Map ��ȡ
		boolean connected = testConnection();
		stats.put("connected", connected);

		return stats;
	}

	/**
	 * ��ӡ����״̬
	 */
	public void printServiceStatus() {
		Map<String, Object> stats = getServiceStatistics();
		System.out.println("\n=== AKShare���ݷ���״̬ ===");
		System.out.printf("��������: %d%n", stats.get("totalRequests"));
		System.out.printf("ʧ������: %d%n", stats.get("failedRequests"));
		System.out.printf("�ɹ���: %.1f%%%n", stats.get("successRate"));
		System.out.printf("ƽ����Ӧʱ��: %dms%n", stats.get("avgResponseTime"));
		System.out.printf("�����С: %d%n", stats.get("cacheSize"));
		// System.out.printf("����״̬: %s%n", stats.get("connected") ? "����" :
		// "�쳣");
		System.out.printf("����״̬: %s%n", Boolean.TRUE.equals(stats.get("connected")) ? "����" : "�쳣");
		System.out.printf("�����ַ: %s%n", stats.get("baseUrl"));
	}

	// ======================��������==============

	// ----��֤�����Ƿ����
	// �� AKShareDataService �������������֤����

	/**
	 * ��ǿ�����ݻ�ȡ���� - ����֤
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

				// ��¼����
				if (result.hasWarnings() && debugMode) {
					System.out.printf("[������֤] %s ����: %s%n", bar.getTimestamp().toLocalDate(),
							String.join("; ", result.getWarnings()));
				}
			} else {
				// �����޸�����
				BarEvent repairedBar = DataValidator.repairBarData(bar);
				DataValidator.ValidationResult repairResult = DataValidator.validateBar(repairedBar);

				if (repairResult.isValid()) {
					validatedData.add(repairedBar);
					repairedCount++;

					if (debugMode) {
						System.out.printf("[�����޸�] %s �޸��ɹ�: %s -> %s%n", bar.getTimestamp().toLocalDate(),
								bar.getClose(), repairedBar.getClose());
					}
				} else {
					invalidCount++;
					if (debugMode) {
						System.out.printf("[������֤] %s ������Ч������: %s%n", bar.getTimestamp().toLocalDate(),
								String.join("; ", result.getErrors()));
					}
				}
			}
		}

		// ��֤�������е�������
		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(validatedData);
		if (seriesResult.hasWarnings() && debugMode) {
			System.out.printf("[������֤] %s ���о���: %s%n", symbol, String.join("; ", seriesResult.getWarnings()));
		}

		if (debugMode) {
			System.out.printf("[������֤] %s: ԭʼ%d��, ��Ч%d��, �޸�%d��, ����%d��%n", symbol, rawData.size(), validatedData.size(),
					repairedCount, invalidCount);
		}

		return validatedData;
	}

	/**
	 * ��ȡ������������
	 */
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		List<BarEvent> data = getStockHistory(symbol, start, end, "daily", "qfq");

		DataQualityReport report = new DataQualityReport(symbol, start, end);

		for (BarEvent bar : data) {
			DataValidator.ValidationResult result = DataValidator.validateBar(bar);
			report.addValidationResult(result);
		}

		// ������֤
		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(data);
		report.setSeriesValidation(seriesResult);

		return report;
	}

	// ==================== ���÷��� ====================

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
		System.out.println("? AKShare���ݻ��������");
	}

	// ==================== ������Ŀ�� ====================

	/**
	 * ������Ŀ��
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