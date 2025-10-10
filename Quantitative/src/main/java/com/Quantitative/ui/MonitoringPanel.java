package com.Quantitative.ui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

//实时监控模块
public class MonitoringPanel extends JPanel {
	public MonitoringPanel() {
		setLayout(new BorderLayout());
		add(new JLabel("实时监控模块 - 开发中...", SwingConstants.CENTER));
	}
}
