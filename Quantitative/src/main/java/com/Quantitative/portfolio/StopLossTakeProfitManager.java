package com.Quantitative.portfolio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.Quantitative.core.events.EnhancedSignalEvent;

/**
 * 止损止盈管理器 - 统一管理所有持仓的风险控制
 */
public class StopLossTakeProfitManager {
	private final Portfolio portfolio;
	private final Map<String, PositionRisk> positionRisks;
	private boolean enabled = true;

	public StopLossTakeProfitManager(Portfolio portfolio) {
		this.portfolio = portfolio;
		this.positionRisks = new ConcurrentHashMap<>();
	}

	/**
	 * 检查并执行止损止盈
	 */
	public List<ExitSignal> checkRiskControls(String symbol, double currentPrice) {
		List<ExitSignal> exitSignals = new ArrayList<>();

		if (!enabled || portfolio == null) {
			return exitSignals;
		}

		Position position = portfolio.getPositions().get(symbol);
		if (position == null || position.getQuantity() == 0) {
			return exitSignals;
		}

		PositionRisk risk = positionRisks.get(symbol);
		if (risk == null) {
			return exitSignals;
		}

		// 检查止损
		if (risk.getStopLossPrice() != null && currentPrice <= risk.getStopLossPrice()) {
			exitSignals.add(new ExitSignal(symbol, "STOP_LOSS", currentPrice,
					"止损触发: " + currentPrice + " <= " + risk.getStopLossPrice()));
		}

		// 检查止盈
		if (risk.getTakeProfitPrice() != null && currentPrice >= risk.getTakeProfitPrice()) {
			exitSignals.add(new ExitSignal(symbol, "TAKE_PROFIT", currentPrice,
					"止盈触发: " + currentPrice + " >= " + risk.getTakeProfitPrice()));
		}

		// 更新移动止损
		updateTrailingStop(symbol, currentPrice, risk);

		return exitSignals;
	}

	/**
	 * 更新移动止损
	 */
	private void updateTrailingStop(String symbol, double currentPrice, PositionRisk risk) {
		if (risk.getTrailingStop() == null || risk.getTrailingStop() <= 0) {
			return;
		}

		// 更新最高价
		if (risk.getHighestPrice() == null || currentPrice > risk.getHighestPrice()) {
			risk.setHighestPrice(currentPrice);

			// 计算新的止损价
			double newStopLoss = currentPrice * (1 - risk.getTrailingStop());
			if (newStopLoss > risk.getStopLossPrice()) {
				risk.setStopLossPrice(newStopLoss);
				System.out.printf("[移动止损] %s 更新止损价: %.2f -> %.2f%n", symbol, risk.getStopLossPrice(), newStopLoss);
			}
		}
	}

	/**
	 * 设置持仓风险管理
	 */
	public void setPositionRisk(String symbol, Double stopLoss, Double takeProfit, Double trailingStop) {
		PositionRisk risk = positionRisks.computeIfAbsent(symbol, k -> new PositionRisk());
		risk.setStopLossPrice(stopLoss);
		risk.setTakeProfitPrice(takeProfit);
		risk.setTrailingStop(trailingStop);
		risk.setHighestPrice(null); // 重置最高价

		System.out.printf("[风险管理] %s 止损=%.2f 止盈=%.2f 移动止损=%.1f%%%n", symbol, stopLoss, takeProfit,
				trailingStop != null ? trailingStop * 100 : 0);
	}

	/**
	 * 从信号事件应用风险管理
	 */
	public void applyRiskFromSignal(EnhancedSignalEvent signal) {
		if (signal.hasRiskManagement()) {
			setPositionRisk(signal.getSymbol(), signal.getStopLossPrice(), signal.getTakeProfitPrice(),
					signal.getTrailingStop());
		}
	}

	/**
	 * 清除持仓风险设置
	 */
	public void clearPositionRisk(String symbol) {
		positionRisks.remove(symbol);
	}

	// Getter/Setter
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * 持仓风险信息
	 */
	public static class PositionRisk {
		private Double stopLossPrice;
		private Double takeProfitPrice;
		private Double trailingStop; // 移动止损比例
		private Double highestPrice; // 用于移动止损的最高价

		// Getter和Setter方法
		public Double getStopLossPrice() {
			return stopLossPrice;
		}

		public void setStopLossPrice(Double stopLossPrice) {
			this.stopLossPrice = stopLossPrice;
		}

		public Double getTakeProfitPrice() {
			return takeProfitPrice;
		}

		public void setTakeProfitPrice(Double takeProfitPrice) {
			this.takeProfitPrice = takeProfitPrice;
		}

		public Double getTrailingStop() {
			return trailingStop;
		}

		public void setTrailingStop(Double trailingStop) {
			this.trailingStop = trailingStop;
		}

		public Double getHighestPrice() {
			return highestPrice;
		}

		public void setHighestPrice(Double highestPrice) {
			this.highestPrice = highestPrice;
		}
	}

	/**
	 * 退出信号
	 */
	public static class ExitSignal {
		private final String symbol;
		private final String reason;
		private final double exitPrice;
		private final String message;

		public ExitSignal(String symbol, String reason, double exitPrice, String message) {
			this.symbol = symbol;
			this.reason = reason;
			this.exitPrice = exitPrice;
			this.message = message;
		}

		// Getter方法
		public String getSymbol() {
			return symbol;
		}

		public String getReason() {
			return reason;
		}

		public double getExitPrice() {
			return exitPrice;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return String.format("ExitSignal{%s %s @%.2f: %s}", symbol, reason, exitPrice, message);
		}
	}
}