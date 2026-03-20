package com.example.abtesting.controller;

import com.example.abtesting.domain.model.*;
import com.example.abtesting.domain.valueobject.AssignmentResult;

/**
 * 分配引擎接口
 */
public interface AssignmentEngine {
    
    /**
     * 执行流量分配
     */
    AssignmentResult assign(User user, Experiment experiment);
}
