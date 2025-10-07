// 新增：实时监控面板
package com.Quantitative.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.portfolio.Portfolio;

/**
 * 实时监控面板 - 控制台版本
 */
public class ConsoleMonitor {
	private final EventDrivenBacktestEngine engine;
	private final ScheduledExecutorService scheduler;
	private final Map<String, Object> metrics;
	private boolean monitoring = false;
	private int updateInterval = 2; // 秒

	public ConsoleMonitor(EventDrivenBacktestEngine engine) {
		this.engine = engine;
		this.scheduler = Executors.newScheduledThreadPool(1);
		this.metrics = new ConcurrentHashMap<>();
		initializeMetrics();
	}

	/**
	 * 初始化监控指标
	 */
	private void initializeMetrics() {
		metrics.put("startTime", System.currentTimeMillis());
		metrics.put("barsProcessed", 0);
		metrics.put("signalsGenerated", 0);
		metrics.put("ordersExecuted", 0);
		metrics.put("currentProgress", 0.0);
		metrics.put("estimatedTimeRemaining", "计算中...");
		metrics.put("portfolioValue", 0.0);
		metrics.put("currentReturn", 0.0);
		metrics.put("maxDrawdown", 0.0);
	}

	/**
	 * 开始监控
	 */
	public void startMonitoring() {
		if (monitoring) {
			System.out.println("⚠️ 监控已在运行中");
			return;
		}

		monitoring = true;
		System.out.println("📊 启动实时监控面板...");

		// 定时更新监控信息
		scheduler.scheduleAtFixedRate(this::updateMonitor, 0, updateInterval, TimeUnit.SECONDS);

		// 显示初始面板
		displayDashboard();
	}

	/**
	 * 停止监控
	 */
	public void stopMonitoring() {
		monitoring = false;
		scheduler.shutdown();
		System.out.println("🛑 监控已停止");

		// 显示最终结果
		displayFinalResults();
	}

	/**
	 * 更新监控指标
	 */
	private void updateMonitor() {
		if (!monitoring)
			return;

		try {
			// 获取引擎状态
			updateEngineMetrics();

			// 获取投资组合状态
			updatePortfolioMetrics();

			// 计算预估时间
			updateTimeEstimation();

			// 更新显示
			displayDashboard();

		} catch (Exception e) {
			System.err.println("监控更新失败: " + e.getMessage());
		}
	}

	/**
	 * 更新引擎指标
	 */
	private void updateEngineMetrics() {
		// 这里可以通过反射或其他方式获取引擎内部状态
		// 简化实现：使用模拟数据
		metrics.put("barsProcessed", (int) metrics.get("barsProcessed") + 1);
		metrics.put("currentProgress", Math.min((double) metrics.get("currentProgress") + 0.1, 100.0));
	}

	/**
	 * 更新投资组合指标
	 */
	private void updatePortfolioMetrics() {
		Portfolio portfolio = engine.getPortfolio();
		if (portfolio != null) {
			double totalValue = portfolio.getTotalValue();
			double initialCapital = portfolio.getInitialCash();
			double currentReturn = ((totalValue - initialCapital) / initialCapital) * 100;

			metrics.put("portfolioValue", totalValue);
			metrics.put("currentReturn", currentReturn);
		}
	}

	/**
	 * 更新时间预估
	 */
	private void updateTimeEstimation() {
		long startTime = (long) metrics.get("startTime");
		long currentTime = System.currentTimeMillis();
		long elapsed = currentTime - startTime;

		double progress = (double) metrics.get("currentProgress");
		if (progress > 0) {
			long totalEstimated = (long) (elapsed / (progress / 100.0));
			long remaining = totalEstimated - elapsed;

			String eta = formatTime(remaining);
			metrics.put("estimatedTimeRemaining", eta);
		}
	}

	/**
	 * 显示监控面板
	 */
	private void displayDashboard() {
		// 清屏（简化实现）
		System.out.print("\033[H\033[2J");
		System.out.flush();

		System.out.println("╔══════════════════════════════════════════════════════════════╗");
		System.out.println("║                     📊 量化回测实时监控面板                   ║");
		System.out.println("╠══════════════════════════════════════════════════════════════╣");

		// 进度条
		displayProgressBar();

		// 关键指标
		displayKeyMetrics();

		// 投资组合信息
		displayPortfolioInfo();

		// 系统状态
		displaySystemStatus();

		System.out.println("╚══════════════════════════════════════════════════════════════╝");
	}

	/**
	 * 显示进度条
	 */
	private void displayProgressBar() {
		double progress = (double) metrics.get("currentProgress");
		int bars = 30;
		int filled = (int) (progress / 100.0 * bars);

		StringBuilder progressBar = new StringBuilder("║ 进度: [");
		for (int i = 0; i < bars; i++) {
			if (i < filled) {
				progressBar.append("█");
			} else {
				progressBar.append("░");
			}
		}
		progressBar.append(String.format("] %.1f%%", progress));
		progressBar.append(Stream.generate(() -> " ").limit(50).collect(Collectors.joining()));
		// System.out.println(Stream.generate(() ->
		// "=").limit(50).collect(Collectors.joining()));
		progressBar.append("║");

		System.out.println(progressBar.toString());
	}

	/**
	 * 显示关键指标
	 */
	private void displayKeyMetrics() {
		System.out.printf("║ 已处理Bar: %-6d   生成信号: %-6d   执行订单: %-6d      ║%n", metrics.get("barsProcessed"),
				metrics.get("signalsGenerated"), metrics.get("ordersExecuted"));
	}

	/**
	 * 显示投资组合信息
	 */
	private void displayPortfolioInfo() {
		System.out.printf("║ 投资组合: ¥%-10.2f  收益率: %-7.2f%%  最大回撤: %-6.2f%%  ║%n", metrics.get("portfolioValue"),
				metrics.get("currentReturn"), metrics.get("maxDrawdown"));
	}

	/**
	 * 显示系统状态
	 */
	private void displaySystemStatus() {
		System.out.printf("║ 运行时间: %-10s  预估剩余: %-10s                  ║%n",
				formatTime(System.currentTimeMillis() - (long) metrics.get("startTime")),
				metrics.get("estimatedTimeRemaining"));
	}

	/**
	 * 显示最终结果
	 */
	private void displayFinalResults() {
		// System.out.println("\n" + "=".repeat(70));
		System.out.println(Stream.generate(() -> "\n" + "=").limit(70).collect(Collectors.joining()));
		System.out.println("🎯 回测完成 - 最终结果");
		// System.out.println("=".repeat(70));
		System.out.println(Stream.generate(() -> "=").limit(70).collect(Collectors.joining()));
		System.out.printf("总运行时间: %s%n", formatTime(System.currentTimeMillis() - (long) metrics.get("startTime")));
		System.out.printf("处理Bar数量: %d%n", metrics.get("barsProcessed"));
		System.out.printf("最终投资组合价值: ¥%.2f%n", metrics.get("portfolioValue"));
		System.out.printf("总收益率: %.2f%%%n", metrics.get("currentReturn"));
	}

	/**
	 * 格式化时间
	 */
	private String formatTime(long millis) {
		long seconds = millis / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;

		if (hours > 0) {
			return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
		} else {
			return String.format("%02d:%02d", minutes, seconds % 60);
		}
	}

	/**
	 * 记录事件
	 */
	public void recordBarProcessed(BarEvent bar) {
		metrics.put("barsProcessed", (int) metrics.get("barsProcessed") + 1);
	}

	public void recordSignalGenerated(SignalEvent signal) {
		metrics.put("signalsGenerated", (int) metrics.get("signalsGenerated") + 1);
	}

	public void recordOrderExecuted() {
		metrics.put("ordersExecuted", (int) metrics.get("ordersExecuted") + 1);
	}

	/**
	 * 设置进度
	 */
	public void setProgress(double progress) {
		metrics.put("currentProgress", Math.min(progress, 100.0));
	}

	// ==================== Getter和Setter ====================

	public boolean isMonitoring() {
		return monitoring;
	}

	public void setUpdateInterval(int seconds) {
		this.updateInterval = seconds;
	}

	public Map<String, Object> getMetrics() {
		return new HashMap<>(metrics);
	}
}