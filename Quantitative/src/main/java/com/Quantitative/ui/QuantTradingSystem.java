package com.Quantitative.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

public class QuantTradingSystem {
	private JFrame mainFrame;
	private JTabbedPane mainTabbedPane;

	// 各个模块面板
	private DashboardPanel dashboardPanel;
	private StrategyManagementPanel strategyManagementPanel;
	private MonitoringPanel monitoringPanel;
	private DataAnalysisPanel dataAnalysisPanel;
	private RiskManagementPanel riskManagementPanel;

	public QuantTradingSystem() {
		setupLookAndFeel();
		initializeComponents();
		setupMainFrame();
	}

	private void setupLookAndFeel() {
		try {
			// 使用深色主题更适合交易系统
			FlatMacDarkLaf.setup();
			UIManager.put("TabbedPane.selectedBackground", new Color(45, 45, 48));
			UIManager.put("TabbedPane.background", new Color(30, 30, 30));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeComponents() {
		mainFrame = new JFrame("量化交易系统");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setLayout(new BorderLayout());

		// 创建顶部菜单
		createMenuBar();

		// 创建主选项卡面板
		mainTabbedPane = new JTabbedPane();
		mainTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		// 初始化各个模块
		dashboardPanel = new DashboardPanel();
		strategyManagementPanel = new StrategyManagementPanel();
		monitoringPanel = new MonitoringPanel();
		dataAnalysisPanel = new DataAnalysisPanel();
		riskManagementPanel = new RiskManagementPanel();

		// 添加选项卡
		mainTabbedPane.addTab("📊 仪表盘", dashboardPanel);
		mainTabbedPane.addTab("🤖 策略管理", strategyManagementPanel);
		mainTabbedPane.addTab("👁️ 实时监控", monitoringPanel);
		mainTabbedPane.addTab("📈 数据分析", dataAnalysisPanel);
		mainTabbedPane.addTab("🛡️ 风险管理", riskManagementPanel);

		mainFrame.add(mainTabbedPane, BorderLayout.CENTER);
	}

	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// 文件菜单
		JMenu fileMenu = new JMenu("文件");
		JMenuItem exitItem = new JMenuItem("退出");
		exitItem.addActionListener(e -> System.exit(0));
		fileMenu.add(exitItem);

		// 视图菜单
		JMenu viewMenu = new JMenu("视图");
		JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("深色模式", true);
		darkModeItem.addActionListener(e -> toggleTheme(darkModeItem.isSelected()));
		viewMenu.add(darkModeItem);

		// 帮助菜单
		JMenu helpMenu = new JMenu("帮助");
		JMenuItem aboutItem = new JMenuItem("关于");
		helpMenu.add(aboutItem);

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		menuBar.add(helpMenu);

		mainFrame.setJMenuBar(menuBar);
	}

	private void toggleTheme(boolean darkMode) {
		try {
			if (darkMode) {
				FlatMacDarkLaf.setup();
			} else {
				FlatMacLightLaf.setup();
			}
			SwingUtilities.updateComponentTreeUI(mainFrame);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setupMainFrame() {
		mainFrame.setSize(1400, 900);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(QuantTradingSystem::new);
	}
}