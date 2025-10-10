package com.Quantitative.data.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.data.model.StockBar;
import com.Quantitative.data.model.StockData;

/**
 * CSV数据加载器 - 负责加载处理后的CSV文件
 */
public class CSVDataLoader {
	private final String dataDirectory;

	public CSVDataLoader() {
		this("data/processed/");
	}

	public CSVDataLoader(String dataDirectory) {
		this.dataDirectory = dataDirectory;
		ensureDirectoryExists();
	}

	/**
	 * 确保数据目录存在
	 */
	private void ensureDirectoryExists() {
		File dir = new File(dataDirectory);
		if (!dir.exists()) {
			if (dir.mkdirs()) {
				TradingLogger.debug("CSVDataLoader", "创建数据目录: %s", dataDirectory);
			} else {
				TradingLogger.logSystemError("CSVDataLoader", "ensureDirectoryExists",
						new Exception("创建数据目录失败: " + dataDirectory));
			}
		}
	}

	/**
	 * 加载股票数据
	 */
	public StockData loadStockData(String symbol) {
		String filename = dataDirectory + symbol + ".csv";
		File file = new File(filename);

		if (!file.exists()) {
			TradingLogger.logRisk("ERROR", "CSVDataLoader", "CSV文件不存在: %s", filename);
			return null;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			List<StockBar> bars = new ArrayList<>();
			String line;
			boolean isFirstLine = true;
			int lineCount = 0;
			int validLineCount = 0;

			TradingLogger.debug("CSVDataLoader", "开始加载CSV文件: %s", filename);

			while ((line = reader.readLine()) != null) {
				lineCount++;

				// 跳过表头
				if (isFirstLine) {
					isFirstLine = false;
					TradingLogger.debug("CSVDataLoader", "表头: %s", line);
					continue;
				}

				// 跳过空行
				if (line.trim().isEmpty()) {
					continue;
				}

				// 解析数据行
				StockBar bar = parseCSVLine(line, symbol);
				if (bar != null) {
					bars.add(bar);
					validLineCount++;
				} else {
					TradingLogger.logRisk("WARN", "CSVDataLoader", "解析失败，跳过行 %d: %s", lineCount, line);
				}
			}

			TradingLogger.debug("CSVDataLoader", "CSV文件加载完成: %s (%d/%d 行有效数据)", symbol, validLineCount, lineCount - 1);

			if (bars.isEmpty()) {
				TradingLogger.logRisk("ERROR", "CSVDataLoader", "没有有效数据: %s", symbol);
				return null;
			}

			// 创建StockData对象
			return new StockData(symbol, bars, createDataInfo(symbol, bars));

		} catch (IOException e) {
			TradingLogger.logSystemError("CSVDataLoader", "loadStockData", e);
			return null;
		}
	}

	/**
	 * 解析CSV数据行 - 修复为逗号分隔
	 */
	private StockBar parseCSVLine(String line, String symbol) {
		try {
			// 使用逗号分割（因为CSV文件是逗号分隔的）
			String[] fields = line.split(",");

			if (fields.length < 7) {
				TradingLogger.logRisk("WARN", "CSVDataLoader", "数据字段不足，跳过行: %s (字段数: %d)", line, fields.length);
				return null;
			}

			// 解析字段
			String dateStr = fields[0].trim();
			double open = Double.parseDouble(fields[1].trim());
			double high = Double.parseDouble(fields[2].trim());
			double low = Double.parseDouble(fields[3].trim());
			double close = Double.parseDouble(fields[4].trim());
			long volume = Long.parseLong(fields[5].trim());
			double turnover = Double.parseDouble(fields[6].trim());

			// 解析日期
			LocalDateTime timestamp = parseDate(dateStr);
			if (timestamp == null) {
				TradingLogger.logRisk("WARN", "CSVDataLoader", "日期解析失败，跳过行: %s", dateStr);
				return null;
			}

			return new StockBar(symbol, timestamp, open, high, low, close, volume, turnover);

		} catch (Exception e) {
			TradingLogger.logRisk("ERROR", "CSVDataLoader", "解析CSV行失败: %s - %s", line, e.getMessage());
			return null;
		}
	}

	/**
	 * 解析日期字符串 - 修复日期解析问题
	 */
	private LocalDateTime parseDate(String dateStr) {
		try {
			// 支持多种日期格式
			DateTimeFormatter[] formatters = { DateTimeFormatter.ofPattern("yyyy/M/d"),
					DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("yyyyMMdd") };

			for (DateTimeFormatter formatter : formatters) {
				try {
					// 尝试解析为LocalDate，然后转换为LocalDateTime
					java.time.LocalDate date = java.time.LocalDate.parse(dateStr, formatter);
					return date.atTime(15, 0, 0); // 设置为下午3点（收盘时间）
				} catch (DateTimeParseException e) {
					// 继续尝试下一个格式
					continue;
				}
			}

			// 如果上面的格式都不行，尝试直接解析
			try {
				// 对于格式 "2024/1/2" 直接处理
				String[] dateParts = dateStr.split("/");
				if (dateParts.length == 3) {
					int year = Integer.parseInt(dateParts[0]);
					int month = Integer.parseInt(dateParts[1]);
					int day = Integer.parseInt(dateParts[2]);
					return LocalDateTime.of(year, month, day, 15, 0, 0);
				}
			} catch (Exception e) {
				TradingLogger.logRisk("WARN", "CSVDataLoader", "日期解析失败: %s - %s", dateStr, e.getMessage());
			}

			return null;

		} catch (Exception e) {
			TradingLogger.logSystemError("CSVDataLoader", "parseDate", e);
			return null;
		}
	}

	/**
	 * 创建数据信息
	 */
	private com.Quantitative.data.DataInfo createDataInfo(String symbol, List<StockBar> bars) {
		if (bars == null || bars.isEmpty()) {
			return new com.Quantitative.data.DataInfo(symbol, null, null, 0, "1d");
		}

		LocalDateTime startTime = bars.get(0).getTimestamp();
		LocalDateTime endTime = bars.get(bars.size() - 1).getTimestamp();

		return new com.Quantitative.data.DataInfo.Builder(symbol).startTime(startTime).endTime(endTime)
				.barCount(bars.size()).timeframe("1d").dataSource("CSV").dataQuality("PROCESSED").build();
	}

	/**
	 * 检查文件是否存在
	 */
	public boolean dataExists(String symbol) {
		String filename = dataDirectory + symbol + ".csv";
		File file = new File(filename);
		return file.exists() && file.length() > 0;
	}

	/**
	 * 获取可用股票代码列表
	 */
	public List<String> getAvailableSymbols() {
		List<String> symbols = new ArrayList<>();
		File directory = new File(dataDirectory);

		if (!directory.exists() || !directory.isDirectory()) {
			TradingLogger.logRisk("WARN", "CSVDataLoader", "数据目录不存在: %s", dataDirectory);
			return symbols;
		}

		File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

		if (files != null) {
			for (File file : files) {
				String fileName = file.getName();
				String symbol = fileName.substring(0, fileName.lastIndexOf('.'));
				symbols.add(symbol);
			}
		}

		TradingLogger.debug("CSVDataLoader", "找到 %d 个可用股票数据文件", symbols.size());
		return symbols;
	}

	/**
	 * 检查CSV文件格式（调试用）
	 */
	public void inspectCSVFile(String symbol) {
		String filename = dataDirectory + symbol + ".csv";
		File file = new File(filename);

		if (!file.exists()) {
			System.out.println("❌ 文件不存在: " + filename);
			return;
		}

		System.out.println("=== CSV文件格式检查 ===");
		System.out.println("文件: " + filename);
		System.out.println("大小: " + file.length() + " bytes");

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			int lineNum = 0;

			while ((line = reader.readLine()) != null && lineNum < 5) {
				lineNum++;
				System.out.println("第" + lineNum + "行: " + line);
				System.out.println("  分隔符分析: " + analyzeSeparator(line));

				// 如果是数据行，显示日期字段
				if (lineNum > 1) {
					String[] fields = line.split(",");
					if (fields.length > 0) {
						System.out.println("  日期字段: '" + fields[0] + "'");
						testDateParsing(fields[0]);
					}
				}
			}

			if (lineNum == 0) {
				System.out.println("⚠ 文件为空");
			}

		} catch (IOException e) {
			System.err.println("读取文件失败: " + e.getMessage());
		}
	}

	/**
	 * 测试日期解析
	 */
	private void testDateParsing(String dateStr) {
		System.out.println("  日期解析测试:");

		// 测试方法1: 直接解析
		try {
			String[] parts = dateStr.split("/");
			if (parts.length == 3) {
				int year = Integer.parseInt(parts[0]);
				int month = Integer.parseInt(parts[1]);
				int day = Integer.parseInt(parts[2]);
				LocalDateTime result = LocalDateTime.of(year, month, day, 15, 0);
				System.out.println("    ✓ 直接解析: " + result);
			}
		} catch (Exception e) {
			System.out.println("    ✗ 直接解析失败: " + e.getMessage());
		}

		// 测试方法2: 格式化解析
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
			java.time.LocalDate date = java.time.LocalDate.parse(dateStr, formatter);
			LocalDateTime result = date.atTime(15, 0);
			System.out.println("    ✓ 格式化解析: " + result);
		} catch (Exception e) {
			System.out.println("    ✗ 格式化解析失败: " + e.getMessage());
		}
	}

	/**
	 * 分析分隔符类型
	 */
	private String analyzeSeparator(String line) {
		int commaCount = line.length() - line.replace(",", "").length();
		int tabCount = line.length() - line.replace("\t", "").length();

		if (commaCount > 0 && tabCount == 0) {
			return "逗号分隔 (," + commaCount + "个逗号)";
		} else if (tabCount > 0 && commaCount == 0) {
			return "制表符分隔 (\\t," + tabCount + "个制表符)";
		} else if (commaCount > 0 && tabCount > 0) {
			return "混合分隔 (," + commaCount + "个逗号, \\t" + tabCount + "个制表符)";
		} else {
			return "无分隔符或其他分隔符";
		}
	}
}