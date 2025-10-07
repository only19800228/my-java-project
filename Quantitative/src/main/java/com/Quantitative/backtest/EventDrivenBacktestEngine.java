package com.Quantitative.backtest;//新增加

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.Quantitative.common.monitor.MonitorUtils;
import com.Quantitative.common.monitor.UnifiedMonitorManager;
import com.Quantitative.common.utils.MemoryMonitor;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.EnhancedSignalEvent;
import com.Quantitative.core.events.Event;
import com.Quantitative.core.events.EventBus;
import com.Quantitative.core.events.FillEvent;
import com.Quantitative.core.events.OrderEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.core.interfaces.EventProcessor;
import com.Quantitative.data.DataFeed;
import com.Quantitative.execution.ExecutionEngine;
import com.Quantitative.execution.SimulatedExecution;
import com.Quantitative.portfolio.Portfolio;
import com.Quantitative.portfolio.Position;
import com.Quantitative.portfolio.RiskManager;
import com.Quantitative.portfolio.StopLossTakeProfitManager;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 修复监控管理器集成的回测引擎
 */
public class EventDrivenBacktestEngine {
	private long startTime;
	// 现有字段...
	private final EventBus eventBus;
	private final DataFeed dataFeed;
	private BaseStrategy strategy;
	private Portfolio portfolio;
	private ExecutionEngine executionEngine;
	private BacktestResult result;
	private BacktestConfig config;
	private boolean isRunning = false;

	// 新增：统一监控管理器
	private final UnifiedMonitorManager monitorManager = UnifiedMonitorManager.getInstance();

	// 风险管理器
	private RiskManager riskManager;
	private StopLossTakeProfitManager riskControlManager;

	// 统计字段...
	private int totalBarsProcessed = 0;
	private int totalSignalsGenerated = 0;
	private int totalSignalsRejectedByRisk = 0;
	private int totalOrdersExecuted = 0;
	private int totalRiskChecks = 0;

	// 事件统计
	private int barEventsProcessed = 0;
	private int signalEventsProcessed = 0;
	private int orderEventsProcessed = 0;
	private int fillEventsProcessed = 0;
	private int totalEventProcessed = 0;

	// 构造函数保持不变...
	public EventDrivenBacktestEngine(DataFeed dataFeed, BacktestConfig config) {
		this.eventBus = new EventBus();
		this.dataFeed = dataFeed;
		this.config = config;
		initializeEventBus();
	}

	/**
	 * 修复事件总线注册
	 */

	private void initializeEventBus() {
		eventBus.setDebugMode(config.isDebugMode());

		// 只注册具体事件类型的处理器，避免重复
		eventBus.registerProcessor(BarEvent.class, new BarEventProcessor());
		eventBus.registerProcessor(SignalEvent.class, new SignalEventProcessor());
		eventBus.registerProcessor(OrderEvent.class, new OrderEventProcessor());
		eventBus.registerProcessor(FillEvent.class, new FillEventProcessor());

		// 移除对 Event.class 的通用注册，避免重复处理
	}

	/**
	 * 运行回测 - 集成监控的修复版本
	 */
	public BacktestResult runBacktest() {
		System.out.println("=== 开始事件驱动回测 ===");

		// 开始监控
		String backtestId = config.getSymbol() + "_" + System.currentTimeMillis();
		monitorManager.startBacktestMonitoring(backtestId);

		startTime = System.currentTimeMillis();

		try {
			// 1. 加载数据
			MonitorUtils.monitorVoid("BacktestEngine", "loadData", () -> {
				loadData();
			});

			// 2. 初始化组件
			MonitorUtils.monitorVoid("BacktestEngine", "initializeComponents", () -> {
				initializeComponents();
			});

			// 3. 运行事件循环
			runEventLoopWithMonitoring();

			// 4. 结束处理
			MonitorUtils.monitorVoid("BacktestEngine", "finishBacktest", () -> {
				finishBacktest();
			});

			long duration = System.currentTimeMillis() - startTime;
			System.out.printf("? 回测完成! 总耗时: %.2f秒%n", duration / 1000.0);

			return result;

		} catch (Exception e) {
			System.err.println("回测执行失败: " + e.getMessage());
			e.printStackTrace();
			throw e;
		} finally {
			// 停止监控
			monitorManager.stopBacktestMonitoring();
		}
	}

	/**
	 * 带监控的事件循环 - 修复版本
	 */
	private void runEventLoopWithMonitoring() {
		System.out.println("步骤3: 开始事件驱动循环...");

		dataFeed.reset();
		List<BarEvent> allBars = dataFeed.getAllBars();
		int totalBars = allBars.size();
		int barCount = 0;
		long totalProcessingTime = 0;

		// 进度跟踪
		int progressInterval = Math.max(1, totalBars / 20);

		for (int i = 0; i < totalBars && isRunning; i++) {
			// 使用操作跟踪器监控每个Bar的处理
			MonitorUtils.OperationTracker tracker = MonitorUtils.startTracking("EventLoop", "processBar");

			try {
				BarEvent bar = allBars.get(i);
				barCount++;
				totalBarsProcessed++;

				// 1. 首先检查止损止盈
				checkAndExecuteRiskControls(bar);

				// 2. 处理Bar事件（生成新信号）
				eventBus.publish(bar);

				long processingTime = tracker.completeAndGetDuration();
				totalProcessingTime += processingTime;

				// 记录Bar处理到监控管理器
				monitorManager.recordBarProcessing(processingTime);

				// 智能进度显示
				if (barCount % progressInterval == 0 || barCount <= 10 || barCount == totalBars) {
					displayEnhancedProgress(barCount, totalBars, totalProcessingTime);

					// 定期检查内存
					if (barCount % 1000 == 0) {
						MemoryMonitor.printMemoryUsage("处理Bar中: " + barCount + "/" + totalBars);
					}
				}

				// 检查停止条件
				if (config.getMaxBars() > 0 && barCount >= config.getMaxBars()) {
					System.out.println("? 达到最大Bar数量限制，停止回测");
					break;
				}

			} catch (Exception e) {
				tracker.complete(); // 确保异常时也记录
				System.err.printf("处理Bar失败: %s%n", e.getMessage());
				if (config.isDebugMode()) {
					e.printStackTrace();
				}
			}
		}

		double avgProcessingTime = totalBars > 0 ? totalProcessingTime / (totalBars * 1_000_000.0) : 0;
		System.out.printf("? 事件循环完成，处理了 %d 个Bar，平均处理时间: %.3fms%n", barCount, avgProcessingTime);
	}

	/**
	 * 增强的Bar事件处理器 - 集成监控
	 */
	private class BarEventProcessor implements EventProcessor {
		@Override
		public void processEvent(Event event) {
			if (event instanceof BarEvent) {
				// 使用监控工具监控Bar处理
				MonitorUtils.monitorVoid("BarEventProcessor", "processBarEvent", () -> {
					BarEvent bar = (BarEvent) event;
					barEventsProcessed++;
					totalEventProcessed++;

					try {
						// 更新市场价格
						if (portfolio != null) {
							portfolio.updateMarketPrice(bar.getSymbol(), bar.getClose());
						}

						// 策略生成信号
						if (strategy != null) {
							List<SignalEvent> signals = strategy.onBar(bar);
							totalSignalsGenerated += signals.size();

							// 发布所有信号事件
							for (SignalEvent signal : signals) {
								eventBus.publish(signal);
							}

							if (config.isDebugMode() && !signals.isEmpty()) {
								System.out.printf("[Bar处理器] %s 生成 %d 个信号%n", bar.getTimestamp().toLocalDate(),
										signals.size());
							}
						}

					} catch (Exception e) {
						System.err.printf("[Bar处理器] 处理Bar事件失败: %s%n", e.getMessage());
						e.printStackTrace();
					}
				});
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return BarEvent.class;
		}

		// TradingComponent 接口方法...
		@Override
		public void initialize() {
		}

		@Override
		public void configure(Map<String, Object> config) {
		}

		@Override
		public String getName() {
			return "BarEventProcessor";
		}

		@Override
		public String getStatus() {
			return "RUNNING";
		}

		@Override
		public void reset() {
		}

		@Override
		public void shutdown() {
		}
	}

	/**
	 * 增强的信号事件处理器 - 集成监控
	 */
	private class SignalEventProcessor implements EventProcessor {
		@Override
		public void processEvent(Event event) {
			if (event instanceof SignalEvent) {
				// 使用监控工具监控信号处理
				MonitorUtils.monitorVoid("SignalEventProcessor", "processSignal", () -> {
					SignalEvent signal = (SignalEvent) event;
					signalEventsProcessed++;
					totalEventProcessed++;

					try {
						// 风险验证
						RiskManager.RiskValidationResult riskResult = riskManager.validateSignal(signal);
						totalRiskChecks++;

						if (!riskResult.isValid()) {
							totalSignalsRejectedByRisk++;
							if (config.isDebugMode()) {
								System.out.printf("[信号处理器] 风险拒绝: %s - %s%n", signal.getSymbol(),
										riskResult.getMessage());
							}
							return;
						}

						// 如果是增强信号，应用风险管理
						if (signal instanceof EnhancedSignalEvent) {
							EnhancedSignalEvent enhancedSignal = (EnhancedSignalEvent) signal;
							riskControlManager.applyRiskFromSignal(enhancedSignal);
						}

						// 投资组合生成订单
						if (portfolio != null) {
							OrderEvent order = portfolio.processSignal(signal);
							if (order != null) {
								eventBus.publish(order);

								if (config.isDebugMode()) {
									System.out.printf("[信号处理器] 生成订单: %s %s %d股%n", order.getSymbol(),
											order.getDirection(), order.getQuantity());
								}
							}
						}

					} catch (Exception e) {
						System.err.printf("[信号处理器] 处理信号事件失败: %s%n", e.getMessage());
						e.printStackTrace();
					}
				});
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return SignalEvent.class;
		}

		// TradingComponent 接口方法...
		@Override
		public void initialize() {
		}

		@Override
		public void configure(Map<String, Object> config) {
		}

		@Override
		public String getName() {
			return "SignalEventProcessor";
		}

		@Override
		public String getStatus() {
			return "RUNNING";
		}

		@Override
		public void reset() {
		}

		@Override
		public void shutdown() {
		}
	}

	/**
	 * 订单事件处理器 - 集成监控
	 */
	private class OrderEventProcessor implements EventProcessor {
		@Override
		public void processEvent(Event event) {
			if (event instanceof OrderEvent) {
				MonitorUtils.monitorVoid("OrderEventProcessor", "processOrder", () -> {
					OrderEvent order = (OrderEvent) event;
					orderEventsProcessed++;
					totalEventProcessed++;

					try {
						// 执行引擎执行订单
						if (executionEngine != null) {
							FillEvent fill = executionEngine.executeOrder(order);
							if (fill != null) {
								totalOrdersExecuted++;
								eventBus.publish(fill);

								if (config.isDebugMode()) {
									System.out.printf("[订单处理器] 订单成交: %s %s %d股 @%.2f%n", fill.getSymbol(),
											fill.getDirection(), fill.getQuantity(), fill.getFillPrice());
								}
							} else {
								System.out.printf("[订单处理器] 订单执行失败: %s%n", order);
							}
						}

					} catch (Exception e) {
						System.err.printf("[订单处理器] 处理订单事件失败: %s%n", e.getMessage());
						e.printStackTrace();
					}
				});
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return OrderEvent.class;
		}

		// TradingComponent 接口方法...
		@Override
		public void initialize() {
		}

		@Override
		public void configure(Map<String, Object> config) {
		}

		@Override
		public String getName() {
			return "OrderEventProcessor";
		}

		@Override
		public String getStatus() {
			return "RUNNING";
		}

		@Override
		public void reset() {
		}

		@Override
		public void shutdown() {
		}
	}

	/**
	 * 成交事件处理器 - 集成监控
	 */
	private class FillEventProcessor implements EventProcessor {
		@Override
		public void processEvent(Event event) {
			if (event instanceof FillEvent) {
				MonitorUtils.monitorVoid("FillEventProcessor", "processFill", () -> {
					FillEvent fill = (FillEvent) event;
					fillEventsProcessed++;
					totalEventProcessed++;

					try {
						// 投资组合处理成交
						if (portfolio != null) {
							portfolio.processFill(fill);
						}

						// 记录到回测结果
						if (result != null) {
							result.addTrade(fill);
							if (portfolio != null) {
								result.addEquityPoint(portfolio.getTotalValue());
							}
						}

						if (config.isDebugMode()) {
							System.out.printf("[成交处理器] 成交处理完成: %s %s %d股%n", fill.getSymbol(), fill.getDirection(),
									fill.getQuantity());
						}

					} catch (Exception e) {
						System.err.printf("[成交处理器] 处理成交事件失败: %s%n", e.getMessage());
						e.printStackTrace();
					}
				});
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return FillEvent.class;
		}

		// TradingComponent 接口方法...
		@Override
		public void initialize() {
		}

		@Override
		public void configure(Map<String, Object> config) {
		}

		@Override
		public String getName() {
			return "FillEventProcessor";
		}

		@Override
		public String getStatus() {
			return "RUNNING";
		}

		@Override
		public void reset() {
		}

		@Override
		public void shutdown() {
		}
	}

	// ==================== 其他现有方法保持不变 ====================

	/**
	 * 检查并执行止损止盈
	 */
	private void checkAndExecuteRiskControls(BarEvent bar) {
		if (riskControlManager == null || !riskControlManager.isEnabled()) {
			return;
		}

		MonitorUtils.monitorVoid("RiskControl", "checkStopLossTakeProfit", () -> {
			String symbol = bar.getSymbol();
			double currentPrice = bar.getClose();

			// 检查止损止盈条件
			List<StopLossTakeProfitManager.ExitSignal> exitSignals = riskControlManager.checkRiskControls(symbol,
					currentPrice);

			// 执行退出信号
			for (StopLossTakeProfitManager.ExitSignal exitSignal : exitSignals) {
				executeExitSignal(exitSignal, bar.getTimestamp());
			}
		});
	}

	/**
	 * 执行退出信号 - 完整实现
	 */
	private void executeExitSignal(StopLossTakeProfitManager.ExitSignal exitSignal, LocalDateTime timestamp) {
		try {
			String symbol = exitSignal.getSymbol();
			Position position = portfolio.getPositions().get(symbol);

			if (position != null && position.getQuantity() > 0) {
				int quantity = position.getQuantity();
				double exitPrice = exitSignal.getExitPrice();

				// 生成平仓订单
				OrderEvent exitOrder = new OrderEvent(timestamp, symbol, "SELL", quantity, exitPrice, "MARKET");

				// 执行订单
				if (executionEngine != null) {
					FillEvent fill = executionEngine.executeOrder(exitOrder);
					if (fill != null) {
						portfolio.processFill(fill);

						// 更新回测结果
						if (result != null) {
							result.addTrade(fill);
							result.addEquityPoint(portfolio.getTotalValue());
						}

						System.out.printf("? %s 执行%s: %d股 @%.2f%n", symbol, exitSignal.getReason(), quantity,
								exitPrice);

						// 清除该持仓的风险设置
						riskControlManager.clearPositionRisk(symbol);
					}
				}
			}
		} catch (Exception e) {
			System.err.printf("? 执行退出信号失败: %s - %s%n", exitSignal, e.getMessage());
		}
	}

	/**
	 * 初始化组件 - 集成监控
	 */
	private void initializeComponents() {
		MonitorUtils.monitorVoid("BacktestEngine", "initializeComponents", () -> {
			System.out.println("步骤2: 初始化组件...");

			// 1. 先初始化投资组合
			this.portfolio = new Portfolio(config.getInitialCapital());

			// 2. 初始化止损止盈管理器
			this.riskControlManager = new StopLossTakeProfitManager(portfolio);

			// 3. 初始化风险管理器
			if (riskManager == null) {
				this.riskManager = new RiskManager();
			}
			riskManager.setPortfolio(portfolio);
			riskManager.configure(config.getRiskParams());
			riskManager.initialize();

			// 4. 初始化策略
			if (strategy != null) {
				strategy.setDataFeed(dataFeed);
				strategy.setPortfolio(portfolio);
				strategy.initialize();
			}

			// 5. 初始化执行引擎
			if (executionEngine == null) {
				this.executionEngine = new SimulatedExecution();
			}

			// 6. 初始化结果
			this.result = new BacktestResult();
			result.setInitialCapital(config.getInitialCapital());
			result.addEquityPoint(config.getInitialCapital());

			System.out.println("? 所有组件初始化完成");
		});
	}

	/**
	 * 加载数据
	 */
	private void loadData() {
		System.out.println("步骤1: 加载历史数据...");
		dataFeed.loadHistoricalData(config.getSymbol(), config.getStartDate(), config.getEndDate());

		List<BarEvent> bars = dataFeed.getAllBars();
		if (bars.isEmpty()) {
			throw new IllegalStateException("没有加载到任何数据！");
		}

		System.out.printf("? 数据加载: %d条记录, 时间范围: %s 到 %s%n", bars.size(), bars.get(0).getTimestamp().toLocalDate(),
				bars.get(bars.size() - 1).getTimestamp().toLocalDate());
	}

	/**
	 * 结束回测
	 */
	private void finishBacktest() {
		System.out.println("步骤4: 结束处理...");

		// 强制平仓
		forceCloseAllPositions();

		// 计算最终结果
		calculateFinalResults();

		// 打印统计信息
		printStatistics();

		isRunning = false;
		System.out.println("=== 回测完成 ===");
	}

	/**
	 * 强制平仓 --修复强制平仓逻辑
	 */
	private void forceCloseAllPositions() {
		if (portfolio == null || portfolio.getPositions().isEmpty()) {
			System.out.println("? 没有持仓需要平仓");
			return;
		}

		System.out.println("? 执行强制平仓...");
		Map<String, Position> positions = portfolio.getPositions();
		int closedPositions = 0;

		// ? 修复：使用最后一条Bar的价格作为平仓价格
		List<BarEvent> allBars = dataFeed.getAllBars();
		if (allBars.isEmpty()) {
			System.out.println("? 警告：没有价格数据，无法平仓");
			return;
		}

		// 获取最后的价格数据
		BarEvent lastBar = allBars.get(allBars.size() - 1);
		double defaultPrice = lastBar.getClose();

		for (Map.Entry<String, Position> entry : positions.entrySet()) {
			String symbol = entry.getKey();
			Position position = entry.getValue();

			if (position.getQuantity() > 0) {
				try {
					// ? 改进：优先使用投资组合中的当前价格，否则使用默认价格
					Double currentPrice = portfolio.getCurrentPrice(symbol);
					if (currentPrice == null || currentPrice <= 0) {
						currentPrice = defaultPrice;
						System.out.printf("? 使用默认价格平仓 %s: %.2f%n", symbol, currentPrice);
					}

					// 生成平仓订单
					OrderEvent closeOrder = new OrderEvent(java.time.LocalDateTime.now(), symbol, "SELL",
							position.getQuantity(), currentPrice, "MARKET");

					// 执行平仓订单
					if (executionEngine != null) {
						FillEvent fill = executionEngine.executeOrder(closeOrder);
						if (fill != null) {
							portfolio.processFill(fill);
							closedPositions++;
							System.out.printf("? 平仓成功: %s %d股 @%.2f%n", symbol, position.getQuantity(),
									fill.getFillPrice());
						} else {
							System.out.printf("? 平仓失败: %s 订单未成交%n", symbol);
						}
					}

				} catch (Exception e) {
					System.err.printf("? %s 平仓失败: %s%n", symbol, e.getMessage());
				}
			}
		}

		System.out.printf("? 平仓完成: 共平仓 %d 个品种%n", closedPositions);
	}

	/**
	 * 计算最终结果 - 完整实现
	 */
	private void calculateFinalResults() {
		System.out.println("? 计算最终回测结果...");

		if (portfolio != null) {
			double finalCapital = portfolio.getTotalValue();
			double totalReturn = portfolio.getTotalReturn();

			result.setFinalCapital(finalCapital);
			result.setTotalReturn(totalReturn);

			// 计算高级指标
			result.calculateAdvancedMetrics();

			// 记录详细的投资组合统计
			recordPortfolioStatistics();

			System.out.printf("? 最终结果: 初始资金=%,.2f, 最终资金=%,.2f, 总收益率=%.2f%%%n", result.getInitialCapital(), finalCapital,
					totalReturn);
		} else {
			System.out.println("? 警告: 投资组合未初始化，无法计算最终结果");
		}
	}

	/**
	 * 记录投资组合详细统计
	 */
	private void recordPortfolioStatistics() {
		if (portfolio == null)
			return;

		Map<String, Object> stats = portfolio.getPortfolioStatistics();
		System.out.println("? 投资组合统计:");
		stats.forEach((key, value) -> System.out.printf("  %s: %s%n", key, value));

		// 记录持仓详情
		if (!portfolio.getPositions().isEmpty()) {
			System.out.println("? 最终持仓情况:");
			portfolio.getPositions().forEach((symbol, position) -> System.out.printf("  %s: %d股, 成本=%.2f, 市值=%.2f%n",
					symbol, position.getQuantity(), position.getAvgCost(), position.getMarketValue()));
		}
	}

	/**
	 * 增强的进度显示 - 修复参数问题
	 */
	private void displayEnhancedProgress(int currentBar, int totalBars, long totalProcessingTime) {
		double currentValue = portfolio != null ? portfolio.getTotalValue() : config.getInitialCapital();
		double returnPercent = portfolio != null ? portfolio.getTotalReturn() : 0.0;
		double progress = (double) currentBar / totalBars * 100;

		// 计算处理速度
		double avgSpeed = totalProcessingTime > 0 ? (currentBar * 1_000_000_000.0) / totalProcessingTime : 0;

		// 预估剩余时间
		long remainingBars = totalBars - currentBar;
		double estimatedRemainingTime = avgSpeed > 0 ? remainingBars / avgSpeed : 0;

		System.out.printf("进度: %d/%d Bars (%.1f%%) | 速度: %.1f Bar/s | 预计剩余: %.1fs | 总资产: %,.2f(%.2f%%)%n", currentBar,
				totalBars, progress, avgSpeed, estimatedRemainingTime, currentValue, returnPercent);
	}

	/**
	 * 打印统计信息 - 增强版本
	 */
	private void printStatistics() {
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		System.out.println("\n=== 回测详细统计 ===");
		System.out.printf("总处理时间: %.2f 秒%n", duration / 1000.0);
		System.out.printf("处理Bar数量: %d%n", totalBarsProcessed);

		// 修复：显示正确的事件统计
		System.out.printf("事件处理统计: Bar=%d, Signal=%d, Order=%d, Fill=%d, Total=%d%n", barEventsProcessed,
				signalEventsProcessed, orderEventsProcessed, fillEventsProcessed, totalEventProcessed);

		System.out.printf("生成信号数量: %d%n", totalSignalsGenerated);
		System.out.printf("风险拒绝信号: %d%n", totalSignalsRejectedByRisk);
		System.out.printf("执行订单数量: %d%n", totalOrdersExecuted);
		System.out.printf("风险检查次数: %d%n", totalRiskChecks);
		System.out.printf("处理效率: %.1f Bar/秒%n", totalBarsProcessed / (duration / 1000.0));

		// 信号效率
		if (totalBarsProcessed > 0) {
			double signalEfficiency = (double) totalSignalsGenerated / totalBarsProcessed;
			System.out.printf("信号效率: %.2f 信号/Bar%n", signalEfficiency);
		}

		// 风险控制效果
		if (totalSignalsGenerated > 0) {
			double riskRejectionRate = (double) totalSignalsRejectedByRisk / totalSignalsGenerated * 100;
			System.out.printf("风险拒绝率: %.1f%%%n", riskRejectionRate);
		}

		// 订单执行率
		if (totalSignalsGenerated > 0) {
			double orderExecutionRate = (double) totalOrdersExecuted
					/ (totalSignalsGenerated - totalSignalsRejectedByRisk) * 100;
			System.out.printf("订单执行率: %.1f%%%n", orderExecutionRate);
		}

		// 事件处理分布
		if (totalEventProcessed > 0) {
			System.out.println("事件处理分布:");
			System.out.printf("  Bar事件: %.1f%%%n", (double) barEventsProcessed / totalEventProcessed * 100);
			System.out.printf("  信号事件: %.1f%%%n", (double) signalEventsProcessed / totalEventProcessed * 100);
			System.out.printf("  订单事件: %.1f%%%n", (double) orderEventsProcessed / totalEventProcessed * 100);
			System.out.printf("  成交事件: %.1f%%%n", (double) fillEventsProcessed / totalEventProcessed * 100);
		}
	}

	// Getter和Setter方法...

	/**
	 * 获取配置
	 */
	public BacktestConfig getConfig() {
		return this.config;
	}

	public void setStrategy(BaseStrategy strategy) {
		this.strategy = strategy;
	}

	public void setExecutionEngine(ExecutionEngine executionEngine) {
		this.executionEngine = executionEngine;
	}

	public BacktestResult getResult() {
		return result;
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	// ... 其他Getter/Setter

	public StopLossTakeProfitManager getRiskControlManager() {
		return riskControlManager;
	}

	public void setRiskControlManager(StopLossTakeProfitManager riskControlManager) {
		this.riskControlManager = riskControlManager;
	}

	public void stop() {
		this.isRunning = false;
	}

	public DataFeed getDataFeed() {
		// TODO Auto-generated method stub
		return dataFeed;
	}

	public Portfolio getPortfolio() {
		// TODO Auto-generated method stub
		return portfolio;
	}
}