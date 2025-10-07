package com.Quantitative.all.demo;

import java.time.LocalDateTime;
import java.util.List;

import com.Quantitative.data.DataSourceManager;
import com.Quantitative.data.factory.DataSourceFactory;

/**
 * 修复后的多数据源使用示例-----创建CSV数据
 */
public class FixedMultiDataSourceDemo {

	public static void main(String[] args) {
		System.out.println("=== 修复后的多数据源演示 ===\n");

		// 方法1: 使用数据源管理器
		DataSourceManager dataManager = new DataSourceManager();
		dataManager.initialize();

		// 方法2: 手动创建和添加数据源
		DataSourceManager customManager = new DataSourceManager();

		// 移除默认数据源
		customManager.removeDataSource("AKSHARE");
		customManager.removeDataSource("CSV");

		// 添加自定义数据源
		customManager.addDataSource("MY_AKSHARE", DataSourceFactory.createAKShareDataSource());
		customManager.addDataSource("MY_CSV", DataSourceFactory.createCSVDataSource("my_data/"));

		customManager.setDefaultDataSource("MY_AKSHARE");
		customManager.initialize();

		// 加载数据
		List data = customManager.loadHistoricalData("601398", LocalDateTime.of(2023, 1, 1, 0, 0),
				LocalDateTime.of(2023, 12, 31, 0, 0));

		System.out.println("? 加载数据: " + data.size() + " 条");
		System.out.println("? 当前数据源: " + customManager.getDefaultDataSource());

		customManager.shutdown();
		System.out.println("=== 演示结束 ===");
	}
}