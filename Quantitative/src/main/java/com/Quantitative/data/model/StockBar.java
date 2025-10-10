package com.Quantitative.data.model;

import java.time.LocalDateTime;

/**
 * 股票K线数据类
 */
public class StockBar {
    private final String symbol;
    private final LocalDateTime timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;
    private final double turnover;

    public StockBar(String symbol, LocalDateTime timestamp, double open, double high, 
                   double low, double close, long volume, double turnover) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.turnover = turnover;
    }

    // Getter方法
    public String getSymbol() { return symbol; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }
    public double getTurnover() { return turnover; }

    @Override
    public String toString() {
        return String.format("StockBar{symbol='%s', time=%s, O=%.2f, H=%.2f, L=%.2f, C=%.2f, V=%,d}",
                symbol, timestamp, open, high, low, close, volume);
    }
}