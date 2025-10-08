package com.Quantitative.all.demo;

public class FindDataBug {
	public static void main(String[] args) {
		System.out.println("=== 寻找数据错误源头 ===");

		// 1. 调试 syncToCSV 方法
		DataSourceManagerDebug.debugSyncToCSV();

		// 2. 调试CSV写入
		CSVWriteDebug.debugCSVWriting();

		// 3. 完整数据流监控
		CompleteDataFlowMonitor.monitorCompleteFlow();

		System.out.println("\n=== 调试完成 ===");
	}
}