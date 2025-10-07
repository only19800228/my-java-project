package com.Quantitative.execution;

import com.Quantitative.core.events.FillEvent;
import com.Quantitative.core.events.OrderEvent;
import com.Quantitative.core.interfaces.TradingComponent;

/**
 * 执行引擎接口 - 统一订单执行接口
 */
public interface ExecutionEngine extends TradingComponent {

	/**
	 * 执行订单
	 */
	FillEvent executeOrder(OrderEvent order);

	/**
	 * 取消订单
	 */
	boolean cancelOrder(String orderId);

	/**
	 * 获取订单状态
	 */
	String getOrderStatus(String orderId);

	/**
	 * 设置执行参数
	 */
	void setExecutionParameter(String key, Object value);

	/**
	 * 获取执行统计
	 */
	java.util.Map<String, Object> getExecutionStatistics();
}