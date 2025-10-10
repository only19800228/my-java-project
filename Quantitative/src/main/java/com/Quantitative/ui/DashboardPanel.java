package com.Quantitative.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

public class DashboardPanel extends JPanel {
	private JLabel totalAssetsLabel;
	private JLabel availableFundsLabel;
	private JLabel dailyPnLLabel;
	private JLabel floatingPnLLabel;
	private JLabel sharpeRatioLabel;
	private JLabel maxDrawdownLabel;

	public DashboardPanel() {
		setLayout(new BorderLayout());
		initializeComponents();
	}

	private void initializeComponents() {
		// 创建顶部指标卡片
		add(createMetricsPanel(), BorderLayout.NORTH);

		// 创建中部图表区域
		add(createChartsPanel(), BorderLayout.CENTER);

		// 创建底部警报和今日表现
		add(createBottomPanel(), BorderLayout.SOUTH);
	}

	private JPanel createMetricsPanel() {
		JPanel metricsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
		metricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		totalAssetsLabel = createMetricCard("总资产", "1,234,567.89", "↗️ 2.34%", Color.GREEN);
		availableFundsLabel = createMetricCard("可用资金", "456,789.12", "冻结: 12,345.67", Color.BLUE);
		dailyPnLLabel = createMetricCard("当日盈亏", "+12,345.67", "+1.23%", Color.GREEN);
		floatingPnLLabel = createMetricCard("浮动盈亏", "+8,901.23", "+0.67%", Color.ORANGE);
		sharpeRatioLabel = createMetricCard("夏普比率", "2.34", "优秀", new Color(0, 200, 0));
		maxDrawdownLabel = createMetricCard("最大回撤", "-5.67%", "可控", new Color(200, 0, 0));

		metricsPanel.add(totalAssetsLabel);
		metricsPanel.add(availableFundsLabel);
		metricsPanel.add(dailyPnLLabel);
		metricsPanel.add(floatingPnLLabel);
		metricsPanel.add(sharpeRatioLabel);
		metricsPanel.add(maxDrawdownLabel);

		return metricsPanel;
	}

	private JLabel createMetricCard(String title, String value, String subText, Color color) {
		JLabel card = new JLabel("<html><div style='text-align: center; padding: 15px;'>"
				+ "<div style='font-size: 12px; color: #888;'>" + title + "</div>"
				+ "<div style='font-size: 18px; font-weight: bold; color: " + getHexColor(color) + ";'>" + value
				+ "</div>" + "<div style='font-size: 11px; color: #aaa;'>" + subText + "</div></div></html>");
		card.setOpaque(true);
		card.setBackground(new Color(45, 45, 48));
		card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		return card;
	}

	private String getHexColor(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private JPanel createChartsPanel() {
		JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
		chartsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// 资金曲线图占位
		JPanel equityChartPanel = createChartPlaceholder("资金曲线", "策略收益 vs 基准指数");
		chartsPanel.add(equityChartPanel);

		// 时间维度选择
		JPanel timeFramePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		String[] timeFrames = { "1D", "1W", "1M", "YTD", "1Y", "All" };
		for (String tf : timeFrames) {
			JButton btn = new JButton(tf);
			btn.setPreferredSize(new Dimension(50, 25));
			timeFramePanel.add(btn);
		}

		JPanel chartContainer = new JPanel(new BorderLayout());
		chartContainer.add(timeFramePanel, BorderLayout.NORTH);
		chartContainer.add(equityChartPanel, BorderLayout.CENTER);

		return chartContainer;
	}

	private JPanel createChartPlaceholder(String title, String description) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)), title,
				TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), Color.WHITE));

		JLabel placeholder = new JLabel("<html><div style='text-align: center; color: #888; padding: 50px;'>"
				+ description + "<br>图表组件将在这里显示</div></html>", SwingConstants.CENTER);
		panel.add(placeholder, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createBottomPanel() {
		JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 10));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// 警报中心
		JPanel alertPanel = createAlertPanel();
		bottomPanel.add(alertPanel);

		// 今日表现
		JPanel performancePanel = createTodayPerformancePanel();
		bottomPanel.add(performancePanel);

		return bottomPanel;
	}

	private JPanel createAlertPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("警报与通知"));

		DefaultListModel<String> alertListModel = new DefaultListModel<>();
		alertListModel.addElement("🔴 策略异常: 均值回归策略 - 连接超时");
		alertListModel.addElement("🟡 风控警告: 单一标的持仓超过限制");
		alertListModel.addElement("🔵 信号产生: 动量策略 - 买入信号");
		alertListModel.addElement("🟢 订单完成: 000001.SH - 买入 1000股 @ 15.67");

		JList<String> alertList = new JList<>(alertListModel);
		alertList.setBackground(new Color(45, 45, 48));
		alertList.setForeground(Color.WHITE);

		JScrollPane scrollPane = new JScrollPane(alertList);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createTodayPerformancePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("今日表现"));

		String[][] performanceData = { { "动量策略", "+3.45%", "12,345.67" }, { "均值回归", "+2.12%", "8,901.23" },
				{ "网格交易", "+1.78%", "6,789.01" }, { "套利策略", "-0.56%", "-2,345.67" } };

		String[] columnNames = { "策略名称", "收益率", "盈亏金额" };

		JTable performanceTable = new JTable(performanceData, columnNames) {
			@Override
			public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (column == 1 || column == 2) {
					String value = getValueAt(row, column).toString();
					if (value.startsWith("+")) {
						c.setForeground(new Color(0, 200, 0));
					} else if (value.startsWith("-")) {
						c.setForeground(new Color(200, 0, 0));
					}
				}
				return c;
			}
		};

		performanceTable.setBackground(new Color(45, 45, 48));
		performanceTable.setForeground(Color.WHITE);
		performanceTable.setGridColor(new Color(60, 60, 60));

		JScrollPane scrollPane = new JScrollPane(performanceTable);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}
}