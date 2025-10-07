// 文件: com/Quantitative/trading/LiveTradingEngine.java
package com.Quantitative.trading;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.config.SystemConfig;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.execution.ExecutionEngine;
import com.Quantitative.portfolio.Portfolio;
import com.Quantitative.portfolio.RiskManager;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * 实时交易引擎
 */
public class LiveTradingEngine {
    private final DataFeed dataFeed;
    private final ExecutionEngine executionEngine;
    private final Portfolio portfolio;
    private final RiskManager riskManager;
    private BaseStrategy strategy;
    
    private ScheduledExecutorService scheduler;
    private Map<String, Object> tradingStatus;
    private boolean isTrading = false;
    private int checkInterval; // 检查间隔(秒)
    
    public LiveTradingEngine(DataFeed dataFeed, ExecutionEngine executionEngine, 
                           Portfolio portfolio, RiskManager riskManager) {
        this.dataFeed = dataFeed;
        this.executionEngine = executionEngine;
        this.portfolio = portfolio;
        this.riskManager = riskManager;
        this.tradingStatus = new ConcurrentHashMap<>();
        this.checkInterval = SystemConfig.getInt("trading.check.interval", 5);
        
        initializeTradingEngine();
    }
    
    private void initializeTradingEngine() {
        // 初始化交易状态
        tradingStatus.put("startTime", LocalDateTime.now());
        tradingStatus.put("totalSignals", 0);
        tradingStatus.put("executedOrders", 0);
        tradingStatus.put("rejectedOrders", 0);
        tradingStatus.put("lastCheck", LocalDateTime.now());
        
        TradingLogger.logSystemError("LiveTrading", "initialize", 
            new Exception("实时交易引擎初始化完成"));
    }
    
    /**
     * 开始实时交易
     */
    public void startTrading(BaseStrategy tradingStrategy) {
        if (isTrading) {
            TradingLogger.logRisk("WARN", "LiveTrading", "交易已在运行中");
            return;
        }
        
        this.strategy = tradingStrategy;
        this.isTrading = true;
        
        // 初始化调度器
        scheduler = Executors.newScheduledThreadPool(2);
        
        // 启动市场数据监听
        scheduler.scheduleAtFixedRate(this::checkMarketData, 0, checkInterval, TimeUnit.SECONDS);
        
        // 启动风险监控
        scheduler.scheduleAtFixedRate(this::monitorRisk, 30, 30, TimeUnit.SECONDS);
        
        // 启动状态报告
        scheduler.scheduleAtFixedRate(this::reportStatus, 60, 60, TimeUnit.SECONDS);
        
        TradingLogger.logTrade("SYSTEM", "START", 0, 0, 0);
        System.out.println("?? 实时交易引擎启动成功");
    }
    
    /**
     * 停止实时交易
     */
    public void stopTrading() {
        if (!isTrading) {
            return;
        }
        
        isTrading = false;
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        TradingLogger.logTrade("SYSTEM", "STOP", 0, 0, 0);
        System.out.println("?? 实时交易引擎已停止");
    }
    
    /**
     * 检查市场数据并生成交易信号
     */
    private void checkMarketData() {
        if (!isTrading || strategy == null) {
            return;
        }
        
        try {
            // 获取最新市场数据
            if (dataFeed.hasNextBar()) {
                BarEvent latestBar = dataFeed.getNextBar();
                
                // 更新投资组合市场价格
                portfolio.updateMarketPrice(latestBar.getSymbol(), latestBar.getClose());
                
                // 生成交易信号
                List<SignalEvent> signals = strategy.onBar(latestBar);
                
                // 处理交易信号
                processTradingSignals(signals);
                
                // 更新状态
                tradingStatus.put("lastCheck", LocalDateTime.now());
                tradingStatus.put("lastPrice", latestBar.getClose());
                tradingStatus.put("lastSymbol", latestBar.getSymbol());
            }
            
        } catch (Exception e) {
            TradingLogger.logSystemError("LiveTrading", "checkMarketData", e);
        }
    }
    
    /**
     * 处理交易信号
     */
    private void processTradingSignals(List<SignalEvent> signals) {
        if (signals == null || signals.isEmpty()) {
            return;
        }
        
        int signalCount = (Integer) tradingStatus.getOrDefault("totalSignals", 0);
        tradingStatus.put("totalSignals", signalCount + signals.size());
        
        for (SignalEvent signal : signals) {
            try {
                // 风险验证
                RiskManager.RiskValidationResult riskResult = riskManager.validateSignal(signal);
                if (!riskResult.isValid()) {
                    TradingLogger.logRisk("WARN", "LiveTrading", 
                        "信号被风险拒绝: {} - {}", signal.getSymbol(), riskResult.getMessage());
                    
                    int rejectedCount = (Integer) tradingStatus.getOrDefault("rejectedOrders", 0);
                    tradingStatus.put("rejectedOrders", rejectedCount + 1);
                    continue;
                }
                
                // 生成订单
                com.Quantitative.core.events.OrderEvent order = portfolio.processSignal(signal);
                if (order != null) {
                    // 执行订单
                    com.Quantitative.core.events.FillEvent fill = executionEngine.executeOrder(order);
                    if (fill != null) {
                        // 处理成交
                        portfolio.processFill(fill);
                        
                        int executedCount = (Integer) tradingStatus.getOrDefault("executedOrders", 0);
                        tradingStatus.put("executedOrders", executedCount + 1);
                        
                        TradingLogger.logTrade(signal.getSymbol(), 
                            signal.getSignalType(), 
                            fill.getFillPrice(), 
                            fill.getQuantity(), 
                            fill.getRealizedPnl());
                        
                        System.out.printf("[实盘交易] 成交: %s %s %d股 @%.2f%n", 
                            signal.getSymbol(), signal.getSignalType(), 
                            fill.getQuantity(), fill.getFillPrice());
                    }
                }
                
            } catch (Exception e) {
                TradingLogger.logSystemError("LiveTrading", "processSignal", e);
            }
        }
    }
    
    /**
     * 风险监控
     */
    private void monitorRisk() {
        if (!isTrading) return;
        
        try {
            // 检查投资组合风险
            double totalValue = portfolio.getTotalValue();
            double drawdown = calculateCurrentDrawdown();
            
            // 风险警报
            if (drawdown > 0.1) { // 10%回撤警报
                TradingLogger.logRisk("WARN", "RiskMonitor", 
                    "当前回撤超过10%: {:.2f}%", drawdown * 100);
            }
            
            // 更新风险状态
            tradingStatus.put("currentDrawdown", drawdown);
            tradingStatus.put("portfolioValue", totalValue);
            tradingStatus.put("cash", portfolio.getCash());
            
        } catch (Exception e) {
            TradingLogger.logSystemError("LiveTrading", "monitorRisk", e);
        }
    }
    
    /**
     * 状态报告
     */
    private void reportStatus() {
        if (!isTrading) return;
        
        System.out.println("\n=== 实时交易状态报告 ===");
        System.out.printf("运行时间: %s%n", tradingStatus.get("startTime"));
        System.out.printf("总信号数: %d%n", tradingStatus.get("totalSignals"));
        System.out.printf("成交订单: %d%n", tradingStatus.get("executedOrders"));
        System.out.printf("拒绝订单: %d%n", tradingStatus.get("rejectedOrders"));
        System.out.printf("投资组合价值: %.2f%n", tradingStatus.getOrDefault("portfolioValue", 0.0));
        System.out.printf("当前回撤: %.2f%%%n", 
            (Double) tradingStatus.getOrDefault("currentDrawdown", 0.0) * 100);
        System.out.printf("最后检查: %s%n", tradingStatus.get("lastCheck"));
        System.out.println("=======================\n");
    }
    
    /**
     * 计算当前回撤
     */
    private double calculateCurrentDrawdown() {
        // 简化实现，实际应该基于峰值计算
        Double portfolioValue = (Double) tradingStatus.get("portfolioValue");
        Double initialCash = portfolio.getInitialCash();
        
        if (portfolioValue != null && initialCash > 0) {
            return Math.max(0, (initialCash - portfolioValue) / initialCash);
        }
        return 0.0;
    }
    
    // ==================== Getter方法 ====================
    
    public boolean isTrading() {
        return isTrading;
    }
    
    public Map<String, Object> getTradingStatus() {
        return new HashMap<>(tradingStatus);
    }
    
    public void setCheckInterval(int intervalSeconds) {
        this.checkInterval = intervalSeconds;
    }
}