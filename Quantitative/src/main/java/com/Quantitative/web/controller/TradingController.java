// 文件: com/Quantitative/web/controller/TradingController.java
package com.Quantitative.web.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Quantitative.backtest.BacktestResult;
import com.Quantitative.backtest.EventDrivenBacktestEngine;
import com.Quantitative.config.BacktestConfig;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.trading.LiveTradingEngine;
import com.Quantitative.web.dto.BacktestRequest;
import com.Quantitative.web.dto.TradingResponse;

@RestController
@RequestMapping("/api/trading")
public class TradingController {
    
    @Autowired
    private LiveTradingEngine liveTradingEngine;
    
    @Autowired
    private AKShareDataFeed dataFeed;
    
    /**
     * 获取交易状态
     */
    @GetMapping("/status")
    public TradingResponse getTradingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isTrading", liveTradingEngine.isTrading());
        status.put("tradingStatus", liveTradingEngine.getTradingStatus());
        status.put("timestamp", LocalDateTime.now());
        
        return TradingResponse.success("获取状态成功", status);
    }
    
    /**
     * 开始回测
     */
    @PostMapping("/backtest")
    public TradingResponse runBacktest(@RequestBody BacktestRequest request) {
        try {
            // 创建回测配置
            BacktestConfig config = new BacktestConfig();
            config.setSymbol(request.getSymbol());
            config.setStartDate(request.getStartDate());
            config.setEndDate(request.getEndDate());
            config.setInitialCapital(request.getInitialCapital());
            config.setDebugMode(false);
            
            // 创建回测引擎
            EventDrivenBacktestEngine engine = new EventDrivenBacktestEngine(dataFeed, config);
            
            // 设置策略（这里需要根据请求创建对应策略）
            // engine.setStrategy(createStrategy(request.getStrategyType()));
            
            // 执行回测
            BacktestResult result = engine.runBacktest();
            
            // 返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("backtestResult", result);
            data.put("report", generateReport(result));
            
            return TradingResponse.success("回测完成", data);
            
        } catch (Exception e) {
            return TradingResponse.error("回测失败: " + e.getMessage());
        }
    }
    
    /**
     * 开始实时交易
     */
    @PostMapping("/start")
    public TradingResponse startTrading(@RequestBody Map<String, Object> request) {
        try {
            // 这里需要创建交易策略
            // BaseStrategy strategy = createTradingStrategy(request);
            
            // liveTradingEngine.startTrading(strategy);
            
            return TradingResponse.success("交易启动成功");
        } catch (Exception e) {
            return TradingResponse.error("启动交易失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止实时交易
     */
    @PostMapping("/stop")
    public TradingResponse stopTrading() {
        try {
            liveTradingEngine.stopTrading();
            return TradingResponse.success("交易停止成功");
        } catch (Exception e) {
            return TradingResponse.error("停止交易失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取投资组合信息
     */
    @GetMapping("/portfolio")
    public TradingResponse getPortfolio() {
        try {
            // 这里需要获取投资组合信息
            Map<String, Object> portfolioInfo = new HashMap<>();
            // portfolioInfo.put("positions", liveTradingEngine.getPortfolioInfo());
            
            return TradingResponse.success("获取投资组合成功", portfolioInfo);
        } catch (Exception e) {
            return TradingResponse.error("获取投资组合失败: " + e.getMessage());
        }
    }
    
    private Map<String, Object> generateReport(BacktestResult result) {
        Map<String, Object> report = new HashMap<>();
        report.put("totalReturn", result.getTotalReturn());
        report.put("annualReturn", result.getAnnualReturn());
        report.put("maxDrawdown", result.getMaxDrawdown());
        report.put("sharpeRatio", result.getSharpeRatio());
        report.put("totalTrades", result.getTotalTrades());
        report.put("winRate", result.getWinRate());
        return report;
    }
}