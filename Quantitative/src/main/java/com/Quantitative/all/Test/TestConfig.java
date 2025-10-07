package com.Quantitative.all.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试配置类
 */
public class TestConfig {

	// 测试时间范围
	public static final LocalDateTime TEST_START_DATE = LocalDateTime.of(2023, 1, 1, 0, 0);
	public static final LocalDateTime TEST_END_DATE = LocalDateTime.of(2023, 12, 31, 0, 0);

	// 测试资金
	public static final double TEST_INITIAL_CAPITAL = 100000.0;

	// 测试股票代码
	public static final String TEST_SYMBOL = "000001";

	// 策略参数配置
	public static Map<String, Object> getRSIStrategyParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("rsiPeriod", 14);
		params.put("overbought", 70.0);
		params.put("oversold", 30.0);
		params.put("positionSizeRatio", 0.02);
		return params;
	}

	public static Map<String, Object> getMACDStrategyParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("fastPeriod", 12);
		params.put("slowPeriod", 26);
		params.put("signalPeriod", 9);
		params.put("useZeroCross", true);
		return params;
	}

	// 性能测试配置
	public static final int PERFORMANCE_TEST_BAR_COUNT = 1000;
	public static final int STRESS_TEST_BAR_COUNT = 10000;
}