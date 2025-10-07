package com.Quantitative;

/**
 * 控制台颜色打印工具类 支持在 Windows、Linux、Mac 系统上显示彩色文字
 */
public class ColorPrinter {

	static {
		initAnsiSupport();
	}

	// ANSI 颜色代码
	public static final String RESET = "\u001B[0m";
	public static final String BLACK = "\u001B[30m";
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String YELLOW = "\u001B[33m";
	public static final String BLUE = "\u001B[34m";
	public static final String PURPLE = "\u001B[35m";
	public static final String CYAN = "\u001B[36m";
	public static final String WHITE = "\u001B[37m";

	// 亮色
	public static final String BRIGHT_BLACK = "\u001B[90m";
	public static final String BRIGHT_RED = "\u001B[91m";
	public static final String BRIGHT_GREEN = "\u001B[92m";
	public static final String BRIGHT_YELLOW = "\u001B[93m";
	public static final String BRIGHT_BLUE = "\u001B[94m";
	public static final String BRIGHT_PURPLE = "\u001B[95m";
	public static final String BRIGHT_CYAN = "\u001B[96m";
	public static final String BRIGHT_WHITE = "\u001B[97m";

	// 背景色
	public static final String BG_RED = "\u001B[41m";
	public static final String BG_GREEN = "\u001B[42m";
	public static final String BG_YELLOW = "\u001B[43m";
	public static final String BG_BLUE = "\u001B[44m";

	/**
	 * 初始化 ANSI 支持（主要针对 Windows 系统）
	 */
	private static void initAnsiSupport() {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			try {
				new ProcessBuilder("cmd", "/c", "color").inheritIO().start().waitFor();
			} catch (Exception e) {
				// 忽略错误，如果无法启用 ANSI 支持，颜色代码可能不会显示
				System.err.println("警告: 无法启用 ANSI 颜色支持");
			}
		}
	}

	/**
	 * 打印红色文字
	 */
	public static void printfRed(String format, Object... args) {
		System.out.printf(RED + format + RESET, args);
	}

	/**
	 * 打印绿色文字
	 */
	public static void printfGreen(String format, Object... args) {
		System.out.printf(GREEN + format + RESET, args);
	}

	/**
	 * 打印黄色文字
	 */
	public static void printfYellow(String format, Object... args) {
		System.out.printf(YELLOW + format + RESET, args);
	}

	/**
	 * 打印蓝色文字
	 */
	public static void printfBlue(String format, Object... args) {
		System.out.printf(BLUE + format + RESET, args);
	}

	/**
	 * 自定义颜色打印
	 */
	public static void printfColor(String color, String format, Object... args) {
		System.out.printf(color + format + RESET, args);
	}

	/**
	 * 打印红色文字（带换行）
	 */
	public static void printlnRed(String message) {
		System.out.println(RED + message + RESET);
	}

	/**
	 * 打印绿色文字（带换行）
	 */
	public static void printlnGreen(String message) {
		System.out.println(GREEN + message + RESET);
	}

	/**
	 * 打印黄色文字（带换行）
	 */
	public static void printlnYellow(String message) {
		System.out.println(YELLOW + message + RESET);
	}

	/**
	 * 打印成功信息（绿色）
	 */
	public static void printSuccess(String format, Object... args) {
		printfGreen("✓ " + format, args);
	}

	/**
	 * 打印错误信息（红色）
	 */
	public static void printError(String format, Object... args) {
		printfRed("✗ " + format, args);
	}

	/**
	 * 打印警告信息（黄色）
	 */
	public static void printWarning(String format, Object... args) {
		printfYellow("⚠ " + format, args);
	}

	/**
	 * 打印信息（蓝色）
	 */
	public static void printInfo(String format, Object... args) {
		printfBlue("ℹ " + format, args);
	}
}