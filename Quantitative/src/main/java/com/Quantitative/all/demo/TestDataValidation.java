package com.Quantitative.all.demo;

import java.time.LocalDateTime;

import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * @author 数据质量测试
 *
 */
public class TestDataValidation {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		testDataValidation();
	}

	// 在你的测试类中使用数据验证

	public static void testDataValidation() {
		AKShareDataFeed dataFeed = new AKShareDataFeed();
		dataFeed.setDebugMode(true);

		// 获取数据质量报告
		DataQualityReport report = dataFeed.getDataService().getDataQualityReport("000001",
				LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 12, 31, 0, 0));

		System.out.println(report.generateReport());

		// 根据质量评分决定是否使用数据
		if (report.getDataQualityScore() > 80) {
			System.out.println("✅ 数据质量良好，可以用于回测");
		} else {
			System.out.println("⚠️ 数据质量较差，建议检查数据源");
		}
	}

}
