package com.Quantitative.backtest;

import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.portfolio.Portfolio;
import com.Quantitative.portfolio.RiskManager;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 信号处理调试工具
 */
public class SignalProcessingDebugger {
	private final EventDrivenBacktestEngine engine;
	private final DataFeed dataFeed;
	private final BaseStrategy strategy;
	private final Portfolio portfolio;
	private final RiskManager riskManager;

	public SignalProcessingDebugger(EventDrivenBacktestEngine engine) {
		this.engine = engine;
		this.dataFeed = engine.getDataFeed();
		this.strategy = null; // 需要通过setter设置
		this.portfolio = engine.getPortfolio();
		this.riskManager = null; // 需要通过setter设置
	}

	/**
	 * 完整的信号处理调试
	 */
	public void debugSignalProcessing() {
		// System.out.println("\n" + "=".repeat(80));
		// 带换行符
		StringBuilder sb = new StringBuilder("\n");
		for (int i = 0; i < 80; i++) {
			sb.append("=");
		}
		System.out.println(sb.toString());
		System.out.println("🚀 开始信号处理调试");
		// System.out.println("=".repeat(80));
		// 不带换行符
		for (int i = 0; i < 80; i++) {
			System.out.print("=");
		}
		System.out.println(); // 换行
		// 1. 检查数据加载
		if (!checkDataLoaded()) {
			return;
		}

		// 2. 检查组件初始化
		if (!checkComponentsInitialized()) {
			return;
		}

		// 3. 测试前5个Bar的信号生成
		testSignalGeneration();

		// 4. 测试信号处理链
		testSignalProcessingChain();

		// System.out.println("=".repeat(80));
		// 不带换行符
		for (int i = 0; i < 80; i++) {
			System.out.print("=");
		}
		System.out.println(); // 换行
		System.out.println("✅ 信号处理调试完成");
		// System.out.println("=".repeat(80));
		// 不带换行符
		for (int i = 0; i < 80; i++) {
			System.out.print("=");
		}
		System.out.println(); // 换行
	}

	/**
	 * 检查数据加载
	 */
	private boolean checkDataLoaded() {
		System.out.println("\n📊 步骤1: 检查数据加载");

		List<BarEvent> bars = dataFeed.getAllBars();
		if (bars == null || bars.isEmpty()) {
			System.out.println("❌ 错误: 没有加载到数据");
			return false;
		}

		System.out.printf("✅ 数据加载成功: %d 条Bar数据%n", bars.size());
		System.out.printf("   时间范围: %s 到 %s%n", bars.get(0).getTimestamp(), bars.get(bars.size() - 1).getTimestamp());

		// 显示前3条数据样例
		System.out.println("   数据样例:");
		for (int i = 0; i < Math.min(3, bars.size()); i++) {
			BarEvent bar = bars.get(i);
			System.out.printf("     %d. %s - O:%.2f H:%.2f L:%.2f C:%.2f V:%.0f%n", i + 1, bar.getTimestamp(),
					bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());
		}

		return true;
	}

	/**
	 * 检查组件初始化
	 */
	private boolean checkComponentsInitialized() {
		System.out.println("\n🔧 步骤2: 检查组件初始化");

		boolean allComponentsOk = true;

		// 检查策略
		if (strategy == null) {
			System.out.println("❌ 策略未设置");
			allComponentsOk = false;
		} else {
			System.out.println("✅ 策略已设置: " + strategy.getClass().getSimpleName());
		}

		// 检查投资组合
		if (portfolio == null) {
			System.out.println("❌ 投资组合未初始化");
			allComponentsOk = false;
		} else {
			System.out.println("✅ 投资组合已初始化");
			System.out.printf("   初始资金: %.2f, 当前资金: %.2f%n", portfolio.getInitialCash(), portfolio.getCash());
		}

		// 检查风险管理器
		if (riskManager == null) {
			System.out.println("⚠️  风险管理器未设置（可能不影响测试）");
		} else {
			System.out.println("✅ 风险管理器已设置");
		}

		return allComponentsOk;
	}

	/**
	 * 测试信号生成
	 */
	private void testSignalGeneration() {
		System.out.println("\n🎯 步骤3: 测试信号生成");

		if (strategy == null) {
			System.out.println("❌ 无法测试信号生成：策略未设置");
			return;
		}

		List<BarEvent> bars = dataFeed.getAllBars();
		int signalCount = 0;

		// 测试前10个Bar
		for (int i = 0; i < Math.min(10, bars.size()); i++) {
			BarEvent bar = bars.get(i);
			System.out.printf("\n🔍 测试Bar %d/%d: %s%n", i + 1, Math.min(10, bars.size()), bar.getTimestamp());

			try {
				// 生成信号
				List<SignalEvent> signals = strategy.onBar(bar);

				if (signals == null || signals.isEmpty()) {
					System.out.println("   📭 没有生成信号");
					continue;
				}

				System.out.printf("   ✅ 生成 %d 个信号:%n", signals.size());
				signalCount += signals.size();

				// 显示每个信号的详细信息
				for (int j = 0; j < signals.size(); j++) {
					SignalEvent signal = signals.get(j);
					System.out.printf("      %d. %s %s 强度:%.2f%n", j + 1, signal.getSymbol(), signal.getDirection(),
							signal.getStrength());
				}

			} catch (Exception e) {
				System.out.printf("   ❌ 生成信号时出错: %s%n", e.getMessage());
				e.printStackTrace();
			}
		}

		System.out.printf("\n📈 信号生成测试结果: 共生成 %d 个信号%n", signalCount);
	}

	/**
	 * 测试信号处理链
	 */
	private void testSignalProcessingChain() {
		System.out.println("\n🔄 步骤4: 测试信号处理链");

		if (strategy == null) {
			System.out.println("❌ 无法测试处理链：策略未设置");
			return;
		}

		List<BarEvent> bars = dataFeed.getAllBars();
		int processedSignals = 0;
		int generatedOrders = 0;

		// 创建信号处理器实例
		EventDrivenBacktestEngine.SignalEventProcessor signalProcessor = engine.new SignalEventProcessor();

		// 测试前5个有信号的Bar
		for (int i = 0; i < Math.min(20, bars.size()); i++) {
			BarEvent bar = bars.get(i);

			// 生成信号
			List<SignalEvent> signals = strategy.onBar(bar);
			if (signals == null || signals.isEmpty()) {
				continue;
			}

			System.out.printf("\n🔗 处理Bar %d: %s (%d个信号)%n", i + 1, bar.getTimestamp(), signals.size());

			// 处理每个信号
			for (SignalEvent signal : signals) {
				processedSignals++;

				System.out.printf("   📡 处理信号: %s %s 强度:%.2f%n", signal.getSymbol(), signal.getDirection(),
						signal.getStrength());

				try {
					// 直接调用信号处理器（绕过事件总线）
					signalProcessor.processEvent(signal);

					// 这里可以添加订单生成的检查
					// 由于processEvent是void方法，我们需要通过其他方式检查是否生成了订单
					System.out.println("   ✅ 信号处理完成");

				} catch (Exception e) {
					System.out.printf("   ❌ 信号处理失败: %s%n", e.getMessage());
					e.printStackTrace();
				}
			}

			// 限制测试数量，避免输出过多
			if (processedSignals >= 10) {
				System.out.println("\n⚠️  已达到最大测试信号数量(10)，停止测试");
				break;
			}
		}

		System.out.printf("\n📊 信号处理链测试结果:%n");
		System.out.printf("   处理信号数量: %d%n", processedSignals);
		System.out.printf("   生成订单数量: %d%n", generatedOrders);
	}

	/**
	 * 快速诊断 - 简化版本
	 */
	public void quickDebug() {
		System.out.println("\n⚡ 快速诊断信号处理问题");

		// 基本检查
		List<BarEvent> bars = dataFeed.getAllBars();
		if (bars.isEmpty()) {
			System.out.println("❌ 没有数据");
			return;
		}

		if (strategy == null) {
			System.out.println("❌ 策略未设置");
			return;
		}

		// 测试第一个Bar
		BarEvent firstBar = bars.get(0);
		System.out.printf("测试第一个Bar: %s%n", firstBar.getTimestamp());

		List<SignalEvent> signals = strategy.onBar(firstBar);
		System.out.printf("信号生成: %s (%d个信号)%n", signals != null && !signals.isEmpty() ? "✅" : "❌",
				signals != null ? signals.size() : 0);

		if (signals != null && !signals.isEmpty()) {
			SignalEvent firstSignal = signals.get(0);
			System.out.printf("第一个信号: %s %s 强度:%.2f%n", firstSignal.getSymbol(), firstSignal.getDirection(),
					firstSignal.getStrength());
		}
	}

	// Setter方法
	public void setStrategy(BaseStrategy strategy) {
		// 这里需要访问engine的strategy，可能需要修改引擎类的访问权限
		// 或者通过反射设置
		try {
			java.lang.reflect.Field strategyField = EventDrivenBacktestEngine.class.getDeclaredField("strategy");
			strategyField.setAccessible(true);
			strategyField.set(engine, strategy);
			System.out.println("✅ 策略设置成功: " + strategy.getClass().getSimpleName());
		} catch (Exception e) {
			System.out.println("❌ 设置策略失败: " + e.getMessage());
		}
	}

	public void setRiskManager(RiskManager riskManager) {
		// 类似上面，设置风险管理器
		try {
			java.lang.reflect.Field riskManagerField = EventDrivenBacktestEngine.class.getDeclaredField("riskManager");
			riskManagerField.setAccessible(true);
			riskManagerField.set(engine, riskManager);
			System.out.println("✅ 风险管理器设置成功");
		} catch (Exception e) {
			System.out.println("❌ 设置风险管理器失败: " + e.getMessage());
		}
	}
}