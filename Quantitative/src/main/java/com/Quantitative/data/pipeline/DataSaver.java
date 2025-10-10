package com.Quantitative.data.pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.data.model.StockBar;
import com.Quantitative.data.model.StockData;

/**
 * 数据存储器 - 负责将处理后的数据保存为CSV文件
 */
public class DataSaver {

	/**
	 * 创建输出目录
	 */
	private void createOutputDirectory(String outputDirectory) {
		File dir = new File(outputDirectory);
		if (!dir.exists()) {
			if (dir.mkdirs()) {
				TradingLogger.debug("DataSaver", "创建输出目录: %s", outputDirectory);
			} else {
				TradingLogger.logSystemError("DataSaver", "createOutputDirectory",
						new Exception("创建输出目录失败: " + outputDirectory));
			}
		}
	}

	/**
	 * 保存处理后的数据到CSV文件
	 */
	/**
	 * 保存处理后的数据到CSV文件
	 */
	public boolean saveData(StockData stockData) {
		if (stockData == null || stockData.isEmpty()) {
			TradingLogger.logRisk("WARN", "DataSaver", "无数据可保存");
			return false;
		}

		String outputDirectory = "data/processed/";
		createOutputDirectory(outputDirectory);

		String symbol = stockData.getSymbol();
		String filename = outputDirectory + symbol + ".csv";
		File file = new File(filename);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			// 写入CSV头部 - 使用逗号分隔，Excel友好
			writer.write("date,open,high,low,close,volume,turnover");
			writer.newLine();

			// 写入数据行 - 使用逗号分隔
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
			List<StockBar> bars = stockData.getBars();

			for (StockBar bar : bars) {
				String line = String.format("%s,%.2f,%.2f,%.2f,%.2f,%d,%.1f", bar.getTimestamp().format(formatter),
						bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume(), bar.getTurnover());
				writer.write(line);
				writer.newLine();
			}

			TradingLogger.debug("DataSaver", "数据保存成功: %s (%d 条记录)", filename, bars.size());

			// 打印前几行作为验证
			printSampleData(bars, symbol);

			return true;

		} catch (IOException e) {
			TradingLogger.logSystemError("DataSaver", "saveData", e);
			return false;
		}
	}

	/**
	 * 打印样本数据用于验证
	 */
	private void printSampleData(List<StockBar> bars, String symbol) {
		System.out.println("=== CSV文件样本数据验证 ===");
		System.out.println("股票代码: " + symbol);
		System.out.println("总记录数: " + bars.size());
		System.out.println("表头: date\topen\thigh\tlow\tclose\tvolume\tturnover");

		// 打印前5条数据
		int sampleCount = Math.min(5, bars.size());
		for (int i = 0; i < sampleCount; i++) {
			StockBar bar = bars.get(i);
			String sampleLine = String.format("%s\t%.2f\t%.2f\t%.2f\t%.2f\t%d\t%.1f",
					bar.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy/M/d")), bar.getOpen(), bar.getHigh(),
					bar.getLow(), bar.getClose(), bar.getVolume(), bar.getTurnover());
			System.out.println("样例 " + (i + 1) + ": " + sampleLine);
		}
		System.out.println("==========================");
	}
}