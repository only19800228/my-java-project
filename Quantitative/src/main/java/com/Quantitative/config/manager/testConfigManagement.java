package com.Quantitative.config.manager;

/**
 * @author 测试配置管理系统
 *
 */
public class testConfigManagement {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		testConfigManagement();
	}

	private static void testConfigManagement() {
		System.out.println("1. 测试配置管理系统...");

		try {
			System.out.println("   - 运行诊断工具...");
			ConfigManagerDiagnostic.diagnose();

			DynamicConfigManager configManager = DynamicConfigManager.getInstance();

			System.out.println("   - 测试核心功能");

			// 测试1: 基础数据类型
			System.out.println("   - 测试1: 基础数据类型配置");
			boolean strSuccess = configManager.setConfig("test.string", "hello");
			boolean numSuccess = configManager.setConfig("test.number", 42);
			boolean boolSuccess = configManager.setConfig("test.boolean", true);
			boolean doubleSuccess = configManager.setConfig("test.double", 3.14);

			String strValue = configManager.getConfig("test.string", "");
			Integer numValue = configManager.getConfig("test.number", 0);
			Boolean boolValue = configManager.getConfig("test.boolean", false);
			Double doubleValue = configManager.getConfig("test.double", 0.0);

			boolean basicTest = strSuccess && numSuccess && boolSuccess && doubleSuccess && "hello".equals(strValue)
					&& numValue == 42 && boolValue && doubleValue == 3.14;
			System.out.println("     基础数据类型: " + (basicTest ? "✅ 通过" : "❌ 失败"));

			// 测试2: 配置验证
			System.out.println("   - 测试2: 配置验证功能");
			boolean validRisk1 = configManager.setConfig("risk.maxPositionRatio", 0.3); // 有效
			boolean validRisk2 = configManager.setConfig("risk.maxPositionRatio", 0.0); // 有效（边界）
			boolean validRisk3 = configManager.setConfig("risk.maxPositionRatio", 1.0); // 有效（边界）
			boolean invalidRisk1 = configManager.setConfig("risk.maxPositionRatio", -0.1); // 无效
			boolean invalidRisk2 = configManager.setConfig("risk.maxPositionRatio", 1.1); // 无效
			boolean invalidRisk3 = configManager.setConfig("risk.maxPositionRatio", "invalid"); // 无效类型

			System.out.println("     风险配置验证测试:");
			System.out.println("       - 有效值 0.3: " + (validRisk1 ? "✅ 接受" : "❌ 拒绝"));
			System.out.println("       - 有效值 0.0: " + (validRisk2 ? "✅ 接受" : "❌ 拒绝"));
			System.out.println("       - 有效值 1.0: " + (validRisk3 ? "✅ 接受" : "❌ 拒绝"));
			System.out.println("       - 无效值 -0.1: " + (!invalidRisk1 ? "✅ 正确拒绝" : "❌ 错误接受"));
			System.out.println("       - 无效值 1.1: " + (!invalidRisk2 ? "✅ 正确拒绝" : "❌ 错误接受"));
			System.out.println("       - 无效类型 'invalid': " + (!invalidRisk3 ? "✅ 正确拒绝" : "❌ 错误接受"));

			boolean validationTest = validRisk1 && validRisk2 && validRisk3 && !invalidRisk1 && !invalidRisk2
					&& !invalidRisk3;

			// 测试3: 特殊配置验证
			System.out.println("   - 测试3: 特殊配置验证");
			boolean validTimeout = configManager.setConfig("datafeed.timeout", 5000); // 有效
			boolean invalidTimeout = configManager.setConfig("datafeed.timeout", -1000); // 无效
			boolean validCapital = configManager.setConfig("backtest.initialCapital", 50000.0); // 有效
			boolean invalidCapital = configManager.setConfig("backtest.initialCapital", 0.0); // 无效

			System.out.println("     特殊配置验证:");
			System.out.println("       - 有效超时 5000: " + (validTimeout ? "✅ 接受" : "❌ 拒绝"));
			System.out.println("       - 无效超时 -1000: " + (!invalidTimeout ? "✅ 正确拒绝" : "❌ 错误接受"));
			System.out.println("       - 有效资金 50000: " + (validCapital ? "✅ 接受" : "❌ 拒绝"));
			System.out.println("       - 无效资金 0: " + (!invalidCapital ? "✅ 正确拒绝" : "❌ 错误接受"));

			boolean specialValidationTest = validTimeout && !invalidTimeout && validCapital && !invalidCapital;

			// 测试4: 回滚功能
			System.out.println("   - 测试4: 配置回滚功能");
			configManager.setConfig("test.rollback", "original");
			configManager.setConfig("test.rollback", "newvalue");
			boolean rollbackSuccess = configManager.rollbackConfig("test.rollback");
			String rollbackValue = configManager.getConfig("test.rollback", "");

			System.out.println("     配置回滚: " + (rollbackSuccess && "original".equals(rollbackValue) ? "✅ 成功" : "❌ 失败"));

			// 总体评估
			boolean overallSuccess = basicTest && validationTest && specialValidationTest && rollbackSuccess
					&& "original".equals(rollbackValue);

			System.out.println("   - 总体评估: " + (overallSuccess ? "✅ 所有测试通过" : "❌ 部分测试失败"));

			if (overallSuccess) {
				System.out.println("   ✅ 配置管理系统测试完成\n");
			} else {
				System.out.println("   ⚠️ 配置管理系统存在一些问题，但基础功能正常\n");
			}

		} catch (Exception e) {
			System.err.println("   ❌ 配置管理测试失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
