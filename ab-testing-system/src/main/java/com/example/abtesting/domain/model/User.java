package com.example.abtesting.domain.model;

import java.util.*;

/**
 * 用户实体
 * 
 * DDD设计：用户是实体，有唯一标识
 */
public class User {
    private final String userId;
    private final int[] tagIndexes; // 标签索引数组
    private final TagSchema tagSchema;
    
    public User(String userId, int[] tagIndexes, TagSchema tagSchema) {
        this.userId = userId;
        this.tagIndexes = Arrays.copyOf(tagIndexes, tagIndexes.length);
        this.tagSchema = tagSchema;
        
        validate();
    }
    
    private void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (tagIndexes == null || tagIndexes.length != tagSchema.getDimensionCount()) {
            throw new IllegalArgumentException(
                String.format("标签数量必须为%d，当前为: %d", 
                    tagSchema.getDimensionCount(), 
                    tagIndexes != null ? tagIndexes.length : 0)
            );
        }
        
        // 验证每个标签索引在有效范围内
        for (int i = 0; i < tagIndexes.length; i++) {
            TagDimension dim = tagSchema.getDimension(i);
            if (tagIndexes[i] < 0 || tagIndexes[i] >= dim.getValueCount()) {
                throw new IllegalArgumentException(
                    String.format("维度'%s'的索引%d超出范围[0, %d)", 
                        dim.getName(), tagIndexes[i], dim.getValueCount())
                );
            }
        }
    }
    
    public String getUserId() { return userId; }
    public int[] getTagIndexes() { return Arrays.copyOf(tagIndexes, tagIndexes.length); }
    public TagSchema getTagSchema() { return tagSchema; }
    
    /**
     * 获取组合键
     */
    public String getComboKey() {
        return tagSchema.generateComboKey(tagIndexes);
    }
    
    /**
     * 获取组合描述
     */
    public String getComboDescription() {
        return tagSchema.generateComboDescription(tagIndexes);
    }
    
    @Override
    public String toString() {
        return String.format("User{userId='%s', comboKey='%s'}", userId, getComboKey());
    }
}
