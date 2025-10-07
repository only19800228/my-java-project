package com.Quantitative.strategy.indicators;

import java.util.ArrayList;
import java.util.List;

import com.Quantitative.common.utils.TradingLogger;
import com.Quantitative.core.events.BarEvent;
import com.Quantitative.core.events.SignalEvent;
import com.Quantitative.strategy.base.BaseStrategy;

/**
 * KDJ随机指标策略 - 动量振荡器，适合短线交易
 */
public class KDJStrategy extends BaseStrategy {
	private int kPeriod;
	private int dPeriod;
	private int jPeriod;
	private double overbought;
	private double oversold;
	private boolean useCrossSignals;

	private List<Double> highPrices = new ArrayList<>();
	private List<Double> lowPrices = new ArrayList<>();
	private List<Double> closePrices = new ArrayList<>();
	private Double lastK;
	private Double lastD;
	private Double lastJ;
	private Double lastClose;

	public KDJStrategy() {
		super("KDJ随机指标策略");
		setDefaultParameters();
	}

	public KDJStrategy(int kPeriod, int dPeriod, int overbought, int oversold) {
		super("KDJ随机指标策略");
		this.kPeriod = kPeriod;
		this.dPeriod = dPeriod;
		this.jPeriod = 3; // 通常J=3
		this.overbought = overbought;
		this.oversold = oversold;
		setDefaultParameters();
	}

	private void setDefaultParameters() {
		setParameter("kPeriod", kPeriod);
		setParameter("dPeriod", dPeriod);
		setParameter("jPeriod", jPeriod);
		setParameter("overbought", overbought);
		setParameter("oversold", oversold);
		setParameter("useCrossSignals", true);
	}

	@Override
	protected void init() {
		TradingLogger.logSignal(getName(), "SYSTEM", "INIT", 1.0,
				String.format("KDJ策略初始化: K=%d, D=%d, J=%d", kPeriod, dPeriod, jPeriod));

		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		lastK = null;
		lastD = null;
		lastJ = null;
		lastClose = null;
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

			// 计算KDJ指标
			KDJResult kdj = calculateKDJ();
			if (kdj != null) {
				lastK = kdj.k;
				lastD = kdj.d;
				lastJ = kdj.j;
			}

			lastClose = bar.getClose();

			// 生成交易信号
			SignalEvent signal = generateKDJSignal(bar, kdj);
			if (signal != null) {
				signals.add(signal);

				TradingLogger.logSignal(getName(), bar.getSymbol(), signal.getSignalType(), signal.getStrength(),
						String.format("K=%.1f, D=%.1f, J=%.1f", kdj.k, kdj.d, kdj.j));
			}

			if (debugMode && kdj != null) {
				TradingLogger.debug(getName(), "%s K=%.1f, D=%.1f, J=%.1f", bar.getTimestamp().toLocalDate(), kdj.k,
						kdj.d, kdj.j);
			}

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateSignals", e);
		}
	}

	/**
	 * KDJ计算结果
	 */
	public static class KDJResult {
		public final double k;
		public final double d;
		public final double j;

		public KDJResult(double k, double d, double j) {
			this.k = k;
			this.d = d;
			this.j = j;
		}
	}

	/**
	 * 计算KDJ指标
	 */
	private KDJResult calculateKDJ() {
		int requiredBars = kPeriod + dPeriod;
		if (highPrices.size() < requiredBars) {
			return null;
		}

		try {
			// 计算最近kPeriod周期的RSV
			double rsv = calculateRSV();

			// 计算K值（RSV的dPeriod周期SMA）
			double k = calculateKValue(rsv);

			// 计算D值（K值的dPeriod周期SMA）
			double d = calculateDValue(k);

			// 计算J值
			double j = 3 * k - 2 * d;

			return new KDJResult(k, d, j);

		} catch (Exception e) {
			TradingLogger.logSystemError(getName(), "calculateKDJ", e);
			return null;
		}
	}

	/**
	 * 计算RSV（未成熟随机值）
	 */
	private double calculateRSV() {
		int startIdx = highPrices.size() - kPeriod;
		int endIdx = highPrices.size();

		double highestHigh = highPrices.subList(startIdx, endIdx).stream().mapToDouble(Double::doubleValue).max()
				.orElse(0.0);
		double lowestLow = lowPrices.subList(startIdx, endIdx).stream().mapToDouble(Double::doubleValue).min()
				.orElse(0.0);
		double currentClose = closePrices.get(closePrices.size() - 1);

		if (highestHigh == lowestLow) {
			return 50.0; // 避免除零
		}

		return ((currentClose - lowestLow) / (highestHigh - lowestLow)) * 100;
	}

	/**
	 * 计算K值
	 */
	private double calculateKValue(double currentRSV) {
		// 简化实现：K = (前一日K值 * 2/3) + (当日RSV * 1/3)
		if (lastK == null) {
			return 50.0; // 初始值
		}
		return (lastK * 2.0 / 3.0) + (currentRSV * 1.0 / 3.0);
	}

	/**
	 * 计算D值
	 */
	private double calculateDValue(double currentK) {
		// 简化实现：D = (前一日D值 * 2/3) + (当日K值 * 1/3)
		if (lastD == null) {
			return 50.0; // 初始值
		}
		return (lastD * 2.0 / 3.0) + (currentK * 1.0 / 3.0);
	}

	/**
	 * 生成KDJ交易信号
	 */
	private SignalEvent generateKDJSignal(BarEvent bar, KDJResult kdj) {
		if (kdj == null) {
			return null;
		}

		boolean hasPosition = portfolio != null && portfolio.hasPosition(bar.getSymbol());

		// 金叉信号（K线上穿D线）
		if (!hasPosition && useCrossSignals) {
			if (isGoldenCross(kdj)) {
				double strength = calculateGoldenCrossStrength(kdj);
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", strength, "KDJ金叉");
			}
		}

		// 死叉信号（K线下穿D线）
		if (hasPosition && useCrossSignals) {
			if (isDeathCross(kdj)) {
				double strength = calculateDeathCrossStrength(kdj);
				return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", strength, "KDJ死叉");
			}
		}

		// 超买超卖信号
		SignalEvent overboughtOversoldSignal = generateOverboughtOversoldSignal(bar, kdj, hasPosition);
		if (overboughtOversoldSignal != null) {
			return overboughtOversoldSignal;
		}

		// J值极端信号
		SignalEvent jExtremeSignal = generateJExtremeSignal(bar, kdj, hasPosition);
		if (jExtremeSignal != null) {
			return jExtremeSignal;
		}

		return null;
	}

	/**
	 * 检查金叉
	 */
	private boolean isGoldenCross(KDJResult kdj) {
		// 需要前一日数据，简化实现
		return kdj.k > kdj.d && kdj.k < 30; // 低位金叉更可靠
	}

	/**
	 * 检查死叉
	 */
	private boolean isDeathCross(KDJResult kdj) {
		// 需要前一日数据，简化实现
		return kdj.k < kdj.d && kdj.k > 70; // 高位死叉更可靠
	}

	/**
	 * 生成超买超卖信号
	 */
	private SignalEvent generateOverboughtOversoldSignal(BarEvent bar, KDJResult kdj, boolean hasPosition) {
		// 超卖区域买入
		if (!hasPosition && kdj.k < oversold && kdj.d < oversold) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.6, "KDJ超卖");
		}

		// 超买区域卖出
		if (hasPosition && kdj.k > overbought && kdj.d > overbought) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.6, "KDJ超买");
		}

		return null;
	}

	/**
	 * 生成J值极端信号
	 */
	private SignalEvent generateJExtremeSignal(BarEvent bar, KDJResult kdj, boolean hasPosition) {
		// J值超过100强烈卖出
		if (hasPosition && kdj.j > 100) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "SELL", 0.8, "J值超买");
		}

		// J值低于0强烈买入
		if (!hasPosition && kdj.j < 0) {
			return new SignalEvent(bar.getTimestamp(), bar.getSymbol(), "BUY", 0.8, "J值超卖");
		}

		return null;
	}

	/**
	 * 计算金叉强度
	 */
	private double calculateGoldenCrossStrength(KDJResult kdj) {
		double kScore = 1.0 - (kdj.k / 100.0); // K值越低强度越高
		double spread = kdj.k - kdj.d;
		double spreadScore = Math.min(Math.abs(spread) / 10.0, 1.0);

		return Math.min((kScore * 0.6 + spreadScore * 0.4), 1.0);
	}

	/**
	 * 计算死叉强度
	 */
	private double calculateDeathCrossStrength(KDJResult kdj) {
		double kScore = kdj.k / 100.0; // K值越高强度越高
		double spread = kdj.d - kdj.k;
		double spreadScore = Math.min(Math.abs(spread) / 10.0, 1.0);

		return Math.min((kScore * 0.6 + spreadScore * 0.4), 1.0);
	}

	private void updatePriceData(double high, double low, double close) {
		highPrices.add(high);
		lowPrices.add(low);
		closePrices.add(close);

		// 限制数据大小
		int maxSize = (kPeriod + dPeriod) * 2;
		if (highPrices.size() > maxSize) {
			highPrices.remove(0);
			lowPrices.remove(0);
			closePrices.remove(0);
		}
	}

	private boolean hasEnoughData() {
		return highPrices.size() >= kPeriod + dPeriod;
	}

	@Override
	public void reset() {
		super.reset();
		highPrices.clear();
		lowPrices.clear();
		closePrices.clear();
		lastK = null;
		lastD = null;
		lastJ = null;
		lastClose = null;
	}

	// Getter和Setter方法
	public int getKPeriod() {
		return kPeriod;
	}

	public void setKPeriod(int kPeriod) {
		this.kPeriod = kPeriod;
		setParameter("kPeriod", kPeriod);
	}

	public int getDPeriod() {
		return dPeriod;
	}

	public void setDPeriod(int dPeriod) {
		this.dPeriod = dPeriod;
		setParameter("dPeriod", dPeriod);
	}

	public Double getLastK() {
		return lastK;
	}

	public Double getLastD() {
		return lastD;
	}

	public Double getLastJ() {
		return lastJ;
	}
}