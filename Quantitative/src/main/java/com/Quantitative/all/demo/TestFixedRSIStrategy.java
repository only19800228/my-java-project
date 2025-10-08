package com.Quantitative.all.demo;

import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;

/**
 * 测试修复后的RSI策略
 */
public class TestFixedRSIStrategy {

	public static void main(String[] args) {
		System.out.println("=== 测试修复后的RSI策略 ===\n");

		System.out.println("\n=== 测试完成 ===");
	}

	public void debugSignalProcessing() {
		System.out.println("=== 开始信号处理调试 ===");

		// 获取一些测试数据
		List<BarEvent> bars = dataFeed.getAllBars();
		if (bars.size() < 10)
			return;

		// 测试前几个Bar
		for (int i = 0; i < Math.min(5, bars.size()); i++) {
			BarEvent bar = bars.get(i);
			System.out.printf("测试Bar %d: %s - %.2f%n", i, bar.getTimestamp(), bar.getClose());

			// 直接调用策略生成信号
			if (strategy != null) {
				List<SignalEvent> signals = strategy.onBar(bar);
				System.out.printf("生成 %d 个信号%n", signals.size());

				// 直接处理每个信号
				for (SignalEvent signal : signals) {
					System.out.printf("处理信号: %s %s 强度:%.2f%n", signal.getTimestamp(), signal.getDirection(),
							signal.getStrength());

					// 直接调用信号处理器
					SignalEventProcessor processor = new SignalEventProcessor();
					processor.processEvent(signal);
				}
			}
		}
		System.out.println("=== 调试完成 ===");
	}

}