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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class BuySellDialog extends JDialog {
	private JTextField quantityField;
	private JLabel currentPriceLabel;
	private JLabel availableCashLabel;
	private JLabel tradableQuantityLabel;
	private JLabel tradeAmountLabel;
	private JLabel feeLabel;
	private JLabel totalAmountLabel;
	private boolean isBuy;

	// 颜色定义
	private final Color PRIMARY_COLOR = new Color(0, 122, 255);
	private final Color SUCCESS_COLOR = new Color(52, 199, 89);
	private final Color DANGER_COLOR = new Color(255, 59, 48);
	private final Color BACKGROUND_COLOR = new Color(242, 242, 247);
	private final Color CARD_COLOR = Color.WHITE;

	public BuySellDialog(JFrame parent, String title, boolean isBuy) {
		super(parent, title, true);
		this.isBuy = isBuy;
		initComponents();
		pack();
		setLocationRelativeTo(parent);
		setSize(450, 550);
		setResizable(false);
	}

	private void initComponents() {
		JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		mainPanel.setBackground(BACKGROUND_COLOR);

		// 标题区域
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(CARD_COLOR);
		titlePanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createEmptyBorder(15, 20, 15, 20)));

		JLabel titleLabel = new JLabel(isBuy ? "💰 买入股票" : "💸 卖出股票");
		titleLabel.setFont(new Font("PingFang SC", Font.BOLD, 18));
		titleLabel.setForeground(isBuy ? PRIMARY_COLOR : DANGER_COLOR);

		JLabel subTitleLabel = new JLabel("请确认交易信息");
		subTitleLabel.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		subTitleLabel.setForeground(new Color(150, 150, 150));

		titlePanel.add(titleLabel, BorderLayout.NORTH);
		titlePanel.add(subTitleLabel, BorderLayout.CENTER);

		// 内容区域
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(CARD_COLOR);
		contentPanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(230, 230, 230), 1),
				BorderFactory.createEmptyBorder(20, 20, 20, 20)));

		// 股票信息区域
		contentPanel.add(createInfoRow("📈 股票名称:", "贵州茅台"));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		contentPanel.add(createInfoRow("🔢 股票代码:", "600519"));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		contentPanel.add(createInfoRow("💰 当前价格:", createValueLabel("1750.00 元")));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		contentPanel.add(createInfoRow("💵 可用资金:", createValueLabel("1,000,000.00 元")));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		contentPanel.add(createInfoRow(isBuy ? "🛒 可买数量:" : "📦 可卖数量:", createValueLabel("571 股")));

		contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// 交易输入区域
		JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
		inputPanel.setBackground(CARD_COLOR);
		inputPanel.setBorder(BorderFactory.createTitledBorder(createLineBorder(isBuy ? PRIMARY_COLOR : DANGER_COLOR, 1),
				"📝 交易信息", TitledBorder.LEFT, TitledBorder.TOP, new Font("PingFang SC", Font.BOLD, 12),
				isBuy ? PRIMARY_COLOR : DANGER_COLOR));

		inputPanel.add(createStyledLabel("交易数量(股):", Font.BOLD, 12));
		quantityField = new JTextField("100");
		quantityField.setFont(new Font("PingFang SC", Font.PLAIN, 12));
		quantityField.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(200, 200, 200), 1),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		inputPanel.add(quantityField);

		inputPanel.add(createStyledLabel("交易金额:", Font.BOLD, 12));
		tradeAmountLabel = createValueLabel("0.00 元");
		inputPanel.add(tradeAmountLabel);

		inputPanel.add(createStyledLabel("手续费:", Font.BOLD, 12));
		feeLabel = createValueLabel("0.00 元");
		inputPanel.add(feeLabel);

		inputPanel.add(createStyledLabel("总金额:", Font.BOLD, 12));
		totalAmountLabel = createValueLabel("0.00 元");
		totalAmountLabel.setForeground(isBuy ? PRIMARY_COLOR : DANGER_COLOR);
		inputPanel.setFont(new Font("PingFang SC", Font.BOLD, 12));
		inputPanel.add(totalAmountLabel);

		contentPanel.add(inputPanel);

		contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// 仓位按钮区域
		JPanel positionPanel = new JPanel(new GridLayout(1, 4, 8, 0));
		positionPanel.setBackground(CARD_COLOR);
		positionPanel.setBorder(BorderFactory.createTitledBorder(createLineBorder(new Color(150, 150, 150), 1),
				"⚡ 快捷仓位", TitledBorder.LEFT, TitledBorder.TOP, new Font("PingFang SC", Font.BOLD, 12),
				new Color(100, 100, 100)));

		JButton fullPositionButton = createPositionButton("全仓", 1.0);
		JButton halfPositionButton = createPositionButton("1/2仓", 0.5);
		JButton thirdPositionButton = createPositionButton("1/3仓", 0.333);
		JButton twoThirdPositionButton = createPositionButton("2/3仓", 0.666);

		positionPanel.add(fullPositionButton);
		positionPanel.add(halfPositionButton);
		positionPanel.add(thirdPositionButton);
		positionPanel.add(twoThirdPositionButton);

		contentPanel.add(positionPanel);

		// 按钮区域
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		buttonPanel.setBackground(CARD_COLOR);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		JButton cancelButton = createSecondaryButton("❌ 取消");
		JButton confirmButton = isBuy ? createPrimaryButton("✅ 确认买入") : createDangerButton("✅ 确认卖出");

		cancelButton.addActionListener(e -> dispose());
		confirmButton.addActionListener(e -> confirmTrade());

		buttonPanel.add(cancelButton);
		buttonPanel.add(confirmButton);

		// 组合所有面板
		mainPanel.add(titlePanel, BorderLayout.NORTH);
		mainPanel.add(contentPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);

		// 添加数量变化监听
		quantityField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				updateTradeInfo();
			}

			public void removeUpdate(DocumentEvent e) {
				updateTradeInfo();
			}

			public void insertUpdate(DocumentEvent e) {
				updateTradeInfo();
			}
		});

		// 初始计算
		updateTradeInfo();
	}

	private JPanel createInfoRow(String label, String value) {
		return createInfoRow(label, new JLabel(value));
	}

	private JPanel createInfoRow(String label, Component valueComponent) {
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(CARD_COLOR);

		JLabel labelComp = createStyledLabel(label, Font.BOLD, 12);
		row.add(labelComp, BorderLayout.WEST);
		row.add(valueComponent, BorderLayout.EAST);

		return row;
	}

	private JLabel createValueLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("PingFang SC", Font.BOLD, 12));
		label.setForeground(PRIMARY_COLOR);
		return label;
	}

	private JLabel createStyledLabel(String text, int style, int size) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("PingFang SC", style, size));
		return label;
	}

	private JButton createPositionButton(String text, double ratio) {
		JButton button = new JButton(text);
		button.setFont(new Font("PingFang SC", Font.BOLD, 11));
		button.setBackground(new Color(240, 240, 240));
		button.setForeground(new Color(80, 80, 80));
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(200, 200, 200), 1),
				BorderFactory.createEmptyBorder(6, 2, 6, 2)));

		button.addActionListener(e -> setPosition(ratio));
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBackground(new Color(220, 220, 220));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(new Color(240, 240, 240));
			}
		});

		return button;
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
				BorderFactory.createEmptyBorder(8, 16, 8, 16)));
		return button;
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
		button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

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

	private Border createLineBorder(Color color, int thickness) {
		return BorderFactory.createLineBorder(color, thickness);
	}

	private void setPosition(double ratio) {
		if (isBuy) {
			double availableCash = 1000000.00; // 示例值
			double price = 1750.00; // 示例值
			int maxQuantity = (int) (availableCash * ratio / price);
			quantityField.setText(String.valueOf(maxQuantity));
		} else {
			// 卖出的逻辑，这里简化处理
			quantityField.setText("100"); // 示例值
		}
	}

	private void updateTradeInfo() {
		try {
			int quantity = Integer.parseInt(quantityField.getText());
			double price = 1750.00; // 示例价格
			double amount = quantity * price;
			double fee = amount * 0.0003; // 示例手续费率
			double totalAmount = isBuy ? amount + fee : amount - fee;

			tradeAmountLabel.setText(String.format("%,.2f 元", amount));
			feeLabel.setText(String.format("%,.2f 元", fee));
			totalAmountLabel.setText(String.format("%,.2f 元", totalAmount));

			// 买入时检查资金是否足够
			if (isBuy && totalAmount > 1000000.00) {
				totalAmountLabel.setForeground(DANGER_COLOR);
				totalAmountLabel.setText(totalAmountLabel.getText() + " (资金不足)");
			} else {
				totalAmountLabel.setForeground(isBuy ? PRIMARY_COLOR : DANGER_COLOR);
			}
		} catch (NumberFormatException e) {
			tradeAmountLabel.setText("0.00 元");
			feeLabel.setText("0.00 元");
			totalAmountLabel.setText("0.00 元");
		}
	}

	private void confirmTrade() {
		try {
			int quantity = Integer.parseInt(quantityField.getText());
			double totalAmount = Double
					.parseDouble(totalAmountLabel.getText().replace("元", "").replace(",", "").trim());

			if (isBuy && totalAmount > 1000000.00) {
				JOptionPane.showMessageDialog(this,
						"❌ 资金不足！\n所需金额: " + String.format("%,.2f", totalAmount) + "元\n可用资金: 1,000,000.00元", "交易失败",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// 这里实现实际的交易逻辑
			String message = isBuy ? String.format("✅ 买入成功！\n数量: %,d股\n总金额: %,.2f元", quantity, totalAmount)
					: String.format("✅ 卖出成功！\n数量: %,d股\n总金额: %,.2f元", quantity, totalAmount);

			JOptionPane.showMessageDialog(this, message, "交易成功", JOptionPane.INFORMATION_MESSAGE);
			dispose();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "请输入有效的数量", "输入错误", JOptionPane.ERROR_MESSAGE);
		}
	}
}