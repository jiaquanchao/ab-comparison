package com.example.abtesting.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 标签维度定义
 * 包含维度名称、枚举值及其在原始客群中的比例
 */
public class TagDimension {
    private final String name;
    private final Map<String, Double> values; // 枚举值 -> 原始比例（0-100）
    
    public TagDimension(String name, Map<String, Double> values) {
        this.name = name;
        // 验证比例总和为100%
        double sum = values.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 100.0) > 0.01) {
            throw new IllegalArgumentException(name + " 的比例总和必须为100%，当前为: " + sum);
        }
        this.values = new LinkedHashMap<>(values);
    }
    
    public String getName() {
        return name;
    }
    
    public Map<String, Double> getValues() {
        return values;
    }
    
    /**
     * 根据随机值选择枚举值
     * @param random 0-1的随机数
     * @return 枚举值索引
     */
    public int selectValueIndex(double random) {
        double cumulative = 0;
        int index = 0;
        for (Double ratio : values.values()) {
            cumulative += ratio / 100.0;
            if (random < cumulative) {
                return index;
            }
            index++;
        }
        return values.size() - 1;
    }
    
    /**
     * 获取枚举值名称列表
     */
    public String[] getValueNames() {
        return values.keySet().toArray(new String[0]);
    }
    
    /**
     * 根据索引获取枚举值名称
     */
    public String getValueName(int index) {
        int i = 0;
        for (String name : values.keySet()) {
            if (i == index) return name;
            i++;
        }
        return null;
    }
    
    /**
     * 根据索引获取枚举值比例
     */
    public double getValueRatio(int index) {
        int i = 0;
        for (Double ratio : values.values()) {
            if (i == index) return ratio;
            i++;
        }
        return 0;
    }
}
