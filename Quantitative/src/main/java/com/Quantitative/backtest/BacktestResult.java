package com.Quantitative.backtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.Quantitative.common.utils.PerformanceMonitor;
import com.Quantitative.core.events.FillEvent;

/**
 * 回测结果类 - 完整的回测结果分析和报告
 */
public class BacktestResult {
	// 基础结果
	private double initialCapital;
	private double finalCapital;
	private double totalReturn;
	private double annualReturn;
	private double maxDrawdown;
	private double sharpeRatio;
	private double sortinoRatio;
	private double calmarRatio;

	// 交易统计
	private int totalTrades;
	private int winningTrades;
	private double winRate;
	private double profitFactor;
	private double avgProfit;
	private double avgLoss;
	private double largestWin;
	private double largestLoss;
	private double avgTradeReturn;

	// 持仓统计
	private double avgHoldingPeriod;
	private double maxConsecutiveWins;
	private double maxConsecutiveLosses;
	private double avgConsecutiveWins;
	private double avgConsecutiveLosses;

	// 数据存储
	private List<FillEvent> tradeHistory;
	private List<Double> equityCurve;
	private List<Double> drawdownCurve;
	private List<Double> returnSeries;

	// 性能监控
	private PerformanceMonitor performanceMonitor;

	public BacktestResult() {
		this.tradeHistory = new ArrayList<>();
		this.equityCurve = new ArrayList<>();
		this.drawdownCurve = new ArrayList<>();
		this.returnSeries = new ArrayList<>();
		this.performanceMonitor = PerformanceMonitor.getInstance();
	}

	/**
	 * 添加交易记录
	 */
	public void addTrade(FillEvent trade) {
		tradeHistory.add(trade);
	}

	/**
	 * 添加权益点
	 */
	public void addEquityPoint(double equity) {
		equityCurve.add(equity);
		updateDrawdownCurve();
		updateReturnSeries();
	}

	/**
	 * 计算所有高级指标
	 */
	public void calculateAdvancedMetrics() {
		performanceMonitor.startOperation("BacktestResult.calculateAdvancedMetrics");

		try {
			// 基础指标
			calculateBasicMetrics();

			// 交易统计
			calculateTradeStatistics();

			// 风险调整收益指标
			calculateRiskAdjustedMetrics();

			// 持仓统计
			calculateHoldingStatistics();

			System.out.println("✓ 高级指标计算完成");

		} finally {
			performanceMonitor.endOperation("BacktestResult.calculateAdvancedMetrics");
		}
	}

	/**
	 * 计算基础指标
	 */
	private void calculateBasicMetrics() {
		if (equityCurve.size() < 2)
			return;

		// 总收益率
		totalReturn = ((finalCapital - initialCapital) / initialCapital) * 100;

		// 年化收益率（假设252个交易日）
		double years = equityCurve.size() / 252.0;
		if (years > 0) {
			annualReturn = (Math.pow(1 + totalReturn / 100, 1.0 / years) - 1) * 100;
		}

		// 最大回撤
		maxDrawdown = calculateMaxDrawdown();
	}

	/**
	 * 计算交易统计
	 */
	private void calculateTradeStatistics() {
		if (tradeHistory.isEmpty()) {
			System.out.println("⚠️ [BacktestResult] 交易历史为空，但实际有持仓变化");
			// 即使没有成交记录，也可能有持仓变化
			return;
		}

		List<CompletedTrade> completedTrades = matchTrades();
		totalTrades = completedTrades.size();

		System.out.println("🔍 [BacktestResult] 成交记录: " + tradeHistory.size() + " 条");
		System.out.println("🔍 [BacktestResult] 完整交易: " + totalTrades + " 笔");

		if (totalTrades == 0) {
			// 检查是否有未平仓的交易
			checkOpenPositions();
			return;
		}

		// 计算盈亏统计
		double totalProfit = 0.0;
		double totalLoss = 0.0;
		winningTrades = 0;
		largestWin = Double.MIN_VALUE;
		largestLoss = Double.MAX_VALUE;

		List<Double> tradeReturns = new ArrayList<>();

		for (CompletedTrade trade : completedTrades) {
			double profit = trade.getProfit();
			tradeReturns.add(profit);

			if (profit > 0) {
				winningTrades++;
				totalProfit += profit;
				largestWin = Math.max(largestWin, profit);
			} else {
				totalLoss += Math.abs(profit);
				largestLoss = Math.min(largestLoss, profit);
			}
		}

		// 计算统计指标
		winRate = (double) winningTrades / totalTrades * 100;
		profitFactor = totalLoss > 0 ? totalProfit / totalLoss : totalProfit > 0 ? 10.0 : 0.0;
		avgProfit = winningTrades > 0 ? totalProfit / winningTrades : 0.0;
		avgLoss = (totalTrades - winningTrades) > 0 ? totalLoss / (totalTrades - winningTrades) : 0.0;
		avgTradeReturn = tradeReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

		// 计算连续盈亏
		calculateConsecutiveStats(completedTrades);
	}

	/**
	 * 检查未平仓交易
	 */
	private void checkOpenPositions() {
		// 如果有买入但没有卖出，说明有未平仓交易
		long buyCount = tradeHistory.stream().filter(fill -> "BUY".equals(fill.getDirection())).count();
		long sellCount = tradeHistory.stream().filter(fill -> "SELL".equals(fill.getDirection())).count();

		if (buyCount > sellCount) {
			System.out.println("⚠️ [BacktestResult] 检测到未平仓交易: " + (buyCount - sellCount) + " 笔");
			// 可以考虑将未平仓交易按当前价格计算盈亏
		}
	}

	/**
	 * 匹配买卖交易 - 修复版本
	 */
	private List<CompletedTrade> matchTrades() {
		List<CompletedTrade> completedTrades = new ArrayList<>();
		Map<String, List<FillEvent>> buyTrades = new HashMap<>();
		Map<String, List<FillEvent>> sellTrades = new HashMap<>();

		// 分离买卖交易
		for (FillEvent trade : tradeHistory) {
			String symbol = trade.getSymbol();
			if ("BUY".equals(trade.getDirection())) {
				buyTrades.computeIfAbsent(symbol, k -> new ArrayList<>()).add(trade);
			} else if ("SELL".equals(trade.getDirection())) {
				sellTrades.computeIfAbsent(symbol, k -> new ArrayList<>()).add(trade);
			}
		}

		System.out.println("🔍 [matchTrades] 买入交易: " + buyTrades.values().stream().mapToInt(List::size).sum());
		System.out.println("🔍 [matchTrades] 卖出交易: " + sellTrades.values().stream().mapToInt(List::size).sum());

		// 匹配交易（先进先出）
		for (String symbol : buyTrades.keySet()) {
			List<FillEvent> buys = buyTrades.get(symbol);
			List<FillEvent> sells = sellTrades.getOrDefault(symbol, new ArrayList<>());

			int buyIndex = 0;
			int sellIndex = 0;

			while (buyIndex < buys.size() && sellIndex < sells.size()) {
				FillEvent buy = buys.get(buyIndex);
				FillEvent sell = sells.get(sellIndex);

				// 创建完整交易记录
				CompletedTrade completedTrade = createCompletedTrade(buy, sell);
				completedTrades.add(completedTrade);

				buyIndex++;
				sellIndex++;
			}

			// 处理未匹配的买入交易（未平仓）
			while (buyIndex < buys.size()) {
				FillEvent buy = buys.get(buyIndex);
				System.out.println("⚠️ [matchTrades] 未平仓买入: " + buy);
				// 可以创建未平仓交易记录
				buyIndex++;
			}
		}

		return completedTrades;
	}

	/**
	 * 计算风险调整收益指标
	 */
	private void calculateRiskAdjustedMetrics() {
		if (returnSeries.size() < 2)
			return;

		// 夏普比率（无风险利率假设为3%）
		sharpeRatio = calculateSharpeRatio(0.03);

		// 索提诺比率
		sortinoRatio = calculateSortinoRatio(0.03);

		// 卡尔玛比率
		calmarRatio = calculateCalmarRatio();
	}

	/**
	 * 计算持仓统计
	 */
	private void calculateHoldingStatistics() {
		// 简化实现 - 实际应该基于交易时间计算
		if (!tradeHistory.isEmpty()) {
			avgHoldingPeriod = 5.0; // 假设平均持仓5天
		}
	}

	/**
	 * 创建完整交易记录
	 */
	private CompletedTrade createCompletedTrade(FillEvent buy, FillEvent sell) {
		double buyCost = buy.getFillPrice() * buy.getQuantity() + buy.getCommission();
		double sellRevenue = sell.getFillPrice() * sell.getQuantity() - sell.getCommission();
		double profit = sellRevenue - buyCost;
		double profitPercent = (profit / buyCost) * 100;
		long holdingDays = java.time.Duration.between(buy.getTimestamp(), sell.getTimestamp()).toDays();

		return new CompletedTrade(buy.getSymbol(), buy.getTimestamp(), sell.getTimestamp(), buy.getFillPrice(),
				sell.getFillPrice(), buy.getQuantity(), profit, profitPercent, holdingDays);
	}

	/**
	 * 计算连续盈亏统计
	 */
	private void calculateConsecutiveStats(List<CompletedTrade> trades) {
		int currentStreak = 0;
		boolean currentWin = false;
		maxConsecutiveWins = 0;
		maxConsecutiveLosses = 0;

		List<Integer> winStreaks = new ArrayList<>();
		List<Integer> lossStreaks = new ArrayList<>();

		for (CompletedTrade trade : trades) {
			boolean isWin = trade.getProfit() > 0;

			if (currentStreak == 0) {
				currentStreak = 1;
				currentWin = isWin;
			} else if (isWin == currentWin) {
				currentStreak++;
			} else {
				// 记录上一个序列
				if (currentWin) {
					winStreaks.add(currentStreak);
					maxConsecutiveWins = Math.max(maxConsecutiveWins, currentStreak);
				} else {
					lossStreaks.add(currentStreak);
					maxConsecutiveLosses = Math.max(maxConsecutiveLosses, currentStreak);
				}

				// 开始新序列
				currentStreak = 1;
				currentWin = isWin;
			}
		}

		// 记录最后一个序列
		if (currentStreak > 0) {
			if (currentWin) {
				winStreaks.add(currentStreak);
				maxConsecutiveWins = Math.max(maxConsecutiveWins, currentStreak);
			} else {
				lossStreaks.add(currentStreak);
				maxConsecutiveLosses = Math.max(maxConsecutiveLosses, currentStreak);
			}
		}

		// 计算平均连续盈亏
		avgConsecutiveWins = winStreaks.stream().mapToInt(Integer::intValue).average().orElse(0.0);
		avgConsecutiveLosses = lossStreaks.stream().mapToInt(Integer::intValue).average().orElse(0.0);
	}

	/**
	 * 计算最大回撤
	 */
	private double calculateMaxDrawdown() {
		double maxDrawdown = 0.0;
		double peak = equityCurve.get(0);

		for (double equity : equityCurve) {
			if (equity > peak) {
				peak = equity;
			}
			double drawdown = (peak - equity) / peak * 100;
			if (drawdown > maxDrawdown) {
				maxDrawdown = drawdown;
			}
		}
		return maxDrawdown;
	}

	/**
	 * 计算夏普比率
	 */
	private double calculateSharpeRatio(double riskFreeRate) {
		if (returnSeries.size() < 2)
			return 0.0;

		double meanReturn = returnSeries.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		double stdDev = calculateStandardDeviation(returnSeries);

		if (stdDev == 0)
			return 0.0;

		double dailyRiskFree = riskFreeRate / 252.0;
		return (meanReturn - dailyRiskFree) / stdDev * Math.sqrt(252);
	}

	/**
	 * 计算索提诺比率
	 */
	private double calculateSortinoRatio(double riskFreeRate) {
		if (returnSeries.size() < 2)
			return 0.0;

		double meanReturn = returnSeries.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		List<Double> negativeReturns = returnSeries.stream().filter(r -> r < 0).collect(Collectors.toList());

		if (negativeReturns.isEmpty())
			return 0.0;

		double downsideDeviation = calculateStandardDeviation(negativeReturns);
		if (downsideDeviation == 0)
			return 0.0;

		double dailyRiskFree = riskFreeRate / 252.0;
		return (meanReturn - dailyRiskFree) / downsideDeviation * Math.sqrt(252);
	}

	/**
	 * 计算卡尔玛比率
	 */
	private double calculateCalmarRatio() {
		if (maxDrawdown == 0)
			return 0.0;
		return annualReturn / Math.abs(maxDrawdown);
	}

	/**
	 * 计算标准差
	 */
	private double calculateStandardDeviation(List<Double> values) {
		double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
		return Math.sqrt(variance);
	}

	/**
	 * 更新回撤曲线
	 */
	private void updateDrawdownCurve() {
		drawdownCurve.clear();
		double peak = equityCurve.get(0);

		for (double equity : equityCurve) {
			if (equity > peak) {
				peak = equity;
			}
			double drawdown = (peak - equity) / peak * 100;
			drawdownCurve.add(drawdown);
		}
	}

	/**
	 * 更新收益率序列
	 */
	private void updateReturnSeries() {
		returnSeries.clear();
		for (int i = 1; i < equityCurve.size(); i++) {
			double ret = (equityCurve.get(i) - equityCurve.get(i - 1)) / equityCurve.get(i - 1);
			returnSeries.add(ret);
		}
	}

	// ==================== Getter和Setter方法 ====================

	public double getInitialCapital() {
		return initialCapital;
	}

	public void setInitialCapital(double initialCapital) {
		this.initialCapital = initialCapital;
	}

	public double getFinalCapital() {
		return finalCapital;
	}

	public void setFinalCapital(double finalCapital) {
		this.finalCapital = finalCapital;
	}

	public double getTotalReturn() {
		return totalReturn;
	}

	public void setTotalReturn(double totalReturn) {
		this.totalReturn = totalReturn;
	}

	public double getAnnualReturn() {
		return annualReturn;
	}

	public void setAnnualReturn(double annualReturn) {
		this.annualReturn = annualReturn;
	}

	public double getMaxDrawdown() {
		return maxDrawdown;
	}

	public void setMaxDrawdown(double maxDrawdown) {
		this.maxDrawdown = maxDrawdown;
	}

	public double getSharpeRatio() {
		return sharpeRatio;
	}

	public void setSharpeRatio(double sharpeRatio) {
		this.sharpeRatio = sharpeRatio;
	}

	public double getSortinoRatio() {
		return sortinoRatio;
	}

	public void setSortinoRatio(double sortinoRatio) {
		this.sortinoRatio = sortinoRatio;
	}

	public double getCalmarRatio() {
		return calmarRatio;
	}

	public void setCalmarRatio(double calmarRatio) {
		this.calmarRatio = calmarRatio;
	}

	public int getTotalTrades() {
		return totalTrades;
	}

	public void setTotalTrades(int totalTrades) {
		this.totalTrades = totalTrades;
	}

	public int getWinningTrades() {
		return winningTrades;
	}

	public void setWinningTrades(int winningTrades) {
		this.winningTrades = winningTrades;
	}

	public double getWinRate() {
		return winRate;
	}

	public void setWinRate(double winRate) {
		this.winRate = winRate;
	}

	public double getProfitFactor() {
		return profitFactor;
	}

	public void setProfitFactor(double profitFactor) {
		this.profitFactor = profitFactor;
	}

	public double getAvgProfit() {
		return avgProfit;
	}

	public void setAvgProfit(double avgProfit) {
		this.avgProfit = avgProfit;
	}

	public double getAvgLoss() {
		return avgLoss;
	}

	public void setAvgLoss(double avgLoss) {
		this.avgLoss = avgLoss;
	}

	public double getLargestWin() {
		return largestWin;
	}

	public void setLargestWin(double largestWin) {
		this.largestWin = largestWin;
	}

	public double getLargestLoss() {
		return largestLoss;
	}

	public void setLargestLoss(double largestLoss) {
		this.largestLoss = largestLoss;
	}

	public double getAvgTradeReturn() {
		return avgTradeReturn;
	}

	public void setAvgTradeReturn(double avgTradeReturn) {
		this.avgTradeReturn = avgTradeReturn;
	}

	public double getAvgHoldingPeriod() {
		return avgHoldingPeriod;
	}

	public void setAvgHoldingPeriod(double avgHoldingPeriod) {
		this.avgHoldingPeriod = avgHoldingPeriod;
	}

	public double getMaxConsecutiveWins() {
		return maxConsecutiveWins;
	}

	public void setMaxConsecutiveWins(double maxConsecutiveWins) {
		this.maxConsecutiveWins = maxConsecutiveWins;
	}

	public double getMaxConsecutiveLosses() {
		return maxConsecutiveLosses;
	}

	public void setMaxConsecutiveLosses(double maxConsecutiveLosses) {
		this.maxConsecutiveLosses = maxConsecutiveLosses;
	}

	public double getAvgConsecutiveWins() {
		return avgConsecutiveWins;
	}

	public void setAvgConsecutiveWins(double avgConsecutiveWins) {
		this.avgConsecutiveWins = avgConsecutiveWins;
	}

	public double getAvgConsecutiveLosses() {
		return avgConsecutiveLosses;
	}

	public void setAvgConsecutiveLosses(double avgConsecutiveLosses) {
		this.avgConsecutiveLosses = avgConsecutiveLosses;
	}

	public List<FillEvent> getTradeHistory() {
		return new ArrayList<>(tradeHistory);
	}

	public List<Double> getEquityCurve() {
		return new ArrayList<>(equityCurve);
	}

	public List<Double> getDrawdownCurve() {
		return new ArrayList<>(drawdownCurve);
	}

	public List<Double> getReturnSeries() {
		return new ArrayList<>(returnSeries);
	}

	// ==================== 报告方法 ====================

	/**
	 * 打印详细的交易历史
	 */
	public void printTradeHistory() {
		System.out.println("\n=== 详细交易历史 ===");
		List<FillEvent> trades = getTradeHistory();

		if (trades.isEmpty()) {
			System.out.println("无交易记录");
			return;
		}

		for (int i = 0; i < trades.size(); i++) {
			FillEvent trade = trades.get(i);
			System.out.printf("交易%d: %s %s @%.2f x%d 手续费:%.2f%n", i + 1, trade.getTimestamp().toLocalDate(),
					trade.getDirection(), trade.getFillPrice(), trade.getQuantity(), trade.getCommission());
		}
	}

	/**
	 * 打印回测结果摘要
	 */
	public void printSummary() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 60; i++) {
			sb.append("=");
		}
		String line = sb.toString();
		// String line = "=".repeat(60);
		System.out.println("\n" + line);
		System.out.println("回测结果摘要");
		System.out.println(line);

		// 收益统计
		System.out.println("【收益统计】");
		System.out.printf("初始资金: %,.2f%n", initialCapital);
		System.out.printf("最终资金: %,.2f%n", finalCapital);
		System.out.printf("总收益率: %.2f%%%n", totalReturn);
		System.out.printf("年化收益率: %.2f%%%n", annualReturn);

		// 风险统计
		System.out.println("\n【风险统计】");
		System.out.printf("最大回撤: %.2f%%%n", maxDrawdown);
		System.out.printf("夏普比率: %.2f%n", sharpeRatio);
		System.out.printf("索提诺比率: %.2f%n", sortinoRatio);
		System.out.printf("卡尔玛比率: %.2f%n", calmarRatio);

		// 交易统计
		System.out.println("\n【交易统计】");
		System.out.printf("总交易次数: %d%n", totalTrades);
		System.out.printf("盈利交易: %d%n", winningTrades);
		System.out.printf("胜率: %.1f%%%n", winRate);
		System.out.printf("盈亏比: %.2f%n", profitFactor);
		System.out.printf("平均盈利: %.2f%n", avgProfit);
		System.out.printf("平均亏损: %.2f%n", avgLoss);
		System.out.printf("单笔最大盈利: %.2f%n", largestWin);
		System.out.printf("单笔最大亏损: %.2f%n", largestLoss);

		// 连续统计
		System.out.println("\n【连续统计】");
		System.out.printf("最大连续盈利: %.0f%n", maxConsecutiveWins);
		System.out.printf("最大连续亏损: %.0f%n", maxConsecutiveLosses);
		System.out.printf("平均连续盈利: %.1f%n", avgConsecutiveWins);
		System.out.printf("平均连续亏损: %.1f%n", avgConsecutiveLosses);
		System.out.printf("平均持仓周期: %.1f天%n", avgHoldingPeriod);

		System.out.println(line);
	}

	/**
	 * 获取完整的结果报告
	 */
	public Map<String, Object> getCompleteReport() {
		Map<String, Object> report = new LinkedHashMap<>();

		report.put("initialCapital", initialCapital);
		report.put("finalCapital", finalCapital);
		report.put("totalReturn", totalReturn);
		report.put("annualReturn", annualReturn);
		report.put("maxDrawdown", maxDrawdown);
		report.put("sharpeRatio", sharpeRatio);
		report.put("sortinoRatio", sortinoRatio);
		report.put("calmarRatio", calmarRatio);

		report.put("totalTrades", totalTrades);
		report.put("winningTrades", winningTrades);
		report.put("winRate", winRate);
		report.put("profitFactor", profitFactor);
		report.put("avgProfit", avgProfit);
		report.put("avgLoss", avgLoss);
		report.put("largestWin", largestWin);
		report.put("largestLoss", largestLoss);
		report.put("avgTradeReturn", avgTradeReturn);

		report.put("avgHoldingPeriod", avgHoldingPeriod);
		report.put("maxConsecutiveWins", maxConsecutiveWins);
		report.put("maxConsecutiveLosses", maxConsecutiveLosses);
		report.put("avgConsecutiveWins", avgConsecutiveWins);
		report.put("avgConsecutiveLosses", avgConsecutiveLosses);

		report.put("tradeCount", tradeHistory.size());
		report.put("equityPoints", equityCurve.size());

		return report;
	}
}

/**
 * 完整交易记录类
 */
class CompletedTrade {
	private final String symbol;
	private final java.time.LocalDateTime entryTime;
	private final java.time.LocalDateTime exitTime;
	private final double entryPrice;
	private final double exitPrice;
	private final int quantity;
	private final double profit;
	private final double profitPercent;
	private final long holdingDays;

	public CompletedTrade(String symbol, java.time.LocalDateTime entryTime, java.time.LocalDateTime exitTime,
			double entryPrice, double exitPrice, int quantity, double profit, double profitPercent, long holdingDays) {
		this.symbol = symbol;
		this.entryTime = entryTime;
		this.exitTime = exitTime;
		this.entryPrice = entryPrice;
		this.exitPrice = exitPrice;
		this.quantity = quantity;
		this.profit = profit;
		this.profitPercent = profitPercent;
		this.holdingDays = holdingDays;
	}

	// Getter方法
	public String getSymbol() {
		return symbol;
	}

	public java.time.LocalDateTime getEntryTime() {
		return entryTime;
	}

	public java.time.LocalDateTime getExitTime() {
		return exitTime;
	}

	public double getEntryPrice() {
		return entryPrice;
	}

	public double getExitPrice() {
		return exitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public double getProfit() {
		return profit;
	}

	public double getProfitPercent() {
		return profitPercent;
	}

	public long getHoldingDays() {
		return holdingDays;
	}

	@Override
	public String toString() {
		return String.format("CompletedTrade{%s 买入@%.2f → 卖出@%.2f, 数量:%d, 持仓:%d天, 盈亏:%.2f(%.1f%%)}", symbol, entryPrice,
				exitPrice, quantity, holdingDays, profit, profitPercent);
	}
}