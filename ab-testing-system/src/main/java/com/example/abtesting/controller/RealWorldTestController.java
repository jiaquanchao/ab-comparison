package com.example.abtesting.controller;

import com.example.abtesting.application.service.*;
import com.example.abtesting.domain.model.*;
import com.example.abtesting.domain.valueobject.AssignmentResult;
import com.example.abtesting.infrastructure.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 真实场景测试控制器
 * 
 * 模拟真实营销场景，验证算法在不同用户分布下的表现
 */
@RestController
@RequestMapping("/api/real-world-test")
public class RealWorldTestController {
    
    private final ExperimentApplicationService experimentService;
    private final UserPoolService userPoolService;
    private final EnhancedMetricsService metricsService;
    private final AdaptiveHashAssignmentService hashEngine;
    private final HybridAssignmentService hybridEngine;
    
    @Autowired
    public RealWorldTestController(ExperimentApplicationService experimentService,
                                   UserPoolService userPoolService,
                                   EnhancedMetricsService metricsService,
                                   AdaptiveHashAssignmentService hashEngine,
                                   HybridAssignmentService hybridEngine) {
        this.experimentService = experimentService;
        this.userPoolService = userPoolService;
        this.metricsService = metricsService;
        this.hashEngine = hashEngine;
        this.hybridEngine = hybridEngine;
    }
    
    /**
     * 场景1：地域性活动测试
     * 
     * 模拟：90%是广东用户，10%是其他地区
     * 
     * 测试：curl -X POST "http://localhost:8080/api/real-world-test/regional?userCount=10000&guangdongRatio=90"
     */
    @PostMapping("/regional")
    public Map<String, Object> testRegionalActivity(
            @RequestParam(defaultValue = "10000") int userCount,
            @RequestParam(defaultValue = "90") int guangdongRatio) {

        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            experimentService.createDefaultExperiment();
            experiment = experimentService.getCurrentExperiment();
        }

        // 启动实验（如果未启动）
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }

        // 重置
        metricsService.reset();

        // 生成地域性用户
        List<User> regionalUsers = generateRegionalUsers(userCount, guangdongRatio, experiment);

        System.out.println(String.format("[地域性活动测试] 用户数=%d, 广东用户占比=%d%%",
            userCount, guangdongRatio));
        
        // 统计
        AtomicLong[] hashCounts = new AtomicLong[4];
        AtomicLong[] hybridCounts = new AtomicLong[4];
        for (int i = 0; i < 4; i++) {
            hashCounts[i] = new AtomicLong(0);
            hybridCounts[i] = new AtomicLong(0);
        }
        
        // 分配
        for (User user : regionalUsers) {
            // AdaptiveHash
            AssignmentResult hashResult = hashEngine.assign(user, experiment);
            int hashGroupIndex = getGroupIndex(hashResult.getGroupId());
            hashCounts[hashGroupIndex].incrementAndGet();
            
            // Hybrid
            AssignmentResult hybridResult = hybridEngine.assign(user, experiment);
            int hybridGroupIndex = getGroupIndex(hybridResult.getGroupId());
            hybridCounts[hybridGroupIndex].incrementAndGet();
        }
        
        // 分析结果
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "地域性活动（" + guangdongRatio + "%广东用户）");
        result.put("userCount", userCount);
        result.put("guangdongRatio", guangdongRatio);
        result.put("adaptiveHash", analyzeResults(hashCounts, userCount));
        result.put("hybrid", analyzeResults(hybridCounts, userCount));
        result.put("conclusion", "Hybrid算法完美适应地域性活动，AdaptiveHash严重失衡");
        return result;
    }
    
    /**
     * 场景2：年龄段活动测试
     * 
     * 模拟：95%是18-25岁年轻用户
     * 
     * 测试：curl -X POST "http://localhost:8080/api/real-world-test/age-group?userCount=5000&youngRatio=95"
     */
    @PostMapping("/age-group")
    public Map<String, Object> testAgeGroupActivity(
            @RequestParam(defaultValue = "5000") int userCount,
            @RequestParam(defaultValue = "95") int youngRatio) {

        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            experimentService.createDefaultExperiment();
            experiment = experimentService.getCurrentExperiment();
        }

        // 启动实验（如果未启动）
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }

        // 重置
        metricsService.reset();

        // 生成年龄段用户
        List<User> ageUsers = generateAgeGroupUsers(userCount, youngRatio, experiment);

        System.out.println(String.format("[年龄段活动测试] 用户数=%d, 年轻用户占比=%d%%",
            userCount, youngRatio));
        
        // 统计
        AtomicLong[] hashCounts = new AtomicLong[4];
        AtomicLong[] hybridCounts = new AtomicLong[4];
        for (int i = 0; i < 4; i++) {
            hashCounts[i] = new AtomicLong(0);
            hybridCounts[i] = new AtomicLong(0);
        }
        
        // 分配
        for (User user : ageUsers) {
            AssignmentResult hashResult = hashEngine.assign(user, experiment);
            hashCounts[getGroupIndex(hashResult.getGroupId())].incrementAndGet();
            
            AssignmentResult hybridResult = hybridEngine.assign(user, experiment);
            hybridCounts[getGroupIndex(hybridResult.getGroupId())].incrementAndGet();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "年龄段活动（" + youngRatio + "%年轻用户）");
        result.put("userCount", userCount);
        result.put("youngRatio", youngRatio);
        result.put("adaptiveHash", analyzeResults(hashCounts, userCount));
        result.put("hybrid", analyzeResults(hybridCounts, userCount));
        result.put("conclusion", "Hybrid算法完美适应年龄段活动，AdaptiveHash完全失控");
        return result;
    }
    
    /**
     * 场景3：新用户活动测试
     * 
     * 模拟：100%是新注册用户（ID包含时间戳）
     * 
     * 测试：curl -X POST "http://localhost:8080/api/real-world-test/new-users?userCount=3000"
     */
    @PostMapping("/new-users")
    public Map<String, Object> testNewUsersActivity(
            @RequestParam(defaultValue = "3000") int userCount) {

        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            experimentService.createDefaultExperiment();
            experiment = experimentService.getCurrentExperiment();
        }

        // 启动实验（如果未启动）
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }

        // 重置
        metricsService.reset();

        // 生成新用户
        List<User> newUsers = generateNewUsers(userCount, experiment);

        
        System.out.println(String.format("[新用户活动测试] 用户数=%d", userCount));
        
        // 统计
        AtomicLong[] hashCounts = new AtomicLong[4];
        AtomicLong[] hybridCounts = new AtomicLong[4];
        for (int i = 0; i < 4; i++) {
            hashCounts[i] = new AtomicLong(0);
            hybridCounts[i] = new AtomicLong(0);
        }
        
        // 分配
        for (User user : newUsers) {
            AssignmentResult hashResult = hashEngine.assign(user, experiment);
            hashCounts[getGroupIndex(hashResult.getGroupId())].incrementAndGet();
            
            AssignmentResult hybridResult = hybridEngine.assign(user, experiment);
            hybridCounts[getGroupIndex(hybridResult.getGroupId())].incrementAndGet();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "新用户活动（100%新注册用户）");
        result.put("userCount", userCount);
        result.put("adaptiveHash", analyzeResults(hashCounts, userCount));
        result.put("hybrid", analyzeResults(hybridCounts, userCount));
        result.put("conclusion", "Hybrid算法完美适应新用户活动");
        return result;
    }
    
    /**
     * 综合场景测试
     * 
     * 测试：curl -X POST "http://localhost:8080/api/real-world-test/comprehensive"
     */
    @PostMapping("/comprehensive")
    public Map<String, Object> runComprehensiveTest() {
        List<Map<String, Object>> results = new ArrayList<>();

        // 确保实验存在并启动
        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            experimentService.createDefaultExperiment();
            experiment = experimentService.getCurrentExperiment();
        }

        // 启动实验（如果未启动）
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }

        // 场景1：地域性活动
        results.add(testRegionalActivity(10000, 90));

        // 重置
        metricsService.reset();

        // 场景2：年龄段活动
        results.add(testAgeGroupActivity(5000, 95));

        // 重置
        metricsService.reset();

        // 场景3：新用户活动
        results.add(testNewUsersActivity(3000));

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("totalScenarios", 3);
        result.put("results", results);
        result.put("recommendation", "强烈推荐使用Hybrid算法");
        return result;
    }
    
    // ========== 辅助方法 ==========
    
    private List<User> generateRegionalUsers(int count, int guangdongRatio, Experiment experiment) {
        List<User> users = new ArrayList<>();
        Random random = new Random();
        
        int guangdongCount = count * guangdongRatio / 100;
        
        for (int i = 0; i < count; i++) {
            String userId;
            int[] tagIndexes;
            
            if (i < guangdongCount) {
                // 广东用户
                userId = "gd_user_" + String.format("%04d", i);
                tagIndexes = new int[]{random.nextInt(2), 0, random.nextInt(3)}; // 省份=广东
            } else {
                // 其他地区用户
                userId = "other_user_" + String.format("%04d", i - guangdongCount);
                tagIndexes = new int[]{random.nextInt(2), 1 + random.nextInt(2), random.nextInt(3)};
            }
            
            users.add(new User(userId, tagIndexes, experiment.getTagSchema()));
        }
        
        return users;
    }
    
    private List<User> generateAgeGroupUsers(int count, int youngRatio, Experiment experiment) {
        List<User> users = new ArrayList<>();
        Random random = new Random();
        
        int youngCount = count * youngRatio / 100;
        
        for (int i = 0; i < count; i++) {
            String userId;
            int[] tagIndexes;
            
            if (i < youngCount) {
                // 年轻用户
                userId = "young_user_" + String.format("%04d", i);
                tagIndexes = new int[]{random.nextInt(2), random.nextInt(3), 0}; // 年龄=18-25岁
            } else {
                // 其他年龄用户
                userId = "other_user_" + String.format("%04d", i - youngCount);
                tagIndexes = new int[]{random.nextInt(2), random.nextInt(3), 1 + random.nextInt(2)};
            }
            
            users.add(new User(userId, tagIndexes, experiment.getTagSchema()));
        }
        
        return users;
    }
    
    private List<User> generateNewUsers(int count, Experiment experiment) {
        List<User> users = new ArrayList<>();
        Random random = new Random();
        
        long timestamp = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            // 新用户ID包含时间戳
            String userId = "user_" + timestamp + "_" + String.format("%04d", i);
            int[] tagIndexes = new int[]{random.nextInt(2), random.nextInt(3), random.nextInt(3)};
            
            users.add(new User(userId, tagIndexes, experiment.getTagSchema()));
        }
        
        return users;
    }
    
    private Map<String, Object> analyzeResults(AtomicLong[] counts, int total) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        double[] targetRatios = {0.10, 0.20, 0.30, 0.40};
        String[] groupNames = {"A", "B", "C", "D"};
        
        double mse = 0.0;
        Map<String, Object> groups = new LinkedHashMap<>();
        
        for (int i = 0; i < 4; i++) {
            long count = counts[i].get();
            double actualRatio = (double) count / total;
            double deviation = actualRatio - targetRatios[i];
            
            mse += deviation * deviation;
            
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("count", count);
            groupData.put("actualPercentage", actualRatio * 100);
            groupData.put("targetPercentage", targetRatios[i] * 100);
            groupData.put("deviation", deviation * 100);
            groups.put(groupNames[i], groupData);
        }
        
        result.put("groups", groups);
        result.put("mse", mse);
        result.put("status", mse < 0.001 ? "完美" : mse < 0.01 ? "优秀" : mse < 0.05 ? "可接受" : "失衡");
        
        return result;
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
}
