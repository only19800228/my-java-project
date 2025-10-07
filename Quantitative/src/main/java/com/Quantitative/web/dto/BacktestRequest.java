package com.Quantitative.web.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class BacktestRequest {
	private String symbol;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private double initialCapital;
	private String strategyType;
	private Map<String, Object> strategyParams;

	// getter/setter
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDateTime startDate) {
		this.startDate = startDate;
	}

	public LocalDateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDateTime endDate) {
		this.endDate = endDate;
	}

	public double getInitialCapital() {
		return initialCapital;
	}

	public void setInitialCapital(double initialCapital) {
		this.initialCapital = initialCapital;
	}

	public String getStrategyType() {
		return strategyType;
	}

	public void setStrategyType(String strategyType) {
		this.strategyType = strategyType;
	}

	public Map<String, Object> getStrategyParams() {
		return strategyParams;
	}

	public void setStrategyParams(Map<String, Object> strategyParams) {
		this.strategyParams = strategyParams;
	}
}