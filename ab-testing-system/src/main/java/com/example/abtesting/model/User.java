package com.example.abtesting.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 用户模型
 * 5个标签，每个标签10个枚举值（0-9）
 */
public class User {
    private final String userId;
    private final int[] tags;

    public User(String userId, int[] tags) {
        this.userId = userId;
        this.tags = tags;
    }

    public String userId() {
        return userId;
    }

    public int[] tags() {
        return tags;
    }

    /**
     * 获取标签组合键，如 "1_3_5_2_7"
     */
    public String getComboKey() {
        return Arrays.stream(tags)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining("_"));
    }

    /**
     * 获取指定维度的标签值
     */
    public int getTag(int dimension) {
        if (dimension < 0 || dimension >= tags.length) {
            throw new IllegalArgumentException("Invalid dimension: " + dimension);
        }
        return tags[dimension];
    }
}
