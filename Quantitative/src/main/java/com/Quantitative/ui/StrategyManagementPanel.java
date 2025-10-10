package com.Quantitative.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class StrategyManagementPanel extends JPanel {
	private JTable strategyTable;
	private JTextArea codeEditor;

	public StrategyManagementPanel() {
		setLayout(new BorderLayout());
		initializeComponents();
	}

	private void initializeComponents() {
		// 创建策略列表和操作按钮
		add(createStrategyListPanel(), BorderLayout.WEST);

		// 创建策略编辑和回测区域
		add(createStrategyEditorPanel(), BorderLayout.CENTER);
	}

	private JPanel createStrategyListPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setPreferredSize(new Dimension(300, 0));
		panel.setBorder(BorderFactory.createTitledBorder("策略列表"));

		// 策略表格数据
		String[][] strategyData = { { "运行中", "动量策略", "000001.SH", "2.34%", "1.23" },
				{ "已停止", "均值回归", "000300.SH", "1.56%", "1.89" }, { "回测中", "网格交易", "BTC/USDT", "3.21%", "2.45" },
				{ "异常", "套利策略", "000001.SH", "-0.45%", "0.87" } };

		String[] columnNames = { "状态", "策略名称", "标的", "收益率", "夏普比率" };

		strategyTable = new JTable(strategyData, columnNames) {
			@Override
			public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (column == 0) {
					String status = getValueAt(row, column).toString();
					switch (status) {
					case "运行中":
						c.setForeground(new Color(0, 200, 0));
						break;
					case "已停止":
						c.setForeground(Color.GRAY);
						break;
					case "回测中":
						c.setForeground(Color.ORANGE);
						break;
					case "异常":
						c.setForeground(Color.RED);
						break;
					}
				}
				return c;
			}
		};

		JScrollPane tableScroll = new JScrollPane(strategyTable);

		// 操作按钮面板
		JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
		JButton startBtn = new JButton("启动");
		JButton stopBtn = new JButton("暂停");
		JButton editBtn = new JButton("编辑");

		buttonPanel.add(startBtn);
		buttonPanel.add(stopBtn);
		buttonPanel.add(editBtn);

		// 创建新策略按钮
		JButton createBtn = new JButton("+ 创建新策略");
		createBtn.setBackground(new Color(0, 120, 215));
		createBtn.setForeground(Color.WHITE);

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(buttonPanel, BorderLayout.CENTER);
		southPanel.add(createBtn, BorderLayout.SOUTH);

		panel.add(tableScroll, BorderLayout.CENTER);
		panel.add(southPanel, BorderLayout.SOUTH);

		return panel;
	}

	private JPanel createStrategyEditorPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		// 创建选项卡面板
		JTabbedPane editorTabbedPane = new JTabbedPane();

		// 代码编辑器选项卡
		editorTabbedPane.addTab("策略代码", createCodeEditorPanel());

		// 参数配置选项卡
		editorTabbedPane.addTab("参数配置", createParameterPanel());

		// 回测结果选项卡
		editorTabbedPane.addTab("回测结果", createBacktestPanel());

		panel.add(editorTabbedPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createCodeEditorPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		// 策略模板选择
		JPanel templatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		templatePanel.add(new JLabel("策略模板:"));
		JComboBox<String> templateCombo = new JComboBox<>(new String[] { "均线交叉", "布林带突破", "RSI动量", "自定义" });
		templatePanel.add(templateCombo);

		// 代码编辑器（简化版）
		codeEditor = new JTextArea();
		codeEditor.setText("// 策略代码示例\n" + "public class MovingAverageStrategy {\n" + "    private double fastMA = 5;\n"
				+ "    private double slowMA = 20;\n" + "    \n"
				+ "    public Signal generateSignal(PriceData data) {\n" + "        // 策略逻辑在这里实现\n"
				+ "        return Signal.HOLD;\n" + "    }\n" + "}");
		codeEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));
		codeEditor.setBackground(new Color(30, 30, 30));
		codeEditor.setForeground(Color.WHITE);
		codeEditor.setCaretColor(Color.WHITE);

		JScrollPane editorScroll = new JScrollPane(codeEditor);

		panel.add(templatePanel, BorderLayout.NORTH);
		panel.add(editorScroll, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createParameterPanel() {
		JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// 参数配置表单
		addParameterField(panel, "快速均线周期", "5");
		addParameterField(panel, "慢速均线周期", "20");
		addParameterField(panel, "止损比例", "2.0");
		addParameterField(panel, "止盈比例", "5.0");
		addParameterField(panel, "最大持仓数量", "1000");
		addParameterField(panel, "交易标的", "000001.SH");

		JButton saveBtn = new JButton("保存参数");
		saveBtn.setBackground(new Color(0, 120, 215));
		saveBtn.setForeground(Color.WHITE);

		JPanel container = new JPanel(new BorderLayout());
		container.add(panel, BorderLayout.CENTER);
		container.add(saveBtn, BorderLayout.SOUTH);

		return container;
	}

	private void addParameterField(JPanel panel, String label, String defaultValue) {
		panel.add(new JLabel(label + ":"));
		JTextField field = new JTextField(defaultValue);
		panel.add(field);
	}

	private JPanel createBacktestPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		// 回测配置
		JPanel configPanel = new JPanel(new GridLayout(2, 4, 10, 10));
		configPanel.setBorder(BorderFactory.createTitledBorder("回测配置"));

		configPanel.add(new JLabel("标的:"));
		configPanel.add(new JTextField("000001.SH"));
		configPanel.add(new JLabel("时间范围:"));
		configPanel.add(new JTextField("2023-01-01 至 2024-01-01"));
		configPanel.add(new JLabel("初始资金:"));
		configPanel.add(new JTextField("1000000"));
		configPanel.add(new JLabel("手续费:"));
		configPanel.add(new JTextField("0.0003"));

		JButton runBacktestBtn = new JButton("运行回测");
		runBacktestBtn.setBackground(new Color(0, 150, 0));
		runBacktestBtn.setForeground(Color.WHITE);

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(configPanel, BorderLayout.CENTER);
		northPanel.add(runBacktestBtn, BorderLayout.EAST);

		// 回测结果占位
		JLabel resultPlaceholder = new JLabel("<html><div style='text-align: center; color: #888; padding: 50px;'>"
				+ "回测结果将在这里显示<br>包括收益曲线、业绩指标等</div></html>", SwingConstants.CENTER);

		panel.add(northPanel, BorderLayout.NORTH);
		panel.add(resultPlaceholder, BorderLayout.CENTER);

		return panel;
	}
}