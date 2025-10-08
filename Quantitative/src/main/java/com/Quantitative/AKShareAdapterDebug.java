package com.Quantitative;

public class AKShareAdapterDebug {

	public static void debugAKShareAdapter() {
		System.out.println("=== 调试 AKShareDataSourceAdapter ===");

		try {
			// 创建AKShare适配器实例
			Class<?> adapterClass = Class.forName("com.Quantitative.data.adapter.AKShareDataSourceAdapter");
			Object adapter = adapterClass.newInstance();

			System.out.println("AKShareDataSourceAdapter 方法列表:");
			for (java.lang.reflect.Method method : adapterClass.getMethods()) {
				if (method.getName().toLowerCase().contains("load") || method.getName().toLowerCase().contains("get")
						|| method.getName().toLowerCase().contains("data")) {
					System.out.println(
							"  " + method.getName() + " - " + java.util.Arrays.toString(method.getParameterTypes()));
				}
			}

			// 检查是否有数据转换逻辑
			checkDataConversionLogic(adapter);

		} catch (Exception e) {
			System.out.println("AKShare适配器调试失败: " + e.getMessage());
		}
	}

	private static void checkDataConversionLogic(Object adapter) {
		try {
			// 检查字段
			java.lang.reflect.Field[] fields = adapter.getClass().getDeclaredFields();
			System.out.println("AKShare适配器字段:");
			for (java.lang.reflect.Field field : fields) {
				field.setAccessible(true);
				String fieldName = field.getName();
				Object value = field.get(adapter);

				// 特别关注可能影响价格的字段
				if (fieldName.toLowerCase().contains("price") || fieldName.toLowerCase().contains("factor")
						|| fieldName.toLowerCase().contains("scale") || fieldName.toLowerCase().contains("adjust")
						|| fieldName.toLowerCase().contains("divisor")) {
					System.out.printf("  ⚠️ %s = %s%n", fieldName, value);
				} else if (fieldName.toLowerCase().contains("data")) {
					System.out.printf("  %s: %s%n", fieldName,
							value != null ? value.getClass().getSimpleName() : "null");
				}
			}
		} catch (Exception e) {
			System.out.println("数据转换逻辑检查失败: " + e.getMessage());
		}
	}
}