package com.example.abtesting.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 压测报告
 */
public class TestReport {
    private String reportId;
    private long startTime;
    private long endTime;
    private int targetQps;
    private int duration;
    private String mode; // "single" or "comparison"
    
    // 指标历史（每秒一个数据点）
    private Map<String, Map<Integer, MetricSnapshot>> metricsHistory = new HashMap<>();
    
    // 汇总指标
    private Map<String, SummaryMetrics> summaryMetrics = new HashMap<>();
    
    public static class SummaryMetrics {
        public long totalRequests;
        public double avgQps;
        public double avgP99;
        public double avgMSE;
        public double finalA_Ratio;
        
        public SummaryMetrics() {}
        
        public SummaryMetrics(long totalRequests, double avgQps, double avgP99, double avgMSE, double finalA_Ratio) {
            this.totalRequests = totalRequests;
            this.avgQps = avgQps;
            this.avgP99 = avgP99;
            this.avgMSE = avgMSE;
            this.finalA_Ratio = finalA_Ratio;
        }
    }
    
    public TestReport() {
        this.reportId = "report_" + System.currentTimeMillis();
    }
    
    public TestReport(long startTime, int targetQps, int duration, String mode) {
        this.reportId = "report_" + startTime;
        this.startTime = startTime;
        this.targetQps = targetQps;
        this.duration = duration;
        this.mode = mode;
    }
    
    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    
    public int getTargetQps() { return targetQps; }
    public void setTargetQps(int targetQps) { this.targetQps = targetQps; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    
    public Map<String, Map<Integer, MetricSnapshot>> getMetricsHistory() { return metricsHistory; }
    public void setMetricsHistory(Map<String, Map<Integer, MetricSnapshot>> metricsHistory) { 
        this.metricsHistory = metricsHistory; 
    }
    
    public Map<String, SummaryMetrics> getSummaryMetrics() { return summaryMetrics; }
    public void setSummaryMetrics(Map<String, SummaryMetrics> summaryMetrics) { 
        this.summaryMetrics = summaryMetrics; 
    }
}
