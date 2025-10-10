package com.Quantitative.data.model;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.data.DataInfo;

/**
 * 股票数据集
 */
public class StockData {
	private final String symbol;
	private final List<StockBar> bars;
	private final DataInfo dataInfo;

	public StockData(String symbol, List<StockBar> bars, DataInfo dataInfo) {
		this.symbol = symbol;
		this.bars = bars != null ? new ArrayList<>(bars) : new ArrayList<>();
		this.dataInfo = dataInfo;
	}

	public String getSymbol() {
		return symbol;
	}

	public List<StockBar> getBars() {
		return new ArrayList<>(bars);
	}

	public DataInfo getDataInfo() {
		return dataInfo;
	}

	public int size() {
		return bars.size();
	}

	public boolean isEmpty() {
		return bars.isEmpty();
	}

	public StockBar getBar(int index) {
		return bars.get(index);
	}

	public void addBar(StockBar bar) {
		if (bar != null && symbol.equals(bar.getSymbol())) {
			bars.add(bar);
		}
	}
}