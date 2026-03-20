package com.example.abtesting.domain.service;

import com.example.abtesting.domain.model.*;
import com.example.abtesting.domain.valueobject.*;

/**
 * 分配领域服务
 * 
 * DDD设计：领域服务，负责流量分配的核心逻辑
 */
public interface AssignmentService {
    
    /**
     * 执行流量分配
     * 
     * @param user 用户
     * @param experiment 实验
     * @return 分配结果
     */
    AssignmentResult assign(User user, Experiment experiment);
    
    /**
     * 获取服务名称
     */
    String getName();
    
    /**
     * 重置状态
     */
    void reset();
}
