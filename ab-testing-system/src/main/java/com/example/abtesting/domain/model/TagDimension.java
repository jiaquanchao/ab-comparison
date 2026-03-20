package com.example.abtesting.domain.model;

import java.util.*;

/**
 * 标签维度值对象
 * 
 * DDD设计：值对象，不可变
 */
public class TagDimension {
    private final String name;
    private final LinkedHashMap<String, Double> values; // 枚举值 -> 比例（百分比）
    private final List<String> valueNames;
    
    public TagDimension(String name, LinkedHashMap<String, Double> values) {
        this.name = name;
        this.values = new LinkedHashMap<>(values);
        this.valueNames = new ArrayList<>(values.keySet());
        
        validate();
    }
    
    private void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("维度名称不能为空");
        }
        
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("维度至少需要一个枚举值");
        }
        
        // 验证比例非负
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            if (entry.getValue() < 0 || entry.getValue() > 100) {
                throw new IllegalArgumentException(
                    String.format("维度'%s'的枚举值'%s'的比例必须在0-100之间，当前为: %.2f", 
                        name, entry.getKey(), entry.getValue())
                );
            }
        }
    }
    
    public String getName() { return name; }
    public LinkedHashMap<String, Double> getValues() { return new LinkedHashMap<>(values); }
    public int getValueCount() { return values.size(); }
    public List<String> getValueNames() { return new ArrayList<>(valueNames); }
    
    /**
     * 根据索引获取枚举值名称
     */
    public String getValueName(int index) {
        if (index < 0 || index >= valueNames.size()) {
            throw new IndexOutOfBoundsException(
                String.format("维度'%s'的索引超出范围[0, %d): %d", name, valueNames.size(), index)
            );
        }
        return valueNames.get(index);
    }
    
    /**
     * 根据索引获取枚举值比例（百分比）
     */
    public double getValueRatio(int index) {
        if (index < 0 || index >= valueNames.size()) {
            throw new IndexOutOfBoundsException(
                String.format("维度'%s'的索引超出范围[0, %d): %d", name, valueNames.size(), index)
            );
        }
        String valueName = valueNames.get(index);
        return values.get(valueName);
    }
    
    /**
     * 根据随机值选择枚举值索引
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
    
    @Override
    public String toString() {
        return String.format("TagDimension{name='%s', values=%s}", name, values);
    }
}
