package com.Quantitative.strategy.base;

import java.util.List;
import java.util.Map;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.core.interfaces.TradingComponent;
import com.Quantitative.data.DataFeed;
import com.Quantitative.portfolio.Portfolio;

/**
 * 策略接口 - 所有交易策略的基类接口
 */
public interface Strategy extends TradingComponent {

	/**
	 * 处理K线数据，生成交易信号
	 */
	List<SignalEvent> onBar(BarEvent bar);

	/**
	 * 设置数据馈送
	 */
	void setDataFeed(DataFeed dataFeed);

	/**
	 * 设置投资组合
	 */
	void setPortfolio(Portfolio portfolio);

	/**
	 * 获取策略名称
	 */
	String getName();

	/**
	 * 获取策略参数
	 */
	Map<String, Object> getParameters();

	/**
	 * 设置策略参数
	 */
	void setParameter(String key, Object value);

	/**
	 * 获取策略状态信息
	 */
	Map<String, Object> getStrategyStatus();

	/**
	 * 设置调试模式
	 */
	void setDebugMode(boolean debugMode);
}