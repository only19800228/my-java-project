// 修复的诊断工具 - 使用公共方法
package com.Quantitative.config.manager;

import java.util.Map;

/**
 * 配置管理器诊断工具 - 使用公共API
 */
public class ConfigManagerDiagnostic {

	public static void diagnose() {
		System.out.println("=== 配置管理器诊断 ===");

		DynamicConfigManager configManager = DynamicConfigManager.getInstance();

		// 检查基础状态
		System.out.println("1. 检查基础状态:");
		System.out.println("   - 初始化状态: " + (configManager.isInitialized() ? "✅ 已初始化" : "❌ 未初始化"));

		// 测试基本操作
		System.out.println("2. 测试基本操作:");
		try {
			boolean result = configManager.setConfig("diagnostic.test", "hello");
			System.out.println("   - setConfig操作: " + (result ? "✅ 成功" : "❌ 失败"));

			String value = configManager.getConfig("diagnostic.test", "default");
			System.out.println("   - getConfig操作: " + ("hello".equals(value) ? "✅ 成功" : "❌ 失败"));

		} catch (Exception e) {
			System.out.println("   - 基本操作: ❌ 异常: " + e.getMessage());
			e.printStackTrace();
		}

		// 检查默认配置
		System.out.println("3. 检查默认配置:");
		try {
			Map<String, Object> configs = configManager.exportAllConfigs();
			System.out.println("   - 配置数量: " + configs.size());
			if (configs.size() > 0) {
				configs.forEach((k, v) -> {
					System.out.println("     " + k + " = " + v + " (类型: "
							+ (v != null ? v.getClass().getSimpleName() : "null") + ")");
				});
			} else {
				System.out.println("   - ⚠️ 没有配置项");
			}
		} catch (Exception e) {
			System.out.println("   - 导出配置: ❌ 异常: " + e.getMessage());
		}

		// 测试配置验证
		System.out.println("4. 测试配置验证:");
		try {
			boolean valid = configManager.setConfig("risk.test", 0.5);
			boolean invalid = configManager.setConfig("risk.test", 1.5);
			System.out.println("   - 有效配置(0.5): " + (valid ? "✅ 接受" : "❌ 拒绝"));
			System.out.println("   - 无效配置(1.5): " + (!invalid ? "✅ 正确拒绝" : "❌ 错误接受"));
		} catch (Exception e) {
			System.out.println("   - 配置验证: ❌ 异常: " + e.getMessage());
		}

		System.out.println("=== 诊断完成 ===");
	}

	public static void main(String[] args) {
		diagnose();
	}
}