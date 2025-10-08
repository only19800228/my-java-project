package com.Quantitative.backtest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.Quantitative.common.exception.ExceptionHandler;
import com.Quantitative.common.monitor.UnifiedMonitorManager;
import com.Quantitative.common.utils.LogUtils;
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
 * 事件驱动回测引擎 - 集成统一监控版本
 */
public class EventDrivenBacktestEngine {
	private final EventBus eventBus;
	private final DataFeed dataFeed;
	private final BacktestConfig config;

	private BaseStrategy strategy;
	private Portfolio portfolio;
	private ExecutionEngine executionEngine;
	private RiskManager riskManager;
	private StopLossTakeProfitManager riskControlManager;
	private BacktestResult result;

	private boolean isRunning = false;
	private long startTime;

	// 统一监控管理器
	private UnifiedMonitorManager monitorManager;

	// 统计信息
	private int totalBarsProcessed = 0;
	private int totalSignalsGenerated = 0;
	private int totalOrdersExecuted = 0;
	private int barEventsProcessed = 0;
	private int signalEventsProcessed = 0;
	private int orderEventsProcessed = 0;
	private int fillEventsProcessed = 0;

	private static final Logger logger = LogUtils.getLogger(EventDrivenBacktestEngine.class);

	public EventDrivenBacktestEngine(DataFeed dataFeed, BacktestConfig config) {
		this.eventBus = new EventBus();
		this.dataFeed = dataFeed;
		this.config = config;

		// 初始化统一监控
		this.monitorManager = UnifiedMonitorManager.getInstance();

		initializeEventBus();
	}

	/**
	 * 初始化事件总线 - 简化版本
	 */
	private void initializeEventBus() {
		eventBus.setDebugMode(config.isDebugMode());

		// 注册事件处理器 - 修复：同时注册 SignalEvent 和 EnhancedSignalEvent
		eventBus.registerProcessor(BarEvent.class, new BarEventProcessor());
		eventBus.registerProcessor(SignalEvent.class, new SignalEventProcessor());
		eventBus.registerProcessor(EnhancedSignalEvent.class, new SignalEventProcessor()); // 新增这行
		eventBus.registerProcessor(OrderEvent.class, new OrderEventProcessor());
		eventBus.registerProcessor(FillEvent.class, new FillEventProcessor());

		System.out.println("🔍 [事件总线注册] 完成:");
		System.out.println("  - BarEvent: ✓");
		System.out.println("  - SignalEvent: ✓");
		System.out.println("  - EnhancedSignalEvent: ✓"); // 新增
		System.out.println("  - OrderEvent: ✓");
		System.out.println("  - FillEvent: ✓");
	}

	/**
	 * 运行回测 - 核心方法
	 */
	public BacktestResult runBacktest() {
		logger.info("=== 开始事件驱动回测 ===");
		logger.info("时间范围: {} 到 {}", config.getStartDate(), config.getEndDate());
		logger.info("初始资金: {,.2f}", config.getInitialCapital());

		return ExceptionHandler.executeSafely(() -> {
			startTime = System.currentTimeMillis();
			isRunning = true;

			// 启动监控
			startMonitoring();

			// 核心执行流程
			loadData();
			initializeComponents();
			runEventLoop();
			finishBacktest();

			// 停止监控
			stopMonitoring();

			return result;
		}, null, "runBacktest");
	}

	/**
	 * 启动统一监控
	 */
	private void startMonitoring() {
		if (monitorManager != null) {
			String backtestId = config.getSymbol() + "_" + System.currentTimeMillis();
			monitorManager.startBacktestMonitoring(backtestId);
			logger.info("统一监控已启动: {}", backtestId);
		}
	}

	/**
	 * 停止监控
	 */
	private void stopMonitoring() {
		if (monitorManager != null) {
			monitorManager.stopBacktestMonitoring();
			logger.info("统一监控已停止");
		}
	}

	/**
	 * 加载数据 - 简化版本
	 */
	private void loadData() {
		logger.info("步骤1: 加载历史数据...");

		long startTime = System.currentTimeMillis();
		dataFeed.loadHistoricalData(config.getSymbol(), config.getStartDate(), config.getEndDate());
		long loadTime = System.currentTimeMillis() - startTime;

		List<BarEvent> bars = dataFeed.getAllBars();
		if (bars.isEmpty()) {
			throw new IllegalStateException("没有加载到任何数据！");
		}

		// 记录监控数据 - 使用recordOperation方法
		if (monitorManager != null) {
			monitorManager.recordOperation("DataFeed", "loadHistoricalData", loadTime * 1_000_000L);
		}

		logger.info("数据加载: {}条记录, 耗时: {}ms", bars.size(), loadTime);
	}

	/**
	 * 初始化组件 - 简化版本
	 */
	private void initializeComponents() {
		logger.info("步骤2: 初始化组件...");

		// 1. 投资组合
		this.portfolio = new Portfolio(config.getInitialCapital());

		// 2. 止损止盈管理器
		this.riskControlManager = new StopLossTakeProfitManager(portfolio);

		// 3. 风险管理器
		this.riskManager = new RiskManager();
		riskManager.setPortfolio(portfolio);
		riskManager.configure(config.getRiskParams());
		riskManager.initialize();

		// 4. 执行引擎
		this.executionEngine = new SimulatedExecution();

		// 5. 回测结果
		this.result = new BacktestResult();
		result.setInitialCapital(config.getInitialCapital());
		result.addEquityPoint(config.getInitialCapital());

		logger.info("所有组件初始化完成");
	}

	/**
	 * 运行事件循环 - 修复版本
	 */
	private void runEventLoop() {
		logger.info("步骤3: 开始事件驱动循环...");

		List<BarEvent> allBars = dataFeed.getAllBars();
		int totalBars = allBars.size();
		int barCount = 0;

		logger.info("准备处理 {} 个Bar数据", totalBars);

		for (int i = 0; i < totalBars && isRunning; i++) {
			long barStartTime = System.nanoTime();

			BarEvent bar = allBars.get(i);
			barCount++;
			totalBarsProcessed++;

			// 调试信息：确认Bar数据有效
			if (barCount <= 3) {
				logger.debug("处理第 {} 个Bar: {} - 收盘价: {}", barCount, bar.getTimestamp(), bar.getClose());
			}

			// 1. 检查止损止盈
			checkAndExecuteRiskControls(bar);

			// 2. 发布Bar事件到事件总线
			eventBus.publish(bar);

			// 记录Bar处理时间
			long barProcessingTime = System.nanoTime() - barStartTime;
			if (monitorManager != null) {
				monitorManager.recordBarProcessing(barProcessingTime);
			}

			// 进度显示
			if (barCount % 50 == 0 || barCount <= 5 || barCount == totalBars) {
				displayProgress(barCount, totalBars);
			}

			if (config.getMaxBars() > 0 && barCount >= config.getMaxBars()) {
				logger.info("达到最大Bar数量限制，停止回测");
				break;
			}
		}

		logger.info("事件循环完成，处理了 {} 个Bar", barCount);
	}

	/**
	 * 显示进度
	 */
	private void displayProgress(int currentBar, int totalBars) {
		double progress = (double) currentBar / totalBars * 100;
		double currentValue = portfolio != null ? portfolio.getTotalValue() : config.getInitialCapital();

		// 修复：使用正确的格式化语法
		System.out.printf("进度: %d/%d Bars (%.1f%%) | 总资产: %,.2f%n", currentBar, totalBars, progress, currentValue);

		// 或者如果坚持用logger，使用正确的占位符
		logger.info("进度: {}/{} Bars ({:.1f}%) | 总资产: {,.2f}", currentBar, totalBars, progress, currentValue);
	}

	/**
	 * 检查止损止盈
	 */
	private void checkAndExecuteRiskControls(BarEvent bar) {
		if (riskControlManager == null || !riskControlManager.isEnabled()) {
			return;
		}

		String symbol = bar.getSymbol();
		double currentPrice = bar.getClose();

		List<StopLossTakeProfitManager.ExitSignal> exitSignals = riskControlManager.checkRiskControls(symbol,
				currentPrice);

		for (StopLossTakeProfitManager.ExitSignal exitSignal : exitSignals) {
			executeExitSignal(exitSignal, bar.getTimestamp());
		}
	}

	/**
	 * 执行退出信号
	 */
	private void executeExitSignal(StopLossTakeProfitManager.ExitSignal exitSignal, LocalDateTime timestamp) {
		try {
			String symbol = exitSignal.getSymbol();
			Position position = portfolio.getPositions().get(symbol);

			if (position != null && position.getQuantity() > 0) {
				int quantity = position.getQuantity();
				double exitPrice = exitSignal.getExitPrice();

				OrderEvent exitOrder = new OrderEvent(timestamp, symbol, "SELL", quantity, exitPrice, "MARKET");

				if (executionEngine != null) {
					FillEvent fill = executionEngine.executeOrder(exitOrder);
					if (fill != null) {
						portfolio.processFill(fill);

						if (result != null) {
							result.addTrade(fill);
							result.addEquityPoint(portfolio.getTotalValue());
						}

						logger.info("{} 执行{}: {}股 @{:.2f}", symbol, exitSignal.getReason(), quantity, exitPrice);

						riskControlManager.clearPositionRisk(symbol);
					}
				}
			}
		} catch (Exception e) {
			logger.error("执行退出信号失败: {} - {}", exitSignal, e.getMessage());
		}
	}

	/**
	 * 结束回测
	 */
	private void finishBacktest() {
		logger.info("步骤4: 结束处理...");

		forceCloseAllPositions();
		calculateFinalResults();
		printStatistics();

		isRunning = false;
		logger.info("=== 回测完成 ===");
	}

	/**
	 * 强制平仓
	 */
	private void forceCloseAllPositions() {
		if (portfolio == null || portfolio.getPositions().isEmpty()) {
			logger.info("没有持仓需要平仓");
			return;
		}

		logger.info("执行强制平仓...");
		// 使用最后一条Bar的价格作为平仓价格
		List<BarEvent> allBars = dataFeed.getAllBars();
		if (allBars.isEmpty()) {
			logger.warn("没有价格数据，无法平仓");
			return;
		}

		BarEvent lastBar = allBars.get(allBars.size() - 1);
		double defaultPrice = lastBar.getClose();

		int closedPositions = 0;
		for (Map.Entry<String, Position> entry : portfolio.getPositions().entrySet()) {
			String symbol = entry.getKey();
			Position position = entry.getValue();

			if (position.getQuantity() > 0) {
				try {
					Double currentPrice = portfolio.getCurrentPrice(symbol);
					if (currentPrice == null || currentPrice <= 0) {
						currentPrice = defaultPrice;
					}

					OrderEvent closeOrder = new OrderEvent(LocalDateTime.now(), symbol, "SELL", position.getQuantity(),
							currentPrice, "MARKET");

					if (executionEngine != null) {
						FillEvent fill = executionEngine.executeOrder(closeOrder);
						if (fill != null) {
							portfolio.processFill(fill);
							closedPositions++;
							logger.info("平仓成功: {} {}股 @{:.2f}", symbol, position.getQuantity(), fill.getFillPrice());
						}
					}
				} catch (Exception e) {
					logger.error("{} 平仓失败: {}", symbol, e.getMessage());
				}
			}
		}

		logger.info("平仓完成: 共平仓 {} 个品种", closedPositions);
	}

	/**
	 * 计算最终结果
	 */
	private void calculateFinalResults() {
		logger.info("计算最终回测结果...");

		if (portfolio != null) {
			double finalCapital = portfolio.getTotalValue();
			double totalReturn = portfolio.getTotalReturn();

			result.setFinalCapital(finalCapital);
			result.setTotalReturn(totalReturn);
			result.calculateAdvancedMetrics();

			logger.info("最终结果: 初始资金={,.2f}, 最终资金={,.2f}, 总收益率={:.2f}%", result.getInitialCapital(), finalCapital,
					totalReturn);
		}
	}

	/**
	 * 打印统计信息
	 */
	private void printStatistics() {
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		logger.info("\n=== 回测详细统计 ===");
		logger.info("总处理时间: {:.2f} 秒", duration / 1000.0);
		logger.info("处理Bar数量: {}", totalBarsProcessed);
		logger.info("生成信号数量: {}", totalSignalsGenerated);
		logger.info("执行订单数量: {}", totalOrdersExecuted);
		logger.info("处理效率: {:.1f} Bar/秒", totalBarsProcessed / (duration / 1000.0));

		// 记录到统一监控
		if (monitorManager != null) {
			monitorManager.recordOperation("BacktestEngine", "total_processing", duration * 1_000_000L);
		}
	}

	// ==================== 事件处理器内部类 ====================

	/**
	 * Bar事件处理器 - 完整实现TradingComponent接口
	 */
	private class BarEventProcessor implements EventProcessor {
		@Override
		public void processEvent(Event event) {
			if (event instanceof BarEvent) {
				barEventsProcessed++;

				BarEvent bar = (BarEvent) event;

				// 添加详细调试信息
				System.out.printf("🔍 [Bar处理器] 处理Bar: %s - 收盘价: %.2f%n", bar.getTimestamp(), bar.getClose());

				try {
					// 更新市场价格
					if (portfolio != null) {
						portfolio.updateMarketPrice(bar.getSymbol(), bar.getClose());
					}

					// 策略生成信号 - 这是关键！
					if (strategy != null) {
						List<SignalEvent> signals = strategy.onBar(bar);
						totalSignalsGenerated += signals.size();

						System.out.printf("🔍 [Bar处理器] 策略生成 %d 个信号%n", signals.size());

						// 发布所有信号事件
						for (SignalEvent signal : signals) {
							System.out.printf("🔍 [Bar处理器] 发布信号: %s %s 强度:%.2f%n", signal.getTimestamp(),
									signal.getDirection(), signal.getStrength());
							eventBus.publish(signal);
						}
					}

				} catch (Exception e) {
					logger.error("[Bar处理器] 处理Bar事件失败: {}", e.getMessage());
				}
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return BarEvent.class;
		}

		// ==================== TradingComponent 接口方法 ====================
		@Override
		public void initialize() {
			// 初始化逻辑
		}

		@Override
		public void configure(Map<String, Object> config) {
			// 配置逻辑
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
			// 重置逻辑
		}

		@Override
		public void shutdown() {
			// 关闭逻辑
		}
	}

	/**
	 * 信号事件处理器 - 完整实现TradingComponent接口
	 */
	class SignalEventProcessor implements EventProcessor {

		@Override
		public void processEvent(Event event) {
			if (event instanceof SignalEvent) {
				signalEventsProcessed++;

				SignalEvent signal = (SignalEvent) event;

				// 添加详细调试信息
				System.out.printf("🔍 [信号处理器] 收到信号: %s %s %s 强度:%.2f%n", signal.getTimestamp(), signal.getSymbol(),
						signal.getDirection(), signal.getStrength());

				try {
					// 风险验证
					RiskManager.RiskValidationResult riskResult = riskManager.validateSignal(signal);

					System.out.printf("🔍 [风险验证] 结果: %s, 消息: %s%n", riskResult.isValid(), riskResult.getMessage());

					if (!riskResult.isValid()) {
						logger.debug("[信号处理器] 风险拒绝: {} - {}", signal.getSymbol(), riskResult.getMessage());
						return;
					}

					// 应用风险管理
					if (signal instanceof EnhancedSignalEvent) {
						riskControlManager.applyRiskFromSignal((EnhancedSignalEvent) signal);
					}

					// 投资组合生成订单
					if (portfolio != null) {
						OrderEvent order = portfolio.processSignal(signal);
						if (order != null) {
							System.out.printf("✅ [订单生成] %s %s %d股 @%.2f%n", order.getSymbol(), order.getDirection(),
									order.getQuantity(), order.getPrice());
							eventBus.publish(order);
						} else {
							System.out.printf("❌ [订单生成失败] 投资组合返回null订单%n");
						}
					}

				} catch (Exception e) {
					logger.error("[信号处理器] 处理信号事件失败: {}", e.getMessage());
				}
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return SignalEvent.class;
		}

		// ==================== TradingComponent 接口方法 ====================
		@Override
		public void initialize() {
			// 初始化逻辑
		}

		@Override
		public void configure(Map<String, Object> config) {
			// 配置逻辑
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
			// 重置逻辑
		}

		@Override
		public void shutdown() {
			// 关闭逻辑
		}
	}

	/**
	 * 订单事件处理器 - 完整实现TradingComponent接口
	 */
	private class OrderEventProcessor implements EventProcessor {
		@Override
		public void processEvent(Event event) {
			if (event instanceof OrderEvent) {
				orderEventsProcessed++;

				OrderEvent order = (OrderEvent) event;

				try {
					if (executionEngine != null) {
						FillEvent fill = executionEngine.executeOrder(order);
						if (fill != null) {
							totalOrdersExecuted++;
							eventBus.publish(fill);

							logger.debug("[订单处理器] 订单成交: {} {} {}股 @{:.2f}", fill.getSymbol(), fill.getDirection(),
									fill.getQuantity(), fill.getFillPrice());
						}
					}

				} catch (Exception e) {
					logger.error("[订单处理器] 处理订单事件失败: {}", e.getMessage());
				}
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return OrderEvent.class;
		}

		// ==================== TradingComponent 接口方法 ====================
		@Override
		public void initialize() {
			// 初始化逻辑
		}

		@Override
		public void configure(Map<String, Object> config) {
			// 配置逻辑
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
			// 重置逻辑
		}

		@Override
		public void shutdown() {
			// 关闭逻辑
		}
	}

	/**
	 * 成交事件处理器 - 完整实现TradingComponent接口
	 */
	private class FillEventProcessor implements EventProcessor {
		@Override
		public void processEvent(Event event) {
			if (event instanceof FillEvent) {
				fillEventsProcessed++;

				FillEvent fill = (FillEvent) event;

				try {
					if (portfolio != null) {
						portfolio.processFill(fill);
					}

					if (result != null) {
						result.addTrade(fill);
						if (portfolio != null) {
							result.addEquityPoint(portfolio.getTotalValue());
						}
					}

					logger.debug("[成交处理器] 成交处理完成: {} {} {}股", fill.getSymbol(), fill.getDirection(),
							fill.getQuantity());

				} catch (Exception e) {
					logger.error("[成交处理器] 处理成交事件失败: {}", e.getMessage());
				}
			}
		}

		@Override
		public Class<? extends Event> getSupportedEventType() {
			return FillEvent.class;
		}

		// ==================== TradingComponent 接口方法 ====================
		@Override
		public void initialize() {
			// 初始化逻辑
		}

		@Override
		public void configure(Map<String, Object> config) {
			// 配置逻辑
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
			// 重置逻辑
		}

		@Override
		public void shutdown() {
			// 关闭逻辑
		}
	}

	// ==================== Getter和Setter方法 ====================

	public void setStrategy(BaseStrategy strategy) {
		this.strategy = strategy;
	}

	public void setRiskManager(RiskManager riskManager) {
		this.riskManager = riskManager;
	}

	public void setExecutionEngine(ExecutionEngine executionEngine) {
		this.executionEngine = executionEngine;
	}

	public BacktestResult getResult() {
		return result;
	}

	public DataFeed getDataFeed() {
		return dataFeed;
	}

	public Portfolio getPortfolio() {
		return portfolio;
	}

	public void stop() {
		this.isRunning = false;
	}

	public Object getConfig() {
		// TODO Auto-generated method stub
		return config;
	}

	public Object getEventBus() {
		// TODO Auto-generated method stub
		return dataFeed;
	}

}