package com.Quantitative.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.formdev.flatlaf.FlatLightLaf;

public class QuantTradingUI extends JFrame {
	private JTable stockTable;
	private JTable positionTable;
	private JTable tradeTable;
	private JTable strategyTable;
	private JComboBox<String> strategyComboBox;
	private JComboBox<String> refreshIntervalComboBox;
	private JLabel cashLabel;
	private JLabel totalAssetsLabel;
	private JLabel todayProfitLabel;
	private JLabel statusLabel;
	private JLabel riskIndicatorLabel;

	// 量化交易专用组件
	private JProgressBar riskProgressBar;
	private JLabel sharpeRatioLabel;
	private JLabel maxDrawdownLabel;
	private ChartPanel klineChartPanel;
	private ChartPanel performanceChartPanel;

	// 颜色定义
	private final Color PRIMARY_COLOR = new Color(0, 122, 255);
	private final Color SUCCESS_COLOR = new Color(52, 199, 89);
	private final Color WARNING_COLOR = new Color(255, 149, 0);
	private final Color DANGER_COLOR = new Color(255, 59, 48);
	private final Color BACKGROUND_COLOR = new Color(242, 242, 247);
	private final Color CARD_COLOR = Color.WHITE;

	// 量化数据
	private Map<String, StrategyPerformance> strategyPerformanceMap = new HashMap<>();
	private RiskManager riskManager = new RiskManager();

	public QuantTradingUI() {
		setupFlatLaf();
		initComponents();
		initQuantData();
	}

	private void setupFlatLaf() {
		try {
			UIManager.put("Button.arc", 8);
			UIManager.put("Component.arc", 8);
			UIManager.put("ProgressBar.arc", 8);
			UIManager.put("TextComponent.arc", 8);

			UIManager.put("Table.showHorizontalLines", true);
			UIManager.put("Table.showVerticalLines", false);
			UIManager.put("Table.rowHeight", 32);
			UIManager.put("TableHeader.height", 35);
			UIManager.put("TableHeader.font", UIManager.getFont("TableHeader.font").deriveFont(Font.BOLD));

			FlatLightLaf.setup();
		} catch (Exception ex) {
			System.err.println("Failed to initialize FlatLaf");
		}
	}

	private void initQuantData() {
		// 初始化策略绩效数据
		strategyPerformanceMap.put("均线策略", new StrategyPerformance(0.152, 0.086, -0.023, 1.82, 0.68));
		strategyPerformanceMap.put("RSI策略", new StrategyPerformance(0.134, 0.072, -0.018, 1.65, 0.62));
		strategyPerformanceMap.put("MACD策略", new StrategyPerformance(0.118, 0.064, -0.015, 1.45, 0.59));

		// 初始化风险指标
		riskManager.updateRiskMetrics(0.234, 0.045, 0.892);
	}

	private void initComponents() {
		setTitle("📈 智能量化交易系统 v2.0");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1400, 900);
		setLocationRelativeTo(null);

		// 创建主面板
		JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		mainPanel.setBackground(BACKGROUND_COLOR);

		// 添加顶部量化仪表盘
		mainPanel.add(createQuantDashboard(), BorderLayout.NORTH);

		// 添加中部选项卡
		mainPanel.add(createCenterTabbedPane(), BorderLayout.CENTER);

		// 添加底部状态栏
		mainPanel.add(createStatusBar(), BorderLayout.SOUTH);

		setContentPane(mainPanel);
	}

	private JPanel createQuantDashboard() {
		JPanel dashboard = new JPanel(new BorderLayout(0, 10));
		dashboard.setBackground(BACKGROUND_COLOR);

		// 第一行：关键指标
		JPanel metricsPanel = new JPanel(new GridLayout(1, 6, 10, 0));
		metricsPanel.setBackground(BACKGROUND_COLOR);

		// 账户资产指标
		JPanel assetPanel = createMetricCard("💰 总资产", "1,234,567.89", "+2.34%", SUCCESS_COLOR, "较昨日");
		JPanel cashPanel = createMetricCard("💵 可用资金", "987,654.32", "充足", PRIMARY_COLOR, "可动用");
		JPanel profitPanel = createMetricCard("📊 今日盈亏", "+23,456.78", "+1.89%", SUCCESS_COLOR, "实时");
		JPanel positionPanel = createMetricCard("📈 持仓市值", "246,913.57", "68.2%", WARNING_COLOR, "仓位比例");

		metricsPanel.add(assetPanel);
		metricsPanel.add(cashPanel);
		metricsPanel.add(profitPanel);
		metricsPanel.add(positionPanel);

		// 风险指标
		JPanel riskPanel = createRiskMetricCard();
		JPanel performancePanel = createPerformanceMetricCard();

		metricsPanel.add(riskPanel);
		metricsPanel.add(performancePanel);

		dashboard.add(metricsPanel, BorderLayout.NORTH);

		// 第二行：策略状态和快速操作
		JPanel strategyPanel = createStrategyStatusPanel();
		dashboard.add(strategyPanel, BorderLayout.CENTER);

		return dashboard;
	}

	private JPanel createMetricCard(String title, String value, String change, Color color, String description) {
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(CARD_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createEmptyBorder(12, 15, 12, 15)));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		titleLabel.setForeground(new Color(150, 150, 150));

		JLabel valueLabel = new JLabel(value);
		valueLabel.setFont(new Font("PingFang SC", Font.BOLD, 16));
		valueLabel.setForeground(color);

		JLabel changeLabel = new JLabel(change);
		changeLabel.setFont(new Font("PingFang SC", Font.BOLD, 12));
		changeLabel.setForeground(color);

		JLabel descLabel = new JLabel(description);
		descLabel.setFont(new Font("PingFang SC", Font.PLAIN, 10));
		descLabel.setForeground(new Color(180, 180, 180));

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(CARD_COLOR);
		topPanel.add(titleLabel, BorderLayout.WEST);
		topPanel.add(changeLabel, BorderLayout.EAST);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(CARD_COLOR);
		bottomPanel.add(valueLabel, BorderLayout.WEST);
		bottomPanel.add(descLabel, BorderLayout.EAST);

		card.add(topPanel, BorderLayout.NORTH);
		card.add(bottomPanel, BorderLayout.CENTER);

		return card;
	}

	private JPanel createRiskMetricCard() {
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(CARD_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createEmptyBorder(12, 15, 12, 15)));

		JLabel titleLabel = new JLabel("⚡ 风险指标");
		titleLabel.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		titleLabel.setForeground(new Color(150, 150, 150));

		riskProgressBar = new JProgressBar(0, 100);
		riskProgressBar.setValue(35); // 示例风险值
		riskProgressBar.setString("中风险");
		riskProgressBar.setStringPainted(true);
		riskProgressBar.setForeground(WARNING_COLOR);
		riskProgressBar.setBackground(new Color(240, 240, 240));

		riskIndicatorLabel = new JLabel("夏普: 1.82 | 回撤: -2.3%");
		riskIndicatorLabel.setFont(new Font("PingFang SC", Font.PLAIN, 11));
		riskIndicatorLabel.setForeground(new Color(100, 100, 100));

		card.add(titleLabel, BorderLayout.NORTH);
		card.add(riskProgressBar, BorderLayout.CENTER);
		card.add(riskIndicatorLabel, BorderLayout.SOUTH);

		return card;
	}

	private JPanel createPerformanceMetricCard() {
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(CARD_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createEmptyBorder(12, 15, 12, 15)));

		JLabel titleLabel = new JLabel("🚀 策略绩效");
		titleLabel.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		titleLabel.setForeground(new Color(150, 150, 150));

		JLabel sharpeLabel = new JLabel("夏普: 1.82");
		sharpeLabel.setFont(new Font("PingFang SC", Font.BOLD, 12));
		sharpeLabel.setForeground(SUCCESS_COLOR);

		JLabel drawdownLabel = new JLabel("回撤: -2.3%");
		drawdownLabel.setFont(new Font("PingFang SC", Font.BOLD, 12));
		drawdownLabel.setForeground(DANGER_COLOR);

		JPanel metricsPanel = new JPanel(new GridLayout(2, 1, 2, 2));
		metricsPanel.setBackground(CARD_COLOR);
		metricsPanel.add(sharpeLabel);
		metricsPanel.add(drawdownLabel);

		card.add(titleLabel, BorderLayout.NORTH);
		card.add(metricsPanel, BorderLayout.CENTER);

		return card;
	}

	private JPanel createStrategyStatusPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		panel.setBackground(CARD_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createEmptyBorder(10, 15, 10, 15)));

		// 策略状态指示器
		JLabel statusLabel = new JLabel("🎯 策略状态:");
		statusLabel.setFont(new Font("PingFang SC", Font.BOLD, 12));

		JLabel runningLabel = createStatusIndicator("运行中", SUCCESS_COLOR);
		JLabel stoppedLabel = createStatusIndicator("已停止", new Color(150, 150, 150));
		JLabel warningLabel = createStatusIndicator("警告", WARNING_COLOR);

		// 快速操作按钮
		JButton startAllBtn = createSmallButton("▶️ 全部启动", SUCCESS_COLOR);
		JButton stopAllBtn = createSmallButton("⏹️ 全部停止", DANGER_COLOR);
		JButton optimizeBtn = createSmallButton("⚙️ 参数优化", PRIMARY_COLOR);
		JButton backtestBtn = createSmallButton("📊 快速回测", WARNING_COLOR);

		panel.add(statusLabel);
		panel.add(runningLabel);
		panel.add(stoppedLabel);
		panel.add(warningLabel);
		panel.add(Box.createHorizontalStrut(20));
		panel.add(startAllBtn);
		panel.add(stopAllBtn);
		panel.add(optimizeBtn);
		panel.add(backtestBtn);

		return panel;
	}

	private JLabel createStatusIndicator(String text, Color color) {
		JLabel label = new JLabel("● " + text);
		label.setFont(new Font("PingFang SC", Font.PLAIN, 11));
		label.setForeground(color);
		label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
		label.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
		label.setOpaque(true);
		return label;
	}

	private JButton createSmallButton(String text, Color color) {
		JButton button = new JButton(text);
		button.setFont(new Font("PingFang SC", Font.PLAIN, 11));
		button.setBackground(color);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		return button;
	}

	private JTabbedPane createCenterTabbedPane() {
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setFont(new Font("PingFang SC", Font.BOLD, 13));

		// 量化专用选项卡
		tabbedPane.addTab("📈 量化监控", createQuantMonitorPanel());
		tabbedPane.addTab("🤖 策略管理", createStrategyManagementPanel());
		tabbedPane.addTab("📊 回测分析", createBacktestPanel());
		tabbedPane.addTab("💼 持仓管理", createPositionPanel());
		tabbedPane.addTab("📋 交易记录", createTradePanel());
		tabbedPane.addTab("⚡ 风险控制", createRiskPanel());

		return tabbedPane;
	}

	private JPanel createQuantMonitorPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		// 上方：图表区域
		JPanel chartPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		chartPanel.setBackground(BACKGROUND_COLOR);

		// K线图
		JPanel klinePanel = createChartPanel("📈 实时K线", createSampleKLineChart());
		// 绩效图
		JPanel performancePanel = createChartPanel("📊 策略绩效", createSamplePerformanceChart());

		chartPanel.add(klinePanel);
		chartPanel.add(performancePanel);

		panel.add(chartPanel, BorderLayout.CENTER);

		// 下方：股票监控表格
		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.setBackground(BACKGROUND_COLOR);

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		controlPanel.setBackground(CARD_COLOR);
		controlPanel.setBorder(createTitledBorder("🎯 量化监控控制", PRIMARY_COLOR));

		strategyComboBox = new JComboBox<>(
				new String[] { "📈 双均线策略", "📊 RSI反转策略", "📉 MACD趋势策略", "📋 布林带突破", "🎯 动量策略", "🔄 均值回归" });
		styleComboBox(strategyComboBox);

		JButton strategySettingsBtn = createSecondaryButton("⚙️ 策略设置");
		JButton startAutoTradeBtn = createSuccessButton("🤖 启动量化交易");
		JButton manualTradeBtn = createPrimaryButton("👨‍💼 手动交易");

		controlPanel.add(createStyledLabel("运行策略:", Font.BOLD, 12));
		controlPanel.add(strategyComboBox);
		controlPanel.add(strategySettingsBtn);
		controlPanel.add(startAutoTradeBtn);
		controlPanel.add(manualTradeBtn);

		// 股票监控表格
		String[] columns = { "股票名称", "代码", "价格", "涨跌幅", "信号", "强度", "预期收益", "风险等级", "操作" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 8; // 只有操作列可编辑
			}
		};

		// 添加示例数据
		addSampleQuantData(model);

		stockTable = new JTable(model);
		setupQuantTableRenderer();
		setupTableAppearance(stockTable);

		JScrollPane scrollPane = new JScrollPane(stockTable);
		scrollPane.setBorder(createTitledBorder("📊 量化信号监控", SUCCESS_COLOR));

		tablePanel.add(controlPanel, BorderLayout.NORTH);
		tablePanel.add(scrollPane, BorderLayout.CENTER);

		panel.add(tablePanel, BorderLayout.SOUTH);

		return panel;
	}

	private void addSampleQuantData(DefaultTableModel model) {
		Object[][] sampleData = { { "贵州茅台", "600519", "1750.50", "+2.34%", "强烈买入", "0.92", "+5.2%", "低风险", "⚡执行" },
				{ "宁德时代", "300750", "185.60", "-1.23%", "观望", "0.45", "-0.8%", "中风险", "⚡执行" },
				{ "招商银行", "600036", "32.45", "+0.62%", "买入", "0.78", "+2.1%", "低风险", "⚡执行" },
				{ "中国平安", "601318", "48.90", "-0.81%", "卖出", "0.67", "-3.2%", "中风险", "⚡执行" },
				{ "比亚迪", "002594", "245.80", "+3.45%", "强烈买入", "0.95", "+6.8%", "低风险", "⚡执行" } };

		for (Object[] row : sampleData) {
			model.addRow(row);
		}
	}

	private JPanel createChartPanel(String title, JFreeChart chart) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(CARD_COLOR);
		panel.setBorder(createTitledBorder(title, PRIMARY_COLOR));

		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(400, 300));
		panel.add(chartPanel, BorderLayout.CENTER);

		return panel;
	}

	private JFreeChart createSampleKLineChart() {
		TimeSeries series = new TimeSeries("股价");
		// 添加示例数据
		series.add(new Day(1, 1, 2024), 100.0);
		series.add(new Day(2, 1, 2024), 102.5);
		series.add(new Day(3, 1, 2024), 101.2);
		series.add(new Day(4, 1, 2024), 103.8);

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(series);

		return ChartFactory.createTimeSeriesChart("股票价格走势", "日期", "价格", dataset, true, true, false);
	}

	private JFreeChart createSamplePerformanceChart() {
		TimeSeries series = new TimeSeries("策略收益");
		// 添加示例数据
		series.add(new Day(1, 1, 2024), 100.0);
		series.add(new Day(2, 1, 2024), 101.5);
		series.add(new Day(3, 1, 2024), 102.2);
		series.add(new Day(4, 1, 2024), 103.8);

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		dataset.addSeries(series);

		return ChartFactory.createTimeSeriesChart("策略绩效曲线", "日期", "收益率%", dataset, true, true, false);
	}

	private JPanel createStrategyManagementPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		// 策略列表和绩效
		JPanel strategyPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		strategyPanel.setBackground(BACKGROUND_COLOR);

		// 策略列表
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.setBackground(CARD_COLOR);
		listPanel.setBorder(createTitledBorder("🤖 策略列表", PRIMARY_COLOR));

		String[] strategyColumns = { "策略名称", "状态", "年化收益", "夏普比率", "最大回撤", "胜率", "操作" };
		DefaultTableModel strategyModel = new DefaultTableModel(strategyColumns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 6 || column == 1;
			}
		};

		// 添加策略数据
		addStrategyData(strategyModel);

		strategyTable = new JTable(strategyModel);
		setupStrategyTableRenderer();
		setupTableAppearance(strategyTable);

		listPanel.add(new JScrollPane(strategyTable), BorderLayout.CENTER);

		// 策略详情
		JPanel detailPanel = new JPanel(new BorderLayout());
		detailPanel.setBackground(CARD_COLOR);
		detailPanel.setBorder(createTitledBorder("📊 策略详情", SUCCESS_COLOR));

		JTextArea detailArea = new JTextArea();
		detailArea.setText("策略名称: 双均线策略\n\n" + "策略描述: 基于5日和20日移动平均线的趋势跟踪策略\n\n" + "参数设置:\n" + "  - 短期均线: 5日\n"
				+ "  - 长期均线: 20日\n" + "  - 止损: -3%\n" + "  - 止盈: +8%\n\n" + "绩效指标:\n" + "  - 年化收益率: 15.2%\n"
				+ "  - 夏普比率: 1.82\n" + "  - 最大回撤: -2.3%\n" + "  - 胜率: 68%");
		detailArea.setEditable(false);
		detailArea.setFont(new Font("PingFang SC", Font.PLAIN, 12));

		detailPanel.add(new JScrollPane(detailArea), BorderLayout.CENTER);

		strategyPanel.add(listPanel);
		strategyPanel.add(detailPanel);

		panel.add(strategyPanel, BorderLayout.CENTER);

		// 底部操作按钮
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		buttonPanel.setBackground(CARD_COLOR);

		JButton newStrategyBtn = createPrimaryButton("🆕 新建策略");
		JButton editStrategyBtn = createSecondaryButton("✏️ 编辑策略");
		JButton backtestBtn = createWarningButton("📊 策略回测");
		JButton optimizeBtn = createSuccessButton("⚙️ 参数优化");
		JButton deployBtn = createPrimaryButton("🚀 部署策略");

		buttonPanel.add(newStrategyBtn);
		buttonPanel.add(editStrategyBtn);
		buttonPanel.add(backtestBtn);
		buttonPanel.add(optimizeBtn);
		buttonPanel.add(deployBtn);

		panel.add(buttonPanel, BorderLayout.SOUTH);

		return panel;
	}

	private void addStrategyData(DefaultTableModel model) {
		Object[][] strategyData = { { "双均线策略", "运行中", "15.2%", "1.82", "-2.3%", "68%", "管理" },
				{ "RSI反转策略", "已停止", "13.4%", "1.65", "-1.8%", "62%", "管理" },
				{ "MACD趋势策略", "运行中", "11.8%", "1.45", "-1.5%", "59%", "管理" },
				{ "布林带突破", "警告", "9.5%", "1.25", "-3.2%", "55%", "管理" } };

		for (Object[] row : strategyData) {
			model.addRow(row);
		}
	}

	private JPanel createRiskPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		// 风险指标卡片
		JPanel riskMetricsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
		riskMetricsPanel.setBackground(BACKGROUND_COLOR);

		riskMetricsPanel.add(createRiskMetricCard("📉 最大回撤", "-2.3%", "良好", SUCCESS_COLOR));
		riskMetricsPanel.add(createRiskMetricCard("⚡ 波动率", "18.5%", "中等", WARNING_COLOR));
		riskMetricsPanel.add(createRiskMetricCard("📊 夏普比率", "1.82", "优秀", SUCCESS_COLOR));
		riskMetricsPanel.add(createRiskMetricCard("🎯 胜率", "68%", "良好", SUCCESS_COLOR));
		riskMetricsPanel.add(createRiskMetricCard("💰 盈亏比", "1.45", "中等", WARNING_COLOR));
		riskMetricsPanel.add(createRiskMetricCard("⏰ 持仓时间", "3.2天", "较短", PRIMARY_COLOR));

		panel.add(riskMetricsPanel, BorderLayout.NORTH);

		// 风险控制设置
		JPanel controlPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		controlPanel.setBackground(BACKGROUND_COLOR);

		// 风险规则设置
		JPanel rulesPanel = new JPanel(new BorderLayout());
		rulesPanel.setBackground(CARD_COLOR);
		rulesPanel.setBorder(createTitledBorder("🛡️ 风险控制规则", DANGER_COLOR));

		JTextArea rulesArea = new JTextArea();
		rulesArea.setText("当前启用的风险规则:\n\n" + "✅ 单票仓位限制: ≤ 20%\n" + "✅ 总体仓位限制: ≤ 80%\n" + "✅ 单日最大亏损: -2%\n"
				+ "✅ 单笔交易风险: ≤ 1%\n" + "✅ 连续止损次数: ≤ 3次\n" + "✅ 交易频率限制: ≤ 10笔/日");
		rulesArea.setEditable(false);
		rulesArea.setFont(new Font("PingFang SC", Font.PLAIN, 12));

		rulesPanel.add(new JScrollPane(rulesArea), BorderLayout.CENTER);

		// 实时风险监控
		JPanel monitorPanel = new JPanel(new BorderLayout());
		monitorPanel.setBackground(CARD_COLOR);
		monitorPanel.setBorder(createTitledBorder("📊 实时风险监控", WARNING_COLOR));

		JTextArea monitorArea = new JTextArea();
		monitorArea.setText("实时风险状态:\n\n" + "🟢 总体风险: 低风险\n" + "🟢 仓位风险: 可控\n" + "🟡 流动性风险: 关注\n" + "🟢 市场风险: 正常\n"
				+ "🟢 信用风险: 无\n\n" + "最后检查: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
		monitorArea.setEditable(false);
		monitorArea.setFont(new Font("PingFang SC", Font.PLAIN, 12));

		monitorPanel.add(new JScrollPane(monitorArea), BorderLayout.CENTER);

		controlPanel.add(rulesPanel);
		controlPanel.add(monitorPanel);

		panel.add(controlPanel, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createRiskMetricCard(String title, String value, String status, Color color) {
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(CARD_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createEmptyBorder(15, 15, 15, 15)));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		titleLabel.setForeground(new Color(150, 150, 150));

		JLabel valueLabel = new JLabel(value);
		valueLabel.setFont(new Font("PingFang SC", Font.BOLD, 18));
		valueLabel.setForeground(color);

		JLabel statusLabel = new JLabel(status);
		statusLabel.setFont(new Font("PingFang SC", Font.PLAIN, 11));
		statusLabel.setForeground(color);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		statusLabel.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
		statusLabel.setOpaque(true);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBackground(CARD_COLOR);
		bottomPanel.add(valueLabel, BorderLayout.WEST);
		bottomPanel.add(statusLabel, BorderLayout.EAST);

		card.add(titleLabel, BorderLayout.NORTH);
		card.add(bottomPanel, BorderLayout.CENTER);

		return card;
	}

	// 由于篇幅限制，其他面板方法（createBacktestPanel, createPositionPanel,
	// createTradePanel）保持类似结构
	// 这里只展示关键改进部分

	private JPanel createBacktestPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		JLabel comingSoon = new JLabel("📊 高级回测分析面板 - 开发中", SwingConstants.CENTER);
		comingSoon.setFont(new Font("PingFang SC", Font.BOLD, 16));
		comingSoon.setForeground(new Color(150, 150, 150));

		panel.add(comingSoon, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createPositionPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		JLabel comingSoon = new JLabel("💼 高级持仓管理面板 - 开发中", SwingConstants.CENTER);
		comingSoon.setFont(new Font("PingFang SC", Font.BOLD, 16));
		comingSoon.setForeground(new Color(150, 150, 150));

		panel.add(comingSoon, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createTradePanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		JLabel comingSoon = new JLabel("📋 高级交易记录面板 - 开发中", SwingConstants.CENTER);
		comingSoon.setFont(new Font("PingFang SC", Font.BOLD, 16));
		comingSoon.setForeground(new Color(150, 150, 150));

		panel.add(comingSoon, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createStatusBar() {
		JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.setBackground(new Color(240, 240, 240));
		statusPanel.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(5, 10, 5, 10)));

		statusLabel = new JLabel("✅ 量化交易系统就绪 | 最后更新: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
		statusLabel.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		statusLabel.setForeground(new Color(100, 100, 100));

		// 右侧系统信息
		JLabel systemInfo = new JLabel("🤖 3个策略运行中 | 📈 15笔今日交易 | ⚡ 低延迟");
		systemInfo.setFont(new Font("PingFang SC", Font.PLAIN, 11));
		systemInfo.setForeground(new Color(150, 150, 150));

		statusPanel.add(statusLabel, BorderLayout.WEST);
		statusPanel.add(systemInfo, BorderLayout.EAST);

		return statusPanel;
	}

	// 样式工具方法（保持与之前相同）
	private JLabel createStyledLabel(String text, int style, int size) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("PingFang SC", style, size));
		return label;
	}

	private JButton createPrimaryButton(String text) {
		return createButton(text, PRIMARY_COLOR, Color.WHITE);
	}

	private JButton createSecondaryButton(String text) {
		JButton button = new JButton(text);
		button.setFont(new Font("PingFang SC", Font.BOLD, 12));
		button.setBackground(Color.WHITE);
		button.setForeground(new Color(100, 100, 100));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(200, 200, 200), 1),
				BorderFactory.createEmptyBorder(6, 12, 6, 12)));
		return button;
	}

	private JButton createSuccessButton(String text) {
		return createButton(text, SUCCESS_COLOR, Color.WHITE);
	}

	private JButton createWarningButton(String text) {
		return createButton(text, WARNING_COLOR, Color.WHITE);
	}

	private JButton createDangerButton(String text) {
		return createButton(text, DANGER_COLOR, Color.WHITE);
	}

	private JButton createButton(String text, Color bgColor, Color fgColor) {
		JButton button = new JButton(text);
		button.setFont(new Font("PingFang SC", Font.BOLD, 12));
		button.setBackground(bgColor);
		button.setForeground(fgColor);
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBackground(bgColor.darker());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(bgColor);
			}
		});

		return button;
	}

	private void styleComboBox(JComboBox<?> comboBox) {
		comboBox.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		comboBox.setBackground(Color.WHITE);
	}

	private Border createTitledBorder(String title, Color color) {
		return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15),
						BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color, 2), title,
								TitledBorder.LEFT, TitledBorder.TOP, new Font("PingFang SC", Font.BOLD, 13), color)));
	}

	private void setupTableAppearance(JTable table) {
		table.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		table.setRowHeight(32);
		table.setShowGrid(true);
		table.setGridColor(new Color(240, 240, 240));
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setFillsViewportHeight(true);

		// 设置表头样式
		JTableHeader header = table.getTableHeader();
		header.setFont(new Font("PingFang SC", Font.BOLD, 12));
		header.setBackground(new Color(250, 250, 250));
		header.setForeground(new Color(80, 80, 80));
		header.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
				BorderFactory.createEmptyBorder(8, 5, 8, 5)));
	}

	private void setupQuantTableRenderer() {
		// 量化专用表格渲染器
		stockTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				// 信号列特殊处理
				if (column == 4) {
					String signal = value.toString();
					if (signal.contains("强烈买入")) {
						c.setForeground(new Color(0, 100, 0));
						c.setBackground(new Color(220, 255, 220));
					} else if (signal.contains("买入")) {
						c.setForeground(new Color(0, 150, 0));
						c.setBackground(new Color(240, 255, 240));
					} else if (signal.contains("卖出")) {
						c.setForeground(new Color(150, 0, 0));
						c.setBackground(new Color(255, 240, 240));
					} else {
						c.setForeground(Color.GRAY);
					}
				}

				// 涨跌幅颜色
				else if (column == 3 && value != null) {
					String change = value.toString();
					if (change.contains("+")) {
						c.setForeground(new Color(220, 0, 0));
					} else if (change.contains("-")) {
						c.setForeground(new Color(0, 150, 0));
					}
				}

				return c;
			}
		});
	}

	private void setupStrategyTableRenderer() {
		// 策略表格专用渲染器
		strategyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				if (column == 1) { // 状态列
					String status = value.toString();
					if ("运行中".equals(status)) {
						c.setForeground(SUCCESS_COLOR);
						c.setFont(c.getFont().deriveFont(Font.BOLD));
					} else if ("警告".equals(status)) {
						c.setForeground(WARNING_COLOR);
						c.setFont(c.getFont().deriveFont(Font.BOLD));
					}
				} else if (column == 2 || column == 3) { // 收益和夏普比率
					try {
						double val = Double.parseDouble(value.toString().replace("%", ""));
						if (val > 0) {
							c.setForeground(SUCCESS_COLOR);
						}
					} catch (NumberFormatException e) {
						// 忽略格式错误
					}
				} else if (column == 4) { // 最大回撤
					try {
						double val = Double.parseDouble(value.toString().replace("%", "").replace("-", ""));
						if (val < 0) {
							c.setForeground(DANGER_COLOR);
						}
					} catch (NumberFormatException e) {
						// 忽略格式错误
					}
				}

				return c;
			}
		});
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(new FlatLightLaf());
			} catch (Exception e) {
				e.printStackTrace();
			}
			new QuantTradingUI().setVisible(true);
		});
	}
}

// 量化专用数据类
class StrategyPerformance {
	double totalReturn;
	double annualReturn;
	double maxDrawdown;
	double sharpeRatio;
	double winRate;

	public StrategyPerformance(double totalReturn, double annualReturn, double maxDrawdown, double sharpeRatio,
			double winRate) {
		this.totalReturn = totalReturn;
		this.annualReturn = annualReturn;
		this.maxDrawdown = maxDrawdown;
		this.sharpeRatio = sharpeRatio;
		this.winRate = winRate;
	}
}

class RiskManager {
	private double volatility;
	private double var; // Value at Risk
	private double riskScore;

	public void updateRiskMetrics(double volatility, double var, double riskScore) {
		this.volatility = volatility;
		this.var = var;
		this.riskScore = riskScore;
	}

	// 风险检查方法
	public boolean checkPositionRisk(String stockCode, double position, double totalAssets) {
		return position / totalAssets <= 0.2; // 单票不超过20%
	}
}