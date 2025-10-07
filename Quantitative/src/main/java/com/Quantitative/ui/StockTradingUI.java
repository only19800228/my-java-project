package com.Quantitative.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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

import com.formdev.flatlaf.FlatLightLaf;

public class StockTradingUI extends JFrame {
	private JTable stockTable;
	private JTable positionTable;
	private JTable tradeTable;
	private JComboBox<String> strategyComboBox;
	private JComboBox<String> refreshIntervalComboBox;
	private JLabel cashLabel;
	private JLabel totalAssetsLabel;
	private JLabel statusLabel;

	// 颜色定义
	private final Color PRIMARY_COLOR = new Color(0, 122, 255);
	private final Color SUCCESS_COLOR = new Color(52, 199, 89);
	private final Color WARNING_COLOR = new Color(255, 149, 0);
	private final Color DANGER_COLOR = new Color(255, 59, 48);
	private final Color BACKGROUND_COLOR = new Color(242, 242, 247);
	private final Color CARD_COLOR = Color.WHITE;

	public StockTradingUI() {
		setupFlatLaf();
		initComponents();
	}

	private void setupFlatLaf() {
		try {
			UIManager.put("Button.arc", 8);
			UIManager.put("Component.arc", 8);
			UIManager.put("ProgressBar.arc", 8);
			UIManager.put("TextComponent.arc", 8);
			UIManager.put("ScrollBar.thumbArc", 999);
			UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

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

	private void initComponents() {
		setTitle("📈 股票智能交易系统");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 800);
		setLocationRelativeTo(null);

		// 设置窗口图标
		setIconImage(createAppIcon());

		// 创建主面板
		JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		mainPanel.setBackground(BACKGROUND_COLOR);

		// 添加顶部输入区
		mainPanel.add(createTopPanel(), BorderLayout.NORTH);

		// 添加中部选项卡
		mainPanel.add(createCenterTabbedPane(), BorderLayout.CENTER);

		// 添加底部状态栏
		mainPanel.add(createStatusBar(), BorderLayout.SOUTH);

		setContentPane(mainPanel);
	}

	private Image createAppIcon() {
		// 创建一个简单的应用图标
		int size = 64;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();

		// 启用抗锯齿
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 绘制背景
		g2d.setColor(PRIMARY_COLOR);
		g2d.fillRoundRect(0, 0, size, size, 16, 16);

		// 绘制股票图表图标
		g2d.setColor(Color.WHITE);
		g2d.setStroke(new BasicStroke(3f));

		// 绘制上升趋势线
		int[] xPoints = { size / 4, size / 2, 3 * size / 4 };
		int[] yPoints = { 3 * size / 4, size / 3, size / 4 };
		g2d.drawPolyline(xPoints, yPoints, 3);

		g2d.dispose();
		return image;
	}

	private JPanel createTopPanel() {
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		topPanel.setBackground(CARD_COLOR);
		topPanel.setBorder(createTitledBorder("🔍 股票查询监控", PRIMARY_COLOR));

		// 股票查询区
		JLabel stockCodeLabel = createStyledLabel("股票代码:", Font.BOLD, 13);
		JTextField stockCodeField = createStyledTextField(10);

		JButton addStockButton = createPrimaryButton("➕ 添加股票");
		JButton exampleBtn = createSecondaryButton("📋 添加示例");
		JButton clearBtn = createDangerButton("🗑️ 清空列表");

		// 监控控制
		JButton monitorBtn = createSuccessButton("▶️ 开始监控");
		JButton refreshBtn = createSecondaryButton("🔄 手动刷新");

		// 刷新间隔设置
		JLabel refreshLabel = createStyledLabel("刷新间隔:", Font.PLAIN, 12);
		refreshIntervalComboBox = new JComboBox<>(new String[] { "5秒", "10秒", "15秒", "20秒" });
		styleComboBox(refreshIntervalComboBox);

		// 开机自启
		JCheckBox autoStartCheckBox = new JCheckBox("🚀 开机自启监控");
		autoStartCheckBox.setFont(new Font("PingFang SC", Font.PLAIN, 12));

		topPanel.add(stockCodeLabel);
		topPanel.add(stockCodeField);
		topPanel.add(addStockButton);
		topPanel.add(exampleBtn);
		topPanel.add(clearBtn);
		topPanel.add(monitorBtn);
		topPanel.add(refreshBtn);
		topPanel.add(refreshLabel);
		topPanel.add(refreshIntervalComboBox);
		topPanel.add(autoStartCheckBox);

		return topPanel;
	}

	private JTabbedPane createCenterTabbedPane() {
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setFont(new Font("PingFang SC", Font.BOLD, 13));

		// 股票监控选项卡
		tabbedPane.addTab("📊 股票监控", createStockIcon(), createStockMonitorPanel());

		// 回测信息选项卡
		tabbedPane.addTab("📈 回测分析", createChartIcon(), createBacktestPanel());

		// 持仓信息选项卡
		tabbedPane.addTab("💼 持仓信息", createWalletIcon(), createPositionPanel());

		// 交易信息选项卡
		tabbedPane.addTab("💳 交易记录", createTransactionIcon(), createTradePanel());

		return tabbedPane;
	}

	private Icon createStockIcon() {
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(PRIMARY_COLOR);
				g2d.fillRect(x, y, 16, 16);
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font("Arial", Font.BOLD, 10));
				g2d.drawString("S", x + 5, y + 12);
				g2d.dispose();
			}

			@Override
			public int getIconWidth() {
				return 16;
			}

			@Override
			public int getIconHeight() {
				return 16;
			}
		};
	}

	private Icon createChartIcon() {
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(SUCCESS_COLOR);
				g2d.fillRect(x, y, 16, 16);
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font("Arial", Font.BOLD, 10));
				g2d.drawString("C", x + 5, y + 12);
				g2d.dispose();
			}

			@Override
			public int getIconWidth() {
				return 16;
			}

			@Override
			public int getIconHeight() {
				return 16;
			}
		};
	}

	private Icon createWalletIcon() {
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(WARNING_COLOR);
				g2d.fillRect(x, y, 16, 16);
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font("Arial", Font.BOLD, 10));
				g2d.drawString("W", x + 5, y + 12);
				g2d.dispose();
			}

			@Override
			public int getIconWidth() {
				return 16;
			}

			@Override
			public int getIconHeight() {
				return 16;
			}
		};
	}

	private Icon createTransactionIcon() {
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(DANGER_COLOR);
				g2d.fillRect(x, y, 16, 16);
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font("Arial", Font.BOLD, 10));
				g2d.drawString("T", x + 5, y + 12);
				g2d.dispose();
			}

			@Override
			public int getIconWidth() {
				return 16;
			}

			@Override
			public int getIconHeight() {
				return 16;
			}
		};
	}

	private JPanel createStockMonitorPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		// 上方控制区域
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		controlPanel.setBackground(CARD_COLOR);
		controlPanel.setBorder(createTitledBorder("🎯 交易控制", PRIMARY_COLOR));

		strategyComboBox = new JComboBox<>(new String[] { "📈 均线策略", "📊 RSI策略", "📉 MACD策略", "📋 布林带策略" });
		styleComboBox(strategyComboBox);

		JButton strategySettingsButton = createSecondaryButton("⚙️ 策略设置");
		JButton startAutoTradeButton = createSuccessButton("🤖 启动自动交易");
		JButton manualBuyButton = createPrimaryButton("💰 手动买入");
		JButton manualSellButton = createDangerButton("💸 手动卖出");

		cashLabel = createValueLabel("💵 现金: 1,000,000.00");
		totalAssetsLabel = createValueLabel("📊 总资产: 1,000,000.00");

		JButton addExampleButton = createSecondaryButton("📥 添加实例");

		controlPanel.add(createStyledLabel("交易策略:", Font.BOLD, 12));
		controlPanel.add(strategyComboBox);
		controlPanel.add(strategySettingsButton);
		controlPanel.add(startAutoTradeButton);
		controlPanel.add(manualBuyButton);
		controlPanel.add(manualSellButton);
		controlPanel.add(cashLabel);
		controlPanel.add(totalAssetsLabel);
		controlPanel.add(addExampleButton);

		panel.add(controlPanel, BorderLayout.NORTH);

		// 下方表格区域
		String[] columnNames = { "股票名称", "代码", "价格", "涨跌幅", "涨跌额", "RSI指标", "信号", "成交量", "更新时间" };
		Object[][] data = {}; // 初始为空数据

		DefaultTableModel model = new DefaultTableModel(data, columnNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

			@Override
			public Class<?> getColumnClass(int column) {
				switch (column) {
				case 2:
				case 3:
				case 4:
				case 5: // 价格、涨跌幅、涨跌额、RSI指标
					return Double.class;
				case 6: // 信号
					return String.class;
				case 7: // 成交量
					return Long.class;
				default:
					return String.class;
				}
			}
		};

		stockTable = new JTable(model);
		setupStockTableRenderer();
		setupTableAppearance(stockTable);

		// 添加右键菜单
		setupStockTableContextMenu();

		JScrollPane scrollPane = new JScrollPane(stockTable);
		scrollPane.setBorder(createTitledBorder("📈 实时监控列表", SUCCESS_COLOR));
		scrollPane.getViewport().setBackground(Color.WHITE);

		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createBacktestPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		// 上方控制区域
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		controlPanel.setBackground(CARD_COLOR);
		controlPanel.setBorder(createTitledBorder("🔬 回测配置", WARNING_COLOR));

		JComboBox<String> backtestStrategyComboBox = new JComboBox<>(
				new String[] { "📈 均线策略", "📊 RSI策略", "📉 MACD策略", "📋 布林带策略" });
		styleComboBox(backtestStrategyComboBox);

		JButton executeBacktestButton = createPrimaryButton("🚀 执行回测");
		JButton parameterSettingsButton = createSecondaryButton("⚙️ 参数设置");

		controlPanel.add(createStyledLabel("策略选择:", Font.BOLD, 12));
		controlPanel.add(backtestStrategyComboBox);
		controlPanel.add(executeBacktestButton);
		controlPanel.add(parameterSettingsButton);

		panel.add(controlPanel, BorderLayout.NORTH);

		// 下方内容区域
		JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		contentPanel.setBackground(BACKGROUND_COLOR);

		// 左边图表区域
		JPanel chartPanel = new JPanel(new BorderLayout());
		chartPanel.setBackground(CARD_COLOR);
		chartPanel.setBorder(createTitledBorder("📊 策略指标走势分析", PRIMARY_COLOR));

		JLabel chartPlaceholder = new JLabel("📈 图表展示区域", SwingConstants.CENTER);
		chartPlaceholder.setFont(new Font("PingFang SC", Font.BOLD, 16));
		chartPlaceholder.setForeground(new Color(150, 150, 150));
		chartPlaceholder.setPreferredSize(new Dimension(400, 300));
		chartPanel.add(chartPlaceholder, BorderLayout.CENTER);

		// 右边结果区域
		JPanel resultPanel = new JPanel(new BorderLayout());
		resultPanel.setBackground(CARD_COLOR);
		resultPanel.setBorder(createTitledBorder("📋 回测结果", SUCCESS_COLOR));

		JTextArea resultTextArea = new JTextArea();
		resultTextArea.setText(
				"📊 回测结果将显示在这里...\n\n" + "💰 总收益率: \n" + "📈 年化收益率: \n" + "📉 最大回撤: \n" + "⚡ 夏普比率: \n" + "🎯 胜率: ");
		resultTextArea.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		resultTextArea.setEditable(false);
		resultTextArea.setBackground(CARD_COLOR);
		resultTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		resultPanel.add(new JScrollPane(resultTextArea), BorderLayout.CENTER);

		contentPanel.add(chartPanel);
		contentPanel.add(resultPanel);

		panel.add(contentPanel, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createPositionPanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		// 上方控制区域
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		controlPanel.setBackground(CARD_COLOR);
		controlPanel.setBorder(createTitledBorder("💼 持仓管理", WARNING_COLOR));

		JButton refreshButton = createSecondaryButton("🔄 刷新持仓");
		JButton quickSellButton = createDangerButton("💸 快捷卖出选中");

		controlPanel.add(refreshButton);
		controlPanel.add(quickSellButton);

		panel.add(controlPanel, BorderLayout.NORTH);

		// 下方表格区域
		String[] columnNames = { "股票代码", "股票名称", "持仓数量", "成本价", "当前价", "市值", "盈亏", "盈亏率" };
		Object[][] data = {}; // 初始为空数据

		DefaultTableModel model = new DefaultTableModel(data, columnNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		positionTable = new JTable(model);
		setupPositionTableRenderer();
		setupTableAppearance(positionTable);

		JScrollPane scrollPane = new JScrollPane(positionTable);
		scrollPane.setBorder(createTitledBorder("📋 持仓明细", WARNING_COLOR));
		scrollPane.getViewport().setBackground(Color.WHITE);

		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createTradePanel() {
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND_COLOR);

		// 上方控制区域
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		controlPanel.setBackground(CARD_COLOR);
		controlPanel.setBorder(createTitledBorder("💳 交易记录管理", DANGER_COLOR));

		JButton exportCsvButton = createSuccessButton("📤 导出CSV");
		JButton clearRecordsButton = createDangerButton("🗑️ 清空记录");

		controlPanel.add(exportCsvButton);
		controlPanel.add(clearRecordsButton);

		panel.add(controlPanel, BorderLayout.NORTH);

		// 下方表格区域
		String[] columnNames = { "时间", "类型", "股票代码", "股票名称", "价格", "数量", "金额", "状态" };
		Object[][] data = {}; // 初始为空数据

		DefaultTableModel model = new DefaultTableModel(data, columnNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		tradeTable = new JTable(model);
		setupTradeTableRenderer();
		setupTableAppearance(tradeTable);

		JScrollPane scrollPane = new JScrollPane(tradeTable);
		scrollPane.setBorder(createTitledBorder("📝 交易记录", DANGER_COLOR));
		scrollPane.getViewport().setBackground(Color.WHITE);

		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createStatusBar() {
		JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.setBackground(new Color(240, 240, 240));
		statusPanel.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(5, 10, 5, 10)));

		statusLabel = new JLabel("✅ 系统就绪 | 最后更新: --:--:--");
		statusLabel.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		statusLabel.setForeground(new Color(100, 100, 100));

		// 右侧系统信息
		JLabel systemInfo = new JLabel("💻 Java " + System.getProperty("java.version") + " | 🖥️ "
				+ Toolkit.getDefaultToolkit().getScreenSize().width + "x"
				+ Toolkit.getDefaultToolkit().getScreenSize().height);
		systemInfo.setFont(new Font("PingFang SC", Font.PLAIN, 11));
		systemInfo.setForeground(new Color(150, 150, 150));

		statusPanel.add(statusLabel, BorderLayout.WEST);
		statusPanel.add(systemInfo, BorderLayout.EAST);

		return statusPanel;
	}

	// 样式工具方法
	private JLabel createStyledLabel(String text, int style, int size) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("PingFang SC", style, size));
		return label;
	}

	private JLabel createValueLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("PingFang SC", Font.BOLD, 12));
		label.setForeground(PRIMARY_COLOR);
		label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
		label.setBackground(new Color(240, 248, 255));
		label.setOpaque(true);
		label.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(200, 225, 255), 1),
				BorderFactory.createEmptyBorder(2, 8, 2, 8)));
		return label;
	}

	private JTextField createStyledTextField(int columns) {
		JTextField field = new JTextField(columns);
		field.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		return field;
	}

	private JButton createPrimaryButton(String text) {
		return createButton(text, PRIMARY_COLOR, Color.WHITE);
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

	private JButton createButton(String text, Color bgColor, Color fgColor) {
		JButton button = new JButton(text);
		button.setFont(new Font("PingFang SC", Font.BOLD, 12));
		button.setBackground(bgColor);
		button.setForeground(fgColor);
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

		// 添加鼠标悬停效果
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

	private void setupStockTableRenderer() {
		// 设置居中对齐的列
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		stockTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // 股票名称
		stockTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // 代码
		stockTable.getColumnModel().getColumn(8).setCellRenderer(centerRenderer); // 更新时间

		// 设置右对齐和颜色变化的列
		ColorChangeRenderer colorRenderer = new ColorChangeRenderer();
		colorRenderer.setHorizontalAlignment(JLabel.RIGHT);
		for (int i = 2; i <= 7; i++) {
			stockTable.getColumnModel().getColumn(i).setCellRenderer(colorRenderer);
		}
	}

	private void setupPositionTableRenderer() {
		ColorChangeRenderer colorRenderer = new ColorChangeRenderer();
		colorRenderer.setHorizontalAlignment(JLabel.RIGHT);

		// 设置右对齐和颜色变化的列
		for (int i = 2; i <= 7; i++) {
			positionTable.getColumnModel().getColumn(i).setCellRenderer(colorRenderer);
		}
	}

	private void setupTradeTableRenderer() {
		ColorChangeRenderer colorRenderer = new ColorChangeRenderer();
		colorRenderer.setHorizontalAlignment(JLabel.RIGHT);

		// 设置右对齐和颜色变化的列（价格、数量、金额）
		for (int i = 4; i <= 6; i++) {
			tradeTable.getColumnModel().getColumn(i).setCellRenderer(colorRenderer);
		}

		// 类型列特殊着色
		DefaultTableCellRenderer typeRenderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (value != null) {
					String type = value.toString();
					if (type.contains("买入") || type.equals("BUY")) {
						c.setForeground(new Color(220, 0, 0));
						setText("💰 " + type);
					} else if (type.contains("卖出") || type.equals("SELL")) {
						c.setForeground(new Color(0, 150, 0));
						setText("💸 " + type);
					}
				}
				setHorizontalAlignment(JLabel.CENTER);
				return c;
			}
		};
		tradeTable.getColumnModel().getColumn(1).setCellRenderer(typeRenderer);
	}

	private void setupStockTableContextMenu() {
		JPopupMenu contextMenu = new JPopupMenu();
		contextMenu.setBackground(Color.WHITE);
		contextMenu.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

		JMenuItem buyMenuItem = createContextMenuItem("💰 买入", PRIMARY_COLOR);
		JMenuItem sellMenuItem = createContextMenuItem("💸 卖出", DANGER_COLOR);
		JMenuItem viewDetailItem = createContextMenuItem("👁️ 查看详情", new Color(100, 100, 100));
		JMenuItem setAlertItem = createContextMenuItem("🔔 设置提醒", WARNING_COLOR);

		buyMenuItem.addActionListener(e -> showBuyDialog());
		sellMenuItem.addActionListener(e -> showSellDialog());

		contextMenu.add(buyMenuItem);
		contextMenu.add(sellMenuItem);
		contextMenu.addSeparator();
		contextMenu.add(viewDetailItem);
		contextMenu.add(setAlertItem);

		stockTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e);
				}
			}

			private void showContextMenu(MouseEvent e) {
				int row = stockTable.rowAtPoint(e.getPoint());
				if (row >= 0 && row < stockTable.getRowCount()) {
					stockTable.setRowSelectionInterval(row, row);
					contextMenu.show(stockTable, e.getX(), e.getY());
				}
			}
		});
	}

	private JMenuItem createContextMenuItem(String text, Color color) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		menuItem.setForeground(color);
		menuItem.setBackground(Color.WHITE);
		menuItem.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		menuItem.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				menuItem.setBackground(new Color(240, 240, 240));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				menuItem.setBackground(Color.WHITE);
			}
		});

		return menuItem;
	}

	private void showBuyDialog() {
		BuySellDialog dialog = new BuySellDialog(this, "💰 买入股票", true);
		dialog.setVisible(true);
	}

	private void showSellDialog() {
		BuySellDialog dialog = new BuySellDialog(this, "💸 卖出股票", false);
		dialog.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(new FlatLightLaf());
			} catch (Exception e) {
				e.printStackTrace();
			}
			new StockTradingUI().setVisible(true);
		});
	}
}

// 颜色变化渲染器
class ColorChangeRenderer extends DefaultTableCellRenderer {
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		if (value != null) {
			String text = value.toString();

			// 根据数值变化设置颜色
			if (text.contains("-") || text.startsWith("-")) {
				c.setForeground(new Color(0, 150, 0)); // 下跌为绿色
			} else if (text.contains("+") || Character.isDigit(text.charAt(0))) {
				// 检查是否是正数或包含+
				try {
					if (!text.equals("0") && !text.equals("0.00") && !text.equals("0.00%")) {
						c.setForeground(new Color(220, 0, 0)); // 上涨为红色
					} else {
						c.setForeground(Color.GRAY); // 不涨不跌为灰色
					}
				} catch (Exception e) {
					c.setForeground(Color.BLACK);
				}
			} else {
				c.setForeground(Color.GRAY); // 不涨不跌为灰色
			}
		}

		return c;
	}
}