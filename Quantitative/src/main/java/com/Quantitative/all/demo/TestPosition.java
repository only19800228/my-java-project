package com.Quantitative.all.demo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.AKShareDataService;
import com.Quantitative.portfolio.Position;

public class TestPosition {
	public static void main(String[] args) {
		// 测试 Position 类
		Position position = new Position("000001");

		// 测试买入
		position.addBuy(1000, 10.0);
		System.out.println("买入后: " + position);

		// 测试卖出
		position.addSell(500, 11.0);
		System.out.println("卖出后: " + position);

		// 测试获取方法
		System.out.println("平均成本: " + position.getAvgCost());
		System.out.println("持仓数量: " + position.getQuantity());

		System.out.println("✓ Position 类测试通过");

		// 创建数据服务实例
		AKShareDataService dataService = new AKShareDataService();
		dataService.setDebugMode(true);

		// 测试连接
		if (dataService.testConnection()) {
			System.out.println("✓ AKShare数据服务连接成功");
		} else {
			System.out.println("⚠ AKShare数据服务连接失败");
		}

		// 获取股票历史数据
		LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2024, 3, 1, 0, 0);
		List<BarEvent> stockData = dataService.getStockHistory("000001", start, end, "daily", "qfq");

		System.out.printf("获取到 %d 条历史数据%n", stockData.size());

		// 获取股票列表
		List<String> stockList = dataService.getStockList();
		System.out.printf("获取到 %d 只股票%n", stockList.size());

		// 获取实时数据
		Map<String, Object> realtimeData = dataService.getStockRealtime("000001");
		System.out.println("实时数据: " + realtimeData);

		// 获取服务统计
		dataService.printServiceStatus();

		// 使用不同的配置
		AKShareDataService customService = new AKShareDataService("http://192.168.10.45:8888/api/akshare");
		customService.setConnectTimeout(5000);
		customService.setMaxRetries(5);
		customService.setCacheExpiryMillis(10 * 60 * 1000); // 10分钟缓存
	}
}