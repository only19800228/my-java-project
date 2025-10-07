package com.Quantitative.test;

import com.Quantitative.test.strategies.TechnicalIndicatorsComprehensiveTest;

/**
 * 技术指标测试运行入口
 */
public class TechnicalIndicatorsTestRunner {
    
    public static void main(String[] args) {
        System.out.println("启动技术指标策略综合测试...\n");
        
        // 运行完整测试套件
        TechnicalIndicatorsComprehensiveTest.runComprehensiveTest();
        
        // 运行参数敏感性测试（可选）
        // TechnicalIndicatorsComprehensiveTest.runParameterSensitivityTest();
        
        // 生成详细分析报告
        TechnicalIndicatorsComprehensiveTest.generateDetailedAnalysisReport();
    }
}