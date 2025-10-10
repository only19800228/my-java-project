// 改进的测试类
package com.Quantitative.test.infrastructure;

import java.time.LocalDateTime;

import com.Quantitative.common.health.SystemHealthChecker;
import com.Quantitative.config.manager.DynamicConfigManager;
import com.Quantitative.data.DataFeed;
import com.Quantitative.data.manager.UnifiedDataManager;
import com.Quantitative.data.validation.DataQualityReport;
import com.Quantitative.data.validation.DataValidator;

/**
 * 第一阶段基础设施测试 - 增强版本
 */
public class Phase1InfrastructureTest {

	public static void main(String[] args) {
		System.out.println("=== 第一阶段基础设施测试 ===\n");

		try {
			// 0. 等待系统初始化
			Thread.sleep(500);

			// 1. 测试配置管理系统
			testConfigManagement();

			// 2. 测试数据管理系统
			testDataManagement();

			// 3. 测试健康检查系统
			testHealthCheck();

			// 4. 综合测试
			testIntegration();

			System.out.println("\n=== 第一阶段测试完成 ===");

		} catch (Exception e) {
			System.err.println("测试过程中出现异常: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void testConfigManagement() {
		System.out.println("1. 测试配置管理系统...");

		try {
			DynamicConfigManager configManager = DynamicConfigManager.getInstance();

			// 测试基本配置操作
			System.out.println("   - 测试基本配置操作");
			configManager.setConfig("test.string", "hello");
			configManager.setConfig("test.number", 42);
			configManager.setConfig("test.boolean", true);

			// 验证配置值
			String stringValue = configManager.getConfig("test.string", "default");
			Integer numberValue = configManager.getConfig("test.number", 0);
			Boolean booleanValue = configManager.getConfig("test.boolean", false);

			boolean configTestPassed = "hello".equals(stringValue) && numberValue == 42 && booleanValue;

			System.out.println("   配置值验证: " + (configTestPassed ? "✅ 通过" : "❌ 失败"));

			// 测试配置监听
			System.out.println("   - 测试配置监听");
			final String[] listenerResult = new String[1];
			configManager.addConfigListener("test.string", value -> {
				listenerResult[0] = "监听到变更: " + value;
				System.out.println("   " + listenerResult[0]);
			});

			// 触发配置变更
			configManager.setConfig("test.string", "world");

			// 测试配置验证
			System.out.println("   - 测试配置验证");
			boolean valid = configManager.setConfig("risk.maxPositionRatio", 0.5);
			boolean invalid = configManager.setConfig("risk.maxPositionRatio", 1.5);

			System.out.println("   风险配置验证: " + (valid ? "✅ 通过" : "❌ 失败"));
			System.out.println("   无效配置验证: " + (!invalid ? "✅ 正确拒绝" : "❌ 错误接受"));

			// 测试配置回滚
			System.out.println("   - 测试配置回滚");
			configManager.setConfig("test.rollback", "original");
			configManager.setConfig("test.rollback", "newvalue");
			boolean rollbackSuccess = configManager.rollbackConfig("test.rollback");
			String rollbackValue = configManager.getConfig("test.rollback", "");

			System.out.println("   配置回滚: " + (rollbackSuccess && "original".equals(rollbackValue) ? "✅ 成功" : "❌ 失败"));

			System.out.println("   ✅ 配置管理测试完成\n");

		} catch (Exception e) {
			System.err.println("   ❌ 配置管理测试失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void testDataManagement() {
		System.out.println("2. 测试数据管理系统...");

		UnifiedDataManager dataManager = UnifiedDataManager.getInstance();

		try {
			// 测试数据信息获取
			LocalDateTime start = LocalDateTime.now().minusMonths(1);
			LocalDateTime end = LocalDateTime.now();

			System.out.println("   - 测试数据可用性检查");
			boolean available = dataManager.isDataAvailable("000001", start, end);
			System.out.println("   数据可用性检查: " + (available ? "✅ 有数据" : "⚠️ 无数据（预期中）"));

			System.out.println("   - 测试数据质量报告");
			DataQualityReport report = new DataQualityReport("TEST", start, end);

			// 创建模拟的验证结果
			java.util.List<String> emptyList = java.util.Collections.emptyList();
			DataValidator.ValidationResult mockResult = new DataValidator.ValidationResult(true, "VALID", emptyList,
					emptyList, LocalDateTime.now());

			report.addValidationResult(mockResult);

			double qualityScore = report.getDataQualityScore();
			System.out.println("   数据质量报告: " + qualityScore + "分 " + (qualityScore >= 80 ? "✅ 良好" : "⚠️ 需改进"));

			System.out.println("   - 测试缓存管理");
			dataManager.clearCache();
			System.out.println("   缓存清理: ✅ 完成");

			System.out.println("   ✅ 数据管理测试完成\n");

		} catch (Exception e) {
			System.err.println("   ❌ 数据管理测试失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void testHealthCheck() {
		System.out.println("3. 测试健康检查系统...");

		SystemHealthChecker healthChecker = SystemHealthChecker.getInstance();

		try {
			System.out.println("   - 测试快速健康检查");
			boolean quickHealthy = healthChecker.quickHealthCheck();
			System.out.println("   快速健康检查: " + (quickHealthy ? "✅ 健康" : "❌ 异常"));

			System.out.println("   - 测试完整健康检查");
			SystemHealthChecker.HealthCheckResult result = healthChecker.performHealthCheck();
			result.printReport();

			boolean overallHealthy = result.isOverallHealthy();
			System.out.println("   完整健康检查: " + (overallHealthy ? "✅ 通过" : "❌ 未通过"));

			System.out.println("   ✅ 健康检查测试完成\n");

		} catch (Exception e) {
			System.err.println("   ❌ 健康检查测试失败: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void testIntegration() {
		System.out.println("4. 测试系统集成...");

		try {
			// 测试各组件协同工作
			DynamicConfigManager configManager = DynamicConfigManager.getInstance();
			UnifiedDataManager dataManager = UnifiedDataManager.getInstance();
			SystemHealthChecker healthChecker = SystemHealthChecker.getInstance();

			// 测试配置影响数据管理
			boolean cacheEnabled = configManager.getConfig("performance.cacheEnabled", true);
			System.out.println("   缓存配置: " + (cacheEnabled ? "✅ 已启用" : "⚠️ 已禁用"));

			// 测试系统健康状态
			boolean systemHealthy = healthChecker.quickHealthCheck();
			System.out.println("   系统健康状态: " + (systemHealthy ? "✅ 健康" : "❌ 异常"));

			// 测试数据管理器状态
			DataFeed defaultFeed = dataManager.getDefaultDataFeed();
			System.out.println("   默认数据馈送: " + (defaultFeed != null ? "✅ 已设置" : "⚠️ 未设置"));

			System.out.println("   ✅ 系统集成测试完成\n");

		} catch (Exception e) {
			System.err.println("   ❌ 系统集成测试失败: " + e.getMessage());
			e.printStackTrace();
		}
	}
}