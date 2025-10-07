package com.Quantitative.common.cache;

import java.util.Map;
import java.util.Set;

/**
 * 修复后的缓存管理器 - 使用 UnifiedCacheManager
 */
public class CacheManager {
	private static final CacheManager INSTANCE = new CacheManager();
	private final UnifiedCacheManager unifiedCacheManager;

	private CacheManager() {
		this.unifiedCacheManager = UnifiedCacheManager.getInstance();
		System.out.println("[缓存管理器] 使用统一缓存管理器");
	}

	public static CacheManager getInstance() {
		return INSTANCE;
	}

	/**
	 * 获取缓存区域 - 兼容原有接口
	 */
	public UnifiedCacheManager.CacheRegion getCache(String name) {
		return unifiedCacheManager.getRegion(name);
	}

	/**
	 * 创建缓存 - 兼容原有接口
	 */
	public UnifiedCacheManager.CacheRegion createCache(String name, int maxSize, int expireAfterAccessMinutes,
			int expireAfterWriteHours) {
		long expireMillis = expireAfterAccessMinutes * 60 * 1000L;
		return unifiedCacheManager.createRegion(name, maxSize, expireMillis);
	}

	/**
	 * 简化版创建缓存
	 */
	public UnifiedCacheManager.CacheRegion createCache(String name, int maxSize, int expireAfterAccessMinutes) {
		return createCache(name, maxSize, expireAfterAccessMinutes, 2);
	}

	public void removeCache(String name) {
		UnifiedCacheManager.CacheRegion region = unifiedCacheManager.getRegion(name);
		if (region != null) {
			region.clear();
			System.out.printf("[缓存管理器] 移除缓存区域: %s%n", name);
		}
	}

	/**
	 * 获取所有缓存统计 - 兼容原有接口
	 */
	public Map<String, UnifiedCacheManager.CacheStats> getAllStats() {
		return unifiedCacheManager.getAllStats();
	}

	/**
	 * 打印所有缓存统计
	 */
	public void printAllStats() {
		unifiedCacheManager.printAllStats();
	}

	/**
	 * 清理所有缓存
	 */
	public void cleanupAll() {
		unifiedCacheManager.cleanupAll();
	}

	/**
	 * 清空所有缓存
	 */
	public void clearAll() {
		unifiedCacheManager.cleanupAll();
	}

	/**
	 * 获取缓存名称列表
	 */
	public Set<String> getCacheNames() {
		Map<String, UnifiedCacheManager.CacheStats> stats = getAllStats();
		return stats.keySet();
	}

	/**
	 * 检查缓存是否存在
	 */
	public boolean containsCache(String name) {
		return unifiedCacheManager.getRegion(name) != null;
	}

	/**
	 * 获取总缓存条目数
	 */
	public long getTotalCacheSize() {
		return unifiedCacheManager.getAllStats().values().stream()
				.mapToLong(UnifiedCacheManager.CacheStats::getCurrentSize).sum();
	}
}