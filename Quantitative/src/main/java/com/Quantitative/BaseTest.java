package com.Quantitative;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

import com.Quantitative.common.utils.LogUtils;

/**
 * 测试基类 - 带日志容错
 */
public abstract class BaseTest {
	protected final Logger logger;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public BaseTest() {
		// 安全的初始化logger
		this.logger = LogUtils.getLogger(getClass());
	}

	@Before
	public void setUp() {
		logger.info("开始执行测试: {}", this.getClass().getSimpleName());
	}

	/**
	 * 创建测试数据
	 */
	protected com.Quantitative.core.events.BarEvent createTestBarEvent(String symbol, double price) {
		return new com.Quantitative.core.events.BarEvent(java.time.LocalDateTime.now(), symbol, price, price + 0.1,
				price - 0.1, price, 1000000);
	}

	/**
	 * 断言不抛出异常
	 */
	protected void assertNoException(Executable executable) {
		try {
			executable.execute();
		} catch (Exception e) {
			fail("期望不抛出异常，但抛出了: " + e.getMessage());
		}
	}

	@FunctionalInterface
	protected interface Executable {
		void execute() throws Exception;
	}
}