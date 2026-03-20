package com.example.abtesting.infrastructure.service;

import com.example.abtesting.domain.model.*;
import com.example.abtesting.domain.service.AssignmentService;
import com.example.abtesting.domain.valueobject.AssignmentResult;
import com.example.abtesting.controller.AssignmentEngine;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 贪心分配服务
 * 
 * 策略：为每个标签组合维护4个计数器，每次选择与目标偏差最大的分组
 */
@Service
public class GreedyAssignmentService implements AssignmentService, AssignmentEngine {
    
    // 组合键 -> [countA, countB, countC, countD]
    private final Map<String, long[]> comboCounters = new ConcurrentHashMap<>();
    
    @Override
    public AssignmentResult assign(User user, Experiment experiment) {
        long startTime = System.nanoTime();
        
        String comboKey = user.getComboKey();
        
        // 获取或创建计数器
        long[] counters = comboCounters.computeIfAbsent(comboKey, k -> new long[4]);
        
        // 计算每个分组的偏差
        int bestGroup = 0;
        double maxDeviation = Double.NEGATIVE_INFINITY;
        
        long total = counters[0] + counters[1] + counters[2] + counters[3];
        
        for (int i = 0; i < 4; i++) {
            Group group = experiment.getGroup(i);
            double targetRatio = group.getTargetRatio();
            
            double actualRatio = total == 0 ? 0 : (double) counters[i] / total;
            double deviation = targetRatio - actualRatio;
            
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
                bestGroup = i;
            }
        }
        
        // 更新计数器
        counters[bestGroup]++;
        
        long endTime = System.nanoTime();
        
        return new AssignmentResult(
            user.getUserId(),
            experiment.getGroup(bestGroup).getName(),
            comboKey,
            endTime - startTime,
            System.currentTimeMillis(),
            user.getTagIndexes()
        );
    }
    
    @Override
    public String getName() {
        return "Greedy";
    }
    
    @Override
    public void reset() {
        comboCounters.clear();
    }
    
    /**
     * 获取组合计数器（用于统计）
     */
    public Map<String, long[]> getComboCounters() {
        return new ConcurrentHashMap<>(comboCounters);
    }
}
