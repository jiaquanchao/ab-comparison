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
 * 混合分配服务（缓存 + 动态调整）
 * 
 * 策略：
 * 1. 老用户：从缓存读取分组（100%一致）
 * 2. 新用户：根据当前偏差动态分配
 * 
 * 优点：
 * - 100%用户一致性
 * - 分组比例精准（动态调整）
 * - 适应任何用户分布（不依赖hash均匀性）
 */
@Service
public class HybridAssignmentService implements AssignmentService, AssignmentEngine {
    
    // 用户分组缓存（内存版，生产环境应使用Redis）
    private final Map<String, String> userGroupCache = new ConcurrentHashMap<>();
    
    // 组合键 -> [countA, countB, countC, countD]
    private final Map<String, long[]> comboCounters = new ConcurrentHashMap<>();
    
    // 全局计数器
    private final AtomicLong[] globalCounters = new AtomicLong[4];
    
    public HybridAssignmentService() {
        for (int i = 0; i < 4; i++) {
            globalCounters[i] = new AtomicLong(0);
        }
    }
    
    @Override
    public AssignmentResult assign(User user, Experiment experiment) {
        long startTime = System.nanoTime();
        
        String userId = user.getUserId();
        String comboKey = user.getComboKey();
        
        // 1. 查询缓存
        String cachedGroup = userGroupCache.get(userId);
        
        String selectedGroup;
        if (cachedGroup != null) {
            // 老用户：使用缓存的分组（100%一致）
            selectedGroup = cachedGroup;
        } else {
            // 2. 新用户：根据当前偏差动态分配
            selectedGroup = selectGroupByDeviation(experiment);
            
            // 3. 缓存分组结果
            userGroupCache.put(userId, selectedGroup);
        }
        
        // 4. 更新计数器
        int groupIndex = getGroupIndex(selectedGroup);
        globalCounters[groupIndex].incrementAndGet();
        
        // 更新组合计数器
        long[] counters = comboCounters.computeIfAbsent(comboKey, k -> new long[4]);
        counters[groupIndex]++;
        
        long endTime = System.nanoTime();
        
        return new AssignmentResult(
            userId,
            selectedGroup,
            comboKey,
            endTime - startTime,
            System.currentTimeMillis(),
            user.getTagIndexes()
        );
    }
    
    /**
     * 根据当前偏差选择分组
     */
    private String selectGroupByDeviation(Experiment experiment) {
        // 计算当前总请求数
        long total = 0;
        for (int i = 0; i < 4; i++) {
            total += globalCounters[i].get();
        }
        
        // 如果是第一个请求，使用默认分配
        if (total == 0) {
            return "A"; // 或者随机选择
        }
        
        // 计算每个组的偏差
        double maxDeviation = Double.NEGATIVE_INFINITY;
        String selectedGroup = "A";
        
        for (int i = 0; i < 4; i++) {
            Group group = experiment.getGroup(i);
            double target = group.getTargetRatio();
            double actual = (double) globalCounters[i].get() / total;
            double deviation = target - actual;
            
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
                selectedGroup = group.getName();
            }
        }
        
        return selectedGroup;
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
    
    @Override
    public String getName() {
        return "Hybrid";
    }
    
    @Override
    public void reset() {
        userGroupCache.clear();
        comboCounters.clear();
        for (int i = 0; i < 4; i++) {
            globalCounters[i].set(0);
        }
    }
    
    /**
     * 设置调整间隔（预留接口）
     */
    public void setAdjustInterval(long intervalMs) {
        System.out.println("[Hybrid] 调整间隔设置为: " + intervalMs + "ms");
    }
    
    /**
     * 获取组合计数器（用于统计）
     */
    public Map<String, long[]> getComboCounters() {
        return new ConcurrentHashMap<>(comboCounters);
    }
    
    /**
     * 获取全局计数器
     */
    public long[] getGlobalCounters() {
        long[] result = new long[4];
        for (int i = 0; i < 4; i++) {
            result[i] = globalCounters[i].get();
        }
        return result;
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return userGroupCache.size();
    }
}
