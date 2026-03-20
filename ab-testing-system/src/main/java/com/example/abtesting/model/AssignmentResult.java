package com.example.abtesting.model;

/**
 * 分流结果
 */
public class AssignmentResult {
    private final String userId;
    private final String group;          // "A" or "B"
    private final String comboKey;       // 标签组合键
    private final long latencyNanos;     // 处理延迟（纳秒）
    private final long timestamp;        // 时间戳
    private final int[] tags;            // 用户标签（用于统计标签分布）

    public AssignmentResult(String userId, String group, String comboKey, long latencyNanos, long timestamp) {
        this(userId, group, comboKey, latencyNanos, timestamp, null);
    }

    public AssignmentResult(String userId, String group, String comboKey, long latencyNanos, long timestamp, int[] tags) {
        this.userId = userId;
        this.group = group;
        this.comboKey = comboKey;
        this.latencyNanos = latencyNanos;
        this.timestamp = timestamp;
        this.tags = tags;
    }

    public String userId() {
        return userId;
    }

    public String group() {
        return group;
    }

    public String comboKey() {
        return comboKey;
    }

    public long latencyNanos() {
        return latencyNanos;
    }

    public long timestamp() {
        return timestamp;
    }

    public int[] tags() {
        return tags;
    }
}
