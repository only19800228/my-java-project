package com.Quantitative.common.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 修复后的高性能缓存工具 - 使用 UnifiedCacheManager
 */
public class OptimizedCacheUtils {
	private static final UnifiedCacheManager cacheManager = UnifiedCacheManager.getInstance();
	private static final Map<String, Object> cacheLocks = new ConcurrentHashMap<>();

	/**
	 * 线程安全的缓存访问
	 */
	public static <T> T getCachedThreadSafe(String cacheName, String key, Supplier<T> supplier) {
		return cacheManager.getCached(cacheName, key, supplier);
	}

	/**
	 * 批量缓存操作
	 */
	public static <T> Map<String, T> batchGetCached(String cacheName, List<String> keys,
			Function<String, T> valueLoader) {
		Map<String, T> results = new HashMap<>();
		List<String> missingKeys = new ArrayList<>();

		// 首先批量获取已缓存的
		UnifiedCacheManager.CacheRegion cache = cacheManager.getRegion(cacheName);
		if (cache == null) {
			cache = cacheManager.createRegion(cacheName, 1000, 30 * 60 * 1000L);
		}

		for (String key : keys) {
			T value = cache.get(key);
			if (value != null) {
				results.put(key, value);
			} else {
				missingKeys.add(key);
			}
		}

		// 批量加载缺失的数据
		if (!missingKeys.isEmpty()) {
			for (String key : missingKeys) {
				T value = getCachedThreadSafe(cacheName, key, () -> valueLoader.apply(key));
				if (value != null) {
					results.put(key, value);
				}
			}
		}

		return results;
	}

	/**
	 * 生成RSI缓存键 - 兼容原有方法
	 */
	public static String generateRSIKey(List<Double> prices, int period) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append("RSI_").append(period);

		// 优化：只使用关键价格数据生成哈希
		if (prices != null && !prices.isEmpty()) {
			int sampleSize = Math.min(5, prices.size());
			double sum = 0;
			for (int i = prices.size() - sampleSize; i < prices.size(); i++) {
				sum += prices.get(i);
			}
			double avg = sum / sampleSize;
			keyBuilder.append("_").append(String.format("%.6f", avg));
		}

		return keyBuilder.toString();
	}

	/**
	 * 生成通用指标缓存键
	 */
	public static String generateIndicatorKey(String indicatorName, List<Double> prices, int period, Object... params) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(indicatorName).append("_").append(period);

		// 使用价格数据的哈希
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
}