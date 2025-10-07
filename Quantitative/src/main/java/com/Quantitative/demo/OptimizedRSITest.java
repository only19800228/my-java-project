// 新增：快速测试类
package com.Quantitative.demo;

import java.time.LocalDateTime;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.config.SystemConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.strategy.indicators.EnhancedRSIStrategy;

/**
 * 优化后的RSI策略测试
 */
public class OptimizedRSITest {

    public static void main(String[] args) {
        System.out.println("=== 优化版RSI策略测试 ===\n");

        try {
            // 设置系统配置
            SystemConfig.setProperty("strategy.rsi.period", "14");
            SystemConfig.setProperty("strategy.rsi.overbought", "70");
            SystemConfig.setProperty("strategy.rsi.oversold", "30");
            SystemConfig.setProperty("log.level", "DEBUG");

            // 创建配置
            BacktestConfig config = new BacktestConfig();
            config.setSymbol("000001");
            config.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
            config.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
            config.setInitialCapital(100000.0);
            config.setDebugMode(true);

            // 创建数据源
            AKShareDataFeed dataFeed = new AKShareDataFeed();

            // 创建策略
            EnhancedRSIStrategy strategy = new EnhancedRSIStrategy();

            // 创建回测引擎
            EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
            engine.setStrategy(strategy);

            // 执行回测
            BacktestResult result = engine.runBacktest();

            // 分析结果
            result.printSummary();

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}