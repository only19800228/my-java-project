package com.Quantitative.core.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.Quantitative.core.interfaces.EventProcessor;

/**
 * 事件总线 - 统一的事件分发机制
 */
public class EventBus {
	private final Map<Class<? extends Event>, List<EventProcessor>> processors;
	private boolean debugMode = false;

	public EventBus() {
		this.processors = new ConcurrentHashMap<>();
	}

	/**
	 * 注册事件处理器
	 */
	public void registerProcessor(Class<? extends Event> eventType, EventProcessor processor) {
		processors.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(processor);

		if (debugMode) {
			System.out.printf("[EventBus] 注册处理器: %s -> %s%n", eventType.getSimpleName(), processor.getName());
		}
	}

	/**
	 * 发布事件
	 */
	public void publish(Event event) {
		System.out.printf("🔍 [事件总线] 发布事件: %s%n", event.getClass().getSimpleName());
		Class<? extends Event> eventType = event.getClass();
		List<EventProcessor> eventProcessors = processors.get(eventType);

		if (eventProcessors != null && !eventProcessors.isEmpty()) {
			System.out.printf("🔍 [事件总线] 找到 %d 个处理器%n", processors.size());
			for (EventProcessor processor : eventProcessors) {
				System.out.printf("🔍 [事件总线] 调用处理器: %s%n", processor.getName());
				try {
					if (debugMode) {
						System.out.printf("[EventBus] 处理事件: %s -> %s%n", eventType.getSimpleName(),
								processor.getName());
					}
					processor.processEvent(event);
				} catch (Exception e) {
					System.err.printf("[EventBus] 处理器 %s 处理事件失败: %s%n", processor.getName(), e.getMessage());
					e.printStackTrace();
				}
			}
		} else {
			System.out.printf("❌ [事件总线] 没有找到 %s 的处理器%n", eventType.getSimpleName());
		}
	}

	/**
	 * 批量发布事件
	 */
	public void publishAll(List<Event> events) {
		for (Event event : events) {
			publish(event);
		}
	}

	/**
	 * 获取事件处理统计
	 */
	public Map<String, Object> getStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalEventTypes", processors.size());

		Map<String, Integer> processorCounts = new HashMap<>();
		for (Map.Entry<Class<? extends Event>, List<EventProcessor>> entry : processors.entrySet()) {
			processorCounts.put(entry.getKey().getSimpleName(), entry.getValue().size());
		}
		stats.put("processorsByType", processorCounts);

		return stats;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
}