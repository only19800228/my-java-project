
// 文件: com/Quantitative/web/dto/TradingResponse.java
package com.Quantitative.web.dto;

public class TradingResponse {
	private boolean success;
	private String message;
	private Object data;
	private long timestamp;

	public TradingResponse(boolean success, String message, Object data) {
		this.success = success;
		this.message = message;
		this.data = data;
		this.timestamp = System.currentTimeMillis();
	}

	public static TradingResponse success(String message) {
		return new TradingResponse(true, message, null);
	}

	public static TradingResponse success(String message, Object data) {
		return new TradingResponse(true, message, data);
	}

	public static TradingResponse error(String message) {
		return new TradingResponse(false, message, null);
	}

	// getter/setter
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}