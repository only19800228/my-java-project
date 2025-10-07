package com.Quantitative.common.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指标计算缓存 - 提高回测性能
 */
public class IndicatorCache {
    private final Map<String, Double> cache;
    private final int maxSize;
    private final LinkedHashMap<String, Long> accessOrder;
    private long hitCount = 0;
    private long missCount = 0;
    
    public IndicatorCache() {
        this(1000); // 默认缓存1000个结果
    }
    
    public IndicatorCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>(maxSize);
        this.accessOrder = new LinkedHashMap<String, Long>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > maxSize;
            }
        };
    }
    
    /**
     * 生成缓存键
     */
    public String generateKey(String indicatorName, List<Double> prices, int period, Object... params) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(indicatorName).append("_");
        keyBuilder.append(period).append("_");
        
        // 包含价格数据的哈希（只取最后几个价格用于哈希）
        int hashCount = Math.min(10, prices.size());
        for (int i = prices.size() - hashCount; i < prices.size(); i++) {
            keyBuilder.append(String.format("%.4f", prices.get(i))).append("_");
        }
        
        // 包含参数
        for (Object param : params) {
            keyBuilder.append(param).append("_");
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 从缓存获取结果
     */
    public Double get(String key) {
        synchronized (accessOrder) {
            Double result = cache.get(key);
            if (result != null) {
                hitCount++;
                accessOrder.put(key, System.currentTimeMillis());
                return result;
            }
            missCount++;
            return null;
        }
    }
    
    /**
     * 存入缓存
     */
    public void put(String key, Double value) {
        synchronized (accessOrder) {
            if (cache.size() >= maxSize) {
                // 移除最久未使用的条目
                Iterator<Map.Entry<String, Long>> iterator = accessOrder.entrySet().iterator();
                if (iterator.hasNext()) {
                    String oldestKey = iterator.next().getKey();
                    cache.remove(oldestKey);
                    iterator.remove();
                }
            }
            
            cache.put(key, value);
            accessOrder.put(key, System.currentTimeMillis());
        }
    }
    
    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        long total = hitCount + missCount;
        return total > 0 ? (double) hitCount / total : 0.0;
    }
    
    /**
     * 获取缓存统计
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("currentSize", cache.size());
        stats.put("maxSize", maxSize);
        stats.put("hitCount", hitCount);
        stats.put("missCount", missCount);
        stats.put("hitRate", getHitRate());
        stats.put("memoryUsage", estimateMemoryUsage());
        return stats;
    }
    
    /**
     * 估算内存使用
     */
    private long estimateMemoryUsage() {
        // 每个条目大约: key(50字节) + Double(16字节) + 其他(20字节) ≈ 86字节
        return cache.size() * 86L;
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        synchronized (accessOrder) {
            cache.clear();
            accessOrder.clear();
            hitCount = 0;
            missCount = 0;
        }
    }
    
    /**
     * 移除特定键
     */
    public void remove(String key) {
        synchronized (accessOrder) {
            cache.remove(key);
            accessOrder.remove(key);
        }
    }
}