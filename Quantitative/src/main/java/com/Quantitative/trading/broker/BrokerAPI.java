// 文件: com/Quantitative/trading/broker/BrokerAPI.java
package com.Quantitative.trading.broker;

import java.util.List;
import java.util.Map;

/**
 * 券商API统一接口
 */
public interface BrokerAPI {
    
    /**
     * 连接券商
     */
    boolean connect(Map<String, String> config);
    
    /**
     * 断开连接
     */
    void disconnect();
    
    /**
     * 查询账户信息
     */
    AccountInfo getAccountInfo();
    
    /**
     * 查询持仓
     */
    List<PositionInfo> getPositions();
    
    /**
     * 下单
     */
    OrderResult placeOrder(OrderRequest order);
    
    /**
     * 撤单
     */
    boolean cancelOrder(String orderId);
    
    /**
     * 查询订单状态
     */
    OrderStatus getOrderStatus(String orderId);
    
    /**
     * 查询市场数据
     */
    MarketData getMarketData(String symbol);
    
    /**
     * 订阅行情
     */
    void subscribeMarketData(List<String> symbols, MarketDataListener listener);
}

// 账户信息类
class AccountInfo {
    private String accountId;
    private double totalAsset;
    private double availableCash;
    private double marketValue;
    private double frozenCash;
    
    // getter/setter
}

// 持仓信息类
class PositionInfo {
    private String symbol;
    private int quantity;
    private double availableQuantity;
    private double costPrice;
    private double marketValue;
    
    // getter/setter
}

// 订单请求类
class OrderRequest {
    private String symbol;
    private String direction; // BUY, SELL
    private int quantity;
    private double price;
    private String orderType; // MARKET, LIMIT
    private String timeInForce; // GTC, IOC, FOK
    
    // getter/setter
}

// 订单结果类
class OrderResult {
    private boolean success;
    private String orderId;
    private String message;
    
    // getter/setter
}

// 订单状态类
class OrderStatus {
    private String orderId;
    private String status; // SUBMITTED, FILLED, CANCELLED, REJECTED
    private int filledQuantity;
    private double filledPrice;
    private String message;
    
    // getter/setter
}

// 市场数据类
class MarketData {
    private String symbol;
    private double lastPrice;
    private double bidPrice;
    private double askPrice;
    private int bidVolume;
    private int askVolume;
    private long volume;
    private double turnover;
    private long timestamp;
    
    // getter/setter
}

// 市场数据监听器
interface MarketDataListener {
    void onMarketData(MarketData data);
}