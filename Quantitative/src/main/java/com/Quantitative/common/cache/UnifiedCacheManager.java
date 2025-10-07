package com.Quantitative.common.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一缓存管理器 - 替换多个重复的缓存实现
 */
public class UnifiedCacheManager {
	private static final UnifiedCacheManager INSTANCE = new UnifiedCacheManager();
	private final Map<String, CacheRegion> cacheRegions;
	private final CacheConfig globalConfig;

	private UnifiedCacheManager() {
		this.cacheRegions = new ConcurrentHashMap<>();
		this.globalConfig = new CacheConfig();
		initializeDefaultRegions();
	}

	public static UnifiedCacheManager getInstance() {
		return INSTANCE;
	}

	private void initializeDefaultRegions() {
		// 指标计算缓存
		createRegion("indicators", 5000, 30 * 60 * 1000L); // 5千条，30分钟
		// 策略计算缓存
		createRegion("strategies", 2000, 60 * 60 * 1000L); // 2千条，60分钟
		// 数据缓存
		createRegion("data", 10000, 10 * 60 * 1000L); // 1万条，10分钟

		System.out.println("[统一缓存] 默认缓存区域初始化完成");
	}

	/**
	 * 创建缓存区域
	 */
	public CacheRegion createRegion(String name, int maxSize, long expireAfterWriteMillis) {
		CacheRegion region = new CacheRegion(name, maxSize, expireAfterWriteMillis);
		cacheRegions.put(name, region);
		System.out.printf("[统一缓存] 创建区域: %s (大小:%,d, 过期:%d分钟)%n", name, maxSize, expireAfterWriteMillis / (60 * 1000));
		return region;
	}

	/**
	 * 获取缓存区域
	 */
	public CacheRegion getRegion(String name) {
		return cacheRegions.get(name);
	}

	/**
	 * 获取或创建缓存区域
	 */
	public CacheRegion getOrCreateRegion(String name, int defaultMaxSize, long defaultExpireMillis) {
		return cacheRegions.computeIfAbsent(name, k -> createRegion(name, defaultMaxSize, defaultExpireMillis));
	}

	/**
	 * 线程安全的缓存访问
	 */
	public <T> T getCached(String regionName, String key, java.util.function.Supplier<T> supplier) {
		CacheRegion region = getOrCreateRegion(regionName, 1000, 30 * 60 * 1000L);
		return region.get(key, supplier);
	}

	/**
	 * 清理所有缓存区域
	 */
	public void cleanupAll() {
		cacheRegions.values().forEach(CacheRegion::cleanup);
		System.out.println("[统一缓存] 执行全局缓存清理");
	}

	/**
	 * 获取缓存统计
	 */
	public Map<String, CacheStats> getAllStats() {
		Map<String, CacheStats> stats = new HashMap<>();
		cacheRegions.forEach((name, region) -> stats.put(name, region.getStats()));
		return stats;
	}

	/**
	 * 打印所有缓存统计
	 */
	public void printAllStats() {
		System.out.println("\n=== 统一缓存统计 ===");
		getAllStats().forEach((name, stats) -> {
			System.out.printf("区域[%s]: %s%n", name, stats);
		});
	}

	// ==================== 内部类 ====================

	/**
	 * 缓存配置
	 */
	public static class CacheConfig {
		private int defaultMaxSize = 1000;
		private long defaultExpireAfterWrite = 30 * 60 * 1000L; // 30分钟
		private boolean statisticsEnabled = true;

		// Getter/Setter
		public int getDefaultMaxSize() {
			return defaultMaxSize;
		}

		public void setDefaultMaxSize(int defaultMaxSize) {
			this.defaultMaxSize = defaultMaxSize;
		}

		public long getDefaultExpireAfterWrite() {
			return defaultExpireAfterWrite;
		}

		public void setDefaultExpireAfterWrite(long defaultExpireAfterWrite) {
			this.defaultExpireAfterWrite = defaultExpireAfterWrite;
		}

		public boolean isStatisticsEnabled() {
			return statisticsEnabled;
		}

		public void setStatisticsEnabled(boolean statisticsEnabled) {
			this.statisticsEnabled = statisticsEnabled;
		}
	}

	/**
	 * 缓存区域
	 */
	public static class CacheRegion {
		private final String name;
		private final int maxSize;
		private final long expireAfterWriteMillis;
		private final Map<String, CacheEntry> cache;
		private final LinkedHashMap<String, Long> accessOrder;

		// 统计
		private long hitCount = 0;
		private long missCount = 0;
		private long evictionCount = 0;

		public CacheRegion(String name, int maxSize, long expireAfterWriteMillis) {
			this.name = name;
			this.maxSize = maxSize;
			this.expireAfterWriteMillis = expireAfterWriteMillis;
			this.cache = new ConcurrentHashMap<>(maxSize);
			this.accessOrder = new LinkedHashMap<>(16, 0.75f, true);
		}

		/**
		 * 获取缓存值，如果不存在则计算
		 */
		@SuppressWarnings("unchecked")
		public <T> T get(String key, java.util.function.Supplier<T> supplier) {
			// 双重检查锁定
			CacheEntry entry = cache.get(key);
			if (entry != null && !entry.isExpired()) {
				hitCount++;
				accessOrder.put(key, System.currentTimeMillis());
				return (T) entry.getValue();
			}

			synchronized (cache) {
				// 再次检查
				entry = cache.get(key);
				if (entry != null && !entry.isExpired()) {
					hitCount++;
					accessOrder.put(key, System.currentTimeMillis());
					return (T) entry.getValue();
				}

				// 计算新值
				missCount++;
				long startTime = System.nanoTime();
				T value = supplier.get();
				long duration = System.nanoTime() - startTime;

				if (value != null) {
					put(key, value);

					if (duration > 1_000_000) { // 记录耗时超过1ms的计算
						System.out.printf("[缓存] %s 计算 %s 耗时: %.3fms%n", name, key, duration / 1_000_000.0);
					}
				}

				return value;
			}
		}

		/**
		 * 直接获取缓存值
		 */
		@SuppressWarnings("unchecked")
		public <T> T get(String key) {
			CacheEntry entry = cache.get(key);
			if (entry != null) {
				if (entry.isExpired()) {
					cache.remove(key);
					accessOrder.remove(key);
					missCount++;
					return null;
				}
				hitCount++;
				accessOrder.put(key, System.currentTimeMillis());
				return (T) entry.getValue();
			}
			missCount++;
			return null;
		}

		/**
		 * 存入缓存
		 */
		public void put(String key, Object value) {
			synchronized (cache) {
				// 检查是否需要清理
				if (cache.size() >= maxSize) {
					evictLRU();
				}

				CacheEntry entry = new CacheEntry(value, System.currentTimeMillis(), expireAfterWriteMillis);
				cache.put(key, entry);
				accessOrder.put(key, System.currentTimeMillis());
			}
		}

		/**
		 * LRU淘汰
		 */
		private void evictLRU() {
			Iterator<String> it = accessOrder.keySet().iterator();
			if (it.hasNext()) {
				String oldestKey = it.next();
				cache.remove(oldestKey);
				it.remove();
				evictionCount++;

				System.out.printf("[缓存] %s LRU淘汰: %s%n", name, oldestKey);
			}
		}

		/**
		 * 清理过期缓存
		 */
		public void cleanup() {
			synchronized (cache) {
				Iterator<Map.Entry<String, CacheEntry>> it = cache.entrySet().iterator();
				int removed = 0;

				while (it.hasNext()) {
					Map.Entry<String, CacheEntry> entry = it.next();
					if (entry.getValue().isExpired()) {
						it.remove();
						accessOrder.remove(entry.getKey());
						removed++;
					}
				}

				if (removed > 0) {
					System.out.printf("[缓存] %s 清理了 %d 个过期条目%n", name, removed);
				}
			}
		}

		/**
		 * 获取缓存统计
		 */
		public CacheStats getStats() {
			synchronized (cache) {
				double hitRate = (hitCount + missCount) > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;

				return new CacheStats(cache.size(), maxSize, hitCount, missCount, evictionCount, hitRate);
			}
		}

		/**
		 * 清空缓存
		 */
		public void clear() {
			synchronized (cache) {
				cache.clear();
				accessOrder.clear();
				hitCount = 0;
				missCount = 0;
				evictionCount = 0;
				System.out.printf("[缓存] %s 已清空%n", name);
			}
		}

		public int size() {
			return cache.size();
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * 缓存条目
	 */
	private static class CacheEntry {
		private final Object value;
		private final long timestamp;
		private final long expireTime;

		public CacheEntry(Object value, long timestamp, long expireAfterWriteMillis) {
			this.value = value;
			this.timestamp = timestamp;
			this.expireTime = timestamp + expireAfterWriteMillis;
		}

		public Object getValue() {
			return value;
		}

		public boolean isExpired() {
			return System.currentTimeMillis() > expireTime;
		}
	}

	/**
	 * 缓存统计
	 */
	public static class CacheStats {
		private final int currentSize;
		private final int maxSize;
		private final long hitCount;
		private final long missCount;
		private final long evictionCount;
		private final double hitRate;

		public CacheStats(int currentSize, int maxSize, long hitCount, long missCount, long evictionCount,
				double hitRate) {
			this.currentSize = currentSize;
			this.maxSize = maxSize;
			this.hitCount = hitCount;
			this.missCount = missCount;
			this.evictionCount = evictionCount;
			this.hitRate = hitRate;
		}

		// Getter方法
		public int getCurrentSize() {
			return currentSize;
		}

		public int getMaxSize() {
			return maxSize;
		}

		public long getHitCount() {
			return hitCount;
		}

		public long getMissCount() {
			return missCount;
		}

		public long getEvictionCount() {
			return evictionCount;
		}

		public double getHitRate() {
			return hitRate;
		}

		@Override
		public String toString() {
			return String.format("CacheStats{size=%d/%d, hits=%,d, misses=%,d, hitRate=%.1f%%, evictions=%,d}",
					currentSize, maxSize, hitCount, missCount, hitRate * 100, evictionCount);
		}
	}
}