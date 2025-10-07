package com.Quantitative.data;

import java.time.LocalDateTime;

/**
 * 数据信息类 - 封装数据源的基本信息
 */
public class DataInfo {
	private final String symbol;
	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	private final int barCount;
	private final String timeframe;
	private final String dataSource;
	private final String dataQuality;

	public DataInfo(String symbol, LocalDateTime startTime, LocalDateTime endTime, int barCount, String timeframe) {
		this(symbol, startTime, endTime, barCount, timeframe, "AKShare", "UNKNOWN");
	}

	public DataInfo(String symbol, LocalDateTime startTime, LocalDateTime endTime, int barCount, String timeframe,
			String dataSource, String dataQuality) {
		this.symbol = symbol;
		this.startTime = startTime;
		this.endTime = endTime;
		this.barCount = barCount;
		this.timeframe = timeframe;
		this.dataSource = dataSource;
		this.dataQuality = dataQuality;
	}

	// ==================== Getter方法 ====================

	public String getSymbol() {
		return symbol;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public int getBarCount() {
		return barCount;
	}

	public String getTimeframe() {
		return timeframe;
	}

	public String getDataSource() {
		return dataSource;
	}

	public String getDataQuality() {
		return dataQuality;
	}

	// ==================== 业务方法 ====================

	/**
	 * 获取数据周期（天数）
	 */
	public long getDataPeriodInDays() {
		if (startTime == null || endTime == null) {
			return 0;
		}
		return java.time.temporal.ChronoUnit.DAYS.between(startTime, endTime);
	}

	/**
	 * 检查数据是否为空
	 */
	public boolean isEmpty() {
		return barCount == 0;
	}

	/**
	 * 检查数据是否完整
	 */
	public boolean isComplete() {
		return "COMPLETE".equals(dataQuality);
	}

	/**
	 * 获取数据密度（每天的平均Bar数）
	 */
	public double getDataDensity() {
		long days = getDataPeriodInDays();
		return days > 0 ? (double) barCount / days : 0.0;
	}

	/**
	 * 获取数据信息摘要
	 */
	public String getSummary() {
		return String.format("%s: %d bars, %s to %s, %s", symbol, barCount,
				startTime != null ? startTime.toLocalDate() : "N/A", endTime != null ? endTime.toLocalDate() : "N/A",
				timeframe);
	}

	@Override
	public String toString() {
		return String.format(
				"DataInfo{symbol='%s', period=%s to %s, bars=%d, timeframe='%s', source='%s', quality='%s'}", symbol,
				startTime != null ? startTime.toLocalDate() : "N/A", endTime != null ? endTime.toLocalDate() : "N/A",
				barCount, timeframe, dataSource, dataQuality);
	}

	/**
	 * 转换为Map格式
	 */
	public java.util.Map<String, Object> toMap() {
		java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
		map.put("symbol", symbol);
		map.put("startTime", startTime);
		map.put("endTime", endTime);
		map.put("barCount", barCount);
		map.put("timeframe", timeframe);
		map.put("dataSource", dataSource);
		map.put("dataQuality", dataQuality);
		map.put("periodInDays", getDataPeriodInDays());
		map.put("dataDensity", getDataDensity());
		return map;
	}

	/**
	 * 构建器模式
	 */
	public static class Builder {
		private String symbol;
		private LocalDateTime startTime;
		private LocalDateTime endTime;
		private int barCount;
		private String timeframe = "1d";
		private String dataSource = "AKShare";
		private String dataQuality = "UNKNOWN";

		public Builder(String symbol) {
			this.symbol = symbol;
		}

		public Builder startTime(LocalDateTime startTime) {
			this.startTime = startTime;
			return this;
		}

		public Builder endTime(LocalDateTime endTime) {
			this.endTime = endTime;
			return this;
		}

		public Builder barCount(int barCount) {
			this.barCount = barCount;
			return this;
		}

		public Builder timeframe(String timeframe) {
			this.timeframe = timeframe;
			return this;
		}

		public Builder dataSource(String dataSource) {
			this.dataSource = dataSource;
			return this;
		}

		public Builder dataQuality(String dataQuality) {
			this.dataQuality = dataQuality;
			return this;
		}

		public DataInfo build() {
			return new DataInfo(symbol, startTime, endTime, barCount, timeframe, dataSource, dataQuality);
		}
	}
}