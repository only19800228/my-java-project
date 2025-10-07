// 新增：异常类型
package com.Quantitative.core.exceptions;

/**
 * 关键交易异常
 */
public class CriticalTradingException extends RuntimeException {
    public CriticalTradingException(String message) {
        super(message);
    }
    
    public CriticalTradingException(String message, Throwable cause) {
        super(message, cause);
    }
}