package com.Quantitative.data.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量报告
 */
public class DataQualityReport {
	private final String symbol;
	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	private final List<DataValidator.ValidationResult> validationResults;
	private DataValidator.ValidationResult seriesValidation;
	private LocalDateTime generatedTime;

	// 统计信息
	private int totalBars;
	private int validBars;
	private int invalidBars;
	private int repairedBars;
	private int totalErrors;
	private int totalWarnings;

	public DataQualityReport(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
		this.symbol = symbol;
		this.startTime = startTime;
		this.endTime = endTime;
		this.validationResults = new ArrayList<>();
		this.generatedTime = LocalDateTime.now();
	}

	public void addValidationResult(DataValidator.ValidationResult result) {
		validationResults.add(result);
		totalBars++;

		if (result.isValid()) {
			validBars++;
		} else {
			invalidBars++;
		}

		totalErrors += result.getErrors().size();
		totalWarnings += result.getWarnings().size();
	}

	public void setSeriesValidation(DataValidator.ValidationResult seriesValidation) {
		this.seriesValidation = seriesValidation;
		totalErrors += seriesValidation.getErrors().size();
		totalWarnings += seriesValidation.getWarnings().size();
	}

	// Getter方法
	public String getSymbol() {
		return symbol;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public int getTotalBars() {
		return totalBars;
	}

	public int getValidBars() {
		return validBars;
	}

	public int getInvalidBars() {
		return invalidBars;
	}

	public int getRepairedBars() {
		return repairedBars;
	}

	public int getTotalErrors() {
		return totalErrors;
	}

	public int getTotalWarnings() {
		return totalWarnings;
	}

	public double getDataQualityScore() {
		if (totalBars == 0)
			return 0.0;
		double baseScore = (double) validBars / totalBars * 100;
		// 根据错误和警告数量扣分
		double penalty = Math.min(totalErrors * 5 + totalWarnings, 30);
		return Math.max(0, baseScore - penalty);
	}

	/**
	 * 生成详细报告
	 */
	public String generateReport() {
		StringBuilder report = new StringBuilder();
		report.append("=== 数据质量报告 ===\n");
		report.append("标的: ").append(symbol).append("\n");
		report.append("时间范围: ").append(startTime.toLocalDate()).append(" 到 ").append(endTime.toLocalDate())
				.append("\n");
		report.append("生成时间: ").append(generatedTime).append("\n\n");

		report.append("数据统计:\n");
		report.append("  总数据条数: ").append(totalBars).append("\n");
		report.append("  有效数据: ").append(validBars).append(" (")
				.append(String.format("%.1f%%", (double) validBars / totalBars * 100)).append(")\n");
		report.append("  无效数据: ").append(invalidBars).append(" (")
				.append(String.format("%.1f%%", (double) invalidBars / totalBars * 100)).append("\n");
		report.append("  总错误数: ").append(totalErrors).append("\n");
		report.append("  总警告数: ").append(totalWarnings).append("\n");
		report.append("  数据质量评分: ").append(String.format("%.1f/100", getDataQualityScore())).append("\n\n");

		// 序列验证结果
		if (seriesValidation != null) {
			report.append("序列验证:\n");
			report.append("  状态: ").append(seriesValidation.getStatus()).append("\n");
			if (seriesValidation.hasWarnings()) {
				report.append("  序列警告:\n");
				for (String warning : seriesValidation.getWarnings()) {
					report.append("    ⚠️ ").append(warning).append("\n");
				}
			}
		}

		// 常见问题汇总
		report.append("\n常见问题:\n");
		if (totalErrors == 0 && totalWarnings == 0) {
			report.append("  ✅ 未发现数据问题\n");
		} else {
			// 这里可以添加具体的问题分类统计
			report.append("  📊 详细问题请查看验证日志\n");
		}

		// 质量评级
		report.append("\n质量评级: ").append(getQualityRating()).append("\n");

		return report.toString();
	}

	private String getQualityRating() {
		double score = getDataQualityScore();
		if (score >= 95)
			return "优秀 ★★★★★";
		if (score >= 85)
			return "良好 ★★★★";
		if (score >= 70)
			return "一般 ★★★";
		if (score >= 60)
			return "及格 ★★";
		return "较差 ★";
	}
}