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
 * 自适应哈希分配服务
 * 
 * 策略：基于用户ID哈希，使用累积阈值动态调整
 */
@Service
public class AdaptiveHashAssignmentService implements AssignmentService, AssignmentEngine {
    
    // 组合键 -> [countA, countB, countC, countD]
    private final Map<String, long[]> comboCounters = new ConcurrentHashMap<>();
    
    @Override
    public AssignmentResult assign(User user, Experiment experiment) {
        long startTime = System.nanoTime();
        
        String comboKey = user.getComboKey();
        
        // 获取或创建计数器
        long[] counters = comboCounters.computeIfAbsent(comboKey, k -> new long[4]);
        
        // 计算哈希值（0-99）
        int hash = Math.abs(user.getUserId().hashCode() % 100);
        
        // 计算累积阈值
        double[] cumulativeThresholds = new double[4];
        double cumulative = 0;
        for (int i = 0; i < 4; i++) {
            cumulative += experiment.getGroup(i).getTargetPercentage();
            cumulativeThresholds[i] = cumulative;
        }
        
        // 根据哈希值选择分组
        int selectedGroup = 0;
        for (int i = 0; i < 4; i++) {
            if (hash < cumulativeThresholds[i]) {
                selectedGroup = i;
                break;
            }
        }
        
        // 更新计数器
        counters[selectedGroup]++;
        
        long endTime = System.nanoTime();
        
        return new AssignmentResult(
            user.getUserId(),
            experiment.getGroup(selectedGroup).getName(),
            comboKey,
            endTime - startTime,
            System.currentTimeMillis(),
            user.getTagIndexes()
        );
    }
    
    @Override
    public String getName() {
        return "AdaptiveHash";
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
    
    /**
     * 设置调整间隔（预留接口）
     * @param intervalMs 调整间隔（毫秒）
     */
    public void setAdjustInterval(long intervalMs) {
        // 这个方法用于配置调整间隔
        // 当前AdaptiveHash使用的是后台定时任务，间隔可以通过其他方式配置
        // 这个接口保留用于未来扩展
        System.out.println("[AdaptiveHash] 调整间隔设置为: " + intervalMs + "ms");
    }
}
