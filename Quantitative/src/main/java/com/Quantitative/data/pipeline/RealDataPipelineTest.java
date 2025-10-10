package com.Quantitative.data.pipeline;

/**
 * @author 测试真实数据流水线
 *
 */
public class RealDataPipelineTest {
	public static void main(String[] args) {
		try {
			// 创建数据流水线
			DataPipeline pipeline = new DataPipeline();

			// 测试处理真实股票数据
			System.out.println("=== 开始测试真实数据流水线 ===");
			boolean success = pipeline.processStockData("000001", "20240101", "20240131");

			if (success) {
				System.out.println("✅ 真实数据处理成功！");
			} else {
				System.out.println("❌ 真实数据处理失败，检查AKShare服务连接");
			}

			// 可以测试更多股票
			// pipeline.processStockData("600519", "20240101", "20240131");
			// pipeline.processStockData("000858", "20240101", "20240131");

		} catch (Exception e) {
			System.err.println("测试失败: " + e.getMessage());
			e.printStackTrace();
		}
	}
}