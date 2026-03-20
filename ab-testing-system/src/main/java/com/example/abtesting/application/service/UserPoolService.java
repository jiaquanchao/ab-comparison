package com.example.abtesting.application.service;

import com.example.abtesting.domain.factory.ExperimentFactory;
import com.example.abtesting.domain.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用户池服务
 * 
 * 负责用户生成和管理
 */
@Service
public class UserPoolService {
    
    private final ExperimentFactory experimentFactory;
    private final ExperimentApplicationService experimentService;
    
    private List<User> userPool;
    private Map<String, AtomicInteger> tagDistributionStats; // 统计标签分布
    
    public UserPoolService(ExperimentFactory experimentFactory, 
                          ExperimentApplicationService experimentService) {
        this.experimentFactory = experimentFactory;
        this.experimentService = experimentService;
        this.userPool = new ArrayList<>();
        this.tagDistributionStats = new HashMap<>();
    }
    
    /**
     * 初始化用户池
     * 
     * @param size 用户数量
     * @return 统计信息
     */
    public Map<String, Object> initializeUserPool(int size) {
        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "实验未创建，请先调用 /api/experiment/create");
            return error;
        }
        
        userPool.clear();
        tagDistributionStats.clear();
        
        TagSchema tagSchema = experiment.getTagSchema();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // 生成用户
        for (int i = 0; i < size; i++) {
            String userId = String.format("user_%06d", i + 1);
            int[] tagIndexes = generateRandomTags(tagSchema, random);
            User user = experimentFactory.createUser(userId, tagIndexes, tagSchema);
            userPool.add(user);
            
            // 统计标签分布
            updateTagDistributionStats(tagIndexes, tagSchema);
        }
        
        return getUserPoolStats();
    }
    
    /**
     * 生成随机标签
     */
    private int[] generateRandomTags(TagSchema tagSchema, ThreadLocalRandom random) {
        int[] tagIndexes = new int[tagSchema.getDimensionCount()];
        
        for (int i = 0; i < tagSchema.getDimensionCount(); i++) {
            TagDimension dim = tagSchema.getDimension(i);
            double rand = random.nextDouble();
            tagIndexes[i] = dim.selectValueIndex(rand);
        }
        
        return tagIndexes;
    }
    
    /**
     * 更新标签分布统计
     */
    private void updateTagDistributionStats(int[] tagIndexes, TagSchema tagSchema) {
        for (int i = 0; i < tagIndexes.length; i++) {
            TagDimension dim = tagSchema.getDimension(i);
            String key = dim.getName() + "_" + dim.getValueName(tagIndexes[i]);
            tagDistributionStats.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }
    
    /**
     * 获取用户池统计信息
     */
    public Map<String, Object> getUserPoolStats() {
        if (userPool.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "用户池未初始化");
            return error;
        }
        
        Experiment experiment = experimentService.getCurrentExperiment();
        TagSchema tagSchema = experiment.getTagSchema();
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userPool.size());
        
        // 标签分布统计
        Map<String, Object> distribution = new LinkedHashMap<>();
        for (int i = 0; i < tagSchema.getDimensionCount(); i++) {
            TagDimension dim = tagSchema.getDimension(i);
            Map<String, Object> dimStats = new LinkedHashMap<>();
            
            for (int j = 0; j < dim.getValueCount(); j++) {
                String valueName = dim.getValueName(j);
                String key = dim.getName() + "_" + valueName;
                int count = tagDistributionStats.getOrDefault(key, new AtomicInteger(0)).get();
                double percentage = (count * 100.0) / userPool.size();
                double targetPercentage = dim.getValueRatio(j);
                double deviation = percentage - targetPercentage;
                
                Map<String, Object> valueStats = new LinkedHashMap<>();
                valueStats.put("count", count);
                valueStats.put("actualPercentage", percentage);
                valueStats.put("targetPercentage", targetPercentage);
                valueStats.put("deviation", deviation);
                
                dimStats.put(valueName, valueStats);
            }
            
            distribution.put(dim.getName(), dimStats);
        }
        stats.put("tagDistribution", distribution);
        
        // 前5个用户示例
        List<String> examples = userPool.stream()
            .limit(5)
            .map(User::toString)
            .collect(Collectors.toList());
        stats.put("userExamples", examples);
        
        return stats;
    }
    
    /**
     * 获取用户池大小
     */
    public int getUserPoolSize() {
        return userPool.size();
    }
    
    /**
     * 获取用户
     */
    public User getUser(int index) {
        if (index < 0 || index >= userPool.size()) {
            throw new IndexOutOfBoundsException("用户索引超出范围: " + index);
        }
        return userPool.get(index);
    }
    
    /**
     * 获取随机用户
     */
    public User getRandomUser() {
        if (userPool.isEmpty()) {
            throw new IllegalStateException("用户池为空");
        }
        int index = ThreadLocalRandom.current().nextInt(userPool.size());
        return userPool.get(index);
    }
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(userPool);
    }
}
