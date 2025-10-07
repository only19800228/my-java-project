package com.Quantitative.data.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.interfaces.TradingComponent;
import com.Quantitative.data.AKShareDataFeed;
import com.Quantitative.data.DataInfo;
import com.Quantitative.data.DataSource;
import com.Quantitative.data.validation.DataQualityReport;

/**
 * AKShare数据源适配器 - 让 AKShareDataFeed 实现 DataSource 接口
 */
public class AKShareDataSourceAdapter implements DataSource {
    
    private final AKShareDataFeed akShareFeed;
    private String status = "CREATED";
    
    public AKShareDataSourceAdapter() {
        this.akShareFeed = new AKShareDataFeed();
    }
    
    public AKShareDataSourceAdapter(AKShareDataFeed akShareFeed) {
        this.akShareFeed = akShareFeed;
    }
    
    @Override
    public void initialize() {
        akShareFeed.initialize();
        this.status = "INITIALIZED";
    }
    
    @Override
    public void configure(Map<String, Object> config) {
        akShareFeed.configure(config);
    }
    
    @Override
    public String getName() {
        return "AKShareDataSourceAdapter";
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    @Override
    public void reset() {
        akShareFeed.reset();
        this.status = "RESET";
    }
    
    @Override
    public void shutdown() {
        akShareFeed.shutdown();
        this.status = "SHUTDOWN";
    }
    
    @Override
    public List<BarEvent> loadHistoricalData(String symbol, LocalDateTime start, LocalDateTime end) {
        return akShareFeed.loadHistoricalData(symbol, start, end);
    }
    
    @Override
    public DataInfo getDataInfo() {
        return akShareFeed.getDataInfo();
    }
    
    @Override
    public boolean isConnected() {
        return akShareFeed.isConnected();
    }
    
    @Override
    public String getDataSourceType() {
        return "AKSHARE_API";
    }
    
    @Override
    public List<String> getAvailableSymbols() {
        return akShareFeed.getAvailableSymbols();
    }
    
    @Override
    public DataQualityReport getDataQualityReport(String symbol, LocalDateTime start, LocalDateTime end) {
        return akShareFeed.getDataQualityReport(symbol, start, end);
    }
    
    /**
     * 获取底层的 AKShareDataFeed 实例
     */
    public AKShareDataFeed getAkShareFeed() {
        return akShareFeed;
    }
}