package com.example.abtesting.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 指标快照，每100ms推送一次
 */
public class MetricSnapshot {
    private long timestamp;
    private long totalRequests;
    private double currentQps;
    private double p50LatencyMs;
    private double p99LatencyMs;
    private double maxLatencyMs;
    private double mse;
    private Map<String, Double> groupRatios;
    
    // 标签分布：dimension -> group -> tagValue -> ratio
    private Map<Integer, Map<String, Map<Integer, Double>>> tagDistributions;
    
    // 方案名称
    private String engineName;
    
    // AdaptiveHash 当前阈值（仅 AdaptiveHash 方案有值）
    private Double adaptiveHashThreshold;

    public MetricSnapshot() {}

    public MetricSnapshot(long timestamp, long totalRequests, double currentQps,
                          double p50LatencyMs, double p99LatencyMs, double maxLatencyMs,
                          double mse, Map<String, Double> groupRatios,
                          Map<Integer, Map<String, Map<Integer, Double>>> tagDistributions, String engineName) {
        this(timestamp, totalRequests, currentQps, p50LatencyMs, p99LatencyMs, maxLatencyMs,
             mse, groupRatios, tagDistributions, engineName, null);
    }
    
    public MetricSnapshot(long timestamp, long totalRequests, double currentQps,
                          double p50LatencyMs, double p99LatencyMs, double maxLatencyMs,
                          double mse, Map<String, Double> groupRatios,
                          Map<Integer, Map<String, Map<Integer, Double>>> tagDistributions, 
                          String engineName, Double adaptiveHashThreshold) {
        this.timestamp = timestamp;
        this.totalRequests = totalRequests;
        this.currentQps = currentQps;
        this.p50LatencyMs = p50LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
        this.maxLatencyMs = maxLatencyMs;
        this.mse = mse;
        this.groupRatios = groupRatios != null ? groupRatios : new HashMap<>();
        this.tagDistributions = tagDistributions != null ? tagDistributions : new HashMap<>();
        this.engineName = engineName;
        this.adaptiveHashThreshold = adaptiveHashThreshold;
    }

    // Getters
    public long getTimestamp() { return timestamp; }
    public long getTotalRequests() { return totalRequests; }
    public double getCurrentQps() { return currentQps; }
    public double getP50LatencyMs() { return p50LatencyMs; }
    public double getP99LatencyMs() { return p99LatencyMs; }
    public double getMaxLatencyMs() { return maxLatencyMs; }
    public double getMse() { return mse; }
    public Map<String, Double> getGroupRatios() { return groupRatios; }
    public Map<Integer, Map<String, Map<Integer, Double>>> getTagDistributions() { return tagDistributions; }
    public String getEngineName() { return engineName; }
    public Double getAdaptiveHashThreshold() { return adaptiveHashThreshold; }

    // Setters
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public void setCurrentQps(double currentQps) { this.currentQps = currentQps; }
    public void setP50LatencyMs(double p50LatencyMs) { this.p50LatencyMs = p50LatencyMs; }
    public void setP99LatencyMs(double p99LatencyMs) { this.p99LatencyMs = p99LatencyMs; }
    public void setMaxLatencyMs(double maxLatencyMs) { this.maxLatencyMs = maxLatencyMs; }
    public void setMse(double mse) { this.mse = mse; }
    public void setGroupRatios(Map<String, Double> groupRatios) { this.groupRatios = groupRatios; }
    public void setTagDistributions(Map<Integer, Map<String, Map<Integer, Double>>> tagDistributions) { 
        this.tagDistributions = tagDistributions; 
    }
    public void setEngineName(String engineName) { this.engineName = engineName; }
    public void setAdaptiveHashThreshold(Double adaptiveHashThreshold) { 
        this.adaptiveHashThreshold = adaptiveHashThreshold; 
    }
}
