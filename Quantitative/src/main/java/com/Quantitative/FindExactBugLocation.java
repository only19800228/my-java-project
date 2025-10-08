package com.Quantitative;

public class FindExactBugLocation {
	public static void main(String[] args) {
		System.out.println("=== 精确定位数据错误 ===");

		// 1. 深入调试 syncToCSV
		SyncToCSVDeepDebug.debugSyncToCSVDetails();

		// 2. 检查AKShare适配器
		AKShareAdapterDebug.debugAKShareAdapter();

		// 3. 直接数据源测试
		DirectDataSourceTest.testDirectDataSources();

		System.out.println("\n=== 深度调试完成 ===");
	}
}
