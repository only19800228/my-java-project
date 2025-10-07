package com.Quantitative;

public class ColorPrinterDemo {
	public static void main(String[] args) {
		// 基本颜色打印
		ColorPrinter.printfRed("这是红色文字: %s\n", "错误信息");
		ColorPrinter.printfGreen("这是绿色文字: %s\n", "成功信息");
		ColorPrinter.printfYellow("这是黄色文字: %s\n", "警告信息");
		ColorPrinter.printfBlue("这是蓝色文字: %s\n", "普通信息");

		// 带图标的功能性打印
		ColorPrinter.printSuccess("操作成功完成\n");
		ColorPrinter.printError("文件读取失败\n");
		ColorPrinter.printWarning("磁盘空间不足\n");
		ColorPrinter.printInfo("系统正在启动\n");

		// 自定义颜色
		ColorPrinter.printfColor(ColorPrinter.PURPLE, "这是紫色文字\n");
		ColorPrinter.printfColor(ColorPrinter.BRIGHT_CYAN, "这是亮青色文字\n");

		// 带背景色
		ColorPrinter.printfColor(ColorPrinter.BG_RED + ColorPrinter.WHITE, "白字红背景: %s\n", "重要提示");
		System.out.println("123");
	}
}