package com.example.abtesting.domain.model;

import java.util.*;

/**
 * 标签维度定义
 * 
 * DDD设计：标签维度是值对象，不可变
 */
public class TagSchema {
    private final List<TagDimension> dimensions;
    private final int totalCombinations;
    
    public TagSchema(List<TagDimension> dimensions) {
        this.dimensions = Collections.unmodifiableList(new ArrayList<>(dimensions));
        this.totalCombinations = calculateTotalCombinations();
        
        validate();
    }
    
    private void validate() {
        if (dimensions == null || dimensions.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个标签维度");
        }
        
        // 验证每个维度的比例总和为100%
        for (TagDimension dim : dimensions) {
            double sum = dim.getValues().values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 100.0) > 0.01) {
                throw new IllegalArgumentException(
                    String.format("维度'%s'的比例总和必须为100%%，当前为: %.2f%%", dim.getName(), sum)
                );
            }
        }
    }
    
    private int calculateTotalCombinations() {
        return dimensions.stream()
            .mapToInt(d -> d.getValues().size())
            .reduce(1, (a, b) -> a * b);
    }
    
    public List<TagDimension> getDimensions() { return dimensions; }
    public int getDimensionCount() { return dimensions.size(); }
    public int getTotalCombinations() { return totalCombinations; }
    
    public TagDimension getDimension(int index) {
        return dimensions.get(index);
    }
    
    /**
     * 生成组合键
     */
    public String generateComboKey(int[] tagIndexes) {
        if (tagIndexes.length != dimensions.size()) {
            throw new IllegalArgumentException("标签数量必须为" + dimensions.size());
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tagIndexes.length; i++) {
            if (i > 0) sb.append("_");
            sb.append(tagIndexes[i]);
        }
        return sb.toString();
    }
    
    /**
     * 生成组合描述
     */
    public String generateComboDescription(int[] tagIndexes) {
        if (tagIndexes.length != dimensions.size()) {
            throw new IllegalArgumentException("标签数量必须为" + dimensions.size());
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tagIndexes.length; i++) {
            if (i > 0) sb.append(", ");
            TagDimension dim = dimensions.get(i);
            sb.append(dim.getName()).append("=").append(dim.getValueName(tagIndexes[i]));
        }
        return sb.toString();
    }
}

