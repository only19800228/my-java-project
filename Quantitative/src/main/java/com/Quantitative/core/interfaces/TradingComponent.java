package com.Quantitative.core.interfaces;

import java.util.Map;

/**
 * 交易系统组件统一接口
 */
public interface TradingComponent {

	/**
	 * 初始化组件
	 */
	void initialize();

	/**
	 * 配置组件参数
	 */
	void configure(Map<String, Object> config);

	/**
	 * 获取组件名称
	 */
	String getName();

	/**
	 * 获取组件状态
	 */
	String getStatus();

	/**
	 * 重置组件状态
	 */
	void reset();

	/**
	 * 关闭组件
	 */
	void shutdown();
}