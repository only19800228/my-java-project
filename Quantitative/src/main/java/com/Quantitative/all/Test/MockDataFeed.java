package com.Quantitative.all.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.data.DataInfo;

/**
 * 用于测试的模拟数据源
 */
public class MockDataFeed implements DataFeed {

	private List<BarEvent> bars;
	private int currentIndex;
	private String status;
	private Map<String, Object> parameters;

	public MockDataFeed() {
		this.bars = new ArrayList<>();
		this.currentIndex = 0;
		this.status = "CREATED";
		this.parameters = new HashMap<>();
	}

	public MockDataFeed(List<BarEvent> bars) {
		this();
		this.bars = new ArrayList<>(bars);
	}

	public void setTestData(List<BarEvent> testBars) {
		this.bars = new ArrayList<>(testBars);
		this.currentIndex = 0;
	}

	@Override
	public void initialize() {
		System.out.println("初始化模拟数据源，数据条数: " + bars.size());
		this.status = "INITIALIZED";
	}

	@Override
	public void configure(Map<String, Object> config) {
		if (config != null) {
			parameters.putAll(config);
		}
	}

	@Override
	public String getName() {
		return "MockDataFeed";
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		this.currentIndex = 0;
		this.status = "RESET";
	}

	@Override
	public void shutdown() {
		this.status = "SHUTDOWN";
	}

	@Override
	public List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end) {
		System.out.printf("加载历史数据: %s %s 到 %s%n", symbol, start, end);
		return new ArrayList<>(bars);
	}

	@Override
	public BarEvent getNextBar() {
		if (currentIndex < bars.size()) {
			return bars.get(currentIndex++);
		}
		return null;
	}

	@Override
	public boolean hasNextBar() {
		return currentIndex < bars.size();
	}

	@Override
	public List<BarEvent> getAllBars() {
		return new ArrayList<>(bars);
	}

	@Override
	public DataInfo getDataInfo() {
		if (bars.isEmpty()) {
			return new DataInfo("TEST", null, null, 0, "daily");
		}

		LocalDateTime start = bars.get(0).getTimestamp();
		LocalDateTime end = bars.get(bars.size() - 1).getTimestamp();
		return new DataInfo("TEST", start, end, bars.size(), "daily");
	}

	@Override
	public void setParameter(String key, Object value) {
		parameters.put(key, value);
	}

	@Override
	public Object getParameter(String key) {
		return parameters.get(key);
	}

	@Override
	public List<String> getAvailableSymbols() {
		List<String> symbols = new ArrayList<>();
		symbols.add("000001");
		symbols.add("000002");
		symbols.add("600519");
		return symbols;
	}

	@Override
	public boolean isConnected() {
		return true;
	}
}