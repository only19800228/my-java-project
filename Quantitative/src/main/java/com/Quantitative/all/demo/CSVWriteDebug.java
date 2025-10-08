package com.Quantitative.all.demo;

import java.time.LocalDateTime;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.csv.CSVDataSource;

public class CSVWriteDebug {

	public static void debugCSVWriting() {
		System.out.println("=== 调试CSV写入过程 ===");

		// 创建测试数据
		BarEvent testBar = new BarEvent(LocalDateTime.of(2014, 1, 2, 0, 0), "601123", 3.60, // 正确价格
				3.70, 3.40, 3.60, 403770, 0);

		System.out.println("原始Bar数据:");
		System.out.printf("  O:%.2f H:%.2f L:%.2f C:%.2f%n", testBar.getOpen(), testBar.getHigh(), testBar.getLow(),
				testBar.getClose());

		// 测试CSV数据源写入
		CSVDataSource csvDataSource = new CSVDataSource();

		try {
			// 检查CSV写入前的数据转换
			debugCSVDataConversion(csvDataSource, testBar);

		} catch (Exception e) {
			System.out.println("CSV写入调试失败: " + e.getMessage());
		}
	}

	private static void debugCSVDataConversion(CSVDataSource csvDataSource, BarEvent bar) {
		try {
			// 获取写入CSV的方法
			java.lang.reflect.Method writeMethod = findWriteMethod(csvDataSource);
			if (writeMethod != null) {
				System.out.println("找到CSV写入方法: " + writeMethod.getName());
			}

			// 检查是否有数据转换逻辑
			java.lang.reflect.Field[] fields = csvDataSource.getClass().getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				if (field.getName().toLowerCase().contains("factor") || field.getName().toLowerCase().contains("scale")
						|| field.getName().toLowerCase().contains("adjust")) {
					field.setAccessible(true);
					Object value = field.get(csvDataSource);
					System.out.printf("发现转换字段: %s = %s%n", field.getName(), value);
				}
			}

		} catch (Exception e) {
			System.out.println("CSV转换调试失败: " + e.getMessage());
		}
	}

	private static java.lang.reflect.Method findWriteMethod(CSVDataSource csvDataSource) {
		for (java.lang.reflect.Method method : csvDataSource.getClass().getDeclaredMethods()) {
			if (method.getName().toLowerCase().contains("write") || method.getName().toLowerCase().contains("save")) {
				return method;
			}
		}
		return null;
	}
}