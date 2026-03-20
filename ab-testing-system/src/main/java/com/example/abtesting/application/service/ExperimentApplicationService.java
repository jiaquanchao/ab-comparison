package com.example.abtesting.application.service;

import com.example.abtesting.domain.factory.ExperimentFactory;
import com.example.abtesting.domain.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 实验应用服务
 * 
 * 应用层服务，协调领域对象
 */
@Service
public class ExperimentApplicationService {
    
    private final ExperimentFactory experimentFactory;
    private Experiment currentExperiment;
    
    public ExperimentApplicationService(ExperimentFactory experimentFactory) {
        this.experimentFactory = experimentFactory;
    }
    
    /**
     * 创建默认实验
     */
    public Map<String, Object> createDefaultExperiment() {
        this.currentExperiment = experimentFactory.createDefaultExperiment();
        return getExperimentInfo();
    }
    
    /**
     * 获取实验信息
     */
    public Map<String, Object> getExperimentInfo() {
        if (currentExperiment == null) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "实验未创建，请先调用 /api/experiment/create");
            return errorMap;
        }
        
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("experimentId", currentExperiment.getId());
        info.put("experimentName", currentExperiment.getName());
        info.put("status", currentExperiment.getStatus().name());
        info.put("groupCount", currentExperiment.getGroupCount());
        
        // 分组信息
        List<Map<String, Object>> groups = currentExperiment.getGroups().stream()
            .map(g -> {
                Map<String, Object> groupInfo = new LinkedHashMap<>();
                groupInfo.put("name", g.getName());
                groupInfo.put("targetRatio", g.getTargetRatio());
                groupInfo.put("targetPercentage", g.getTargetPercentage());
                groupInfo.put("description", g.getDescription());
                return groupInfo;
            })
            .collect(Collectors.toList());
        info.put("groups", groups);
        
        // 标签维度信息
        TagSchema tagSchema = currentExperiment.getTagSchema();
        List<Map<String, Object>> dimensions = new ArrayList<>();
        for (int i = 0; i < tagSchema.getDimensionCount(); i++) {
            TagDimension dim = tagSchema.getDimension(i);
            Map<String, Object> dimInfo = new LinkedHashMap<>();
            dimInfo.put("name", dim.getName());
            dimInfo.put("valueCount", dim.getValueCount());
            dimInfo.put("values", dim.getValues());
            dimensions.add(dimInfo);
        }
        info.put("tagDimensions", dimensions);
        info.put("totalCombinations", tagSchema.getTotalCombinations());
        
        return info;
    }
    
    /**
     * 获取当前实验
     */
    public Experiment getCurrentExperiment() {
        return currentExperiment;
    }
}
