package com.Quantitative.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class StrategyEditorDialog extends JDialog {
	private JTextArea codeEditor;
	private JComboBox<String> languageCombo;
	private JComboBox<String> strategyTypeCombo;

	public StrategyEditorDialog(JFrame parent) {
		super(parent, "🤖 量化策略编辑器", true);
		initComponents();
		setSize(800, 600);
		setLocationRelativeTo(parent);
	}

	private void initComponents() {
		JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// 顶部工具栏
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		toolbar.add(new JLabel("策略类型:"));
		strategyTypeCombo = new JComboBox<>(new String[] { "趋势跟踪", "均值回归", "动量策略", "套利策略", "市场中性" });
		toolbar.add(strategyTypeCombo);

		toolbar.add(new JLabel("编程语言:"));
		languageCombo = new JComboBox<>(new String[] { "Python", "Java", "JavaScript" });
		toolbar.add(languageCombo);

		JButton templateBtn = new JButton("📋 加载模板");
		JButton validateBtn = new JButton("✅ 验证代码");
		JButton backtestBtn = new JButton("📊 快速回测");

		toolbar.add(templateBtn);
		toolbar.add(validateBtn);
		toolbar.add(backtestBtn);

		// 代码编辑器
		codeEditor = new JTextArea();
		codeEditor.setText(
				"# 量化策略模板\n# 在这里编写您的交易策略\n\ndef initialize(context):\n    # 策略初始化\n    pass\n\ndef handle_data(context, data):\n    # 每个交易周期执行\n    pass");
		codeEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));

		JScrollPane editorScroll = new JScrollPane(codeEditor);
		editorScroll.setBorder(BorderFactory.createTitledBorder("策略代码"));

		// 底部按钮
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveBtn = new JButton("💾 保存策略");
		JButton deployBtn = new JButton("🚀 部署运行");
		JButton cancelBtn = new JButton("❌ 取消");

		buttonPanel.add(saveBtn);
		buttonPanel.add(deployBtn);
		buttonPanel.add(cancelBtn);

		mainPanel.add(toolbar, BorderLayout.NORTH);
		mainPanel.add(editorScroll, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);
	}
}