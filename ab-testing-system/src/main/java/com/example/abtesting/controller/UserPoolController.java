package com.example.abtesting.controller;

import com.example.abtesting.application.service.ExperimentApplicationService;
import com.example.abtesting.application.service.UserPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户池控制器
 *
 * 提供用户池管理的REST API
 */
@RestController
@RequestMapping("/api/user-pool")
public class UserPoolController {

    private final UserPoolService userPoolService;
    private final ExperimentApplicationService experimentService;

    @Autowired
    public UserPoolController(UserPoolService userPoolService,
                             ExperimentApplicationService experimentService) {
        this.userPoolService = userPoolService;
        this.experimentService = experimentService;
    }

    /**
     * 初始化用户池
     *
     * 测试：curl -X POST "http://localhost:8080/api/user-pool/init?size=10000"
     */
    @PostMapping("/init")
    public Map<String, Object> initializeUserPool(@RequestParam(defaultValue = "10000") int size) {
        return userPoolService.initializeUserPool(size);
    }

    /**
     * 获取用户池统计信息
     *
     * 测试：curl http://localhost:8080/api/user-pool/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getUserPoolStats() {
        return userPoolService.getUserPoolStats();
    }

    /**
     * 获取用户池大小
     *
     * 测试：curl http://localhost:8080/api/user-pool/size
     */
    @GetMapping("/size")
    public Map<String, Object> getUserPoolSize() {
        Map<String, Object> result = new HashMap<>();
        result.put("size", userPoolService.getUserPoolSize());
        result.put("experiment", experimentService.getExperimentInfo());
        return result;
    }
}
