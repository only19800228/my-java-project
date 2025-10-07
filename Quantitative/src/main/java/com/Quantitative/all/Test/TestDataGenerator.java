package com.Quantitative.all.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.Quantitative.core.events.BarEvent;

/**
 * 测试数据生成工具
 */
public class TestDataGenerator {

	public static List<BarEvent> generateTrendingBars(int count, double startPrice, double trend) {
		List<BarEvent> bars = new ArrayList<>();
		LocalDateTime time = LocalDateTime.now();
		double price = startPrice;

		for (int i = 0; i < count; i++) {
			// 趋势 + 随机噪声
			double change = trend + (Math.random() - 0.5) * 0.3;
			price += change;

			BarEvent bar = new BarEvent(time.plusMinutes(i), "TEST", price - 0.1, price + 0.2, price - 0.2, price,
					(long) (1000000 + Math.random() * 1000000));
			bars.add(bar);
		}

		return bars;
	}

	public static List<BarEvent> generateRangeBars(int count, double basePrice, double range) {
		List<BarEvent> bars = new ArrayList<>();
		LocalDateTime time = LocalDateTime.now();

		for (int i = 0; i < count; i++) {
			// 在范围内震荡
			double price = basePrice + (Math.random() - 0.5) * range * 2;

			BarEvent bar = new BarEvent(time.plusMinutes(i), "TEST", price - 0.1, price + 0.2, price - 0.2, price,
					(long) (1000000 + Math.random() * 1000000));
			bars.add(bar);
		}

		return bars;
	}
}