package com.Quantitative.data.pipeline;

import java.util.Arrays;
import java.util.List;

/**
 * @author 数据流水线 测试 获取数据 加工数据 验证数据质量 保存CSV
 *
 */
public class DataPipelineTest {
	public static void main(String[] args) {
		// 创建数据流水线
		DataPipeline pipeline = new DataPipeline();

		// 测试处理股票数据
		boolean success = pipeline.processStockData("000001", "20230101", "20231231");
		System.out.println("数据处理结果: " + (success ? "成功" : "失败"));

		// 测试批量处理
		List<String> symbols = Arrays.asList("000001", "601398", "601985", "600079", "600660", "000021", "300059",
				"600097");
		pipeline.processMultipleStocks(symbols, "20070101", "20251009");
	}
}