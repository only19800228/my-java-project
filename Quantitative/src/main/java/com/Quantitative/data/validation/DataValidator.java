package com.Quantitative.data.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.Quantitative.core.events.BarEvent;

/**
 * 数据验证器 - 验证市场数据的合理性和完整性
 */
public class DataValidator {

	// 验证配置
	private static final double MAX_PRICE_CHANGE_PERCENT = 50.0; // 单日最大涨跌幅50%
	private static final double MIN_PRICE = 0.01; // 最小价格
	private static final double MAX_PRICE = 100000.0; // 最大价格
	private static final long MIN_VOLUME = 100; // 最小成交量
	private static final long MAX_VOLUME = 10000000000L; // 最大成交量

	/**
	 * 验证单个Bar数据的合理性
	 */
	public static ValidationResult validateBar(BarEvent bar) {
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		String symbol = bar.getSymbol();
		double open = bar.getOpen();
		double high = bar.getHigh();
		double low = bar.getLow();
		double close = bar.getClose();
		long volume = bar.getVolume();

		// 1. 基础价格验证
		if (!isValidPrice(open)) {
			errors.add("开盘价异常: " + open);
		}
		if (!isValidPrice(high)) {
			errors.add("最高价异常: " + high);
		}
		if (!isValidPrice(low)) {
			errors.add("最低价异常: " + low);
		}
		if (!isValidPrice(close)) {
			errors.add("收盘价异常: " + close);
		}

		// 2. 价格关系验证
		if (high < low) {
			errors.add("最高价低于最低价: " + high + " < " + low);
		}
		if (open > high || open < low) {
			errors.add("开盘价超出价格范围: " + open + " not in [" + low + "," + high + "]");
		}
		if (close > high || close < low) {
			errors.add("收盘价超出价格范围: " + close + " not in [" + low + "," + high + "]");
		}

		// 3. 成交量验证
		if (volume < MIN_VOLUME) {
			warnings.add("成交量过低: " + volume);
		}
		if (volume > MAX_VOLUME) {
			warnings.add("成交量异常高: " + volume);
		}

		// 4. 价格变动验证
		if (bar.getTimestamp() != null) {
			double changePercent = Math.abs((close - open) / open * 100);
			if (changePercent > MAX_PRICE_CHANGE_PERCENT) {
				warnings.add("价格变动过大: " + String.format("%.2f%%", changePercent));
			}
		}

		boolean isValid = errors.isEmpty();
		String status = isValid ? "VALID" : "INVALID";

		return new ValidationResult(isValid, status, errors, warnings, bar.getTimestamp());
	}

	/**
	 * 验证价格序列的连续性
	 */
	public static ValidationResult validatePriceSeries(List<BarEvent> bars) {
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		if (bars == null || bars.size() < 2) {
			return new ValidationResult(true, "INSUFFICIENT_DATA", errors, warnings, null);
		}

		// 按时间排序
		bars.sort((b1, b2) -> b1.getTimestamp().compareTo(b2.getTimestamp()));

		for (int i = 1; i < bars.size(); i++) {
			BarEvent prevBar = bars.get(i - 1);
			BarEvent currentBar = bars.get(i);

			// 检查时间连续性
			long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(prevBar.getTimestamp().toLocalDate(),
					currentBar.getTimestamp().toLocalDate());

			if (daysBetween > 7) { // 允许周末和节假日，但超过7天可能有数据缺失
				warnings.add("数据间隔过长: " + prevBar.getTimestamp().toLocalDate() + " 到 "
						+ currentBar.getTimestamp().toLocalDate() + " (" + daysBetween + "天)");
			}

			// 检查价格跳空（除权除息除外）
			double prevClose = prevBar.getClose();
			double currentOpen = currentBar.getOpen();
			double gapPercent = Math.abs((currentOpen - prevClose) / prevClose * 100);

			if (gapPercent > 20.0) { // 超过20%的跳空可能是除权除息
				warnings.add("异常价格跳空: " + String.format("%.2f%%", gapPercent) + " (" + prevClose + " -> " + currentOpen
						+ ")");
			}
		}

		boolean isValid = errors.isEmpty();
		String status = isValid ? "VALID" : "INVALID";

		return new ValidationResult(isValid, status, errors, warnings, bars.get(bars.size() - 1).getTimestamp());
	}

	/**
	 * 验证价格是否在合理范围内
	 */
	private static boolean isValidPrice(double price) {
		if (price <= MIN_PRICE) {
			return false;
		}
		if (price > MAX_PRICE) {
			return false;
		}
		if (Double.isNaN(price) || Double.isInfinite(price)) {
			return false;
		}
		return true;
	}

	/**
	 * 数据修复 - 尝试修复常见的数据问题
	 */
	public static BarEvent repairBarData(BarEvent bar) {
		double open = bar.getOpen();
		double high = bar.getHigh();
		double low = bar.getLow();
		double close = bar.getClose();

		// 修复价格关系
		if (high < low) {
			double temp = high;
			high = low;
			low = temp;
		}

		if (open > high)
			open = high;
		if (open < low)
			open = low;
		if (close > high)
			close = high;
		if (close < low)
			close = low;

		// 创建修复后的Bar
		return new BarEvent(bar.getTimestamp(), bar.getSymbol(), open, high, low, close, Math.max(bar.getVolume(), 0),
				Math.max(bar.getTurnover(), 0));
	}

	/**
	 * 验证结果类
	 */
	public static class ValidationResult {
		private final boolean valid;
		private final String status;
		private final List<String> errors;
		private final List<String> warnings;
		private final LocalDateTime timestamp;

		public ValidationResult(boolean valid, String status, List<String> errors, List<String> warnings,
				LocalDateTime timestamp) {
			this.valid = valid;
			this.status = status;
			this.errors = new ArrayList<>(errors);
			this.warnings = new ArrayList<>(warnings);
			this.timestamp = timestamp;
		}

		// Getter方法
		public boolean isValid() {
			return valid;
		}

		public String getStatus() {
			return status;
		}

		public List<String> getErrors() {
			return new ArrayList<>(errors);
		}

		public List<String> getWarnings() {
			return new ArrayList<>(warnings);
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		public boolean hasErrors() {
			return !errors.isEmpty();
		}

		public boolean hasWarnings() {
			return !warnings.isEmpty();
		}

		@Override
		public String toString() {
			return String.format("ValidationResult{valid=%s, errors=%d, warnings=%d}", valid, errors.size(),
					warnings.size());
		}

		/**
		 * 生成详细报告
		 */
		public String generateReport() {
			StringBuilder report = new StringBuilder();
			report.append("=== 数据验证报告 ===\n");
			report.append("状态: ").append(status).append("\n");
			report.append("时间: ").append(timestamp).append("\n");

			if (!errors.isEmpty()) {
				report.append("\n错误 (").append(errors.size()).append("):\n");
				for (String error : errors) {
					report.append("  ❌ ").append(error).append("\n");
				}
			}

			if (!warnings.isEmpty()) {
				report.append("\n警告 (").append(warnings.size()).append("):\n");
				for (String warning : warnings) {
					report.append("  ⚠️ ").append(warning).append("\n");
				}
			}

			if (errors.isEmpty() && warnings.isEmpty()) {
				report.append("\n✅ 数据验证通过，无问题\n");
			}

			return report.toString();
		}
	}
}