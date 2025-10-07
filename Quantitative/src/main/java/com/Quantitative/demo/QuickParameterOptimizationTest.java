// 新增：快速参数优化测试
package com.Quantitative.demo;

import java.time.LocalDateTime;
import java.util.Arrays;

import com.Quantitative.backtest.optimization.ParameterOptimizer;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 快速参数优化测试（使用随机搜索）
 */
public class QuickParameterOptimizationTest {

    public static void main(String[] args) {
        System.out.println("⚡ 快速参数优化测试开始\n");

        try {
            AKShareDataFeed dataFeed = new AKShareDataFeed();
            EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();
            
            BacktestConfig baseConfig = new BacktestConfig();
            baseConfig.setSymbol("000001");
            baseConfig.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
            baseConfig.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
            baseConfig.setInitialCapital(100000.0);

            ParameterOptimizer optimizer = new ParameterOptimizer(dataFeed, strategy, baseConfig);
            
            ParameterOptimizer.OptimizationConfig optConfig = new ParameterOptimizer.OptimizationConfig();
            optConfig.setMethod(ParameterOptimizer.OptimizationMethod.RANDOM_SEARCH);
            optConfig.setMaxIterations(20); // 只测试20个随机组合
            
            optConfig.addParameter("rsiPeriod", Arrays.asList(12, 14, 16, 18));
            optConfig.addParameter("overbought", Arrays.asList(68.0, 70.0, 72.0));
            optConfig.addParameter("oversold", Arrays.asList(28.0, 30.0, 32.0));
            
            optimizer.setOptimizationConfig(optConfig);

            ParameterOptimizer.OptimizationResult result = optimizer.optimize();
            result.printReport();

        } catch (Exception e) {
            System.err.println("⚠️ 快速优化测试异常: " + e.getMessage());
        }
    }
}