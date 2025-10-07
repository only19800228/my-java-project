package com.Quantitative.core.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 基础事件类 - 所有事件的基类 采用事件驱动架构的核心组件
 */
public abstract class Event {
	private final String eventId;
	private final LocalDateTime timestamp;
	private final String eventType;
	private final String source;
	private final int priority;

	// 事件优先级常量
	public static final int PRIORITY_LOW = 1;
	public static final int PRIORITY_NORMAL = 5;
	public static final int PRIORITY_HIGH = 10;
	public static final int PRIORITY_CRITICAL = 15;

	/**
	 * 基础构造函数
	 */
	public Event(LocalDateTime timestamp, String eventType) {
		this(timestamp, eventType, "System", PRIORITY_NORMAL);
	}

	/**
	 * 完整构造函数
	 */
	public Event(LocalDateTime timestamp, String eventType, String source, int priority) {
		this.eventId = generateEventId();
		this.timestamp = timestamp;
		this.eventType = eventType;
		this.source = source;
		this.priority = validatePriority(priority);
	}

	/**
	 * 生成唯一事件ID
	 */
	private String generateEventId() {
		return "EVENT_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * 验证优先级
	 */
	private int validatePriority(int priority) {
		if (priority < PRIORITY_LOW || priority > PRIORITY_CRITICAL) {
			throw new IllegalArgumentException("事件优先级必须在 " + PRIORITY_LOW + " 和 " + PRIORITY_CRITICAL + " 之间");
		}
		return priority;
	}

	// ==================== Getter方法 ====================

	public String getEventId() {
		return eventId;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public String getEventType() {
		return eventType;
	}

	public String getSource() {
		return source;
	}

	public int getPriority() {
		return priority;
	}

	// ==================== 工具方法 ====================

	/**
	 * 获取事件年龄（毫秒）
	 */
	public long getAgeInMillis() {
		return java.time.Duration.between(timestamp, LocalDateTime.now()).toMillis();
	}

	/**
	 * 检查事件是否过期
	 */
	public boolean isExpired(long maxAgeMillis) {
		return getAgeInMillis() > maxAgeMillis;
	}

	/**
	 * 检查是否是高优先级事件
	 */
	public boolean isHighPriority() {
		return priority >= PRIORITY_HIGH;
	}

	/**
	 * 检查是否是关键事件
	 */
	public boolean isCritical() {
		return priority >= PRIORITY_CRITICAL;
	}

	/**
	 * 获取事件基本信息
	 */
	public EventInfo getEventInfo() {
		return new EventInfo(eventId, timestamp, eventType, source, priority);
	}

	@Override
	public String toString() {
		return String.format("Event{id=%s, time=%s, type=%s, source=%s, priority=%d}", eventId, timestamp, eventType,
				source, priority);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Event event = (Event) obj;
		return eventId.equals(event.eventId);
	}

	@Override
	public int hashCode() {
		return eventId.hashCode();
	}

	/**
	 * 事件信息封装类
	 */
	public static class EventInfo {
		private final String eventId;
		private final LocalDateTime timestamp;
		private final String eventType;
		private final String source;
		private final int priority;

		public EventInfo(String eventId, LocalDateTime timestamp, String eventType, String source, int priority) {
			this.eventId = eventId;
			this.timestamp = timestamp;
			this.eventType = eventType;
			this.source = source;
			this.priority = priority;
		}

		// Getter方法
		public String getEventId() {
			return eventId;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		public String getEventType() {
			return eventType;
		}

		public String getSource() {
			return source;
		}

		public int getPriority() {
			return priority;
		}

		@Override
		public String toString() {
			return String.format("EventInfo{id=%s, type=%s, source=%s}", eventId, eventType, source);
		}
	}
}