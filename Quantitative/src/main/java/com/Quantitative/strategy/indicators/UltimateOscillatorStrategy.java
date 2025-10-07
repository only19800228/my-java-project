package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * UO终极振荡器策略 - 多时间框架动量指标
 * 
 * UO结合了三个不同时间周期的动量，提供更可靠的超买超卖信号 计算公式: UO = 100 * [(4*PD7) + (2*PD14) + PD28] /
 * (4+2+1) 其中PDn = (BPsum / TRsum) 对于周期n BP = 买入压力 = Close - Min(Low, Previous
 * Close) TR = 真实波幅
 */
public class UltimateOscillatorStrategy extends BaseStrategy {
	// UO参数
	private int shortPeriod;
	private int mediumPeriod;
	private int longPeriod;
	private double overbought;
	private double oversold;
	private double signalThreshold;

	// 状态跟踪
	private List<Double> highPrices = new ArrayList<>();
	private List<Double> lowPrices = new ArrayList<>();
	private List<Double> closePrices = new ArrayList<>();
	private List<Double> uoValues = new ArrayList<>();
	private Double lastUO;
	private Double lastBuyingPressure;

	// 信号确认
	private boolean useDivergence;
	private boolean useTrendConfirmation;
	private int confirmationPeriod;

	public UltimateOscillatorStrategy() {
		super("UO终极振荡器策略");
		setDefaultParameters();
	}

	public UltimateOscillatorStrategy(int shortPeriod, int mediumPeriod, int longPeriod, double overbought,
			double oversold) {
		super("UO终极振荡器策略");
		this.shortPeriod = shortPeriod;
		this.mediumPeriod = mediumPeriod;
		this.longPeriod = longPeriod;
		this.overbought = overbought;
		this.oversold = oversold;
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		// 默认参数：Williams推荐的参数
		this.shortPeriod = 7;
		this.mediumPeriod = 14;
		this.longPeriod = 28;
		this.overbought = 70.0;
		this.oversold = 30.0;
		this.signalThreshold = 5.0;
		this.useDivergence = true;
		this.useTrendConfirmation = true;
		this.confirmationPeriod = 5;

		setParameter("shortPeriod", shortPeriod);
		setParameter("mediumPeriod", mediumPeriod);
		setParameter("longPeriod", longPeriod);
		setParameter("overbought", overbought);
		setParameter("oversold", oversold);
		setParameter("signalThreshold", signalThreshold);
		setParameter("useDivergence", useDivergence);
		setParameter("useTrendConfirmation", useTrendConfirmation);
		setParameter("confirmationPeriod", confirmationPeriod);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("UO策略初始化: 周期=(%d,%d,%d), 超买=%.1f, 超卖=%.1f", shortPeriod, mediumPeriod, longPeriod,
						overbought, oversold));

		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		uoValues.clear();
		lastUO = null;
		lastBuyingPressure = null;
	}

	@Override
	protected void calculateSignals(BarEvent bar, List<SignalEvent> signals) {
		try {
			// 更新价格数据
			updatePriceData(bar.getHigh(), bar.getLow(), bar.getClose());

			// 检查是否有足够数据
			if (!hasEnoughData()) {
				return;
			}

			// 计算UO指标
			Double uo = calculateUltimateOscillator();
			if (uo == null) {
				return;
			}

			uoValues.add(uo);
			lastUO = uo;

			// 限制数据大小
			if (uoValues.size() > longPeriod * 3) {
				uoValues.remove(0);
			}

			// 生成交易信号
			SignalEvent signal = generateUOSignal(bar, uo);
			if (signal != null) {
				signals.add(signal);
				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(),
						String.format("UO=%.1f", uo));
			}

			if (debugMode) {
				TradingLogger.debug(getName(), "%s UO=%.1f, 价格=%.2f", bar.getTimestamp().toLocalDate(), uo,
						bar.getClose());
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * 计算终极振荡器(UO)
	 */
	private Double calculateUltimateOscillator() {
		int requiredBars = longPeriod + 1; // 需要longPeriod+1条数据
		if (highPrices.size() < requiredBars || lowPrices.size() < requiredBars || closePrices.size() < requiredBars) {
			return null;
		}

		try {
			// 1. 计算购买压力(Buying Pressure)
			double buyingPressure = calculateBuyingPressure();
			lastBuyingPressure = buyingPressure;

			// 2. 计算真实波幅(True Range)
			double trueRange = calculateTrueRange();

			if (trueRange == 0) {
				return 50.0; // 避免除零
			}

			// 3. 计算三个周期的平均购买压力比
			double avg1 = calculateAverageRatio(shortPeriod, buyingPressure, trueRange);
			double avg2 = calculateAverageRatio(mediumPeriod, buyingPressure, trueRange);
			double avg3 = calculateAverageRatio(longPeriod, buyingPressure, trueRange);

			// 4. 计算UO：加权平均
			double uo = (avg1 * 4.0 + avg2 * 2.0 + avg3 * 1.0) / 7.0 * 100;

			return uo;

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateUltimateOscillator", e);
			return null;
		}
	}

	/**
	 * 计算购买压力
	 */
	private double calculateBuyingPressure() {
		int currentIndex = closePrices.size() - 1;
		double currentClose = closePrices.get(currentIndex);
		double currentLow = lowPrices.get(currentIndex);
		double previousClose = closePrices.get(currentIndex - 1);

		// 购买压力 = 当前收盘价 - Min(当前最低价, 前收盘价)
		return currentClose - Math.min(currentLow, previousClose);
	}

	/**
	 * 计算真实波幅
	 */
	private double calculateTrueRange() {
		int currentIndex = closePrices.size() - 1;
		double currentHigh = highPrices.get(currentIndex);
		double currentLow = lowPrices.get(currentIndex);
		double previousClose = closePrices.get(currentIndex - 1);

		double tr1 = currentHigh - currentLow;
		double tr2 = Math.abs(currentHigh - previousClose);
		double tr3 = Math.abs(currentLow - previousClose);

		return Math.max(tr1, Math.max(tr2, tr3));
	}

	/**
	 * 计算平均比率
	 */
	private double calculateAverageRatio(int period, double currentBP, double currentTR) {
		double sumBP = currentBP;
		double sumTR = currentTR;

		// 累加前period-1个周期的数据
		for (int i = 1; i < period; i++) {
			int index = closePrices.size() - 1 - i;
			if (index <= 0)
				break;

			double bp = calculateHistoricalBuyingPressure(index);
			double tr = calculateHistoricalTrueRange(index);

			sumBP += bp;
			sumTR += tr;
		}

		return sumTR != 0 ? sumBP / sumTR : 0.0;
	}

	/**
	 * 计算历史购买压力
	 */
	private double calculateHistoricalBuyingPressure(int index) {
		double currentClose = closePrices.get(index);
		double currentLow = lowPrices.get(index);
		double previousClose = closePrices.get(index - 1);

		return currentClose - Math.min(currentLow, previousClose);
	}

	/**
	 * 计算历史真实波幅
	 */
	private double calculateHistoricalTrueRange(int index) {
		double currentHigh = highPrices.get(index);
		double currentLow = lowPrices.get(index);
		double previousClose = closePrices.get(index - 1);

		double tr1 = currentHigh - currentLow;
		double tr2 = Math.abs(currentHigh - previousClose);
		double tr3 = Math.abs(currentLow - previousClose);

		return Math.max(tr1, Math.max(tr2, tr3));
	}

	/**
	 * 生成UO交易信号
	 */
	private SignalEvent generateUOSignal(BarEvent bar, double uo) {
		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());

		// 超卖区域买入信号
		if (!hasPosition && uo < oversold) {
			// 信号确认
			if (isBuySignalConfirmed(uo)) {
				double strength = calculateBuySignalStrength(uo);
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "UO超卖");
			}
		}

		// 超买区域卖出信号
		if (hasPosition && uo > overbought) {
			// 信号确认
			if (isSellSignalConfirmed(uo)) {
				double strength = calculateSellSignalStrength(uo);
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", strength, "UO超买");
			}
		}

		// 背离信号
		SignalEvent divergenceSignal = generateDivergenceSignal(bar, uo, hasPosition);
		if (divergenceSignal != null) {
			return divergenceSignal;
		}

		return null;
	}

	/**
	 * 买入信号确认
	 */
	private boolean isBuySignalConfirmed(double uo) {
		// 1. 超卖确认
		boolean oversoldConfirmed = (oversold - uo) >= signalThreshold;

		// 2. 趋势确认（可选）
		boolean trendConfirmed = !useTrendConfirmation || isUptrend();

		// 3. 背离确认（可选）
		boolean divergenceConfirmed = !useDivergence || checkBullishDivergence();

		return oversoldConfirmed && trendConfirmed && divergenceConfirmed;
	}

	/**
	 * 卖出信号确认
	 */
	private boolean isSellSignalConfirmed(double uo) {
		// 1. 超买确认
		boolean overboughtConfirmed = (uo - overbought) >= signalThreshold;

		// 2. 趋势确认（可选）
		boolean trendConfirmed = !useTrendConfirmation || isDowntrend();

		// 3. 背离确认（可选）
		boolean divergenceConfirmed = !useDivergence || checkBearishDivergence();

		return overboughtConfirmed && trendConfirmed && divergenceConfirmed;
	}

	/**
	 * 检查看涨背离
	 */
	private boolean checkBullishDivergence() {
		if (uoValues.size() < confirmationPeriod + 1)
			return false;

		// 价格创新低但UO没有创新低
		double currentPrice = closePrices.get(closePrices.size() - 1);
		double currentUO = uoValues.get(uoValues.size() - 1);

		for (int i = 2; i <= confirmationPeriod; i++) {
			int priceIndex = closePrices.size() - i;
			int uoIndex = uoValues.size() - i;

			if (priceIndex < 0 || uoIndex < 0)
				break;

			double historicalPrice = closePrices.get(priceIndex);
			double historicalUO = uoValues.get(uoIndex);

			if (currentPrice < historicalPrice && currentUO > historicalUO) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 检查看跌背离
	 */
	private boolean checkBearishDivergence() {
		if (uoValues.size() < confirmationPeriod + 1)
			return false;

		// 价格创新高但UO没有创新高
		double currentPrice = closePrices.get(closePrices.size() - 1);
		double currentUO = uoValues.get(uoValues.size() - 1);

		for (int i = 2; i <= confirmationPeriod; i++) {
			int priceIndex = closePrices.size() - i;
			int uoIndex = uoValues.size() - i;

			if (priceIndex < 0 || uoIndex < 0)
				break;

			double historicalPrice = closePrices.get(priceIndex);
			double historicalUO = uoValues.get(uoIndex);

			if (currentPrice > historicalPrice && currentUO < historicalUO) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 生成背离信号
	 */
	private SignalEvent generateDivergenceSignal(BarEvent bar, double uo, boolean hasPosition) {
		boolean bullishDivergence = checkBullishDivergence();
		boolean bearishDivergence = checkBearishDivergence();

		if (!hasPosition && bullishDivergence) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.7, "UO底背离");
		}

		if (hasPosition && bearishDivergence) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.7, "UO顶背离");
		}

		return null;
	}

	/**
	 * 计算买入信号强度
	 */
	private double calculateBuySignalStrength(double uo) {
		double distanceFromOversold = Math.max(0, oversold - uo);
		double maxDistance = oversold;
		double baseStrength = distanceFromOversold / maxDistance;

		// 背离信号增强强度
		double divergenceBonus = checkBullishDivergence() ? 0.2 : 0.0;

		return Math.min(baseStrength * 0.8 + 0.2 + divergenceBonus, 1.0);
	}

	/**
	 * 计算卖出信号强度
	 */
	private double calculateSellSignalStrength(double uo) {
		double distanceFromOverbought = Math.max(0, uo - overbought);
		double maxDistance = 100 - overbought;
		double baseStrength = distanceFromOverbought / maxDistance;

		// 背离信号增强强度
		double divergenceBonus = checkBearishDivergence() ? 0.2 : 0.0;

		return Math.min(baseStrength * 0.8 + 0.2 + divergenceBonus, 1.0);
	}

	/**
	 * 判断上升趋势
	 */
	private boolean isUptrend() {
		if (closePrices.size() < 20)
			return true; // 数据不足时默认通过

		double currentPrice = closePrices.get(closePrices.size() - 1);
		double pastPrice = closePrices.get(closePrices.size() - 20);
		return currentPrice > pastPrice;
	}

	/**
	 * 判断下降趋势
	 */
	private boolean isDowntrend() {
		if (closePrices.size() < 20)
			return true; // 数据不足时默认通过

		double currentPrice = closePrices.get(closePrices.size() - 1);
		double pastPrice = closePrices.get(closePrices.size() - 20);
		return currentPrice < pastPrice;
	}

	private void updatePriceData(double high, double low, double close) {
		highPrices.add(high);
		lowPrices.add(low);
		closePrices.add(close);

		// 限制数据大小
		int maxSize = longPeriod * 3;
		if (highPrices.size() > maxSize) {
			highPrices.remove(0);
			lowPrices.remove(0);
			closePrices.remove(0);
		}
	}

	private boolean hasEnoughData() {
		return highPrices.size() >= longPeriod + 1;
	}

	@Override
	public void reset() {
		super.reset();
		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		uoValues.clear();
		lastUO = null;
		lastBuyingPressure = null;
	}

	// ==================== Getter和Setter方法 ====================

	public int getShortPeriod() {
		return shortPeriod;
	}

	public void setShortPeriod(int shortPeriod) {
		this.shortPeriod = shortPeriod;
		setParameter("shortPeriod", shortPeriod);
	}

	public int getMediumPeriod() {
		return mediumPeriod;
	}

	public void setMediumPeriod(int mediumPeriod) {
		this.mediumPeriod = mediumPeriod;
		setParameter("mediumPeriod", mediumPeriod);
	}

	public int getLongPeriod() {
		return longPeriod;
	}

	public void setLongPeriod(int longPeriod) {
		this.longPeriod = longPeriod;
		setParameter("longPeriod", longPeriod);
	}

	public double getOverbought() {
		return overbought;
	}

	public void setOverbought(double overbought) {
		this.overbought = overbought;
		setParameter("overbought", overbought);
	}

	public double getOversold() {
		return oversold;
	}

	public void setOversold(double oversold) {
		this.oversold = oversold;
		setParameter("oversold", oversold);
	}

	public Double getLastUO() {
		return lastUO;
	}

	public Double getLastBuyingPressure() {
		return lastBuyingPressure;
	}

	/**
	 * 获取UO指标描述
	 */
	public String getUODescription() {
		return String.format("UO(%d,%d,%d) - 多时间框架动量振荡器", shortPeriod, mediumPeriod, longPeriod);
	}
}