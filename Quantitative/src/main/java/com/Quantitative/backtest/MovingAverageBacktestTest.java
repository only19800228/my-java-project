package com.Quantitative.backtest;

import com.Quantitative.data.repository.CSVDataLoader;
import com.Quantitative.data.model.StockData;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.common.utils.TradingLogger;
import java.util.List;

/**
 * 移动平均线策略回测测试
 */
public class MovingAverageBacktestTest {
    public static void main(String[] args) {
        System.out.println("=== 移动平均线策略回测测试 ===");
        
        // 1. 加载数据
        System.out.println("\n1. 加载股票数据...");
        CSVDataLoader loader = new CSVDataLoader();
        StockData stockData = loader.loadStockData("000001");
        
        if (stockData == null || stockData.isEmpty()) {
            System.out.println("❌ 数据加载失败");
            return;
        }
        System.out.println("✅ 数据加载成功: " + stockData.size() + " 条记录");
        
        // 2. 创建策略
        System.out.println("\n2. 创建移动平均线策略...");
        MovingAverageStrategy strategy = new MovingAverageStrategy(5, 20);
        strategy.setDebugMode(true);
        strategy.initialize();
        
        System.out.println("策略: " + strategy.toString());
        
        // 3. 运行策略回测
        System.out.println("\n3. 运行策略回测...");
        runStrategyBacktest(strategy, stockData);
        
        System.out.println("\n🎉 移动平均线策略回测完成!");
    }
    
    /**
     * 运行策略回测
     */
    private static void runStrategyBacktest(MovingAverageStrategy strategy, StockData stockData) {
        int totalSignals = 0;
        int buySignals = 0;
        int sellSignals = 0;
        int previousPosition = 0;
        
        System.out.println("日期\t\t价格\t短MA\t长MA\t信号\t强度\t仓位");
        System.out.println("------------------------------------------------------------------------");
        
        for (int i = 0; i < stockData.size(); i++) {
            com.Quantitative.data.model.StockBar stockBar = stockData.getBar(i);
            
            // 创建BarEvent对象（将StockBar转换为BarEvent）
            BarEvent barEvent = convertToBarEvent(stockBar);
            
            // 处理K线数据
            List<SignalEvent> signals = strategy.onBar(barEvent);
            
            // 显示结果
            String signalInfo = "HOLD";
            String strengthInfo = "-";
            if (!signals.isEmpty()) {
                SignalEvent signal = signals.get(0);
                signalInfo = signal.getSignalType();
                strengthInfo = signal.getFormattedStrength();
                totalSignals++;
                if (signal.isBuySignal()) buySignals++;
                if (signal.isSellSignal()) sellSignals++;
            }
            
            int currentPosition = strategy.getPosition();
            String positionStr = getPositionString(currentPosition);
            
            // 只显示有信号或者每3条显示一次
            if (!signals.isEmpty() || i % 3 == 0 || i == stockData.size() - 1) {
                System.out.printf("%s\t%.2f\t%.2f\t%.2f\t%s\t%s\t%s\n",
                    stockBar.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    stockBar.getClose(),
                    strategy.getShortMA(),
                    strategy.getLongMA(),
                    signalInfo,
                    strengthInfo,
                    positionStr
                );
            }
            
            previousPosition = currentPosition;
        }
        
        // 显示统计结果
        System.out.println("\n=== 策略统计 ===");
        System.out.println("总交易信号: " + totalSignals);
        System.out.println("买入信号: " + buySignals);
        System.out.println("卖出信号: " + sellSignals);
        System.out.println("最终仓位: " + getPositionString(strategy.getPosition()));
        
        // 显示策略性能摘要
        System.out.println("\n=== 策略性能 ===");
        if (totalSignals > 0) {
            double winRate = (double) buySignals / totalSignals * 100;
            System.out.printf("买入信号占比: %.1f%%\n", winRate);
        }
        System.out.printf("移动平均线差值: %.4f\n", strategy.getShortMA() - strategy.getLongMA());
    }
    
    /**
     * 将StockBar转换为BarEvent
     */
    private static BarEvent convertToBarEvent(com.Quantitative.data.model.StockBar stockBar) {
        return new BarEvent(
            stockBar.getTimestamp(),
            stockBar.getSymbol(),
            stockBar.getOpen(),
            stockBar.getHigh(),
            stockBar.getLow(),
            stockBar.getClose(),
            stockBar.getVolume(),
            stockBar.getTurnover()
        );
    }
    
    /**
     * 获取仓位字符串
     */
    private static String getPositionString(int position) {
        return position == 1 ? "多头" : position == -1 ? "空头" : "空仓";
    }
}