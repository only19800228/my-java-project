package com.Quantitative.data.factory;

import com.Quantitative.data.DataSource;
import com.Quantitative.data.adapter.AKShareDataSourceAdapter;
import com.Quantitative.data.csv.CSVDataSource;

/**
 * 数据源工厂 - 创建和管理数据源实例
 */
public class DataSourceFactory {
    
    /**
     * 创建AKShare数据源
     */
    public static DataSource createAKShareDataSource() {
        return new AKShareDataSourceAdapter();
    }
    
    /**
     * 创建CSV数据源
     */
    public static DataSource createCSVDataSource() {
        return new CSVDataSource();
    }
    
    /**
     * 创建CSV数据源（指定目录）
     */
    public static DataSource createCSVDataSource(String dataDirectory) {
        return new CSVDataSource(dataDirectory);
    }
    
    /**
     * 创建内存数据源（用于测试）
     */
    public static DataSource createMemoryDataSource() {
        // 可以后续实现一个内存数据源
        return null;
    }
}