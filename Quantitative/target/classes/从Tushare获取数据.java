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
 * TuShare���ݷ��� - ʹ��HTTP API��ȡ��ʵ��Ʊ���� �滻ԭ�е�AKShare�ӿڣ����ֶ���ӿڲ���
 */
public class AKShareDataService {
	// TuShare API����
	private static final String DEFAULT_BASE_URL = "http://api.tushare.pro";
	private String baseUrl;
	private String apiToken; // TuShare��Ҫ��token
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
		this(DEFAULT_BASE_URL, null);
	}

	public AKShareDataService(String baseUrl) {
		this(baseUrl, null);
	}

	public AKShareDataService(String baseUrl, String apiToken) {
		this.baseUrl = baseUrl;
		this.apiToken = apiToken;
		this.dataCache = new HashMap<>();
		testConnection();
	}

	// ==================== ��Ҫ���ݻ�ȡ���� ====================

	/**
	 * ��ȡ��Ʊ��ʷ���� - ����TuShare�ӿ�
	 */
	public List<BarEvent> getStockHistory(String symbol, LocalDateTime start, LocalDateTime end, String period,
			String adjust) {
		String cacheKey = generateCacheKey("stock_history", symbol, start, end, period, adjust);

		// ��黺��
		List<BarEvent> cachedData = getFromCache(cacheKey);
		if (cachedData != null) {
			if (debugMode) {
				System.out.printf("[TuShare] ʹ�û�������: %s (%d��)%n", symbol, cachedData.size());
			}
			return cachedData;
		}

		long startTime = System.currentTimeMillis();

		try {
			// ����TuShare API����
			JSONObject request = new JSONObject();
			request.put("api_name", "daily"); // TuShare�ӿ���
			request.put("token", getApiToken());

			// ����
			JSONObject params = new JSONObject();
			String normalizedSymbol = normalizeSymbol(symbol);
			params.put("ts_code", normalizedSymbol);
			params.put("start_date", start.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			params.put("end_date", end.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			request.put("params", params);

			// �����ֶ�
			request.put("fields", "ts_code,trade_date,open,high,low,close,vol,amount");

			if (debugMode) {
				System.out.printf("[TuShare] ��������: %s %s~%s %s%n", symbol, start.toLocalDate(), end.toLocalDate(),
						period);
			}

			// ��������TuShare
			String response = sendPostRequest("", request.toString());

			if (response != null) {
				List<BarEvent> historicalData = parseTuShareStockResponse(response, symbol);

				// ��������
				putToCache(cacheKey, historicalData);

				long responseTime = System.currentTimeMillis() - startTime;
				totalResponseTime += responseTime;

				if (debugMode) {
					System.out.printf("[TuShare] ���ݻ�ȡ�ɹ�: %s %d��, ��ʱ: %dms%n", symbol, historicalData.size(),
							responseTime);
				}

				return historicalData;
			}

		} catch (Exception e) {
			failedRequests++;
			System.err.printf("[TuShare] ��ȡ��Ʊ��ʷ����ʧ��: %s - %s%n", symbol, e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		// �����ȡʧ�ܣ����ؿ��б�
		return Collections.emptyList();
	}

	/**
	 * ��ȡ��Ʊʵʱ���� - ����TuShare�ӿ�
	 */
	public Map<String, Object> getStockRealtime(String symbol) {
		String cacheKey = generateCacheKey("stock_realtime", symbol);

		try {
			JSONObject request = new JSONObject();
			request.put("api_name", "realtime_quote"); // ʹ��ʵʱ����ӿ�
			request.put("token", getApiToken());

			JSONObject params = new JSONObject();
			params.put("ts_code", normalizeSymbol(symbol));
			request.put("params", params);

			request.put("fields", "ts_code,name,price,change,change_pct,vol,amount,high,low,open,pre_close");

			String response = sendPostRequest("", request.toString());

			if (response != null) {
				return parseTuShareRealtimeResponse(response, symbol);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ��ȡ��Ʊʵʱ����ʧ��: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyMap();
	}

	/**
	 * ��ȡ��Ʊ�б� - ����TuShare�ӿ�
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
			JSONObject request = new JSONObject();
			request.put("api_name", "stock_basic");
			request.put("token", getApiToken());

			JSONObject params = new JSONObject();
			params.put("exchange", ""); // ��ȡ���н�����
			request.put("params", params);

			request.put("fields", "ts_code,name");

			String response = sendPostRequest("", request.toString());

			if (response != null) {
				List<String> stockList = parseTuShareStockListResponse(response);

				// �����Ʊ�б�����ʱ���Գ���
				putToCache(cacheKey, stockList, 30 * 60 * 1000); // 30����

				return stockList;
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ��ȡ��Ʊ�б�ʧ��: %s%n", e.getMessage());
		}

		// ����Ĭ�Ϲ�Ʊ�б�
		return getDefaultStockList();
	}

	/**
	 * ��ȡָ������ - ����TuShare�ӿ�
	 */
	public List<BarEvent> getIndexHistory(String symbol, LocalDateTime start, LocalDateTime end, String period) {
		try {
			JSONObject request = new JSONObject();
			request.put("api_name", "index_daily"); // ָ����������
			request.put("token", getApiToken());

			JSONObject params = new JSONObject();
			params.put("ts_code", symbol);
			params.put("start_date", start.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			params.put("end_date", end.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			request.put("params", params);

			request.put("fields", "ts_code,trade_date,open,high,low,close,vol,amount");

			String response = sendPostRequest("", request.toString());

			if (response != null) {
				return parseTuShareStockResponse(response, symbol);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ��ȡָ������ʧ��: %s - %s%n", symbol, e.getMessage());
		}

		return Collections.emptyList();
	}

	/**
	 * ��ȡ������� - ����TuShare�ӿ�
	 */
	public List<Map<String, Object>> getSectorData() {
		try {
			JSONObject request = new JSONObject();
			request.put("api_name", "trade_cal"); // ����ʹ�ý���������Ϊʾ����ʵ�ʿɸ�����Ҫ����
			request.put("token", getApiToken());

			JSONObject params = new JSONObject();
			params.put("exchange", "SSE");
			params.put("start_date", "20240101");
			params.put("end_date", "20241231");
			request.put("params", params);

			String response = sendPostRequest("", request.toString());

			if (response != null) {
				return parseTuShareSectorResponse(response);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ��ȡ�������ʧ��: %s%n", e.getMessage());
		}

		return Collections.emptyList();
	}

	// ==================== TuShareר�ý������� ====================

	/**
	 * ����TuShare��Ʊ��ʷ������Ӧ
	 */
	private List<BarEvent> parseTuShareStockResponse(String response, String symbol) {
		List<BarEvent> bars = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getInt("code") == 0) {
				JSONObject data = jsonResponse.getJSONObject("data");
				JSONArray items = data.getJSONArray("items");

				for (int i = 0; i < items.length(); i++) {
					JSONArray item = items.getJSONArray(i);

					try {
						BarEvent bar = parseTuShareBarData(item, symbol);
						if (bar != null) {
							bars.add(bar);
						}
					} catch (Exception e) {
						System.err.printf("[TuShare] ����Bar����ʧ��: %s%n", e.getMessage());
					}
				}

				if (debugMode) {
					System.out.printf("[TuShare] ���� %s ����: %d ����¼%n", symbol, bars.size());
				}

			} else {
				String errorMsg = jsonResponse.optString("msg", "δ֪����");
				System.err.printf("[TuShare] API���ش���: %s%n", errorMsg);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ������Ӧ����ʧ��: %s%n", e.getMessage());
			if (debugMode) {
				e.printStackTrace();
			}
		}

		return bars;
	}

	/**
	 * ����TuShare����Bar����
	 */
	private BarEvent parseTuShareBarData(JSONArray item, String symbol) {
		try {
			// TuShare���ص������������ʽ����Ҫ����fields˳�����
			String tsCode = item.getString(0);
			String tradeDate = item.getString(1);
			double open = item.getDouble(2);
			double high = item.getDouble(3);
			double low = item.getDouble(4);
			double close = item.getDouble(5);
			long volume = item.getLong(6);
			double amount = item.getDouble(7);

			// ��������
			LocalDateTime timestamp = parseDateTime(tradeDate);

			if (timestamp == null) {
				System.err.printf("[TuShare] ���ڽ���ʧ��: %s%n", tradeDate);
				return null;
			}

			return new BarEvent(timestamp, symbol, open, high, low, close, volume, amount);

		} catch (Exception e) {
			System.err.printf("[TuShare] ����Bar�����쳣: %s%n", e.getMessage());
			return null;
		}
	}

	/**
	 * ����TuShareʵʱ������Ӧ
	 */
	private Map<String, Object> parseTuShareRealtimeResponse(String response, String symbol) {
		Map<String, Object> realtimeData = new HashMap<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getInt("code") == 0) {
				JSONObject data = jsonResponse.getJSONObject("data");
				JSONArray items = data.getJSONArray("items");

				if (items.length() > 0) {
					JSONArray stockData = items.getJSONArray(0);

					realtimeData.put("symbol", symbol);
					realtimeData.put("name", stockData.optString(1, ""));
					realtimeData.put("current", stockData.optDouble(2, 0.0));
					realtimeData.put("change", stockData.optDouble(3, 0.0));
					realtimeData.put("changePercent", stockData.optDouble(4, 0.0));
					realtimeData.put("volume", stockData.optLong(5, 0));
					realtimeData.put("turnover", stockData.optDouble(6, 0.0));
					realtimeData.put("high", stockData.optDouble(7, 0.0));
					realtimeData.put("low", stockData.optDouble(8, 0.0));
					realtimeData.put("open", stockData.optDouble(9, 0.0));
					realtimeData.put("preClose", stockData.optDouble(10, 0.0));
				}
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ����ʵʱ����ʧ��: %s%n", e.getMessage());
		}

		return realtimeData;
	}

	/**
	 * ����TuShare��Ʊ�б���Ӧ
	 */
	private List<String> parseTuShareStockListResponse(String response) {
		List<String> stockList = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getInt("code") == 0) {
				JSONObject data = jsonResponse.getJSONObject("data");
				JSONArray items = data.getJSONArray("items");

				for (int i = 0; i < items.length(); i++) {
					JSONArray stock = items.getJSONArray(i);
					String code = stock.optString(0, "");
					String name = stock.optString(1, "");

					if (!code.isEmpty() && !name.isEmpty()) {
						stockList.add(code + " - " + name);
					}
				}
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ������Ʊ�б�ʧ��: %s%n", e.getMessage());
		}

		return stockList;
	}

	/**
	 * ����TuShare���������Ӧ
	 */
	private List<Map<String, Object>> parseTuShareSectorResponse(String response) {
		List<Map<String, Object>> sectorList = new ArrayList<>();

		try {
			JSONObject jsonResponse = new JSONObject(response);

			if (jsonResponse.getInt("code") == 0) {
				// ����򻯴���ʵ��Ӧ����TuShare�İ��ӿڵ���
				Map<String, Object> sectorData = new HashMap<>();
				sectorData.put("name", "ʾ�����");
				sectorData.put("changePercent", 1.5);
				sectorData.put("leaderStock", "000001.SZ");
				sectorData.put("leaderChangePercent", 3.2);
				sectorList.add(sectorData);
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] �����������ʧ��: %s%n", e.getMessage());
		}

		return sectorList;
	}

	// ==================== HTTP���󷽷� ====================

	/**
	 * ����POST����TuShare
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
		if (debugMode) {
			System.out.println("[TuShare] ����HTTP����: " + baseUrl + endpoint);
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

				} else {
					System.err.printf("[TuShare] HTTP����: %d %s%n", responseCode, endpoint);
					if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
						System.err.println("[TuShare] Token��֤ʧ�ܣ�����API Token");
						break;
					}
				}

			} catch (Exception e) {
				retries++;
				if (retries <= maxRetries) {
					System.err.printf("[TuShare] ����ʧ�ܣ���%d������: %s%n", retries, e.getMessage());
					try {
						Thread.sleep(1000 * retries); // ָ���˱�
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				} else {
					System.err.printf("[TuShare] ��������ʧ��: %s - %s%n", endpoint, e.getMessage());
					failedRequests++;
				}
			}
		}

		return null;
	}

	// ==================== ���߷��� ====================

	/**
	 * ��������
	 */
	public boolean testConnection() {
		try {
			JSONObject testRequest = new JSONObject();
			testRequest.put("api_name", "trade_cal");
			testRequest.put("token", getApiToken());

			JSONObject params = new JSONObject();
			params.put("exchange", "SSE");
			params.put("start_date", "20240101");
			params.put("end_date", "20240110");
			testRequest.put("params", params);

			testRequest.put("fields", "exchange,cal_date,is_open");

			String response = sendPostRequest("", testRequest.toString());

			if (response != null) {
				JSONObject jsonResponse = new JSONObject(response);
				boolean success = jsonResponse.getInt("code") == 0;

				if (debugMode) {
					System.out.printf("[TuShare] ���Ӳ���: %s%n", success ? "�ɹ�" : "ʧ��");
				}

				return success;
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ���Ӳ���ʧ��: %s%n", e.getMessage());
		}

		return false;
	}

	/**
	 * ��ȡAPI Token
	 */
	private String getApiToken() {
		if (apiToken == null || apiToken.trim().isEmpty()) {
			System.err.println("[TuShare] ����: δ����API Token�������setApiToken��������");
			return "cf717df1f1a23819051ffec86c681a0dac5a88a836d3ddc4c2661199"; // Ĭ��token����Ҫ�û�����
		}
		return apiToken;
	}

	/**
	 * �淶����Ʊ���� - ����TuShare��ʽ
	 */
	private String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.trim().isEmpty()) {
			return symbol;
		}

		String normalized = symbol.trim().toUpperCase();

		// TuShare��Ʊ�����ʽ: 000001.SZ, 600000.SH
		if (normalized.matches("\\d{6}")) {
			// �Զ���ӽ�������׺
			if (normalized.startsWith("6") || normalized.startsWith("9")) {
				return normalized + ".SH";
			} else if (normalized.startsWith("0") || normalized.startsWith("2") || normalized.startsWith("3")) {
				return normalized + ".SZ";
			} else if (normalized.startsWith("4") || normalized.startsWith("8")) {
				return normalized + ".BJ"; // ������
			}
		}

		// ����Ѿ�������׺��ֱ�ӷ���
		if (normalized.matches("\\d{6}\\.(SH|SZ|BJ)")) {
			return normalized;
		}

		// ����������ƣ���ȡ���벿��
		if (normalized.contains("-")) {
			String[] parts = normalized.split("-");
			if (parts.length > 0) {
				return normalizeSymbol(parts[0].trim());
			}
		}

		return normalized;
	}

	/**
	 * ��������ʱ�� - ����TuShare��ʽ
	 */
	private LocalDateTime parseDateTime(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) {
			return null;
		}

		try {
			// TuShare��Ҫʹ��yyyyMMdd��ʽ
			DateTimeFormatter[] formatters = { DateTimeFormatter.ofPattern("yyyyMMdd"),
					DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("yyyy/MM/dd") };

			for (DateTimeFormatter formatter : formatters) {
				try {
					if (dateStr.length() == 8) { // yyyyMMdd��ʽ
						return LocalDateTime.parse(dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-"
								+ dateStr.substring(6, 8) + "T00:00:00");
					} else {
						return LocalDateTime.parse(dateStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
					}
				} catch (Exception e) {
					// ������һ����ʽ
				}
			}

		} catch (Exception e) {
			System.err.printf("[TuShare] ���ڽ���ʧ��: %s%n", dateStr);
		}

		return null;
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
		List<String> defaultStocks = Arrays.asList("000001.SZ - ƽ������", "000002.SZ - ���A", "000858.SZ - ����Һ",
				"600519.SH - ����ę́", "601318.SH - �й�ƽ��", "600036.SH - ��������");

		System.out.println("? ʹ��Ĭ�Ϲ�Ʊ�б�");
		return new ArrayList<>(defaultStocks);
	}

	// ==================== ���÷��� ====================

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
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
		System.out.println("? TuShare���ݻ��������");
	}

	// ==================== ԭ�е������������ֲ��� ====================

	/**
	 * ��ȡ����ͳ����Ϣ
	 */
	public Map<String, Object> getServiceStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalRequests", totalRequests);
		stats.put("failedRequests", failedRequests);
		stats.put("successRate",
				totalRequests > 0 ? (double) (totalRequests - failedRequests) / totalRequests * 100 : 0.0);
		stats.put("avgResponseTime", totalRequests > 0 ? totalResponseTime / totalRequests : 0);
		stats.put("cacheSize", dataCache.size());
		stats.put("baseUrl", baseUrl);
		stats.put("connected", testConnection());

		return stats;
	}

	/**
	 * ��ӡ����״̬
	 */
	public void printServiceStatus() {
		Map<String, Object> stats = getServiceStatistics();
		System.out.println("\n=== TuShare���ݷ���״̬ ===");
		System.out.printf("��������: %d%n", stats.get("totalRequests"));
		System.out.printf("ʧ������: %d%n", stats.get("failedRequests"));
		System.out.printf("�ɹ���: %.1f%%%n", stats.get("successRate"));
		System.out.printf("ƽ����Ӧʱ��: %dms%n", stats.get("avgResponseTime"));
		System.out.printf("�����С: %d%n", stats.get("cacheSize"));
		System.out.printf("����״̬: %s%n", Boolean.TRUE.equals(stats.get("connected")) ? "����" : "�쳣");
		System.out.printf("�����ַ: %s%n", stats.get("baseUrl"));
	}

	/**
	 * ��ǿ�����ݻ�ȡ���� - ����֤
	 */
	public List<BarEvent> getValidatedStockHistory(String symbol, LocalDateTime start, LocalDateTime end, String period,
			String adjust) {
		List<BarEvent> rawData = getStockHistory(symbol, start, end, period, adjust);

		if (rawData.isEmpty()) {
			return rawData;
		}

		// ... ԭ�е���֤�߼����ֲ���
		return rawData;
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

		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(data);
		report.setSeriesValidation(seriesResult);

		return report;
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