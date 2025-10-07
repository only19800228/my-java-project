package com.Quantitative.data.adaptive;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.data.DataInfo;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * 自适应数据源的 DataFeed 适配器 - 完整实现
 */
public class AdaptiveDataFeed implements DataFeed {

	private final AdaptiveDataSource adaptiveDataSource;
	private List<BarEvent> currentBars;
	private int currentIndex = 0;
	private String status = "CREATED";
	private Map<String, Object> parameters;

	public AdaptiveDataFeed() {
		this.adaptiveDataSource = new AdaptiveDataSource();
		this.currentBars = new ArrayList<>();
		this.parameters = new java.util.HashMap<>();
	}

	public AdaptiveDataFeed(String csvDataDirectory) {
		this.adaptiveDataSource = new AdaptiveDataSource(csvDataDirectory);
		this.currentBars = new ArrayList<>();
		this.parameters = new java.util.HashMap<>();
	}

	@Override
	public void initialize() {
		adaptiveDataSource.initialize();
		this.status = "INITIALIZED";
		System.out.println("? 自适应数据源已初始化");
	}

	@Override
	public void configure(Map<String, Object> config) {
		adaptiveDataSource.configure(config);
	}

	@Override
	public List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end) {
		System.out.printf("? 加载历史数据: %s (%s 到 %s)%n", symbol, start, end);

		List<BarEvent> bars = adaptiveDataSource.loadHistoricalData(symbol, start, end);
		this.currentBars = new ArrayList<>(bars); // 创建副本避免修改
		this.currentIndex = 0;

		System.out.printf("? 数据加载完成: %d 条记录，当前索引重置为 0%n", currentBars.size());
		return bars;
	}

	@Override
	public BarEvent getNextBar() {
		if (currentIndex < currentBars.size()) {
			BarEvent bar = currentBars.get(currentIndex);
			currentIndex++;

			// 调试信息（可选）
			if (currentIndex % 100 == 0 || currentIndex <= 5) {
				System.out.printf("? 获取第 %d 个Bar: %s %s%n", currentIndex, bar.getSymbol(), bar.getTimestamp());
			}

			return bar;
		}
		return null;
	}

	@Override
	public boolean hasNextBar() {
		boolean hasNext = currentIndex < currentBars.size();

		// 调试信息（可选）
		if (!hasNext) {
			System.out.println("? 数据流结束，没有更多Bar数据");
		}

		return hasNext;
	}

	@Override
	public void reset() {
		System.out.printf("? 重置数据源指针: %d -> 0 (总数据量: %d)%n", currentIndex, currentBars.size());
		currentIndex = 0;
		this.status = "RESET";
	}

	@Override
	public List<BarEvent> getAllBars() {
		return new ArrayList<>(currentBars);
	}

	@Override
	public DataInfo getDataInfo() {
		return adaptiveDataSource.getDataInfo();
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
		return adaptiveDataSource.getAvailableSymbols();
	}

	@Override
	public boolean isConnected() {
		return adaptiveDataSource.isConnected();
	}

	@Override
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		return adaptiveDataSource.getDataQualityReport(symbol, start, end);
	}

	@Override
	public String getName() {
		return "AdaptiveDataFeed";
	}

	@Override
	public String getStatus() {
		return status + " [索引: " + currentIndex + "/" + currentBars.size() + "]";
	}

	@Override
	public void shutdown() {
		adaptiveDataSource.shutdown();
		currentBars.clear();
		currentIndex = 0;
		this.status = "SHUTDOWN";
		System.out.println("? 自适应数据源已关闭");
	}

	// 代理 AdaptiveDataSource 的方法
	public void setDataSourcePriority(List<String> priority) {
		adaptiveDataSource.setDataSourcePriority(priority);
	}

	public void syncToLocal(String symbol, LocalDateTime start, LocalDateTime end) {
		adaptiveDataSource.syncToLocal(symbol, start, end);
	}

	public void batchSyncToLocal(List<String> symbols, LocalDateTime start, LocalDateTime end) {
		adaptiveDataSource.batchSyncToLocal(symbols, start, end);
	}

	// 获取当前状态信息
	public String getCurrentStatus() {
		return String.format("数据量: %d, 当前索引: %d, 剩余: %d", currentBars.size(), currentIndex,
				currentBars.size() - currentIndex);
	}
}