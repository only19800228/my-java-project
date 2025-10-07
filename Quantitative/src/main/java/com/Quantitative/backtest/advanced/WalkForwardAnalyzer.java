// 新增：Walk-Forward分析框架
package com.Quantitative.backtest.advanced;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.DataFeed;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * Walk-Forward分析器 - 滚动窗口回测
 */
public class WalkForwardAnalyzer {
	private final DataFeed dataFeed;
	private BaseStrategy strategy;
	private WalkForwardConfig config;
	private AtomicInteger currentRound = new AtomicInteger(0);

	public WalkForwardAnalyzer(DataFeed dataFeed) {
		this.dataFeed = dataFeed;
		this.config = new WalkForwardConfig();
	}

	public WalkForwardAnalyzer(DataFeed dataFeed, WalkForwardConfig config) {
		this.dataFeed = dataFeed;
		this.config = config;
	}

	/**
	 * 执行Walk-Forward分析
	 */
	public WalkForwardResult analyze() {
		System.out.println("🚀 开始Walk-Forward分析...");
		System.out.printf("时间范围: %s 到 %s%n", config.getStartDate(), config.getEndDate());
		System.out.printf("训练窗口: %d个月, 测试窗口: %d个月, 滚动步长: %d个月%n", config.getTrainingMonths(), config.getTestingMonths(),
				config.getRollingStepMonths());

		WalkForwardResult result = new WalkForwardResult();
		List<TimeWindow> windows = generateTimeWindows();

		System.out.printf("生成 %d 个时间窗口%n", windows.size());

		for (int i = 0; i < windows.size(); i++) {
			TimeWindow window = windows.get(i);
			currentRound.set(i + 1);

			System.out.printf("\n=== 第%d/%d轮 Walk-Forward ===%n", i + 1, windows.size());
			System.out.printf("训练期: %s 到 %s%n", window.getTrainStart().toLocalDate(),
					window.getTrainEnd().toLocalDate());
			System.out.printf("测试期: %s 到 %s%n", window.getTrainEnd().toLocalDate(), window.getTestEnd().toLocalDate());

			// 执行单轮Walk-Forward
			WalkForwardRound roundResult = executeWalkForwardRound(window, i + 1);
			result.addRound(roundResult);

			// 显示进度
			showProgress(i + 1, windows.size());
		}

		result.calculateFinalMetrics();
		System.out.println("\n✅ Walk-Forward分析完成!");
		return result;
	}

	/**
	 * 生成时间窗口
	 */
	private List<TimeWindow> generateTimeWindows() {
		List<TimeWindow> windows = new ArrayList<>();
		LocalDateTime current = config.getStartDate();

		while (true) {
			LocalDateTime trainEnd = current.plusMonths(config.getTrainingMonths());
			LocalDateTime testEnd = trainEnd.plusMonths(config.getTestingMonths());

			// 检查是否超出数据范围
			if (testEnd.isAfter(config.getEndDate())) {
				break;
			}

			windows.add(new TimeWindow(current, trainEnd, testEnd));
			current = current.plusMonths(config.getRollingStepMonths());
		}

		return windows;
	}

	/**
	 * 执行单轮Walk-Forward
	 */
	private WalkForwardRound executeWalkForwardRound(TimeWindow window, int roundNumber) {
		WalkForwardRound round = new WalkForwardRound(roundNumber, window);

		try {
			// 阶段1: 参数优化（在训练期）
			System.out.println("  阶段1: 参数优化...");
			long startTime = System.currentTimeMillis();

			Map<String, Object> optimizedParams = optimizeParameters(window.getTrainStart(), window.getTrainEnd());
			round.setOptimizedParameters(optimizedParams);

			long optimizeTime = System.currentTimeMillis() - startTime;

			// 阶段2: 前向测试（在测试期）
			System.out.println("  阶段2: 前向测试...");
			startTime = System.currentTimeMillis();

			BacktestResult testResult = runOutOfSampleTest(window.getTrainEnd(), window.getTestEnd(), optimizedParams);
			round.setTestResult(testResult);

			long testTime = System.currentTimeMillis() - startTime;

			System.out.printf("  ✅ 第%d轮完成: 收益率=%.2f%%, 优化耗时=%dms, 测试耗时=%dms%n", roundNumber,
					testResult.getTotalReturn(), optimizeTime, testTime);

		} catch (Exception e) {
			System.err.printf("  ❌ 第%d轮执行失败: %s%n", roundNumber, e.getMessage());
			round.setFailed(true);
			round.setErrorMessage(e.getMessage());
		}

		return round;
	}

	/**
	 * 参数优化
	 */
	private Map<String, Object> optimizeParameters(LocalDateTime start, LocalDateTime end) {
		switch (config.getOptimizationMethod()) {
		case GRID_SEARCH:
			return gridSearchOptimization(start, end);
		case RANDOM_SEARCH:
			return randomSearchOptimization(start, end);
		case FIXED_PARAMS:
			return fixedParametersOptimization(start, end);
		default:
			return simpleOptimization(start, end);
		}
	}

	/**
	 * 网格搜索优化
	 */
	private Map<String, Object> gridSearchOptimization(LocalDateTime start, LocalDateTime end) {
		System.out.println("    使用网格搜索优化参数...");

		BacktestConfig trainConfig = createConfig(start, end);
		Map<String, Object> bestParams = new HashMap<>();
		double bestScore = -Double.MAX_VALUE;
		int totalCombinations = 0;
		int testedCombinations = 0;

		// 定义参数搜索空间
		int[] periods = { 10, 12, 14, 16, 18, 20 };
		double[] overboughtLevels = { 65, 68, 70, 72, 75 };
		double[] oversoldLevels = { 25, 28, 30, 32, 35 };

		totalCombinations = periods.length * overboughtLevels.length * oversoldLevels.length;

		for (int period : periods) {
			for (double overbought : overboughtLevels) {
				for (double oversold : oversoldLevels) {
					testedCombinations++;

					if (testedCombinations % 10 == 0) {
						System.out.printf("    进度: %d/%d (%.1f%%)%n", testedCombinations, totalCombinations,
								(double) testedCombinations / totalCombinations * 100);
					}

					try {
						EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, trainConfig);

						// 创建策略实例并设置参数
						BaseStrategy testStrategy = createStrategyWithParams(period, overbought, oversold);
						engine.setStrategy(testStrategy);

						BacktestResult result = engine.runBacktest();

						// 使用夏普比率作为评分标准
						double score = calculateOptimizationScore(result);

						if (score > bestScore) {
							bestScore = score;
							bestParams.clear();
							bestParams.put("rsiPeriod", period);
							bestParams.put("overbought", overbought);
							bestParams.put("oversold", oversold);
							bestParams.put("optimizationScore", score);
						}

					} catch (Exception e) {
						System.err.printf("    参数组合测试失败: period=%d, overbought=%.1f, oversold=%.1f%n", period,
								overbought, oversold);
					}
				}
			}
		}

		System.out.printf("    ✅ 最佳参数: %s, 优化得分: %.3f%n", bestParams, bestScore);
		return bestParams;
	}

	/**
	 * 随机搜索优化
	 */
	private Map<String, Object> randomSearchOptimization(LocalDateTime start, LocalDateTime end) {
		System.out.println("    使用随机搜索优化参数...");

		BacktestConfig trainConfig = createConfig(start, end);
		Map<String, Object> bestParams = new HashMap<>();
		double bestScore = -Double.MAX_VALUE;

		int maxIterations = 50; // 随机搜索次数

		for (int i = 0; i < maxIterations; i++) {
			// 生成随机参数
			int period = 10 + (int) (Math.random() * 11); // 10-20
			double overbought = 65 + Math.random() * 15; // 65-80
			double oversold = 20 + Math.random() * 15; // 20-35

			try {
				EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, trainConfig);
				BaseStrategy testStrategy = createStrategyWithParams(period, overbought, oversold);
				engine.setStrategy(testStrategy);

				BacktestResult result = engine.runBacktest();
				double score = calculateOptimizationScore(result);

				if (score > bestScore) {
					bestScore = score;
					bestParams.clear();
					bestParams.put("rsiPeriod", period);
					bestParams.put("overbought", overbought);
					bestParams.put("oversold", oversold);
					bestParams.put("optimizationScore", score);
				}

			} catch (Exception e) {
				// 忽略单次失败，继续搜索
			}

			if ((i + 1) % 10 == 0) {
				System.out.printf("    进度: %d/%d, 当前最佳得分: %.3f%n", i + 1, maxIterations, bestScore);
			}
		}

		System.out.printf("    ✅ 最佳参数: %s, 优化得分: %.3f%n", bestParams, bestScore);
		return bestParams;
	}

	/**
	 * 固定参数（不优化）
	 */
	private Map<String, Object> fixedParametersOptimization(LocalDateTime start, LocalDateTime end) {
		System.out.println("    使用固定参数...");

		Map<String, Object> params = new HashMap<>();
		params.put("rsiPeriod", 14);
		params.put("overbought", 70.0);
		params.put("oversold", 30.0);
		params.put("optimizationScore", 0.0);

		return params;
	}

	/**
	 * 简单优化
	 */
	private Map<String, Object> simpleOptimization(LocalDateTime start, LocalDateTime end) {
		return fixedParametersOptimization(start, end);
	}

	/**
	 * 计算优化评分（综合考虑收益率和风险）
	 */
	private double calculateOptimizationScore(BacktestResult result) {
		double returnScore = result.getTotalReturn() / 100.0; // 归一化
		double riskPenalty = result.getMaxDrawdown() / 100.0; // 回撤惩罚
		double sharpeBonus = Math.max(0, result.getSharpeRatio()) * 0.1; // 夏普奖励

		return returnScore - riskPenalty + sharpeBonus;
	}

	/**
	 * 样本外测试
	 */
	private BacktestResult runOutOfSampleTest(LocalDateTime start, LocalDateTime end,
			Map<String, Object> optimizedParams) {
		BacktestConfig testConfig = createConfig(start, end);
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, testConfig);

		// 使用优化后的参数创建策略
		BaseStrategy testStrategy = createStrategyWithParams((Integer) optimizedParams.get("rsiPeriod"),
				(Double) optimizedParams.get("overbought"), (Double) optimizedParams.get("oversold"));

		engine.setStrategy(testStrategy);
		return engine.runBacktest();
	}

	/**
	 * 使用参数创建策略实例
	 */
	private BaseStrategy createStrategyWithParams(int period, double overbought, double oversold) {
		try {
			BaseStrategy newStrategy = strategy.getClass().getDeclaredConstructor().newInstance();
			newStrategy.setParameter("rsiPeriod", period);
			newStrategy.setParameter("overbought", overbought);
			newStrategy.setParameter("oversold", oversold);
			return newStrategy;
		} catch (Exception e) {
			throw new RuntimeException("创建策略实例失败: " + e.getMessage(), e);
		}
	}

	private BacktestConfig createConfig(LocalDateTime start, LocalDateTime end) {
		BacktestConfig config = new BacktestConfig();
		config.setStartDate(start);
		config.setEndDate(end);
		config.setInitialCapital(100000.0);
		config.setDebugMode(false);
		config.setSymbol(this.config.getSymbol());
		return config;
	}

	private void showProgress(int current, int total) {
		double progress = (double) current / total * 100;
		System.out.printf("📊 总体进度: %d/%d (%.1f%%)%n", current, total, progress);
	}

	// ==================== 内部类 ====================

	/**
	 * 时间窗口
	 */
	public static class TimeWindow {
		private final LocalDateTime trainStart;
		private final LocalDateTime trainEnd;
		private final LocalDateTime testEnd;

		public TimeWindow(LocalDateTime trainStart, LocalDateTime trainEnd, LocalDateTime testEnd) {
			this.trainStart = trainStart;
			this.trainEnd = trainEnd;
			this.testEnd = testEnd;
		}

		public LocalDateTime getTrainStart() {
			return trainStart;
		}

		public LocalDateTime getTrainEnd() {
			return trainEnd;
		}

		public LocalDateTime getTestEnd() {
			return testEnd;
		}
	}

	/**
	 * Walk-Forward配置
	 */
	public static class WalkForwardConfig {
		private String symbol = "000001";
		private LocalDateTime startDate;
		private LocalDateTime endDate;
		private int trainingMonths = 24;
		private int testingMonths = 6;
		private int rollingStepMonths = 6;
		private OptimizationMethod optimizationMethod = OptimizationMethod.GRID_SEARCH;

		public enum OptimizationMethod {
			GRID_SEARCH, RANDOM_SEARCH, FIXED_PARAMS
		}

		// Getter和Setter
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

		public int getTrainingMonths() {
			return trainingMonths;
		}

		public void setTrainingMonths(int trainingMonths) {
			this.trainingMonths = trainingMonths;
		}

		public int getTestingMonths() {
			return testingMonths;
		}

		public void setTestingMonths(int testingMonths) {
			this.testingMonths = testingMonths;
		}

		public int getRollingStepMonths() {
			return rollingStepMonths;
		}

		public void setRollingStepMonths(int rollingStepMonths) {
			this.rollingStepMonths = rollingStepMonths;
		}

		public OptimizationMethod getOptimizationMethod() {
			return optimizationMethod;
		}

		public void setOptimizationMethod(OptimizationMethod optimizationMethod) {
			this.optimizationMethod = optimizationMethod;
		}
	}

	/**
	 * Walk-Forward单轮结果
	 */
	public static class WalkForwardRound {
		private final int roundNumber;
		private final TimeWindow timeWindow;
		private Map<String, Object> optimizedParameters;
		private BacktestResult testResult;
		private boolean failed = false;
		private String errorMessage;

		public WalkForwardRound(int roundNumber, TimeWindow timeWindow) {
			this.roundNumber = roundNumber;
			this.timeWindow = timeWindow;
		}

		// Getter和Setter
		public int getRoundNumber() {
			return roundNumber;
		}

		public TimeWindow getTimeWindow() {
			return timeWindow;
		}

		public Map<String, Object> getOptimizedParameters() {
			return optimizedParameters;
		}

		public void setOptimizedParameters(Map<String, Object> optimizedParameters) {
			this.optimizedParameters = optimizedParameters;
		}

		public BacktestResult getTestResult() {
			return testResult;
		}

		public void setTestResult(BacktestResult testResult) {
			this.testResult = testResult;
		}

		public boolean isFailed() {
			return failed;
		}

		public void setFailed(boolean failed) {
			this.failed = failed;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public double getTestReturn() {
			return (testResult != null && !failed) ? testResult.getTotalReturn() : 0.0;
		}

		public String getParameterSummary() {
			if (optimizedParameters == null)
				return "N/A";
			return String.format("RSI(%d, %.1f, %.1f)", (Integer) optimizedParameters.get("rsiPeriod"),
					(Double) optimizedParameters.get("overbought"), (Double) optimizedParameters.get("oversold"));
		}
	}

	/**
	 * Walk-Forward总结果
	 */
	public static class WalkForwardResult {
		private final List<WalkForwardRound> rounds = new ArrayList<>();
		private double averageReturn;
		private double returnStdDev;
		private double winRate;
		private double consistencyScore;
		private double bestReturn = -Double.MAX_VALUE;
		private double worstReturn = Double.MAX_VALUE;
		private int successfulRounds;

		public void addRound(WalkForwardRound round) {
			rounds.add(round);
			if (!round.isFailed()) {
				successfulRounds++;
			}
		}

		public void calculateFinalMetrics() {
			List<Double> returns = getSuccessfulReturns();
			if (returns.isEmpty())
				return;

			// 基础统计
			averageReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
			bestReturn = returns.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
			worstReturn = returns.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

			// 标准差
			double variance = returns.stream().mapToDouble(r -> Math.pow(r - averageReturn, 2)).average().orElse(0.0);
			returnStdDev = Math.sqrt(variance);

			// 胜率
			long winningRounds = returns.stream().filter(r -> r > 0).count();
			winRate = (double) winningRounds / returns.size() * 100;

			// 一致性得分
			consistencyScore = calculateConsistencyScore(returns);
		}

		private List<Double> getSuccessfulReturns() {
			return rounds.stream().filter(round -> !round.isFailed() && round.getTestResult() != null)
					.map(WalkForwardRound::getTestReturn)
					// .toList();
					.collect(Collectors.toList());
		}

		private double calculateConsistencyScore(List<Double> returns) {
			if (returns.size() < 2)
				return 0.0;

			int consistentCount = 0;
			for (int i = 1; i < returns.size(); i++) {
				if (returns.get(i) * returns.get(i - 1) > 0) {
					consistentCount++;
				}
			}

			return (double) consistentCount / (returns.size() - 1) * 100;
		}

		public void printReport() {
			// System.out.println("\n" + "=".repeat(100));
			System.out.println(Stream.generate(() -> "\n" + "=").limit(100).collect(Collectors.joining()));
			System.out.println("📊 WALK-FORWARD 分析报告");
			// System.out.println("=".repeat(100));
			System.out.println(Stream.generate(() -> "=").limit(100).collect(Collectors.joining()));
			System.out.printf("总轮次: %d, 成功轮次: %d, 成功率: %.1f%%%n", rounds.size(), successfulRounds,
					(double) successfulRounds / rounds.size() * 100);
			System.out.printf("平均收益率: %.2f%%%n", averageReturn);
			System.out.printf("最佳收益率: %.2f%%%n", bestReturn);
			System.out.printf("最差收益率: %.2f%%%n", worstReturn);
			System.out.printf("收益率标准差: %.2f%%%n", returnStdDev);
			System.out.printf("胜率: %.1f%%%n", winRate);
			System.out.printf("一致性得分: %.1f%%%n", consistencyScore);

			// 风险调整收益
			double riskAdjustedReturn = returnStdDev > 0 ? averageReturn / returnStdDev : 0;
			System.out.printf("风险调整收益: %.3f%n", riskAdjustedReturn);

			System.out.println("\n各轮次详情:");
			System.out.println("轮次 | 训练期           | 测试期           | 参数配置        | 收益率%   | 状态");
			System.out.println("-----|------------------|------------------|-----------------|-----------|------");

			for (WalkForwardRound round : rounds) {
				String status = round.isFailed() ? "❌失败" : "✅成功";
				double returnPct = round.getTestReturn();
				String params = round.getParameterSummary();

				System.out.printf("%2d  | %s | %s | %-15s | %7.2f  | %s%n", round.getRoundNumber(),
						formatDateRange(round.getTimeWindow().getTrainStart(), round.getTimeWindow().getTrainEnd()),
						formatDateRange(round.getTimeWindow().getTrainEnd(), round.getTimeWindow().getTestEnd()),
						params, returnPct, status);
			}

			// System.out.println("=".repeat(100));
			System.out.println(Stream.generate(() -> "=").limit(100).collect(Collectors.joining()));
		}

		private String formatDateRange(LocalDateTime start, LocalDateTime end) {
			return start.toLocalDate() + "~" + end.toLocalDate();
		}

		// Getter方法
		public List<WalkForwardRound> getRounds() {
			return new ArrayList<>(rounds);
		}

		public double getAverageReturn() {
			return averageReturn;
		}

		public double getReturnStdDev() {
			return returnStdDev;
		}

		public double getWinRate() {
			return winRate;
		}

		public double getConsistencyScore() {
			return consistencyScore;
		}

		public double getBestReturn() {
			return bestReturn;
		}

		public double getWorstReturn() {
			return worstReturn;
		}

		public int getSuccessfulRounds() {
			return successfulRounds;
		}
	}

	// ==================== Getter和Setter ====================

	public void setStrategy(BaseStrategy strategy) {
		this.strategy = strategy;
	}

	public void setConfig(WalkForwardConfig config) {
		this.config = config;
	}

	public WalkForwardConfig getConfig() {
		return config;
	}

	public int getCurrentRound() {
		return currentRound.get();
	}
}