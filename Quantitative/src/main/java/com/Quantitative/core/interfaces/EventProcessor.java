package com.Quantitative.core.interfaces;

import com.Quantitative.core.events.Event;

/**
 * 事件处理器接口
 */
public interface EventProcessor extends TradingComponent {

	/**
	 * 处理事件
	 */
	void processEvent(Event event);

	/**
	 * 获取支持处理的事件类型
	 */
	Class<? extends Event> getSupportedEventType();
}