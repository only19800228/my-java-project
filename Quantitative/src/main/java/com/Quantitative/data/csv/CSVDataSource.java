package com.Quantitative.data.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.DataInfo;
import com.Quantitative.data.DataSource;
import com.Quantitative.data.validation.DataQualityReport;
import com.Quantitative.data.validation.DataValidator;

/**
 * CSV文件数据源 - 从本地CSV文件加载数据
 */
public class CSVDataSource implements DataSource {

	private String dataDirectory;
	private Map<String, List<BarEvent>> cache;
	private String status = "CREATED";
	private boolean cacheEnabled = true;

	// CSV文件格式配置 - 修复日期格式
	private String dateFormat = "yyyy-MM-dd";
	private LocalTime defaultTime = LocalTime.of(15, 0); // 默认设置为收盘时间 15:00
	private String[] expectedHeaders = { "date", "open", "high", "low", "close", "volume" };

	public CSVDataSource() {
		this("data/csv");
	}

	public CSVDataSource(String dataDirectory) {
		this.dataDirectory = dataDirectory;
		this.cache = new HashMap<>();
		initializeDataDirectory();
	}

	private void initializeDataDirectory() {
		File dir = new File(dataDirectory);
		if (!dir.exists()) {
			boolean created = dir.mkdirs();
			if (created) {
				System.out.println("? 创建数据目录: " + dataDirectory);
			}
		}
	}

	@Override
	public void initialize() {
		System.out.println("初始化CSV数据源: " + dataDirectory);
		this.status = "INITIALIZED";

		// 预加载缓存（可选）
		if (cacheEnabled) {
			preloadCache();
		}
	}

	@Override
	public void configure(Map<String, Object> config) {
		if (config != null) {
			if (config.containsKey("dataDirectory")) {
				this.dataDirectory = (String) config.get("dataDirectory");
				initializeDataDirectory();
			}
			if (config.containsKey("cacheEnabled")) {
				this.cacheEnabled = (Boolean) config.get("cacheEnabled");
			}
			if (config.containsKey("dateFormat")) {
				this.dateFormat = (String) config.get("dateFormat");
			}
		}
	}

	@Override
	public String getName() {
		return "CSVDataSource";
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public void reset() {
		cache.clear();
		this.status = "RESET";
	}

	@Override
	public void shutdown() {
		cache.clear();
		this.status = "SHUTDOWN";
		System.out.println("CSV数据源已关闭");
	}

	@Override
	public List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end) {
		String filename = getFilename(symbol);
		File file = new File(dataDirectory, filename);

		if (!file.exists()) {
			System.out.println("? CSV文件不存在: " + file.getAbsolutePath());
			return new ArrayList<>();
		}

		try {
			List<BarEvent> bars = loadFromCSV(file, symbol);

			// 过滤时间范围
			List<BarEvent> filteredBars = filterByDateRange(bars, start, end);

			// 数据验证
			List<BarEvent> validatedBars = validateAndRepairData(filteredBars);

			// 缓存数据
			if (cacheEnabled) {
				cache.put(symbol, validatedBars);
			}

			System.out.printf("? 从CSV加载数据: %s, %d条记录%n", symbol, validatedBars.size());
			return validatedBars;

		} catch (IOException e) {
			System.err.println("加载CSV文件失败: " + e.getMessage());
			return new ArrayList<>();
		}
	}

	/**
	 * 从CSV文件加载数据 - 修复日期解析
	 */
	private List<BarEvent> loadFromCSV(File file, String symbol) throws IOException {
		List<BarEvent> bars = new ArrayList<>();
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			boolean isFirstLine = true;
			int lineNumber = 0;

			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (isFirstLine) {
					isFirstLine = false;
					// 跳过表头或验证表头
					if (!validateHeader(line)) {
						System.out.println("? CSV表头格式不匹配: " + line);
					}
					continue;
				}

				BarEvent bar = parseCSVLine(line, symbol, dateFormatter, lineNumber);
				if (bar != null) {
					bars.add(bar);
				}
			}
		}

		return bars;
	}

	/**
	 * 解析CSV行 - 修复日期时间处理
	 */
	private BarEvent parseCSVLine(String line, String symbol, DateTimeFormatter dateFormatter, int lineNumber) {
		try {
			String[] parts = line.split(",");
			if (parts.length < 6) {
				System.out.printf("? 第%d行数据不完整: %s%n", lineNumber, line);
				return null;
			}

			// 解析日期 - 修复：先解析为LocalDate，再转换为LocalDateTime
			String dateStr = parts[0].trim();
			LocalDate localDate = LocalDate.parse(dateStr, dateFormatter);
			LocalDateTime timestamp = LocalDateTime.of(localDate, defaultTime);

			// 解析价格和成交量
			double open = safeParseDouble(parts[1].trim(), lineNumber, "open");
			double high = safeParseDouble(parts[2].trim(), lineNumber, "high");
			double low = safeParseDouble(parts[3].trim(), lineNumber, "low");
			double close = safeParseDouble(parts[4].trim(), lineNumber, "close");
			long volume = safeParseLong(parts[5].trim(), lineNumber, "volume");

			// 可选：成交额
			double turnover = parts.length > 6 ? safeParseDouble(parts[6].trim(), lineNumber, "turnover") : 0.0;

			// 验证数据有效性
			if (open <= 0 || high <= 0 || low <= 0 || close <= 0 || volume < 0) {
				System.out.printf("? 第%d行数据无效: %s%n", lineNumber, line);
				return null;
			}

			// 验证价格合理性
			if (high < low || high < open || high < close || low > open || low > close) {
				System.out.printf("? 第%d行价格数据不合理: %s%n", lineNumber, line);
				return null;
			}

			return new BarEvent(timestamp, symbol, open, high, low, close, volume, turnover);

		} catch (Exception e) {
			System.out.printf("? 解析第%d行失败: %s - %s%n", lineNumber, line, e.getMessage());
			return null;
		}
	}

	/**
	 * 安全的double解析
	 */
	private double safeParseDouble(String value, int lineNumber, String fieldName) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			System.out.printf("? 第%d行 %s 格式错误: %s%n", lineNumber, fieldName, value);
			return 0.0;
		}
	}

	/**
	 * 安全的long解析
	 */
	private long safeParseLong(String value, int lineNumber, String fieldName) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			System.out.printf("? 第%d行 %s 格式错误: %s%n", lineNumber, fieldName, value);
			return 0L;
		}
	}

	/**
	 * 保存数据到CSV文件 - 保持一致的格式
	 */
	public void saveToCSV(String symbol, List<BarEvent> bars) throws IOException {
		String filename = getFilename(symbol);
		File file = new File(dataDirectory, filename);

		// 确保目录存在
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			// 写入表头
			writer.write("date,open,high,low,close,volume,turnover\n");

			// 写入数据
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
			for (BarEvent bar : bars) {
				String line = String.format("%s,%.4f,%.4f,%.4f,%.4f,%d,%.2f\n", bar.getTimestamp().format(formatter), // 只格式化日期部分
						bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume(), bar.getTurnover());
				writer.write(line);
			}
		}

		System.out.printf("? 数据已保存到CSV: %s, %d条记录%n", filename, bars.size());
	}

	// 00000000000000000000000000

	/**
	 * 解析CSV行
	 */
	private BarEvent parseCSVLine(String line, String symbol, DateTimeFormatter formatter) {
		try {
			String[] parts = line.split(",");
			if (parts.length < 6) {
				System.out.println("? CSV行数据不完整: " + line);
				return null;
			}

			// 解析日期
			LocalDateTime timestamp = LocalDateTime.parse(parts[0].trim(), formatter);

			// 解析价格和成交量
			double open = Double.parseDouble(parts[1].trim());
			double high = Double.parseDouble(parts[2].trim());
			double low = Double.parseDouble(parts[3].trim());
			double close = Double.parseDouble(parts[4].trim());
			long volume = Long.parseLong(parts[5].trim());

			// 可选：成交额
			double turnover = parts.length > 6 ? Double.parseDouble(parts[6].trim()) : 0.0;

			return new BarEvent(timestamp, symbol, open, high, low, close, volume, turnover);

		} catch (Exception e) {
			System.out.println("? 解析CSV行失败: " + line + " - " + e.getMessage());
			return null;
		}
	}

	/**
	 * 验证CSV表头
	 */
	private boolean validateHeader(String headerLine) {
		String[] headers = headerLine.split(",");
		if (headers.length < expectedHeaders.length) {
			return false;
		}

		for (int i = 0; i < expectedHeaders.length; i++) {
			if (!headers[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 从AKShare数据源同步数据到CSV
	 */
	public void syncFromAKShare(com.Quantitative.data.AKShareDataFeed akShareFeed, String symbol, LocalDateTime start,
			LocalDateTime end) {
		try {
			System.out.printf("从AKShare同步数据到CSV: %s %s 到 %s%n", symbol, start, end);

			List<BarEvent> bars = akShareFeed.loadHistoricalData(symbol, start, end);
			if (!bars.isEmpty()) {
				saveToCSV(symbol, bars);
				System.out.println("? 数据同步完成");
			} else {
				System.out.println("? 无数据可同步");
			}

		} catch (Exception e) {
			System.err.println("数据同步失败: " + e.getMessage());
		}
	}

	/**
	 * 数据验证和修复
	 */
	private List<BarEvent> validateAndRepairData(List<BarEvent> bars) {
		List<BarEvent> validatedBars = new ArrayList<>();
		int repairedCount = 0;

		for (BarEvent bar : bars) {
			DataValidator.ValidationResult result = DataValidator.validateBar(bar);

			if (result.isValid()) {
				validatedBars.add(bar);
			} else {
				// 尝试修复
				BarEvent repairedBar = DataValidator.repairBarData(bar);
				DataValidator.ValidationResult repairResult = DataValidator.validateBar(repairedBar);

				if (repairResult.isValid()) {
					validatedBars.add(repairedBar);
					repairedCount++;
				}
			}
		}

		if (repairedCount > 0) {
			System.out.printf("? 数据修复: %d条记录已修复%n", repairedCount);
		}

		return validatedBars;
	}

	/**
	 * 按时间范围过滤数据
	 */
	private List<BarEvent> filterByDateRange(List<BarEvent> bars, LocalDateTime start, LocalDateTime end) {
		List<BarEvent> filtered = new ArrayList<>();

		for (BarEvent bar : bars) {
			LocalDateTime timestamp = bar.getTimestamp();
			if ((start == null || !timestamp.isBefore(start)) && (end == null || !timestamp.isAfter(end))) {
				filtered.add(bar);
			}
		}

		return filtered;
	}

	/**
	 * 预加载缓存
	 */
	private void preloadCache() {
		File dir = new File(dataDirectory);
		File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));

		if (files != null) {
			for (File file : files) {
				String symbol = file.getName().replace(".csv", "");
				try {
					List<BarEvent> bars = loadFromCSV(file, symbol);
					cache.put(symbol, bars);
					System.out.printf("? 预加载缓存: %s, %d条记录%n", symbol, bars.size());
				} catch (IOException e) {
					System.err.println("预加载缓存失败: " + file.getName());
				}
			}
		}
	}

	private String getFilename(String symbol) {
		return symbol + ".csv";
	}

	@Override
	public DataInfo getDataInfo() {
		// 实现数据信息获取
		return new DataInfo("CSV", null, null, cache.size(), "file");
	}

	@Override
	public boolean isConnected() {
		File dir = new File(dataDirectory);
		return dir.exists() && dir.isDirectory();
	}

	@Override
	public String getDataSourceType() {
		return "CSV_FILE";
	}

	@Override
	public List<String> getAvailableSymbols() {
		List<String> symbols = new ArrayList<>();
		File dir = new File(dataDirectory);
		File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));

		if (files != null) {
			for (File file : files) {
				symbols.add(file.getName().replace(".csv", ""));
			}
		}

		return symbols;
	}

	@Override
	public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
		List<BarEvent> bars = loadHistoricalData(symbol, start, end);
		DataQualityReport report = new DataQualityReport(symbol, start, end);

		for (BarEvent bar : bars) {
			DataValidator.ValidationResult result = DataValidator.validateBar(bar);
			report.addValidationResult(result);
		}

		DataValidator.ValidationResult seriesResult = DataValidator.validatePriceSeries(bars);
		report.setSeriesValidation(seriesResult);

		return report;
	}
}