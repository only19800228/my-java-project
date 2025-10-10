package com.Quantitative.data.repository;

import java.util.List;

/**
 * @author 检查CSV文件格式
 *
 */
public class CSVFormatChecker {
	public static void main(String[] args) {
		System.out.println("=== CSV文件格式检查 ===");

		CSVDataLoader loader = new CSVDataLoader();

		// 检查可用股票
		List<String> symbols = loader.getAvailableSymbols();
		System.out.println("可用股票: " + symbols);

		if (!symbols.isEmpty()) {
			String testSymbol = symbols.get(0);
			loader.inspectCSVFile(testSymbol);
		}
	}
}