// 新增：参数优化框架
package com.Quantitative.backtest.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.DataFeed;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 通用参数优化框架----WallkForwardAnalzer 延伸
 */
public class ParameterOptimizer {
	private final DataFeed dataFeed;
	private final BaseStrategy baseStrategy;
	private final BacktestConfig baseConfig;
	private OptimizationConfig optimizationConfig;

	public ParameterOptimizer(DataFeed dataFeed, BaseStrategy strategy, BacktestConfig config) {
		this.dataFeed = dataFeed;
		this.baseStrategy = strategy;
		this.baseConfig = config;
		this.optimizationConfig = new OptimizationConfig();
	}

	/**
	 * 执行参数优化
	 */
	public OptimizationResult optimize() {
		System.out.println("🔧 开始参数优化...");
		System.out.printf("优化方法: %s, 参数空间大小: %d%n", optimizationConfig.getMethod(), calculateParameterSpaceSize());

		long startTime = System.currentTimeMillis();
		OptimizationResult result;

		switch (optimizationConfig.getMethod()) {
		case GRID_SEARCH:
			result = gridSearchOptimization();
			break;
		case RANDOM_SEARCH:
			result = randomSearchOptimization();
			break;
		case GENETIC:
			result = geneticOptimization();
			break;
		case BAYESIAN:
			result = bayesianOptimization();
			break;
		default:
			result = gridSearchOptimization();
		}

		long duration = System.currentTimeMillis() - startTime;
		result.setOptimizationTime(duration);

		System.out.printf("✅ 参数优化完成! 耗时: %.2f秒%n", duration / 1000.0);
		return result;
	}

	/**
	 * 网格搜索优化
	 */
	private OptimizationResult gridSearchOptimization() {
		System.out.println("📊 使用网格搜索...");

		List<Map<String, Object>> allParams = generateParameterCombinations();
		OptimizationResult result = new OptimizationResult(OptimizationMethod.GRID_SEARCH);

		System.out.printf("  总参数组合: %,d%n", allParams.size());

		// 使用线程池并行优化
		ExecutorService executor = Executors
				.newFixedThreadPool(Math.min(optimizationConfig.getMaxThreads(), allParams.size()));

		List<Future<ParameterEvaluation>> futures = new ArrayList<>();
		AtomicInteger completed = new AtomicInteger(0);

		for (Map<String, Object> params : allParams) {
			Callable<ParameterEvaluation> task = () -> {
				ParameterEvaluation eval = evaluateParameters(params);
				int progress = completed.incrementAndGet();

				if (progress % 10 == 0 || progress == allParams.size()) {
					System.out.printf("  进度: %d/%d (%.1f%%)%n", progress, allParams.size(),
							(double) progress / allParams.size() * 100);
				}

				return eval;
			};
			futures.add(executor.submit(task));
		}

		// 收集结果
		for (Future<ParameterEvaluation> future : futures) {
			try {
				ParameterEvaluation eval = future.get();
				result.addEvaluation(eval);
			} catch (Exception e) {
				System.err.println("  参数评估失败: " + e.getMessage());
			}
		}

		executor.shutdown();
		result.finalizeResult();
		return result;
	}

	/**
	 * 随机搜索优化
	 */
	private OptimizationResult randomSearchOptimization() {
		System.out.println("🎲 使用随机搜索...");

		OptimizationResult result = new OptimizationResult(OptimizationMethod.RANDOM_SEARCH);
		int maxIterations = optimizationConfig.getMaxIterations();

		for (int i = 0; i < maxIterations; i++) {
			Map<String, Object> randomParams = generateRandomParameters();
			ParameterEvaluation eval = evaluateParameters(randomParams);
			result.addEvaluation(eval);

			if ((i + 1) % 10 == 0) {
				System.out.printf("  进度: %d/%d, 当前最佳: %.3f%n", i + 1, maxIterations, result.getBestScore());
			}
		}

		result.finalizeResult();
		return result;
	}

	/**
	 * 遗传算法优化
	 */
	private OptimizationResult geneticOptimization() {
		System.out.println("🧬 使用遗传算法...");
		// 实现遗传算法...
		return new OptimizationResult(OptimizationMethod.GENETIC);
	}

	/**
	 * 贝叶斯优化
	 */
	private OptimizationResult bayesianOptimization() {
		System.out.println("📈 使用贝叶斯优化...");
		// 实现贝叶斯优化...
		return new OptimizationResult(OptimizationMethod.BAYESIAN);
	}

	/**
	 * 评估参数组合
	 */
	private ParameterEvaluation evaluateParameters(Map<String, Object> parameters) {
		try {
			BacktestConfig testConfig = createTestConfig();
			EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, testConfig);

			BaseStrategy testStrategy = createStrategyWithParameters(parameters);
			engine.setStrategy(testStrategy);

			BacktestResult backtestResult = engine.runBacktest();
			double score = calculateScore(backtestResult, parameters);

			return new ParameterEvaluation(parameters, backtestResult, score);

		} catch (Exception e) {
			System.err.println("  参数评估失败: " + parameters + " - " + e.getMessage());
			return new ParameterEvaluation(parameters, null, Double.NEGATIVE_INFINITY);
		}
	}

	/**
	 * 计算参数评分
	 */
	private double calculateScore(BacktestResult result, Map<String, Object> parameters) {
		double baseScore = 0.0;

		// 收益率权重
		baseScore += result.getTotalReturn() * optimizationConfig.getReturnWeight();

		// 夏普比率权重
		baseScore += result.getSharpeRatio() * optimizationConfig.getSharpeWeight();

		// 最大回撤惩罚
		baseScore -= result.getMaxDrawdown() * optimizationConfig.getDrawdownWeight();

		// 胜率奖励
		baseScore += result.getWinRate() * optimizationConfig.getWinRateWeight();

		// 交易次数惩罚（避免过度交易）
		int tradeCount = result.getTotalTrades();
		if (tradeCount > optimizationConfig.getMaxPreferredTrades()) {
			baseScore -= (tradeCount - optimizationConfig.getMaxPreferredTrades()) * 0.1;
		}

		return baseScore;
	}

	/**
	 * 生成所有参数组合
	 */
	private List<Map<String, Object>> generateParameterCombinations() {
		List<Map<String, Object>> combinations = new ArrayList<>();
		List<ParameterDefinition> paramDefs = optimizationConfig.getParameterDefinitions();

		// 使用递归生成所有组合
		generateCombinationsRecursive(combinations, new HashMap<>(), paramDefs, 0);

		return combinations;
	}

	private void generateCombinationsRecursive(List<Map<String, Object>> combinations, Map<String, Object> current,
			List<ParameterDefinition> paramDefs, int index) {
		if (index == paramDefs.size()) {
			combinations.add(new HashMap<>(current));
			return;
		}

		ParameterDefinition paramDef = paramDefs.get(index);
		for (Object value : paramDef.getValues()) {
			current.put(paramDef.getName(), value);
			generateCombinationsRecursive(combinations, current, paramDefs, index + 1);
		}
	}

	/**
	 * 生成随机参数
	 */
	private Map<String, Object> generateRandomParameters() {
		Map<String, Object> params = new HashMap<>();
		Random random = new Random();

		for (ParameterDefinition paramDef : optimizationConfig.getParameterDefinitions()) {
			List<?> values = paramDef.getValues();
			Object randomValue = values.get(random.nextInt(values.size()));
			params.put(paramDef.getName(), randomValue);
		}

		return params;
	}

	/**
	 * 计算参数空间大小
	 */
	private int calculateParameterSpaceSize() {
		int size = 1;
		for (ParameterDefinition paramDef : optimizationConfig.getParameterDefinitions()) {
			size *= paramDef.getValues().size();
		}
		return size;
	}

	/**
	 * 使用参数创建策略
	 */
	private BaseStrategy createStrategyWithParameters(Map<String, Object> parameters) {
		try {
			BaseStrategy newStrategy = baseStrategy.getClass().getDeclaredConstructor().newInstance();
			for (Map.Entry<String, Object> entry : parameters.entrySet()) {
				newStrategy.setParameter(entry.getKey(), entry.getValue());
			}
			return newStrategy;
		} catch (Exception e) {
			throw new RuntimeException("创建策略失败: " + e.getMessage(), e);
		}
	}

	private BacktestConfig createTestConfig() {
		// 克隆基础配置
		BacktestConfig config = new BacktestConfig();
		config.setSymbol(baseConfig.getSymbol());
		config.setStartDate(baseConfig.getStartDate());
		config.setEndDate(baseConfig.getEndDate());
		config.setInitialCapital(baseConfig.getInitialCapital());
		config.setDebugMode(false);
		return config;
	}

	// ==================== 配置和结果类 ====================

	public enum OptimizationMethod {
		GRID_SEARCH, RANDOM_SEARCH, GENETIC, BAYESIAN
	}

	/**
	 * 优化配置
	 */
	public static class OptimizationConfig {
		private OptimizationMethod method = OptimizationMethod.GRID_SEARCH;
		private List<ParameterDefinition> parameterDefinitions = new ArrayList<>();
		private int maxThreads = Runtime.getRuntime().availableProcessors();
		private int maxIterations = 100;

		// 评分权重
		private double returnWeight = 1.0;
		private double sharpeWeight = 0.5;
		private double drawdownWeight = 2.0;
		private double winRateWeight = 0.3;
		private int maxPreferredTrades = 50;

		public void addParameter(String name, List<?> values) {
			parameterDefinitions.add(new ParameterDefinition(name, values));
		}

		public void addParameter(String name, int min, int max, int step) {
			List<Integer> values = new ArrayList<>();
			for (int i = min; i <= max; i += step) {
				values.add(i);
			}
			addParameter(name, values);
		}

		public void addParameter(String name, double min, double max, double step) {
			List<Double> values = new ArrayList<>();
			for (double i = min; i <= max; i += step) {
				values.add(Math.round(i * 100.0) / 100.0); // 保留2位小数
			}
			addParameter(name, values);
		}

		// Getter和Setter
		public OptimizationMethod getMethod() {
			return method;
		}

		public void setMethod(OptimizationMethod method) {
			this.method = method;
		}

		public List<ParameterDefinition> getParameterDefinitions() {
			return parameterDefinitions;
		}

		public int getMaxThreads() {
			return maxThreads;
		}

		public void setMaxThreads(int maxThreads) {
			this.maxThreads = maxThreads;
		}

		public int getMaxIterations() {
			return maxIterations;
		}

		public void setMaxIterations(int maxIterations) {
			this.maxIterations = maxIterations;
		}

		public double getReturnWeight() {
			return returnWeight;
		}

		public void setReturnWeight(double returnWeight) {
			this.returnWeight = returnWeight;
		}

		public double getSharpeWeight() {
			return sharpeWeight;
		}

		public void setSharpeWeight(double sharpeWeight) {
			this.sharpeWeight = sharpeWeight;
		}

		public double getDrawdownWeight() {
			return drawdownWeight;
		}

		public void setDrawdownWeight(double drawdownWeight) {
			this.drawdownWeight = drawdownWeight;
		}

		public double getWinRateWeight() {
			return winRateWeight;
		}

		public void setWinRateWeight(double winRateWeight) {
			this.winRateWeight = winRateWeight;
		}

		public int getMaxPreferredTrades() {
			return maxPreferredTrades;
		}

		public void setMaxPreferredTrades(int maxPreferredTrades) {
			this.maxPreferredTrades = maxPreferredTrades;
		}
	}

	/**
	 * 参数定义
	 */
	public static class ParameterDefinition {
		private final String name;
		private final List<?> values;

		public ParameterDefinition(String name, List<?> values) {
			this.name = name;
			this.values = values;
		}

		public String getName() {
			return name;
		}

		public List<?> getValues() {
			return values;
		}
	}

	/**
	 * 参数评估结果
	 */
	public static class ParameterEvaluation {
		private final Map<String, Object> parameters;
		private final BacktestResult backtestResult;
		private final double score;
		private final long evaluationTime;

		public ParameterEvaluation(Map<String, Object> parameters, BacktestResult backtestResult, double score) {
			this.parameters = parameters;
			this.backtestResult = backtestResult;
			this.score = score;
			this.evaluationTime = System.currentTimeMillis();
		}

		// Getter方法
		public Map<String, Object> getParameters() {
			return parameters;
		}

		public BacktestResult getBacktestResult() {
			return backtestResult;
		}

		public double getScore() {
			return score;
		}

		public long getEvaluationTime() {
			return evaluationTime;
		}
	}

	/**
	 * 优化结果
	 */
	public static class OptimizationResult {
		private final OptimizationMethod method;
		private final List<ParameterEvaluation> evaluations = new ArrayList<>();
		private ParameterEvaluation bestEvaluation;
		private long optimizationTime;
		private Map<String, Object> bestParameters;

		public OptimizationResult(OptimizationMethod method) {
			this.method = method;
		}

		public void addEvaluation(ParameterEvaluation evaluation) {
			evaluations.add(evaluation);

			// 更新最佳结果
			if (bestEvaluation == null || evaluation.getScore() > bestEvaluation.getScore()) {
				bestEvaluation = evaluation;
				bestParameters = evaluation.getParameters();
			}
		}

		public void finalizeResult() {
			// 按评分排序
			evaluations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
		}

		public void printReport() {
			// System.out.println("\n" + "=".repeat(100));
			System.out.println(Stream.generate(() -> "\n" + "=").limit(100).collect(Collectors.joining()));
			System.out.println("🏆 参数优化报告 - " + method);
			// System.out.println("=".repeat(100));
			System.out.println(Stream.generate(() -> "=").limit(100).collect(Collectors.joining()));
			System.out.printf("总评估次数: %,d%n", evaluations.size());
			System.out.printf("优化耗时: %.2f秒%n", optimizationTime / 1000.0);

			if (bestEvaluation != null) {
				System.out.println("\n🎯 最佳参数组合:");
				System.out.println("  参数: " + bestParameters);
				System.out.printf("  评分: %.3f%n", bestEvaluation.getScore());

				BacktestResult result = bestEvaluation.getBacktestResult();
				if (result != null) {
					System.out.printf("  收益率: %.2f%%%n", result.getTotalReturn());
					System.out.printf("  夏普比率: %.2f%n", result.getSharpeRatio());
					System.out.printf("  最大回撤: %.2f%%%n", result.getMaxDrawdown());
					System.out.printf("  胜率: %.1f%%%n", result.getWinRate());
				}
			}

			// 显示前10名
			System.out.println("\n🏅 前10名参数组合:");
			System.out.println("排名 | 评分    | 参数配置");
			System.out.println("----|---------|----------");

			int displayCount = Math.min(10, evaluations.size());
			for (int i = 0; i < displayCount; i++) {
				ParameterEvaluation eval = evaluations.get(i);
				System.out.printf("%2d  | %7.3f | %s%n", i + 1, eval.getScore(), eval.getParameters());
			}

			// System.out.println("=".repeat(100));
			System.out.println(Stream.generate(() -> "=").limit(100).collect(Collectors.joining()));
		}

		// Getter方法
		public OptimizationMethod getMethod() {
			return method;
		}

		public List<ParameterEvaluation> getEvaluations() {
			return new ArrayList<>(evaluations);
		}

		public ParameterEvaluation getBestEvaluation() {
			return bestEvaluation;
		}

		public Map<String, Object> getBestParameters() {
			return bestParameters;
		}

		public long getOptimizationTime() {
			return optimizationTime;
		}

		public void setOptimizationTime(long optimizationTime) {
			this.optimizationTime = optimizationTime;
		}

		public double getBestScore() {
			return bestEvaluation != null ? bestEvaluation.getScore() : 0.0;
		}
	}

	// ==================== Getter和Setter ====================

	public void setOptimizationConfig(OptimizationConfig optimizationConfig) {
		this.optimizationConfig = optimizationConfig;
	}

	public OptimizationConfig getOptimizationConfig() {
		return optimizationConfig;
	}
}