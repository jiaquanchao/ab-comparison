package com.example.abtesting.domain.valueobject;

import java.util.*;

/**
 * 分配结果值对象
 * 
 * DDD设计：值对象，不可变
 */
public class AssignmentResult {
    private final String userId;
    private final String groupId;
    private final String comboKey;
    private final long latencyNanos;
    private final long timestamp;
    private final int[] tagIndexes;
    
    public AssignmentResult(String userId, String groupId, String comboKey, 
                           long latencyNanos, long timestamp, int[] tagIndexes) {
        this.userId = userId;
        this.groupId = groupId;
        this.comboKey = comboKey;
        this.latencyNanos = latencyNanos;
        this.timestamp = timestamp;
        this.tagIndexes = tagIndexes != null ? Arrays.copyOf(tagIndexes, tagIndexes.length) : null;
    }
    
    public String getUserId() { return userId; }
    public String getGroupId() { return groupId; }
    public String getComboKey() { return comboKey; }
    public long getLatencyNanos() { return latencyNanos; }
    public long getTimestamp() { return timestamp; }
    public int[] getTagIndexes() { 
        return tagIndexes != null ? Arrays.copyOf(tagIndexes, tagIndexes.length) : null; 
    }
    
    public double getLatencyMs() {
        return latencyNanos / 1_000_000.0;
    }
    
    @Override
    public String toString() {
        return String.format("AssignmentResult{userId='%s', groupId='%s', latency=%.2fms}", 
            userId, groupId, getLatencyMs());
    }
}
