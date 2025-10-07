package com.Quantitative.core.events;

import java.time.LocalDateTime;

/**
 * K线数据事件
 */
public class BarEvent extends Event {
    private final String symbol;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;
    private final double turnover; // 成交额
    
    public BarEvent(LocalDateTime timestamp, String symbol, double open, 
                   double high, double low, double close, long volume) {
        this(timestamp, symbol, open, high, low, close, volume, 0.0);
    }
    
    public BarEvent(LocalDateTime timestamp, String symbol, double open,
                   double high, double low, double close, long volume, double turnover) {
        super(timestamp, "BAR");
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.turnover = turnover;
    }
    
    // Getter方法
    public String getSymbol() { return symbol; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }
    public double getTurnover() { return turnover; }
    
    /**
     * 计算价格变化
     */
    public double getPriceChange() {
        return close - open;
    }
    
    /**
     * 计算价格变化百分比
     */
    public double getPriceChangePercent() {
        return open != 0 ? (close - open) / open * 100 : 0.0;
    }
    
    /**
     * 计算振幅
     */
    public double getAmplitude() {
        return high != low ? (high - low) / low * 100 : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("BarEvent{%s %s O:%.2f H:%.2f L:%.2f C:%.2f V:%,d}", 
            getTimestamp(), symbol, open, high, low, close, volume);
    }
}