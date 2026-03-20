package com.example.abtesting.domain.factory;

import com.example.abtesting.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 实验工厂
 * 
 * DDD设计：工厂模式，负责创建实验聚合
 */
@Component
public class ExperimentFactory {
    
    private static int experimentCounter = 0;
    
    /**
     * 创建默认的4客群实验
     * 
     * 配置：
     * - 4个分组：A(10%), B(20%), C(30%), D(40%)
     * - 3个标签维度（简化版）：
     *   - 性别：男(55%), 女(45%)
     *   - 省份：广东(40%), 山东(35%), 江苏(25%)  
     *   - 年龄段：18-25岁(40%), 26-35岁(35%), 36-45岁(25%)
     * - 总组合数：2 × 3 × 3 = 18
     */
    public Experiment createDefaultExperiment() {
        // 创建4个分组
        List<Group> groups = Arrays.asList(
            new Group("A", 0.10, "对照组A - 10%"),
            new Group("B", 0.20, "实验组B - 20%"),
            new Group("C", 0.30, "实验组C - 30%"),
            new Group("D", 0.40, "实验组D - 40%")
        );
        
        // 创建3个简化的标签维度
        List<TagDimension> dimensions = new ArrayList<>();
        
        // 1. 性别（2个枚举）
        LinkedHashMap<String, Double> genderValues = new LinkedHashMap<>();
        genderValues.put("男", 55.0);
        genderValues.put("女", 45.0);
        dimensions.add(new TagDimension("性别", genderValues));
        
        // 2. 省份（3个枚举）
        LinkedHashMap<String, Double> provinceValues = new LinkedHashMap<>();
        provinceValues.put("广东", 40.0);
        provinceValues.put("山东", 35.0);
        provinceValues.put("江苏", 25.0);
        dimensions.add(new TagDimension("省份", provinceValues));
        
        // 3. 年龄段（3个枚举）
        LinkedHashMap<String, Double> ageValues = new LinkedHashMap<>();
        ageValues.put("18-25岁", 40.0);
        ageValues.put("26-35岁", 35.0);
        ageValues.put("36-45岁", 25.0);
        dimensions.add(new TagDimension("年龄段", ageValues));
        
        TagSchema tagSchema = new TagSchema(dimensions);
        
        String experimentId = String.format("exp-%03d", ++experimentCounter);
        return new Experiment(experimentId, "四客群实验", groups, tagSchema);
    }
    
    /**
     * 创建用户
     */
    public User createUser(String userId, int[] tagIndexes, TagSchema tagSchema) {
        return new User(userId, tagIndexes, tagSchema);
    }
}
