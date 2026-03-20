package com.example.abtesting.application.service;

import com.example.abtesting.domain.model.*;
import com.example.abtesting.domain.valueobject.AssignmentResult;
import com.example.abtesting.infrastructure.service.GreedyAssignmentService;
import com.example.abtesting.infrastructure.service.AdaptiveHashAssignmentService;
import com.example.abtesting.infrastructure.service.HybridAssignmentService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 增强版指标收集服务（支持3种算法）
 */
@Service
public class EnhancedMetricsService {
    
    private final GreedyAssignmentService greedyService;
    private final AdaptiveHashAssignmentService hashService;
    private final HybridAssignmentService hybridService;
    
    // 全局统计
    private final AtomicLong[] greedyCounts = new AtomicLong[4];
    private final AtomicLong[] hashCounts = new AtomicLong[4];
    private final AtomicLong[] hybridCounts = new AtomicLong[4];
    
    // 延迟统计（微秒）
    private final List<Long> greedyLatencies = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> hashLatencies = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> hybridLatencies = Collections.synchronizedList(new ArrayList<>());
    
    // 用户一致性统计
    private final Map<String, String> greedyUserGroupMapping = new ConcurrentHashMap<>();
    private final Map<String, String> hashUserGroupMapping = new ConcurrentHashMap<>();
    private final Map<String, String> hybridUserGroupMapping = new ConcurrentHashMap<>();
    
    private final AtomicLong greedyConsistentCount = new AtomicLong(0);
    private final AtomicLong greedyInconsistentCount = new AtomicLong(0);
    private final AtomicLong hashConsistentCount = new AtomicLong(0);
    private final AtomicLong hashInconsistentCount = new AtomicLong(0);
    private final AtomicLong hybridConsistentCount = new AtomicLong(0);
    private final AtomicLong hybridInconsistentCount = new AtomicLong(0);
    
    public EnhancedMetricsService(GreedyAssignmentService greedyService, 
                                 AdaptiveHashAssignmentService hashService,
                                 HybridAssignmentService hybridService) {
        this.greedyService = greedyService;
        this.hashService = hashService;
        this.hybridService = hybridService;
        
        for (int i = 0; i < 4; i++) {
            greedyCounts[i] = new AtomicLong(0);
            hashCounts[i] = new AtomicLong(0);
            hybridCounts[i] = new AtomicLong(0);
        }
    }
    
    public void recordGreedyResult(AssignmentResult result) {
        int groupIndex = getGroupIndex(result.getGroupId());
        greedyCounts[groupIndex].incrementAndGet();
        greedyLatencies.add(result.getLatencyNanos() / 1000);
        recordConsistency(result, greedyUserGroupMapping, greedyConsistentCount, greedyInconsistentCount);
    }
    
    public void recordHashResult(AssignmentResult result) {
        int groupIndex = getGroupIndex(result.getGroupId());
        hashCounts[groupIndex].incrementAndGet();
        hashLatencies.add(result.getLatencyNanos() / 1000);
        recordConsistency(result, hashUserGroupMapping, hashConsistentCount, hashInconsistentCount);
    }
    
    public void recordHybridResult(AssignmentResult result) {
        int groupIndex = getGroupIndex(result.getGroupId());
        hybridCounts[groupIndex].incrementAndGet();
        hybridLatencies.add(result.getLatencyNanos() / 1000);
        recordConsistency(result, hybridUserGroupMapping, hybridConsistentCount, hybridInconsistentCount);
    }
    
    private void recordConsistency(AssignmentResult result, Map<String, String> mapping, 
                                   AtomicLong consistent, AtomicLong inconsistent) {
        String userId = result.getUserId();
        String groupId = result.getGroupId();
        
        String previousGroup = mapping.putIfAbsent(userId, groupId);
        if (previousGroup != null) {
            if (previousGroup.equals(groupId)) {
                consistent.incrementAndGet();
            } else {
                inconsistent.incrementAndGet();
            }
        }
    }
    
    private int getGroupIndex(String groupName) {
        switch (groupName) {
            case "A": return 0;
            case "B": return 1;
            case "C": return 2;
            case "D": return 3;
            default: return 0;
        }
    }

    private Map<String, Double> calculatePercentiles(List<Long> latencies) {
        if (latencies.isEmpty()) {
            Map<String, Double> result = new HashMap<>();
            result.put("p50", 0.0);
            result.put("p90", 0.0);
            result.put("p95", 0.0);
            result.put("p99", 0.0);
            result.put("p999", 0.0);
            result.put("avg", 0.0);
            result.put("max", 0.0);
            return result;
        }

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        Map<String, Double> result = new HashMap<>();
        result.put("p50", getPercentile(sorted, 50) / 1000.0);
        result.put("p90", getPercentile(sorted, 90) / 1000.0);
        result.put("p95", getPercentile(sorted, 95) / 1000.0);
        result.put("p99", getPercentile(sorted, 99) / 1000.0);
        result.put("p999", getPercentile(sorted, 99.9) / 1000.0);
        result.put("avg", sorted.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0);
        result.put("max", sorted.get(sorted.size() - 1) / 1000.0);
        return result;
    }
    
    private double getPercentile(List<Long> sorted, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
    
    public Map<String, Object> getEnhancedMetrics(Experiment experiment) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        metrics.put("greedy", getEngineMetrics("Greedy", greedyCounts, greedyLatencies, 
                                               greedyConsistentCount, greedyInconsistentCount, experiment));
        
        metrics.put("adaptiveHash", getEngineMetrics("AdaptiveHash", hashCounts, hashLatencies, 
                                                     hashConsistentCount, hashInconsistentCount, experiment));
        
        metrics.put("hybrid", getEngineMetrics("Hybrid", hybridCounts, hybridLatencies, 
                                              hybridConsistentCount, hybridInconsistentCount, experiment));
        
        metrics.put("comparison", compareAlgorithms(metrics));
        
        return metrics;
    }
    
    private Map<String, Object> getEngineMetrics(String engineName, AtomicLong[] counts,
                                                 List<Long> latencies,
                                                 AtomicLong consistentCount,
                                                 AtomicLong inconsistentCount,
                                                 Experiment experiment) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        long total = Arrays.stream(counts).mapToLong(AtomicLong::get).sum();
        metrics.put("engine", engineName);
        metrics.put("totalRequests", total);
        
        if (total == 0) {
            Map<String, Object> emptyLatency = new HashMap<>();
            emptyLatency.put("p50", 0.0);
            emptyLatency.put("p90", 0.0);
            emptyLatency.put("p95", 0.0);
            emptyLatency.put("p99", 0.0);
            emptyLatency.put("p999", 0.0);
            emptyLatency.put("avg", 0.0);
            emptyLatency.put("max", 0.0);
            metrics.put("latency", emptyLatency);

            metrics.put("groups", Collections.emptyMap());
            metrics.put("mse", 0.0);

            Map<String, Object> emptyConsistency = new HashMap<>();
            emptyConsistency.put("totalRevisits", 0);
            emptyConsistency.put("consistentCount", 0);
            emptyConsistency.put("inconsistentCount", 0);
            emptyConsistency.put("consistencyRate", 100.0);
            metrics.put("userConsistency", emptyConsistency);

            return metrics;
        }
        
        metrics.put("latency", calculatePercentiles(latencies));
        
        Map<String, Object> groupStats = new LinkedHashMap<>();
        double mse = 0.0;
        
        for (int i = 0; i < 4; i++) {
            Group group = experiment.getGroup(i);
            long count = counts[i].get();
            double actualRatio = (double) count / total;
            double targetRatio = group.getTargetRatio();
            double deviation = actualRatio - targetRatio;
            
            mse += deviation * deviation;
            
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("count", count);
            groupData.put("actualPercentage", actualRatio * 100);
            groupData.put("targetPercentage", targetRatio * 100);
            groupData.put("deviation", deviation * 100);
            groupStats.put(group.getName(), groupData);
        }
        
        metrics.put("groups", groupStats);
        metrics.put("mse", mse);
        
        long totalRevisits = consistentCount.get() + inconsistentCount.get();
        double consistencyRate = totalRevisits > 0 
            ? (consistentCount.get() * 100.0) / totalRevisits 
            : 100.0;
        
        Map<String, Object> consistency = new HashMap<>();
        consistency.put("totalRevisits", totalRevisits);
        consistency.put("consistentCount", consistentCount.get());
        consistency.put("inconsistentCount", inconsistentCount.get());
        consistency.put("consistencyRate", consistencyRate);
        metrics.put("userConsistency", consistency);
        
        return metrics;
    }
    
    private Map<String, Object> compareAlgorithms(Map<String, Object> metrics) {
        Map<String, Object> comparison = new LinkedHashMap<>();
        
        Map<String, Object> greedy = (Map<String, Object>) metrics.get("greedy");
        Map<String, Object> hash = (Map<String, Object>) metrics.get("adaptiveHash");
        Map<String, Object> hybrid = (Map<String, Object>) metrics.get("hybrid");
        
        Map<String, Object> accuracy = new HashMap<>();
        accuracy.put("greedyMSE", greedy.get("mse"));
        accuracy.put("hashMSE", hash.get("mse"));
        accuracy.put("hybridMSE", hybrid.get("mse"));
        accuracy.put("bestAccuracy", findBestAccuracy(greedy, hash, hybrid));
        comparison.put("accuracy", accuracy);
        
        Map<String, Object> greedyLatency = (Map<String, Object>) greedy.get("latency");
        Map<String, Object> hashLatency = (Map<String, Object>) hash.get("latency");
        Map<String, Object> hybridLatency = (Map<String, Object>) hybrid.get("latency");
        
        Map<String, Object> performance = new HashMap<>();
        performance.put("greedyP999", greedyLatency.get("p999"));
        performance.put("hashP999", hashLatency.get("p999"));
        performance.put("hybridP999", hybridLatency.get("p999"));
        performance.put("bestPerformance", findBestPerformance(greedyLatency, hashLatency, hybridLatency));
        comparison.put("performance", performance);
        
        Map<String, Object> greedyConsistency = (Map<String, Object>) greedy.get("userConsistency");
        Map<String, Object> hashConsistency = (Map<String, Object>) hash.get("userConsistency");
        Map<String, Object> hybridConsistency = (Map<String, Object>) hybrid.get("userConsistency");
        
        Map<String, Object> consistency = new HashMap<>();
        consistency.put("greedy", greedyConsistency.get("consistencyRate"));
        consistency.put("hash", hashConsistency.get("consistencyRate"));
        consistency.put("hybrid", hybridConsistency.get("consistencyRate"));
        consistency.put("bestConsistency", findBestConsistency(greedyConsistency, hashConsistency, hybridConsistency));
        comparison.put("consistency", consistency);
        
        double greedyScore = calculateScore(greedy);
        double hashScore = calculateScore(hash);
        double hybridScore = calculateScore(hybrid);
        
        Map<String, Object> overall = new HashMap<>();
        overall.put("greedyScore", greedyScore);
        overall.put("hashScore", hashScore);
        overall.put("hybridScore", hybridScore);
        overall.put("recommendation", getRecommendation(greedyScore, hashScore, hybridScore));
        comparison.put("overall", overall);
        
        return comparison;
    }
    
    private String findBestAccuracy(Map<String, Object> greedy, Map<String, Object> hash, 
                                   Map<String, Object> hybrid) {
        double greedyMSE = (double) greedy.get("mse");
        double hashMSE = (double) hash.get("mse");
        double hybridMSE = (double) hybrid.get("mse");
        
        if (hybridMSE <= greedyMSE && hybridMSE <= hashMSE) return "Hybrid";
        if (greedyMSE <= hashMSE) return "Greedy";
        return "AdaptiveHash";
    }
    
    private String findBestPerformance(Map<String, Object> greedy, Map<String, Object> hash, 
                                      Map<String, Object> hybrid) {
        double greedyP999 = (double) greedy.get("p999");
        double hashP999 = (double) hash.get("p999");
        double hybridP999 = (double) hybrid.get("p999");
        
        if (hybridP999 <= greedyP999 && hybridP999 <= hashP999) return "Hybrid";
        if (hashP999 <= greedyP999) return "AdaptiveHash";
        return "Greedy";
    }
    
    private String findBestConsistency(Map<String, Object> greedy, Map<String, Object> hash, 
                                      Map<String, Object> hybrid) {
        double greedyConsistency = (double) greedy.get("consistencyRate");
        double hashConsistency = (double) hash.get("consistencyRate");
        double hybridConsistency = (double) hybrid.get("consistencyRate");
        
        if (hybridConsistency >= greedyConsistency && hybridConsistency >= hashConsistency) return "Hybrid";
        if (hashConsistency >= greedyConsistency) return "AdaptiveHash";
        return "Greedy";
    }
    
    private double calculateScore(Map<String, Object> metrics) {
        double mse = (double) metrics.get("mse");
        Map<String, Object> latency = (Map<String, Object>) metrics.get("latency");
        double p999 = (double) latency.get("p999");
        Map<String, Object> consistency = (Map<String, Object>) metrics.get("userConsistency");
        double consistencyRate = (double) consistency.get("consistencyRate");
        
        double accuracyScore = Math.max(0, (1 - mse * 100)) * 30;
        double performanceScore = Math.max(0, (1 - p999 / 100)) * 30;
        double consistencyScore = (consistencyRate / 100) * 40;
        
        return accuracyScore + performanceScore + consistencyScore;
    }
    
    private String getRecommendation(double greedyScore, double hashScore, double hybridScore) {
        if (hybridScore >= hashScore && hybridScore >= greedyScore) {
            return "Hybrid（强烈推荐）";
        } else if (hashScore >= greedyScore) {
            return "AdaptiveHash（推荐）";
        } else {
            return "Greedy（不推荐）";
        }
    }
    
    public void reset() {
        for (int i = 0; i < 4; i++) {
            greedyCounts[i].set(0);
            hashCounts[i].set(0);
            hybridCounts[i].set(0);
        }
        
        greedyLatencies.clear();
        hashLatencies.clear();
        hybridLatencies.clear();
        
        greedyUserGroupMapping.clear();
        hashUserGroupMapping.clear();
        hybridUserGroupMapping.clear();
        
        greedyConsistentCount.set(0);
        greedyInconsistentCount.set(0);
        hashConsistentCount.set(0);
        hashInconsistentCount.set(0);
        hybridConsistentCount.set(0);
        hybridInconsistentCount.set(0);
        
        greedyService.reset();
        hashService.reset();
        hybridService.reset();
    }
}
