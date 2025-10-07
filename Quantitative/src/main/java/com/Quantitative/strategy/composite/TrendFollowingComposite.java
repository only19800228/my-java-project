package com.Quantitative.strategy.composite;

import java.util.List;

import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;
import com.Quantitative.strategy.indicators.MovingAverageStrategy;

/**
 * 趋势跟踪组合策略 - 双均线 + 成交量确认
 */
public class TrendFollowingComposite extends BaseStrategy {
    private MovingAverageStrategy maStrategy;
    private double volumeThreshold;

    public TrendFollowingComposite() {
        super("趋势跟踪组合策略");
        this.maStrategy = new MovingAverageStrategy(5, 20, 5, 1.2);
        this.volumeThreshold = 1.2;
        initializeParameters();
    }

    public TrendFollowingComposite(int fastPeriod, int slowPeriod, int volumePeriod, double volumeThreshold) {
        super("趋势跟踪组合策略");
        this.maStrategy = new MovingAverageStrategy(fastPeriod, slowPeriod, volumePeriod, volumeThreshold);
        this.volumeThreshold = volumeThreshold;
        initializeParameters();
    }

    private void initializeParameters() {
        setParameter("volumeThreshold", volumeThreshold);
    }

    @Override
    protected void init() {
        if (dataFeed != null) {
            maStrategy.setDataFeed(dataFeed);
        }
        if (portfolio != null) {
            maStrategy.setPortfolio(portfolio);
        }
        maStrategy.initialize();
    }

    @Override
    protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
        // 使用移动平均线策略生成信号
        List<SignalEvent> maSignals = maStrategy.onBar(bar);
        signals.addAll(maSignals);

        // 可以在这里添加其他过滤条件
        // 例如：只在大趋势向上时交易
    }

    // 委托方法到内部策略
    public void setFastPeriod(int fastPeriod) {
        maStrategy.setFastPeriod(fastPeriod);
    }

    public void setSlowPeriod(int slowPeriod) {
        maStrategy.setSlowPeriod(slowPeriod);
    }

    public void setVolumeThreshold(double volumeThreshold) {
        this.volumeThreshold = volumeThreshold;
        maStrategy.setParameter("volumeThreshold", volumeThreshold);
    }
}