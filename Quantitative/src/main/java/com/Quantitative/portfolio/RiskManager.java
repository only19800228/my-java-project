package com.Quantitative.portfolio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.Quantitative.common.utils.PerformanceMonitor;
import com.Quantitative.core.events.OrderEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.core.interfaces.TradingComponent;

/**
 * 统一风险管理器
 */
public class RiskManager implements TradingComponent {
	private final String name = "RiskManager";
	private Portfolio portfolio;

	// 风险限制
	private double maxPositionRatio = 0.1; // 单品种最大仓位10%
	private double maxTotalRisk = 0.3; // 总风险暴露30%
	private double maxDrawdownLimit = 0.2; // 最大回撤限制20%
	private double dailyLossLimit = 0.05; // 单日亏损限制5%
	private int maxConsecutiveLosses = 5; // 最大连续亏损次数

	// 状态跟踪
	private Map<String, Integer> consecutiveLosses = new ConcurrentHashMap<>();
	private Map<String, Double> dailyPnL = new ConcurrentHashMap<>();
	private double initialCapital;
	private double peakCapital;
	private String status = "CREATED";

	// 性能监控
	private PerformanceMonitor performanceMonitor;

	public RiskManager() {
		this.performanceMonitor = PerformanceMonitor.getInstance();
	}

	public RiskManager(double maxPositionRatio, double maxDrawdownLimit, double dailyLossLimit) {
		this();
		this.maxPositionRatio = maxPositionRatio;
		this.maxDrawdownLimit = maxDrawdownLimit;
		this.dailyLossLimit = dailyLossLimit;
	}

	@Override
	public void initialize() {
		System.out.println("初始化风险管理器");
		if (portfolio != null) {
			this.initialCapital = portfolio.getInitialCash();
			this.peakCapital = initialCapital;
		}
		this.status = "INITIALIZED";
	}

	@Override
	public void configure(Map<String, Object> config) {
		if (config != null) {
			if (config.containsKey("maxPositionRatio")) {
				this.maxPositionRatio = (Double) config.get("maxPositionRatio");
			}
			if (config.containsKey("maxDrawdownLimit")) {
				this.maxDrawdownLimit = (Double) config.get("maxDrawdownLimit");
			}
			if (config.containsKey("dailyLossLimit")) {
				this.dailyLossLimit = (Double) config.get("dailyLossLimit");
			}
			if (config.containsKey("maxConsecutiveLosses")) {
				this.maxConsecutiveLosses = (Integer) config.get("maxConsecutiveLosses");
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		System.out.println("重置风险管理器");
		consecutiveLosses.clear();
		dailyPnL.clear();
		if (portfolio != null) {
			this.initialCapital = portfolio.getInitialCash();
			this.peakCapital = initialCapital;
		}
		this.status = "RESET";
	}

	@Override
	public void shutdown() {
		System.out.println("关闭风险管理器");
		printRiskSummary();
		this.status = "SHUTDOWN";
	}

	/**
	 * 验证信号风险
	 */
	public RiskValidationResult validateSignal(SignalEvent signal) {
		performanceMonitor.startOperation("RiskManager.validateSignal");

		try {
			if (portfolio == null) {
				return new RiskValidationResult(true, "Portfolio not set");
			}

			String symbol = signal.getSymbol();
			List<String> violations = new ArrayList<>();

			// 1. 检查单品种仓位限制
			if (!checkPositionLimit(symbol)) {
				violations.add(String.format("单品种仓位超过限制 %.1f%%", maxPositionRatio * 100));
			}

			// 2. 检查总风险暴露
			if (!checkTotalRiskExposure()) {
				violations.add(String.format("总风险暴露超过限制 %.1f%%", maxTotalRisk * 100));
			}

			// 3. 检查最大回撤
			if (!checkMaxDrawdown()) {
				violations.add(String.format("最大回撤超过限制 %.1f%%", maxDrawdownLimit * 100));
			}

			// 4. 检查连续亏损
			if (!checkConsecutiveLosses(symbol)) {
				violations.add(String.format("连续亏损次数超过限制 %d次", maxConsecutiveLosses));
			}

			// 5. 检查单日亏损
			if (!checkDailyLossLimit(symbol)) {
				violations.add(String.format("单日亏损超过限制 %.1f%%", dailyLossLimit * 100));
			}

			boolean isValid = violations.isEmpty();
			String message = isValid ? "风险检查通过" : String.join("; ", violations);

			return new RiskValidationResult(isValid, message);

		} finally {
			performanceMonitor.endOperation("RiskManager.validateSignal");
		}
	}

	/**
	 * 验证订单风险
	 */
	public RiskValidationResult validateOrder(OrderEvent order) {
		// 这里可以添加订单级别的风险检查
		// 比如：最小交易单位、价格限制等
		return new RiskValidationResult(true, "订单风险检查通过");
	}

	/**
	 * 计算建议仓位大小
	 */
	public double calculateRecommendedPositionSize(SignalEvent signal, double currentPrice) {
		if (portfolio == null) {
			return 0.0;
		}

		double totalCapital = portfolio.getTotalValue();
		double basePosition = totalCapital * maxPositionRatio;

		// 根据信号强度调整
		double strength = signal.getStrength();
		double adjustedPosition = basePosition * strength;

		// 考虑连续亏损情况
		String symbol = signal.getSymbol();
		int losses = consecutiveLosses.getOrDefault(symbol, 0);
		if (losses > 0) {
			double reductionFactor = Math.max(0.5, 1.0 - (losses * 0.1));
			adjustedPosition *= reductionFactor;
		}

		// 确保不超过单品种限制
		double maxAllowed = totalCapital * maxPositionRatio;
		return Math.min(adjustedPosition, maxAllowed);
	}

	/**
	 * 记录交易结果（用于风险计算）
	 */
	public void recordTradeResult(String symbol, double pnl) {
		// 更新连续亏损计数
		if (pnl < 0) {
			int losses = consecutiveLosses.getOrDefault(symbol, 0);
			consecutiveLosses.put(symbol, losses + 1);
		} else {
			consecutiveLosses.put(symbol, 0); // 重置连续亏损计数
		}

		// 更新每日盈亏
		String today = java.time.LocalDate.now().toString();
		double dailyPnl = dailyPnL.getOrDefault(today, 0.0);
		dailyPnL.put(today, dailyPnl + pnl);

		// 更新峰值资本
		if (portfolio != null) {
			double currentCapital = portfolio.getTotalValue();
			if (currentCapital > peakCapital) {
				peakCapital = currentCapital;
			}
		}
	}

	// ==================== 风险检查方法 ====================

	private boolean checkPositionLimit(String symbol) {
		if (portfolio == null)
			return true;

		Map<String, Object> positionInfo = portfolio.getPositionInfo(symbol);
		if (positionInfo != null) {
			double positionRatio = (Double) positionInfo.getOrDefault("positionRatio", 0.0);
			return positionRatio <= maxPositionRatio * 100; // 转换为百分比
		}
		return true;
	}

	private boolean checkTotalRiskExposure() {
		if (portfolio == null)
			return true;

		double totalValue = portfolio.getTotalValue();
		double stockValue = totalValue - portfolio.getCash();
		double riskExposure = stockValue / totalValue;

		return riskExposure <= maxTotalRisk;
	}

	private boolean checkMaxDrawdown() {
		if (portfolio == null)
			return true;

		double currentCapital = portfolio.getTotalValue();
		double drawdown = (peakCapital - currentCapital) / peakCapital;

		return drawdown <= maxDrawdownLimit;
	}

	private boolean checkConsecutiveLosses(String symbol) {
		int losses = consecutiveLosses.getOrDefault(symbol, 0);
		return losses <= maxConsecutiveLosses;
	}

	private boolean checkDailyLossLimit(String symbol) {
		String today = java.time.LocalDate.now().toString();
		double dailyLoss = dailyPnL.getOrDefault(today, 0.0);

		if (portfolio != null && dailyLoss < 0) {
			double lossRatio = Math.abs(dailyLoss) / portfolio.getInitialCash();
			return lossRatio <= dailyLossLimit;
		}
		return true;
	}

	// ==================== 风险报告方法 ====================

	/**
	 * 生成风险报告
	 */
	public Map<String, Object> generateRiskReport() {
		Map<String, Object> report = new HashMap<>();

		if (portfolio != null) {
			double currentCapital = portfolio.getTotalValue();
			double drawdown = (peakCapital - currentCapital) / peakCapital * 100;

			report.put("currentCapital", currentCapital);
			report.put("peakCapital", peakCapital);
			report.put("drawdown", String.format("%.2f%%", drawdown));
			report.put("drawdownStatus", drawdown <= maxDrawdownLimit * 100 ? "正常" : "超限");
		}

		report.put("consecutiveLosses", new HashMap<>(consecutiveLosses));
		report.put("dailyPnL", new HashMap<>(dailyPnL));
		report.put("riskLimits", getRiskLimits());

		return report;
	}

	/**
	 * 获取风险限制
	 */
	public Map<String, Object> getRiskLimits() {
		Map<String, Object> limits = new HashMap<>();
		limits.put("maxPositionRatio", String.format("%.1f%%", maxPositionRatio * 100));
		limits.put("maxDrawdownLimit", String.format("%.1f%%", maxDrawdownLimit * 100));
		limits.put("dailyLossLimit", String.format("%.1f%%", dailyLossLimit * 100));
		limits.put("maxConsecutiveLosses", maxConsecutiveLosses);
		return limits;
	}

	/**
	 * 打印风险摘要
	 */
	public void printRiskSummary() {
		Map<String, Object> report = generateRiskReport();
		System.out.println("\n=== 风险摘要 ===");

		for (Map.Entry<String, Object> entry : report.entrySet()) {
			if (entry.getValue() instanceof Map) {
				System.out.println(entry.getKey() + ":");
				@SuppressWarnings("unchecked")
				Map<String, Object> subMap = (Map<String, Object>) entry.getValue();
				for (Map.Entry<String, Object> subEntry : subMap.entrySet()) {
					System.out.println("  " + subEntry.getKey() + ": " + subEntry.getValue());
				}
			} else {
				System.out.println(entry.getKey() + ": " + entry.getValue());
			}
		}
	}

	// ==================== Getter和Setter方法 ====================

	public void setPortfolio(Portfolio portfolio) {
		this.portfolio = portfolio;
		if (portfolio != null) {
			this.initialCapital = portfolio.getInitialCash();
			this.peakCapital = initialCapital;
		}
	}

	public double getMaxPositionRatio() {
		return maxPositionRatio;
	}

	public void setMaxPositionRatio(double maxPositionRatio) {
		this.maxPositionRatio = maxPositionRatio;
	}

	public double getMaxDrawdownLimit() {
		return maxDrawdownLimit;
	}

	public void setMaxDrawdownLimit(double maxDrawdownLimit) {
		this.maxDrawdownLimit = maxDrawdownLimit;
	}

	/**
	 * 风险验证结果类
	 */
	public static class RiskValidationResult {
		private final boolean isValid;
		private final String message;

		public RiskValidationResult(boolean isValid, String message) {
			this.isValid = isValid;
			this.message = message;
		}

		public boolean isValid() {
			return isValid;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return String.format("RiskValidationResult{valid=%s, message='%s'}", isValid, message);
		}
	}
}