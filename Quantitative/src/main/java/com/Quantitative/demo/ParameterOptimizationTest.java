// 新增：参数优化测试
package com.Quantitative.demo;

import java.time.LocalDateTime;
import java.util.Arrays;

import com.Quantitative.backtest.optimization.ParameterOptimizer;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 参数优化测试
 */
public class ParameterOptimizationTest {

    public static void main(String[] args) {
        System.out.println("🔧 参数优化测试开始\n");

        try {
            // 1. 创建基础组件
            AKShareDataFeed dataFeed = new AKShareDataFeed();
            EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();
            
            BacktestConfig baseConfig = new BacktestConfig();
            baseConfig.setSymbol("000001");
            baseConfig.setStartDate(LocalDateTime.of(2022, 1, 1, 0, 0));
            baseConfig.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
            baseConfig.setInitialCapital(100000.0);
            baseConfig.setDebugMode(false);

            // 2. 创建优化器
            ParameterOptimizer optimizer = new ParameterOptimizer(dataFeed, strategy, baseConfig);
            
            // 3. 配置优化参数
            ParameterOptimizer.OptimizationConfig optConfig = new ParameterOptimizer.OptimizationConfig();
            optConfig.setMethod(ParameterOptimizer.OptimizationMethod.GRID_SEARCH);
            optConfig.setMaxThreads(4); // 使用4个线程
            
            // 定义参数搜索空间
            optConfig.addParameter("rsiPeriod", Arrays.asList(10, 12, 14, 16, 18, 20));
            optConfig.addParameter("overbought", Arrays.asList(65.0, 68.0, 70.0, 72.0, 75.0));
            optConfig.addParameter("oversold", Arrays.asList(25.0, 28.0, 30.0, 32.0, 35.0));
            
            // 设置评分权重
            optConfig.setReturnWeight(1.0);
            optConfig.setSharpeWeight(0.8);
            optConfig.setDrawdownWeight(2.0);
            optConfig.setWinRateWeight(0.5);
            
            optimizer.setOptimizationConfig(optConfig);

            // 4. 执行优化
            ParameterOptimizer.OptimizationResult result = optimizer.optimize();

            // 5. 显示结果
            result.printReport();

        } catch (Exception e) {
            System.err.println("❌ 参数优化测试失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n🔧 参数优化测试结束");
    }
}