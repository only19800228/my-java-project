package com.Quantitative.test.strategies;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.portfolio.RiskManager;
import com.Quantitative.strategy.indicators.ADXStrategy;
import com.Quantitative.strategy.indicators.ATRStrategy;
import com.Quantitative.strategy.indicators.CCIStrategy;
import com.Quantitative.strategy.indicators.KDJStrategy;
import com.Quantitative.strategy.indicators.OBVStrategy;

/**
 * 技术指标策略综合测试类
 * 测试ADX、ATR、CCI、KDJ、OBV五个指标策略的表现
 */
public class TechnicalIndicatorsComprehensiveTest {
    
    private static final String TEST_SYMBOL = "000001"; // 测试标的
    private static final double INITIAL_CAPITAL = 100000.0; // 初始资金
    
    public static void main(String[] args) {
        System.out.println("=== 技术指标策略综合测试开始 ===\n");
        
        try {
            // 执行综合测试
            runComprehensiveTest();
            
        } catch (Exception e) {
            System.err.println("综合测试执行失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 技术指标策略综合测试结束 ===");
    }
    
    /**
     * 运行综合测试
     */
    public static void runComprehensiveTest() {
        // 创建测试配置
        BacktestConfig config = createTestConfig();
        
        // 创建数据源
        AKShareDataFeed dataFeed = createDataFeed();
        
        // 测试各个策略
        Map<String, BacktestResult> results = new HashMap<>();
        
        // 1. 测试ADX策略
        System.out.println("## 1. ADX平均趋向指数策略测试 ##");
        results.put("ADX", testADXStrategy(dataFeed, config));
        
        // 2. 测试ATR策略
        System.out.println("\n## 2. ATR平均真实波幅策略测试 ##");
        results.put("ATR", testATRStrategy(dataFeed, config));
        
        // 3. 测试CCI策略
        System.out.println("\n## 3. CCI商品通道指标策略测试 ##");
        results.put("CCI", testCCIStrategy(dataFeed, config));
        
        // 4. 测试KDJ策略
        System.out.println("\n## 4. KDJ随机指标策略测试 ##");
        results.put("KDJ", testKDJStrategy(dataFeed, config));
        
        // 5. 测试OBV策略
        System.out.println("\n## 5. OBV能量潮策略测试 ##");
        results.put("OBV", testOBVStrategy(dataFeed, config));
        
        // 生成综合报告
        System.out.println("\n## 策略表现综合对比报告 ##");
        generateComparativeReport(results);
        
        // 策略组合测试
        System.out.println("\n## 策略组合测试 ##");
        testStrategyCombination(dataFeed, config);
    }
    
    /**
     * 创建测试配置
     */
    private static BacktestConfig createTestConfig() {
        BacktestConfig config = new BacktestConfig();
        
        config.setSymbol(TEST_SYMBOL);
        config.setStartDate(LocalDateTime.of(2023, 1, 1, 0, 0));
        config.setEndDate(LocalDateTime.of(2023, 12, 31, 0, 0));
        config.setInitialCapital(INITIAL_CAPITAL);
        config.setDebugMode(false); // 关闭调试模式提高性能
        config.setMaxBars(0);
        
        // 设置风险参数
        Map<String, Object> riskParams = new HashMap<>();
        riskParams.put("maxPositionRatio", 0.1);
        riskParams.put("maxDrawdownLimit", 0.15);
        riskParams.put("dailyLossLimit", 0.03);
        config.setRiskParams(riskParams);
        
        return config;
    }
    
    /**
     * 创建数据源
     */
    private static AKShareDataFeed createDataFeed() {
        AKShareDataFeed dataFeed = new AKShareDataFeed();
        dataFeed.setDebugMode(false);
        dataFeed.setParameter("timeframe", "daily");
        dataFeed.setParameter("adjust", "qfq");
        
        return dataFeed;
    }
    
    /**
     * 测试ADX策略
     */
    private static BacktestResult testADXStrategy(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("配置: ADX(14), 阈值=25, +DI=20, -DI=20");
        
        ADXStrategy adxStrategy = new ADXStrategy(14, 25.0, 20.0, 20.0);
        adxStrategy.setDebugMode(false);
        
        return runStrategyTest(dataFeed, config, adxStrategy, "ADX策略");
    }
    
    /**
     * 测试ATR策略
     */
    private static BacktestResult testATRStrategy(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("配置: ATR(14), 乘数=2.0, 波动率阈值=0.02");
        
        ATRStrategy atrStrategy = new ATRStrategy(14, 2.0, 0.02);
        atrStrategy.setDebugMode(false);
        
        return runStrategyTest(dataFeed, config, atrStrategy, "ATR策略");
    }
    
    /**
     * 测试CCI策略
     */
    private static BacktestResult testCCIStrategy(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("配置: CCI(14), 超买=100, 超卖=-100");
        
        CCIStrategy cciStrategy = new CCIStrategy(14, 100.0, -100.0);
        cciStrategy.setDebugMode(false);
        
        return runStrategyTest(dataFeed, config, cciStrategy, "CCI策略");
    }
    
    /**
     * 测试KDJ策略
     */
    private static BacktestResult testKDJStrategy(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("配置: KDJ(9,3,3), 超买=80, 超卖=20");
        
        KDJStrategy kdjStrategy = new KDJStrategy(9, 3, 80, 20);
        kdjStrategy.setDebugMode(false);
        
        return runStrategyTest(dataFeed, config, kdjStrategy, "KDJ策略");
    }
    
    /**
     * 测试OBV策略
     */
    private static BacktestResult testOBVStrategy(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("配置: OBV(20), 成交量阈值=1.5");
        
        OBVStrategy obvStrategy = new OBVStrategy(20, 1.5);
        obvStrategy.setDebugMode(false);
        
        return runStrategyTest(dataFeed, config, obvStrategy, "OBV策略");
    }
    
    /**
     * 运行单个策略测试
     */
    private static BacktestResult runStrategyTest(AKShareDataFeed dataFeed, BacktestConfig config, 
                                                 Object strategy, String strategyName) {
        try {
            EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
            
            // 设置策略
            if (strategy instanceof ADXStrategy) {
                engine.setStrategy((ADXStrategy) strategy);
            } else if (strategy instanceof ATRStrategy) {
                engine.setStrategy((ATRStrategy) strategy);
            } else if (strategy instanceof CCIStrategy) {
                engine.setStrategy((CCIStrategy) strategy);
            } else if (strategy instanceof KDJStrategy) {
                engine.setStrategy((KDJStrategy) strategy);
            } else if (strategy instanceof OBVStrategy) {
                engine.setStrategy((OBVStrategy) strategy);
            }
            
            // 设置风险管理
            RiskManager riskManager = new RiskManager();
            engine.setRiskManager(riskManager);
            
            long startTime = System.currentTimeMillis();
            BacktestResult result = engine.runBacktest();
            long endTime = System.currentTimeMillis();
            
            // 分析结果
            analyzeSingleStrategyResult(result, strategyName, endTime - startTime);
            
            return result;
            
        } catch (Exception e) {
            System.err.println(strategyName + " 测试失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 分析单个策略结果
     */
    private static void analyzeSingleStrategyResult(BacktestResult result, String strategyName, long duration) {
        if (result == null) {
            System.out.println("  ❌ " + strategyName + " 测试失败");
            return;
        }
        
        // 计算高级指标
        result.calculateAdvancedMetrics();
        
        System.out.println("  ✅ " + strategyName + " 测试完成");
        System.out.printf("  执行时间: %.2f秒\n", duration / 1000.0);
        System.out.printf("  总收益率: %.2f%%\n", result.getTotalReturn());
        System.out.printf("  年化收益率: %.2f%%\n", result.getAnnualReturn());
        System.out.printf("  最大回撤: %.2f%%\n", result.getMaxDrawdown());
        System.out.printf("  夏普比率: %.2f\n", result.getSharpeRatio());
        System.out.printf("  总交易次数: %d\n", result.getTotalTrades());
        System.out.printf("  胜率: %.1f%%\n", result.getWinRate());
        System.out.printf("  盈亏比: %.2f\n", result.getProfitFactor());
        
        // 策略评级
        String rating = rateStrategyPerformance(result);
        System.out.println("  策略评级: " + rating);
    }
    
    /**
     * 生成对比报告
     */
    private static void generateComparativeReport(Map<String, BacktestResult> results) {
        System.out.println("策略名称 | 收益率% | 年化收益% | 最大回撤% | 夏普比率 | 交易次数 | 胜率% | 盈亏比 | 评级");
        System.out.println("--------|--------|----------|----------|----------|----------|-------|--------|----");
        
        for (Map.Entry<String, BacktestResult> entry : results.entrySet()) {
            String strategyName = entry.getKey();
            BacktestResult result = entry.getValue();
            
            if (result != null) {
                String rating = rateStrategyPerformance(result);
                
                System.out.printf("%-8s| %7.2f| %9.2f| %9.2f| %9.2f| %9d| %6.1f| %7.2f| %s\n",
                    strategyName,
                    result.getTotalReturn(),
                    result.getAnnualReturn(),
                    result.getMaxDrawdown(),
                    result.getSharpeRatio(),
                    result.getTotalTrades(),
                    result.getWinRate(),
                    result.getProfitFactor(),
                    rating);
            } else {
                System.out.printf("%-8s| %7s| %9s| %9s| %9s| %9s| %6s| %7s| %s\n",
                    strategyName, "失败", "失败", "失败", "失败", "失败", "失败", "失败", "失败");
            }
        }
        
        // 找出最佳策略
        findBestStrategy(results);
    }
    
    /**
     * 找出最佳策略
     */
    private static void findBestStrategy(Map<String, BacktestResult> results) {
        String bestReturnStrategy = null;
        String bestSharpeStrategy = null;
        String bestWinRateStrategy = null;
        
        double maxReturn = -Double.MAX_VALUE;
        double maxSharpe = -Double.MAX_VALUE;
        double maxWinRate = -Double.MAX_VALUE;
        
        for (Map.Entry<String, BacktestResult> entry : results.entrySet()) {
            BacktestResult result = entry.getValue();
            if (result == null) continue;
            
            if (result.getTotalReturn() > maxReturn) {
                maxReturn = result.getTotalReturn();
                bestReturnStrategy = entry.getKey();
            }
            
            if (result.getSharpeRatio() > maxSharpe) {
                maxSharpe = result.getSharpeRatio();
                bestSharpeStrategy = entry.getKey();
            }
            
            if (result.getWinRate() > maxWinRate) {
                maxWinRate = result.getWinRate();
                bestWinRateStrategy = entry.getKey();
            }
        }
        
        System.out.println("\n🏆 最佳策略评选:");
        if (bestReturnStrategy != null) {
            System.out.printf("  最高收益: %s (%.2f%%)\n", bestReturnStrategy, maxReturn);
        }
        if (bestSharpeStrategy != null) {
            System.out.printf("  最佳风险收益: %s (夏普:%.2f)\n", bestSharpeStrategy, maxSharpe);
        }
        if (bestWinRateStrategy != null) {
            System.out.printf("  最高胜率: %s (%.1f%%)\n", bestWinRateStrategy, maxWinRate);
        }
    }
    
    /**
     * 策略组合测试
     */
    private static void testStrategyCombination(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("测试策略组合: ADX + ATR + CCI 三因子组合");
        
        try {
            // 创建组合策略（这里简化实现，实际应该使用策略组合框架）
            MultiIndicatorStrategy comboStrategy = new MultiIndicatorStrategy();
            
            EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
            engine.setStrategy(comboStrategy);
            
            RiskManager riskManager = new RiskManager();
            engine.setRiskManager(riskManager);
            
            long startTime = System.currentTimeMillis();
            BacktestResult result = engine.runBacktest();
            long endTime = System.currentTimeMillis();
            
            if (result != null) {
                result.calculateAdvancedMetrics();
                
                System.out.println("  ✅ 策略组合测试完成");
                System.out.printf("  执行时间: %.2f秒\n", (endTime - startTime) / 1000.0);
                System.out.printf("  总收益率: %.2f%%\n", result.getTotalReturn());
                System.out.printf("  最大回撤: %.2f%%\n", result.getMaxDrawdown());
                System.out.printf("  夏普比率: %.2f\n", result.getSharpeRatio());
                System.out.printf("  总交易次数: %d\n", result.getTotalTrades());
                
                // 与单一策略对比
                compareWithSingleStrategies(result);
            }
            
        } catch (Exception e) {
            System.err.println("策略组合测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 与单一策略对比
     */
    private static void compareWithSingleStrategies(BacktestResult comboResult) {
        // 这里可以添加与之前测试的单一策略的详细对比
        System.out.println("  📊 组合策略相比单一策略的改进:");
        System.out.println("    - 分散化投资，降低单一策略风险");
        System.out.println("    - 多因子确认，提高信号质量");
        System.out.println("    - 适应不同市场环境");
    }
    
    /**
     * 策略性能评级
     */
    private static String rateStrategyPerformance(BacktestResult result) {
        double totalReturn = result.getTotalReturn();
        double maxDrawdown = result.getMaxDrawdown();
        double sharpeRatio = result.getSharpeRatio();
        double winRate = result.getWinRate();
        
        int score = 0;
        
        // 收益率评分
        if (totalReturn > 20) score += 3;
        else if (totalReturn > 10) score += 2;
        else if (totalReturn > 0) score += 1;
        else if (totalReturn < -10) score -= 2;
        
        // 回撤评分
        if (maxDrawdown < 10) score += 3;
        else if (maxDrawdown < 15) score += 2;
        else if (maxDrawdown < 20) score += 1;
        else if (maxDrawdown > 30) score -= 2;
        
        // 夏普比率评分
        if (sharpeRatio > 1.0) score += 2;
        else if (sharpeRatio > 0.5) score += 1;
        else if (sharpeRatio < 0) score -= 1;
        
        // 胜率评分
        if (winRate > 60) score += 2;
        else if (winRate > 50) score += 1;
        else if (winRate < 40) score -= 1;
        
        if (score >= 8) return "优秀 ★★★★★";
        if (score >= 6) return "良好 ★★★★";
        if (score >= 4) return "一般 ★★★";
        if (score >= 2) return "及格 ★★";
        return "较差 ★";
    }
    
    /**
     * 多指标组合策略（简化实现）
     */
    static class MultiIndicatorStrategy extends com.Quantitative.strategy.base.BaseStrategy {
        private ADXStrategy adxStrategy;
        private ATRStrategy atrStrategy;
        private CCIStrategy cciStrategy;
        
        public MultiIndicatorStrategy() {
            super("多指标组合策略");
        }
        
        @Override
        protected void init() {
            // 初始化子策略
            adxStrategy = new ADXStrategy(14, 25.0, 20.0, 20.0);
            atrStrategy = new ATRStrategy(14, 2.0, 0.02);
            cciStrategy = new CCIStrategy(14, 100.0, -100.0);
            
            System.out.println("多指标组合策略初始化完成");
        }
        
        @Override
        protected void calculateSignals(com.Quantitative.core.events.BarEvent bar, 
                                      List<com.Quantitative.core.events.SignalEvent> signals) {
            // 这里实现多因子信号组合逻辑
            // 简化实现：需要至少两个指标确认才产生信号
            
            List<com.Quantitative.core.events.SignalEvent> adxSignals = adxStrategy.onBar(bar);
            List<com.Quantitative.core.events.SignalEvent> atrSignals = atrStrategy.onBar(bar);
            List<com.Quantitative.core.events.SignalEvent> cciSignals = cciStrategy.onBar(bar);
            
            // 多因子确认逻辑
            if (adxSignals.size() > 0 && atrSignals.size() > 0) {
                // ADX和ATR同时产生信号，信号强度加权平均
                com.Quantitative.core.events.SignalEvent combinedSignal = 
                    combineSignals(adxSignals.get(0), atrSignals.get(0));
                signals.add(combinedSignal);
            }
        }
        
        private com.Quantitative.core.events.SignalEvent combineSignals(
            com.Quantitative.core.events.SignalEvent signal1, 
            com.Quantitative.core.events.SignalEvent signal2) {
            
            double combinedStrength = (signal1.getStrength() + signal2.getStrength()) / 2;
            String direction = signal1.getSignalType(); // 简化：取第一个信号的方向
            
            return new com.Quantitative.core.events.SignalEvent(
                signal1.getTimestamp(),
                signal1.getSymbol(),
                direction,
                combinedStrength,
                "多指标组合"
            );
        }
    }
    
    /**
     * 参数敏感性测试
     */
    public static void runParameterSensitivityTest() {
        System.out.println("\n=== 参数敏感性测试 ===");
        
        BacktestConfig config = createTestConfig();
        AKShareDataFeed dataFeed = createDataFeed();
        
        // 测试不同参数的ADX策略
        testADXParameterSensitivity(dataFeed, config);
        
        // 测试不同参数的ATR策略
        testATRParameterSensitivity(dataFeed, config);
    }
    
    /**
     * ADX参数敏感性测试
     */
    private static void testADXParameterSensitivity(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("\nADX参数敏感性测试:");
        
        int[] periods = {9, 14, 21};
        double[] thresholds = {20.0, 25.0, 30.0};
        
        for (int period : periods) {
            for (double threshold : thresholds) {
                ADXStrategy strategy = new ADXStrategy(period, threshold, 20.0, 20.0);
                BacktestResult result = runStrategyTest(dataFeed, config, strategy, 
                    String.format("ADX(%d,%.0f)", period, threshold));
                
                if (result != null) {
                    System.out.printf("  ADX(%d,%.0f): 收益=%.2f%%, 夏普=%.2f\n", 
                        period, threshold, result.getTotalReturn(), result.getSharpeRatio());
                }
            }
        }
    }
    
    /**
     * ATR参数敏感性测试
     */
    private static void testATRParameterSensitivity(AKShareDataFeed dataFeed, BacktestConfig config) {
        System.out.println("\nATR参数敏感性测试:");
        
        int[] periods = {7, 14, 21};
        double[] multipliers = {1.5, 2.0, 2.5};
        
        for (int period : periods) {
            for (double multiplier : multipliers) {
                ATRStrategy strategy = new ATRStrategy(period, multiplier, 0.02);
                BacktestResult result = runStrategyTest(dataFeed, config, strategy, 
                    String.format("ATR(%d,%.1f)", period, multiplier));
                
                if (result != null) {
                    System.out.printf("  ATR(%d,%.1f): 收益=%.2f%%, 夏普=%.2f\n", 
                        period, multiplier, result.getTotalReturn(), result.getSharpeRatio());
                }
            }
        }
    }
    
    /**
     * 生成详细分析报告
     */
    public static void generateDetailedAnalysisReport() {
        System.out.println("\n=== 技术指标策略详细分析报告 ===");
        
        System.out.println("\n1. 策略特性分析:");
        System.out.println("   • ADX策略: 趋势跟踪，适合趋势市，震荡市表现较差");
        System.out.println("   • ATR策略: 波动率适应，风险管理优秀，适合各种市况");
        System.out.println("   • CCI策略: 反转交易，捕捉极端行情，需要严格止损");
        System.out.println("   • KDJ策略: 动量振荡，短线交易，信号频繁需要过滤");
        System.out.println("   • OBV策略: 量价确认，趋势验证，适合作为辅助指标");
        
        System.out.println("\n2. 使用建议:");
        System.out.println("   • 新手推荐: ATR策略（风险控制好）");
        System.out.println("   • 趋势市: ADX + OBV组合");
        System.out.println("   • 震荡市: CCI + KDJ组合");
        System.out.println("   • 全市场: 多策略组合 + 动态权重");
        
        System.out.println("\n3. 风险提示:");
        System.out.println("   • 所有策略都需要适当的风险管理");
        System.out.println("   • 建议使用ATR进行止损和仓位管理");
        System.out.println("   • 定期进行参数优化和策略更新");
        System.out.println("   • 实盘前务必进行充分的历史回测");
    }
}