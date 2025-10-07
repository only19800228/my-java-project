package com.Quantitative.core.exceptions;

/**
 * 错误码枚举
 */
public enum ErrorCode {
	// 数据相关错误
	DATA_LOAD_FAILED("DATA_001", "数据加载失败"), INSUFFICIENT_DATA("DATA_002", "数据不足"), INVALID_DATA_FORMAT("DATA_003",
			"数据格式错误"),

	// 策略相关错误
	STRATEGY_INIT_FAILED("STRATEGY_001", "策略初始化失败"), STRATEGY_EXECUTION_FAILED("STRATEGY_002",
			"策略执行失败"), INVALID_SIGNAL("STRATEGY_003", "无效信号"),

	// 订单相关错误
	ORDER_REJECTED("ORDER_001", "订单被拒绝"), INSUFFICIENT_CASH("ORDER_002", "资金不足"), INVALID_ORDER("ORDER_003", "无效订单"),

	// 执行相关错误
	EXECUTION_FAILED("EXECUTION_001", "订单执行失败"), EXECUTION_TIMEOUT("EXECUTION_002", "订单执行超时"),

	// 风险相关错误
	RISK_LIMIT_EXCEEDED("RISK_001", "超过风险限制"), POSITION_LIMIT_EXCEEDED("RISK_002", "超过持仓限制"),

	// 系统错误
	SYSTEM_ERROR("SYS_001", "系统内部错误"), CONFIGURATION_ERROR("SYS_002", "配置错误");

	private final String code;
	private final String description;

	ErrorCode(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return code + ": " + description;
	}
}