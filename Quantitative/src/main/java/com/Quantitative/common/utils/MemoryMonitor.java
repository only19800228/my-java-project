package com.Quantitative.common.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * 内存监控工具
 */
public class MemoryMonitor {

	private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	private static long lastGcTime = System.currentTimeMillis();

	/**
	 * 打印内存使用情况
	 */
	public static void printMemoryUsage(String context) {
		MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

		long usedHeap = heapUsage.getUsed() / (1024 * 1024);
		long maxHeap = heapUsage.getMax() / (1024 * 1024);
		double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;

		System.out.printf("[内存监控] %s - 堆内存: %dMB/%dMB (%.1f%%) | 非堆: %dMB\n", context, usedHeap, maxHeap,
				heapUsagePercent, nonHeapUsage.getUsed() / (1024 * 1024));

		// 如果内存使用率过高，建议GC
		if (heapUsagePercent > 80 && System.currentTimeMillis() - lastGcTime > 30000) {
			System.gc();
			lastGcTime = System.currentTimeMillis();
			System.out.println("[内存监控] 触发垃圾回收");
		}
	}

	/**
	 * 检查内存使用是否健康
	 */
	public static boolean isMemoryHealthy() {
		MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
		double usagePercent = (double) heapUsage.getUsed() / heapUsage.getMax();
		return usagePercent < 0.9; // 使用率低于90%认为健康
	}

	/**
	 * 获取内存使用率
	 */
	public static double getMemoryUsagePercent() {
		MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
		return (double) heapUsage.getUsed() / heapUsage.getMax();
	}
}