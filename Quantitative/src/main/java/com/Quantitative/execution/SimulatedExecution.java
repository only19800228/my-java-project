package com.Quantitative.execution;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.Quantitative.common.utils.PerformanceMonitor;
import com.Quantitative.core.events.FillEvent;
import com.Quantitative.core.events.OrderEvent;

/**
 * 模拟执行引擎 - 用于回测的订单执行模拟
 */
public class SimulatedExecution implements ExecutionEngine {
	private Map<String, OrderEvent> activeOrders;
	private Map<String, FillEvent> executedOrders;
	private Map<String, Object> executionParameters;
	private String status = "CREATED";

	// 执行统计
	private AtomicLong totalOrdersExecuted = new AtomicLong(0);
	private AtomicLong totalOrdersRejected = new AtomicLong(0);
	private AtomicLong totalOrdersCancelled = new AtomicLong(0);

	// 性能监控
	private PerformanceMonitor performanceMonitor;

	public SimulatedExecution() {
		this.activeOrders = new ConcurrentHashMap<>();
		this.executedOrders = new ConcurrentHashMap<>();
		this.executionParameters = new HashMap<>();
		this.performanceMonitor = PerformanceMonitor.getInstance();

		initializeDefaultParameters();
	}

	private void initializeDefaultParameters() {
		executionParameters.put("slippage", 0.001); // 滑点 0.1%
		executionParameters.put("fillRate", 1.0); // 成交率 100%
		executionParameters.put("latencyMs", 10); // 延迟 10ms
		executionParameters.put("commissionRate", 0.0003); // 手续费率
	}

	@Override
	public void initialize() {
		System.out.println("初始化模拟执行引擎...");
		this.status = "INITIALIZED";
		System.out.println("✓ 模拟执行引擎初始化完成");
	}

	@Override
	public void configure(Map<String, Object> config) {
		if (config != null) {
			executionParameters.putAll(config);
		}
	}

	@Override
	public String getName() {
		return "SimulatedExecution";
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		this.activeOrders.clear();
		this.executedOrders.clear();
		this.totalOrdersExecuted.set(0);
		this.totalOrdersRejected.set(0);
		this.totalOrdersCancelled.set(0);
		this.status = "RESET";
		System.out.println("✓ 模拟执行引擎已重置");
	}

	@Override
	public void shutdown() {
		System.out.println("关闭模拟执行引擎...");
		printExecutionStatistics();
		this.status = "SHUTDOWN";
	}

	@Override
	public FillEvent executeOrder(OrderEvent order) {
		performanceMonitor.startOperation("ExecutionEngine.executeOrder");

		try {
			// 模拟执行延迟
			simulateExecutionLatency();

			// 检查订单有效性
			if (!validateOrder(order)) {
				totalOrdersRejected.incrementAndGet();
				System.out.printf("[执行引擎] 订单被拒绝: %s%n", order);
				return null;
			}

			// 模拟滑点
			double executionPrice = applySlippage(order.getPrice(), order.getDirection());

			// 计算手续费
			double commission = calculateCommission(order, executionPrice);

			// 生成成交事件
			String orderId = generateOrderId();
			FillEvent fill = new FillEvent(LocalDateTime.now(), order.getSymbol(), order.getDirection(),
					order.getQuantity(), executionPrice, commission, orderId);

			// 记录成交
			activeOrders.put(orderId, order);
			executedOrders.put(orderId, fill);
			totalOrdersExecuted.incrementAndGet();

			System.out.printf("[执行引擎] 订单成交: %s @%.2f, 手续费: %.2f%n", order.getSymbol(), executionPrice, commission);

			return fill;

		} finally {
			performanceMonitor.endOperation("ExecutionEngine.executeOrder");
		}
	}

	@Override
	public boolean cancelOrder(String orderId) {
		if (activeOrders.containsKey(orderId)) {
			activeOrders.remove(orderId);
			totalOrdersCancelled.incrementAndGet();
			System.out.printf("[执行引擎] 订单已取消: %s%n", orderId);
			return true;
		}
		return false;
	}

	@Override
	public String getOrderStatus(String orderId) {
		if (executedOrders.containsKey(orderId)) {
			return "FILLED";
		} else if (activeOrders.containsKey(orderId)) {
			return "ACTIVE";
		} else {
			return "UNKNOWN";
		}
	}

	@Override
	public void setExecutionParameter(String key, Object value) {
		executionParameters.put(key, value);
	}

	@Override
	public Map<String, Object> getExecutionStatistics() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("totalOrdersExecuted", totalOrdersExecuted.get());
		stats.put("totalOrdersRejected", totalOrdersRejected.get());
		stats.put("totalOrdersCancelled", totalOrdersCancelled.get());
		stats.put("activeOrders", activeOrders.size());
		stats.put("executedOrders", executedOrders.size());
		stats.put("parameters", new HashMap<>(executionParameters));
		return stats;
	}

	// ==================== 私有方法 ====================

	/**
	 * 验证订单
	 */
	private boolean validateOrder(OrderEvent order) {
		// 检查基本参数
		if (order.getQuantity() <= 0) {
			System.out.println("⚠ 订单数量必须大于0");
			return false;
		}

		if (order.getPrice() <= 0) {
			System.out.println("⚠ 订单价格必须大于0");
			return false;
		}

		// 检查成交率
		double fillRate = (Double) executionParameters.get("fillRate");
		if (Math.random() > fillRate) {
			System.out.println("⚠ 订单因成交率限制被拒绝");
			return false;
		}

		return true;
	}

	/**
	 * 应用滑点
	 */
	private double applySlippage(double price, String direction) {
		double slippage = (Double) executionParameters.get("slippage");
		double slippageAmount = price * slippage;

		if ("BUY".equals(direction)) {
			// 买入时价格上浮
			return price + slippageAmount;
		} else if ("SELL".equals(direction)) {
			// 卖出时价格下浮
			return price - slippageAmount;
		}

		return price;
	}

	/**
	 * 计算手续费
	 */
	private double calculateCommission(OrderEvent order, double executionPrice) {
		double commissionRate = (Double) executionParameters.get("commissionRate");
		double tradeAmount = executionPrice * order.getQuantity();
		return tradeAmount * commissionRate;
	}

	/**
	 * 模拟执行延迟
	 */
	private void simulateExecutionLatency() {
		int latencyMs = (Integer) executionParameters.get("latencyMs");
		if (latencyMs > 0) {
			try {
				Thread.sleep(latencyMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * 生成订单ID
	 */
	private String generateOrderId() {
		return "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
	}

	/**
	 * 打印执行统计
	 */
	public void printExecutionStatistics() {
		Map<String, Object> stats = getExecutionStatistics();
		System.out.println("\n=== 执行引擎统计 ===");
		System.out.printf("总执行订单: %d%n", stats.get("totalOrdersExecuted"));
		System.out.printf("总拒绝订单: %d%n", stats.get("totalOrdersRejected"));
		System.out.printf("总取消订单: %d%n", stats.get("totalOrdersCancelled"));
		System.out.printf("活跃订单: %d%n", stats.get("activeOrders"));
		System.out.printf("已成交订单: %d%n", stats.get("executedOrders"));

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) stats.get("parameters");
		System.out.println("执行参数:");
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			System.out.printf("  %s: %s%n", entry.getKey(), entry.getValue());
		}
	}
}