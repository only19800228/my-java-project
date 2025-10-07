// 修复后的策略组合管理器
package com.Quantitative.portfolio.composite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 策略组合管理器 - 多策略组合回测
 */
public class StrategyCompositeManager {
	private final DataFeed dataFeed;
	private final BacktestConfig baseConfig;
	private final Map<String, StrategyAllocation> strategies;
	private CompositeConfig compositeConfig;
	private SignalCombiner signalCombiner;

	public StrategyCompositeManager(DataFeed dataFeed, BacktestConfig baseConfig) {
		this.dataFeed = dataFeed;
		this.baseConfig = baseConfig;
		this.strategies = new ConcurrentHashMap<>();
		this.compositeConfig = new CompositeConfig();
		this.signalCombiner = new WeightedSignalCombiner();
	}

	/**
	 * 添加策略到组合
	 */
	public void addStrategy(String strategyName, BaseStrategy strategy, double weight) {
		if (weight <= 0) {
			throw new IllegalArgumentException("策略权重必须大于0: " + weight);
		}

		strategies.put(strategyName, new StrategyAllocation(strategyName, strategy, weight));
		System.out.printf("✅ 添加策略: %s (权重: %.1f%%)%n", strategyName, weight * 100);
	}

	/**
	 * 执行组合回测---标记2
	 */
	/**
	 * 执行组合回测
	 */
	public CompositeBacktestResult runCompositeBacktest() {
		System.out.println("🎯 开始策略组合回测...");
		System.out.printf("组合包含 %d 个策略%n", strategies.size());

		CompositeBacktestResult result = new CompositeBacktestResult();
		Map<String, BacktestResult> individualResults = new HashMap<>();

		// 1. 执行单个策略回测
		System.out.println("\n阶段1: 执行单个策略回测...");
		for (StrategyAllocation allocation : strategies.values()) {
			System.out.printf("  执行策略: %s...%n", allocation.getName());

			BacktestResult strategyResult = runIndividualStrategy(allocation.getStrategy());
			individualResults.put(allocation.getName(), strategyResult);

			System.out.printf("  ✅ %s: 收益率=%.2f%%, 夏普=%.2f%n", allocation.getName(), strategyResult.getTotalReturn(),
					strategyResult.getSharpeRatio());
		}

		// 2. 执行组合回测
		System.out.println("\n阶段2: 执行组合回测...");
		BacktestResult compositeResult = runCompositeStrategy();

		// 设置结果
		result.setCompositeResult(compositeResult);
		result.setIndividualResults(individualResults);

		// 3. 计算组合指标
		System.out.println("\n阶段3: 计算组合指标...");
		result.calculateCompositeMetrics(strategies);

		System.out.println("✅ 策略组合回测完成!");
		return result;
	}

	/**
	 * 执行单个策略回测
	 */
	private BacktestResult runIndividualStrategy(BaseStrategy strategy) {
		BacktestConfig config = createBacktestConfig();
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
		engine.setStrategy(strategy);
		return engine.runBacktest();
	}

	/**
	 * 执行组合策略回测
	 */
	private BacktestResult runCompositeStrategy() {
		BacktestConfig config = createBacktestConfig();
		EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);

		// 创建组合策略
		CompositeStrategy compositeStrategy = new CompositeStrategy(strategies, signalCombiner);
		engine.setStrategy(compositeStrategy);

		return engine.runBacktest();
	}

	/**
	 * 优化策略权重
	 */
	public WeightOptimizationResult optimizeWeights() {
		System.out.println("🔧 开始策略权重优化...");

		WeightOptimizationResult result = new WeightOptimizationResult();
		Map<String, BacktestResult> individualResults = new HashMap<>();

		// 先获取各策略单独表现
		for (StrategyAllocation allocation : strategies.values()) {
			BacktestResult strategyResult = runIndividualStrategy(allocation.getStrategy());
			individualResults.put(allocation.getName(), strategyResult);
		}

		// 使用不同的权重优化方法
		switch (compositeConfig.getWeightOptimizationMethod()) {
		case EQUAL_WEIGHT:
			result = optimizeEqualWeights(individualResults);
			break;
		case RISK_PARITY:
			result = optimizeRiskParityWeights(individualResults);
			break;
		case MARKOWITZ:
			result = optimizeMarkowitzWeights(individualResults);
			break;
		case CUSTOM:
			result = optimizeCustomWeights(individualResults);
			break;
		}

		// 应用优化后的权重
		applyOptimizedWeights(result.getOptimizedWeights());

		return result;
	}

	/**
	 * 等权重优化
	 */
	private WeightOptimizationResult optimizeEqualWeights(Map<String, BacktestResult> individualResults) {
		System.out.println("  使用等权重优化...");

		WeightOptimizationResult result = new WeightOptimizationResult();
		Map<String, Double> equalWeights = new HashMap<>();

		double equalWeight = 1.0 / strategies.size();
		for (String strategyName : strategies.keySet()) {
			equalWeights.put(strategyName, equalWeight);
		}

		result.setOptimizedWeights(equalWeights);
		result.setMethod(WeightOptimizationMethod.EQUAL_WEIGHT);

		return result;
	}

	/**
	 * 风险平价权重优化
	 */
	private WeightOptimizationResult optimizeRiskParityWeights(Map<String, BacktestResult> individualResults) {
		System.out.println("  使用风险平价优化...");

		WeightOptimizationResult result = new WeightOptimizationResult();
		Map<String, Double> riskWeights = new HashMap<>();

		// 基于波动率分配权重（波动率越低，权重越高）
		double totalInverseVolatility = 0.0;
		Map<String, Double> volatilities = new HashMap<>();

		for (Map.Entry<String, BacktestResult> entry : individualResults.entrySet()) {
			double volatility = calculateStrategyVolatility(entry.getValue());
			volatilities.put(entry.getKey(), volatility);
			totalInverseVolatility += 1.0 / volatility;
		}

		for (Map.Entry<String, Double> entry : volatilities.entrySet()) {
			double weight = (1.0 / entry.getValue()) / totalInverseVolatility;
			riskWeights.put(entry.getKey(), weight);
		}

		result.setOptimizedWeights(riskWeights);
		result.setMethod(WeightOptimizationMethod.RISK_PARITY);

		return result;
	}

	/**
	 * 马科维茨均值方差优化
	 */
	private WeightOptimizationResult optimizeMarkowitzWeights(Map<String, BacktestResult> individualResults) {
		System.out.println("  使用马科维茨优化...");
		// 简化实现 - 实际应该计算协方差矩阵等
		return optimizeRiskParityWeights(individualResults);
	}

	/**
	 * 自定义权重优化
	 */
	private WeightOptimizationResult optimizeCustomWeights(Map<String, BacktestResult> individualResults) {
		System.out.println("  使用自定义优化...");
		// 基于自定义规则优化权重
		return optimizeEqualWeights(individualResults);
	}

	/**
	 * 计算策略波动率
	 */
	private double calculateStrategyVolatility(BacktestResult result) {
		// 使用最大回撤作为波动率代理
		return Math.max(result.getMaxDrawdown(), 1.0); // 避免除零
	}

	/**
	 * 应用优化后的权重
	 */
	private void applyOptimizedWeights(Map<String, Double> optimizedWeights) {
		for (Map.Entry<String, Double> entry : optimizedWeights.entrySet()) {
			String strategyName = entry.getKey();
			double newWeight = entry.getValue();

			StrategyAllocation allocation = strategies.get(strategyName);
			if (allocation != null) {
				allocation.setWeight(newWeight);
				System.out.printf("  ✅ %s 权重调整为: %.1f%%%n", strategyName, newWeight * 100);
			}
		}
	}

	private BacktestConfig createBacktestConfig() {
		BacktestConfig config = new BacktestConfig();
		config.setSymbol(baseConfig.getSymbol());
		config.setStartDate(baseConfig.getStartDate());
		config.setEndDate(baseConfig.getEndDate());
		config.setInitialCapital(baseConfig.getInitialCapital());
		config.setDebugMode(false);
		return config;
	}

	// ==================== 内部类 ====================

	/**
	 * 策略配置
	 */
	public static class StrategyAllocation {
		private final String name;
		private final BaseStrategy strategy;
		private double weight;

		public StrategyAllocation(String name, BaseStrategy strategy, double weight) {
			this.name = name;
			this.strategy = strategy;
			this.weight = weight;
		}

		// Getter和Setter
		public String getName() {
			return name;
		}

		public BaseStrategy getStrategy() {
			return strategy;
		}

		public double getWeight() {
			return weight;
		}

		public void setWeight(double weight) {
			this.weight = weight;
		}
	}

	/**
	 * 组合配置
	 */
	public static class CompositeConfig {
		private WeightOptimizationMethod weightOptimizationMethod = WeightOptimizationMethod.EQUAL_WEIGHT;
		private double maxSingleStrategyWeight = 0.5; // 单策略最大权重50%
		private boolean enableDynamicRebalancing = true;
		private int rebalancingFrequency = 30; // 30天再平衡

		public enum WeightOptimizationMethod {
			EQUAL_WEIGHT, RISK_PARITY, MARKOWITZ, CUSTOM
		}

		// Getter和Setter
		public WeightOptimizationMethod getWeightOptimizationMethod() {
			return weightOptimizationMethod;
		}

		public void setWeightOptimizationMethod(WeightOptimizationMethod method) {
			this.weightOptimizationMethod = method;
		}

		public double getMaxSingleStrategyWeight() {
			return maxSingleStrategyWeight;
		}

		public void setMaxSingleStrategyWeight(double maxSingleStrategyWeight) {
			this.maxSingleStrategyWeight = maxSingleStrategyWeight;
		}

		public boolean isEnableDynamicRebalancing() {
			return enableDynamicRebalancing;
		}

		public void setEnableDynamicRebalancing(boolean enableDynamicRebalancing) {
			this.enableDynamicRebalancing = enableDynamicRebalancing;
		}

		public int getRebalancingFrequency() {
			return rebalancingFrequency;
		}

		public void setRebalancingFrequency(int rebalancingFrequency) {
			this.rebalancingFrequency = rebalancingFrequency;
		}
	}

	/**
	 * 组合策略（处理多个策略的信号）
	 */
	public class CompositeStrategy extends BaseStrategy {
		private final Map<String, StrategyAllocation> strategies;
		private final SignalCombiner signalCombiner;
		private final Map<String, List<SignalEvent>> strategySignals;

		public CompositeStrategy(Map<String, StrategyAllocation> strategies, SignalCombiner signalCombiner) {
			super("策略组合");
			this.strategies = new HashMap<>(strategies);
			this.signalCombiner = signalCombiner;
			this.strategySignals = new ConcurrentHashMap<>();
		}

		@Override
		protected void init() {
			System.out.println("初始化策略组合...");

			// 初始化所有子策略
			for (StrategyAllocation allocation : strategies.values()) {
				try {
					// 使用父类的dataFeed和portfolio来设置子策略
					if (this.dataFeed != null) {
						allocation.getStrategy().setDataFeed(this.dataFeed);
					}
					if (this.portfolio != null) {
						allocation.getStrategy().setPortfolio(this.portfolio);
					}

					allocation.getStrategy().initialize();
					strategySignals.put(allocation.getName(), new ArrayList<>());

					System.out.printf("  ✅ 初始化子策略: %s%n", allocation.getName());

				} catch (Exception e) {
					System.err.printf("  ❌ 初始化子策略失败: %s - %s%n", allocation.getName(), e.getMessage());
				}
			}

			System.out.println("策略组合初始化完成");
		}

		@Override
		protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
			// 收集所有子策略的信号
			Map<String, List<SignalEvent>> allSignals = new HashMap<>();

			for (StrategyAllocation allocation : strategies.values()) {
				try {
					List<SignalEvent> strategySignals = allocation.getStrategy().onBar(bar);
					allSignals.put(allocation.getName(), strategySignals);

					if (!strategySignals.isEmpty()) {
						System.out.printf("  📊 %s 生成 %d 个信号%n", allocation.getName(), strategySignals.size());
					}

				} catch (Exception e) {
					System.err.printf("  ❌ %s 处理Bar失败: %s%n", allocation.getName(), e.getMessage());
					allSignals.put(allocation.getName(), new ArrayList<>());
				}
			}

			// 合并信号
			try {
				List<SignalEvent> combinedSignals = signalCombiner.combineSignals(allSignals, strategies);
				signals.addAll(combinedSignals);

				if (!combinedSignals.isEmpty()) {
					System.out.printf("  🎯 组合策略生成 %d 个合并信号%n", combinedSignals.size());
				}

			} catch (Exception e) {
				System.err.println("  ❌ 信号合并失败: " + e.getMessage());
			}
		}
	}

	/**
	 * 信号合并器接口
	 */
	public interface SignalCombiner {
		List<SignalEvent> combineSignals(Map<String, List<SignalEvent>> allSignals,
				Map<String, StrategyAllocation> strategies);
	}

	/**
	 * 加权信号合并器
	 */
	// 修复 WeightedSignalCombiner 中的静态引用
	public static class WeightedSignalCombiner implements SignalCombiner {
		@Override
		public List<SignalEvent> combineSignals(Map<String, List<SignalEvent>> allSignals,
				Map<String, StrategyAllocation> strategies) {
			List<SignalEvent> combinedSignals = new ArrayList<>();
			Map<String, Double> symbolVotes = new HashMap<>(); // 符号: 加权得分

			for (Map.Entry<String, List<SignalEvent>> entry : allSignals.entrySet()) {
				String strategyName = entry.getKey();
				StrategyAllocation allocation = strategies.get(strategyName);

				if (allocation == null) {
					continue; // 跳过不存在的策略
				}

				double weight = allocation.getWeight();

				for (SignalEvent signal : entry.getValue()) {
					String symbol = signal.getSymbol();
					double signalStrength = signal.getStrength();
					double weightedScore = signalStrength * weight;

					// 买入信号为正，卖出信号为负
					double direction = signal.isBuySignal() ? 1.0 : -1.0;
					double score = weightedScore * direction;

					symbolVotes.merge(symbol, score, Double::sum);
				}
			}

			// 根据加权得分生成最终信号
			for (Map.Entry<String, Double> entry : symbolVotes.entrySet()) {
				String symbol = entry.getKey();
				double totalScore = entry.getValue();
				double absScore = Math.abs(totalScore);

				if (absScore > 0.1) { // 阈值过滤
					String signalType = totalScore > 0 ? "BUY" : "SELL";
					double strength = Math.min(absScore, 1.0); // 限制在0-1之间

					SignalEvent combinedSignal = new SignalEvent(java.time.LocalDateTime.now(), symbol, signalType,
							strength, "CompositeStrategy");
					combinedSignals.add(combinedSignal);
				}
			}

			return combinedSignals;
		}
	}

	/**
	 * 权重优化方法枚举
	 */
	public enum WeightOptimizationMethod {
		EQUAL_WEIGHT, RISK_PARITY, MARKOWITZ, CUSTOM
	}

	/**
	 * 权重优化结果
	 */
	public static class WeightOptimizationResult {
		private WeightOptimizationMethod method;
		private Map<String, Double> optimizedWeights;
		private double expectedReturn;
		private double expectedRisk;
		private double diversificationScore;

		// Getter和Setter
		public WeightOptimizationMethod getMethod() {
			return method;
		}

		public void setMethod(WeightOptimizationMethod method) {
			this.method = method;
		}

		public Map<String, Double> getOptimizedWeights() {
			return optimizedWeights;
		}

		public void setOptimizedWeights(Map<String, Double> optimizedWeights) {
			this.optimizedWeights = optimizedWeights;
		}

		public double getExpectedReturn() {
			return expectedReturn;
		}

		public void setExpectedReturn(double expectedReturn) {
			this.expectedReturn = expectedReturn;
		}

		public double getExpectedRisk() {
			return expectedRisk;
		}

		public void setExpectedRisk(double expectedRisk) {
			this.expectedRisk = expectedRisk;
		}

		public double getDiversificationScore() {
			return diversificationScore;
		}

		public void setDiversificationScore(double diversificationScore) {
			this.diversificationScore = diversificationScore;
		}
	}

	/**
	 * 组合回测结果-- 标记1
	 */
	// 完整的 CompositeBacktestResult 类，包含所有必要的Setter方法
	public static class CompositeBacktestResult {
		private BacktestResult compositeResult;
		private Map<String, BacktestResult> individualResults;
		private Map<String, Double> strategyWeights;
		private double diversificationBenefit;
		private double riskReduction;
		private double consistencyImprovement;
		private Map<String, Double> strategyContributions;

		// ====================新添加方法======================
		// ==================== 修复：添加缺失的Setter方法 ====================
		public void setCompositeResult(BacktestResult compositeResult) {
			this.compositeResult = compositeResult;
		}

		public void setIndividualResults(Map<String, BacktestResult> individualResults) {
			this.individualResults = individualResults;
		}

		public void setStrategyWeights(Map<String, Double> strategyWeights) {
			this.strategyWeights = strategyWeights;
		}

		public void setDiversificationBenefit(double diversificationBenefit) {
			this.diversificationBenefit = diversificationBenefit;
		}

		public void setRiskReduction(double riskReduction) {
			this.riskReduction = riskReduction;
		}

		public void setConsistencyImprovement(double consistencyImprovement) {
			this.consistencyImprovement = consistencyImprovement;
		}

		public void setStrategyContributions(Map<String, Double> strategyContributions) {
			this.strategyContributions = strategyContributions;
		}

		// ==================== Getter方法 ====================
		public BacktestResult getCompositeResult() {
			return compositeResult;
		}

		public Map<String, BacktestResult> getIndividualResults() {
			return individualResults != null ? new HashMap<>(individualResults) : new HashMap<>();
		}

		public double getDiversificationBenefit() {
			return diversificationBenefit;
		}

		public double getRiskReduction() {
			return riskReduction;
		}

		public double getConsistencyImprovement() {
			return consistencyImprovement;
		}

		public Map<String, Double> getStrategyContributions() {
			return strategyContributions != null ? new HashMap<>(strategyContributions) : new HashMap<>();
		}

		/**
		 * 修复的计算方法
		 */
		public void calculateCompositeMetrics(Map<String, StrategyAllocation> strategies) {
			if (strategies == null)
				return;

			// 设置策略权重
			this.strategyWeights = new HashMap<>();
			for (Map.Entry<String, StrategyAllocation> entry : strategies.entrySet()) {
				strategyWeights.put(entry.getKey(), entry.getValue().getWeight());
			}

			// 计算分散化收益
			this.diversificationBenefit = calculateDiversificationBenefit();

			// 计算风险降低
			this.riskReduction = calculateRiskReduction();

			// 计算策略贡献度
			this.strategyContributions = calculateStrategyContributions(strategies);
		}

		private double calculateDiversificationBenefit() {
			if (compositeResult == null || individualResults == null || individualResults.isEmpty()) {
				return 0.0;
			}

			double weightedIndividualReturn = 0.0;
			for (BacktestResult result : individualResults.values()) {
				weightedIndividualReturn += result.getTotalReturn();
			}
			weightedIndividualReturn /= individualResults.size();

			double compositeReturn = compositeResult.getTotalReturn();
			return compositeReturn - weightedIndividualReturn;
		}

		private double calculateRiskReduction() {
			if (compositeResult == null || individualResults == null || individualResults.isEmpty()) {
				return 0.0;
			}

			double maxIndividualDrawdown = individualResults.values().stream()
					.mapToDouble(BacktestResult::getMaxDrawdown).max().orElse(0.0);

			double compositeDrawdown = compositeResult.getMaxDrawdown();
			return maxIndividualDrawdown - compositeDrawdown;
		}

		private Map<String, Double> calculateStrategyContributions(Map<String, StrategyAllocation> strategies) {
			Map<String, Double> contributions = new HashMap<>();

			if (individualResults == null || strategies == null) {
				return contributions;
			}

			for (Map.Entry<String, BacktestResult> entry : individualResults.entrySet()) {
				String strategyName = entry.getKey();
				StrategyAllocation allocation = strategies.get(strategyName);
				BacktestResult result = entry.getValue();

				if (allocation != null && result != null) {
					double contribution = result.getTotalReturn() * allocation.getWeight();
					contributions.put(strategyName, contribution);
				}
			}

			return contributions;
		}

		public void printReport() {
			// System.out.println("\n" + "=".repeat(100));
			System.out.println(Stream.generate(() -> "\n" + "=").limit(100).collect(Collectors.joining()));
			System.out.println("📊 策略组合分析报告");
			// System.out.println("=".repeat(100));
			// System.out.println(Stream.generate(() ->
			// "=").limit(100).collect(Collectors.joining()));
			if (compositeResult != null) {
				System.out.printf("组合总收益率: %.2f%%%n", compositeResult.getTotalReturn());
				System.out.printf("组合夏普比率: %.2f%n", compositeResult.getSharpeRatio());
				System.out.printf("组合最大回撤: %.2f%%%n", compositeResult.getMaxDrawdown());
				System.out.printf("分散化收益: %.2f%%%n", diversificationBenefit);
				System.out.printf("风险降低: %.2f%%%n", riskReduction);
			}

			System.out.println("\n各策略表现:");
			System.out.println("策略名称        | 权重%  | 单独收益率% | 对组合贡献% | 夏普比率");
			System.out.println("----------------|--------|-------------|-------------|----------");

			if (individualResults != null && strategyContributions != null && strategyWeights != null) {
				for (Map.Entry<String, BacktestResult> entry : individualResults.entrySet()) {
					String strategyName = entry.getKey();
					BacktestResult result = entry.getValue();
					double weight = strategyWeights.getOrDefault(strategyName, 0.0) * 100;
					double contribution = strategyContributions.getOrDefault(strategyName, 0.0);

					System.out.printf("%-14s | %6.1f | %11.2f | %11.2f | %8.2f%n", strategyName, weight,
							result.getTotalReturn(), contribution, result.getSharpeRatio());
				}
			}

			/// System.out.println("=".repeat(100));
			System.out.println(Stream.generate(() -> "=").limit(100).collect(Collectors.joining()));
		}
	}

	// 标记2

	// ==================== Getter和Setter ====================

	public void setCompositeConfig(CompositeConfig compositeConfig) {
		this.compositeConfig = compositeConfig;
	}

	public void setSignalCombiner(SignalCombiner signalCombiner) {
		this.signalCombiner = signalCombiner;
	}

	public Map<String, StrategyAllocation> getStrategies() {
		return new HashMap<>(strategies);
	}
}