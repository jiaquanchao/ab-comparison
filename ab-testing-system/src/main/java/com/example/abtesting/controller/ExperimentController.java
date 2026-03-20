package com.example.abtesting.controller;

import com.example.abtesting.application.service.ExperimentApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实验控制器
 * 
 * 提供实验管理的REST API
 */
@RestController
@RequestMapping("/api/experiment")
public class ExperimentController {
    
    private final ExperimentApplicationService experimentService;
    
    @Autowired
    public ExperimentController(ExperimentApplicationService experimentService) {
        this.experimentService = experimentService;
    }
    
    /**
     * 创建默认实验
     * 
     * 测试：curl -X POST http://localhost:8080/api/experiment/create
     */
    @PostMapping("/create")
    public Map<String, Object> createExperiment() {
        return experimentService.createDefaultExperiment();
    }
    
    /**
     * 获取实验信息
     * 
     * 测试：curl http://localhost:8080/api/experiment/info
     */
    @GetMapping("/info")
    public Map<String, Object> getExperimentInfo() {
        return experimentService.getExperimentInfo();
    }
}
