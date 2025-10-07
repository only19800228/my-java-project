package com.Quantitative.data.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期时间工具类
 */
public class DateTimeUtils {
    
    // 常见日期格式
    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    // 默认时间（股票收盘时间）
    public static final LocalTime DEFAULT_MARKET_CLOSE_TIME = LocalTime.of(15, 0);
    public static final LocalTime DEFAULT_MARKET_OPEN_TIME = LocalTime.of(9, 30);
    
    /**
     * 解析日期字符串为LocalDateTime
     */
    public static LocalDateTime parseToDateTime(String dateStr, String pattern) {
        return parseToDateTime(dateStr, pattern, DEFAULT_MARKET_CLOSE_TIME);
    }
    
    /**
     * 解析日期字符串为LocalDateTime（指定时间）
     */
    public static LocalDateTime parseToDateTime(String dateStr, String pattern, LocalTime time) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDate date = LocalDate.parse(dateStr, formatter);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("无法解析日期: " + dateStr + " 格式: " + pattern, e);
        }
    }
    
    /**
     * 自动检测并解析日期
     */
    public static LocalDateTime autoParseDateTime(String dateStr) {
        return autoParseDateTime(dateStr, DEFAULT_MARKET_CLOSE_TIME);
    }
    
    /**
     * 自动检测并解析日期（指定时间）
     */
    public static LocalDateTime autoParseDateTime(String dateStr, LocalTime time) {
        // 尝试常见格式
        String[] patterns = {
            DATE_FORMAT_YYYY_MM_DD,
            DATE_FORMAT_YYYYMMDD,
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy"
        };
        
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return LocalDateTime.of(date, time);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        
        throw new IllegalArgumentException("无法自动解析日期: " + dateStr);
    }
    
    /**
     * 格式化LocalDateTime为字符串
     */
    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }
}