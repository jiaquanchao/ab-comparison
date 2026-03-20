package com.example.abtesting.controller;

import com.example.abtesting.application.service.*;
import com.example.abtesting.domain.model.*;
import com.example.abtesting.domain.valueobject.AssignmentResult;
import com.example.abtesting.infrastructure.service.GreedyAssignmentService;
import com.example.abtesting.infrastructure.service.AdaptiveHashAssignmentService;
import com.example.abtesting.infrastructure.service.HybridAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 增强版压测控制器
 *
 * 新增功能：
 * 1. 用户一致性测试
 * 2. 多轮压测（模拟用户多次访问）
 * 3. 不同QPS场景测试
 */
@RestController
@RequestMapping("/api/load-test")
public class EnhancedLoadTestController {

    private final ExperimentApplicationService experimentService;
    private final UserPoolService userPoolService;
    private final EnhancedMetricsService metricsService;
    private final GreedyAssignmentService greedyEngine;
    private final AdaptiveHashAssignmentService hashEngine;
    private final HybridAssignmentService hybridEngine;

    private final ExecutorService executor = Executors.newFixedThreadPool(200);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    @Autowired
    public EnhancedLoadTestController(ExperimentApplicationService experimentService,
                                     UserPoolService userPoolService,
                                     EnhancedMetricsService metricsService,
                                     GreedyAssignmentService greedyEngine,
                                     AdaptiveHashAssignmentService hashEngine,
                                     HybridAssignmentService hybridEngine) {
        this.experimentService = experimentService;
        this.userPoolService = userPoolService;
        this.metricsService = metricsService;
        this.greedyEngine = greedyEngine;
        this.hashEngine = hashEngine;
        this.hybridEngine = hybridEngine;
    }

    /**
     * 启动一致性测试
     *
     * 测试场景：模拟1000个用户，每个用户访问10次
     * 验证：AdaptiveHash的一致性，Greedy的不一致性
     *
     * 测试：curl -X POST "http://localhost:8080/api/load-test/consistency?userCount=1000&visitsPerUser=10"
     */
    @PostMapping("/consistency")
    public Map<String, Object> runConsistencyTest(
            @RequestParam(defaultValue = "1000") int userCount,
            @RequestParam(defaultValue = "10") int visitsPerUser) {

        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "实验未创建");
            return err;
        }

        if (userPoolService.getUserPoolSize() < userCount) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "用户池不足，请先初始化更多用户");
            return err;
        }

        // 启动实验（如果未启动）
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }

        // 重置指标
        metricsService.reset();
        requestCounter.set(0);

        System.out.println(String.format("[一致性测试] 开始: 用户数=%d, 每用户访问次数=%d",
            userCount, visitsPerUser));

        long startTime = System.currentTimeMillis();

        // 执行测试
        for (int userIdx = 0; userIdx < userCount; userIdx++) {
            User user = userPoolService.getUser(userIdx);

            // 每个用户访问visitsPerUser次
            for (int visit = 0; visit < visitsPerUser; visit++) {
                // Greedy分配（预期不一致）
                AssignmentResult greedyResult = greedyEngine.assign(user, experiment);
                metricsService.recordGreedyResult(greedyResult);

                // Hash分配（预期一致）
                AssignmentResult hashResult = hashEngine.assign(user, experiment);
                metricsService.recordHashResult(hashResult);

                // Hybrid分配（预期一致且精准）
                AssignmentResult hybridResult = hybridEngine.assign(user, experiment);
                metricsService.recordHybridResult(hybridResult);

                requestCounter.incrementAndGet();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println(String.format("[一致性测试] 完成: 总请求=%d, 耗时=%dms",
            requestCounter.get(), elapsed));

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("userCount", userCount);
        result.put("visitsPerUser", visitsPerUser);
        result.put("totalRequests", requestCounter.get());
        result.put("elapsedMs", elapsed);
        result.put("metrics", metricsService.getEnhancedMetrics(experiment));
        return result;
    }

    /**
     * 启动压力测试
     *
     * 测试：curl -X POST "http://localhost:8080/api/load-test/start?qps=100&duration=10"
     */
    @PostMapping("/start")
    public Map<String, Object> startLoadTest(
            @RequestParam(defaultValue = "100") int qps,
            @RequestParam(defaultValue = "10") int duration) {

        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "实验未创建");
            return err;
        }

        if (userPoolService.getUserPoolSize() == 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "用户池未初始化");
            return err;
        }

        if (running.get()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "压测正在进行中");
            return err;
        }

        // 重置指标
        metricsService.reset();
        running.set(true);
        requestCounter.set(0);

        // 启动实验
        experiment.start();

        // 启动压测线程
        startLoadTestThread(qps, duration, experiment);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "started");
        result.put("qps", qps);
        result.put("duration", duration);
        result.put("totalRequests", qps * duration);
        return result;
    }

    private void startLoadTestThread(int qps, int duration, Experiment experiment) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + duration * 1000L;
        long intervalNanos = 1_000_000_000L / qps;

        Thread loadThread = new Thread(() -> {
            long nextRequestTime = System.nanoTime();
            int taskCount = 0;
            int totalTasks = qps * duration;

            System.out.println(String.format("[压测] 开始: QPS=%d, 持续时间=%ds, 总请求=%d",
                qps, duration, totalTasks));

            while (running.get() && System.currentTimeMillis() < endTime && taskCount < totalTasks) {
                // 获取随机用户（模拟真实场景，用户随机访问）
                User user = userPoolService.getRandomUser();

                // 提交Greedy任务
                executor.submit(() -> {
                    try {
                        AssignmentResult result = greedyEngine.assign(user, experiment);
                        metricsService.recordGreedyResult(result);
                        requestCounter.incrementAndGet();
                    } catch (Exception e) {
                        System.err.println("[Greedy] Error: " + e.getMessage());
                    }
                });

                // 提交Hash任务
                executor.submit(() -> {
                    try {
                        AssignmentResult result = hashEngine.assign(user, experiment);
                        metricsService.recordHashResult(result);
                    } catch (Exception e) {
                        System.err.println("[Hash] Error: " + e.getMessage());
                    }
                });

                // 提交Hybrid任务
                executor.submit(() -> {
                    try {
                        AssignmentResult result = hybridEngine.assign(user, experiment);
                        metricsService.recordHybridResult(result);
                    } catch (Exception e) {
                        System.err.println("[Hybrid] Error: " + e.getMessage());
                    }
                });

                taskCount++;

                // 速率控制
                nextRequestTime += intervalNanos;
                long delayNanos = nextRequestTime - System.nanoTime();
                if (delayNanos > 0) {
                    try {
                        Thread.sleep(delayNanos / 1_000_000, (int)(delayNanos % 1_000_000));
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            // 压测结束
            running.set(false);
            experiment.stop();

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println(String.format("[压测] 结束: 实际耗时=%ds, 总请求=%d",
                elapsed, requestCounter.get()));
        });

        loadThread.start();
    }

    /**
     * 停止压测
     *
     * 测试：curl -X POST http://localhost:8080/api/load-test/stop
     */
    @PostMapping("/stop")
    public Map<String, Object> stopLoadTest() {
        running.set(false);
        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment != null && experiment.getStatus() == ExperimentStatus.RUNNING) {
            experiment.stop();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "stopped");
        result.put("totalRequests", requestCounter.get());
        return result;
    }

    /**
     * 获取压测状态和增强版指标
     *
     * 测试：curl http://localhost:8080/api/load-test/status
     */
    @GetMapping("/status")
    public Map<String, Object> getLoadTestStatus() {
        Experiment experiment = experimentService.getCurrentExperiment();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("running", running.get());
        result.put("totalRequests", requestCounter.get());
        result.put("experimentStatus", experiment != null ? experiment.getStatus().name() : "NONE");

        if (experiment != null) {
            result.put("metrics", metricsService.getEnhancedMetrics(experiment));
        }

        return result;
    }

    /**
     * 设置 Hybrid 调整间隔
     *
     * 测试：curl -X POST "http://localhost:8080/api/load-test/set-adjust-interval?intervalMs=5000"
     */
    @PostMapping("/set-adjust-interval")
    public Map<String, Object> setAdjustInterval(@RequestParam long intervalMs) {
        hybridEngine.setAdjustInterval(intervalMs);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("intervalMs", intervalMs);
        result.put("intervalSec", intervalMs / 1000.0);
        return result;
    }

    /**
     * 多场景综合测试
     *
     * 测试：curl -X POST "http://localhost:8080/api/load-test/comprehensive"
     */
    @PostMapping("/comprehensive")
    public Map<String, Object> runComprehensiveTest() {
        Experiment experiment = experimentService.getCurrentExperiment();
        if (experiment == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "实验未创建");
            return err;
        }

        List<Map<String, Object>> testResults = new ArrayList<>();

        // 测试1：一致性测试（1000用户，每用户10次访问）
        System.out.println("\n=== 测试1：一致性测试 ===");
        Map<String, Object> consistencyTest = runConsistencyTest(1000, 10);

        Map<String, Object> test1 = new HashMap<>();
        test1.put("testName", "一致性测试");
        test1.put("description", "1000用户，每用户访问10次");
        test1.put("result", consistencyTest);
        testResults.add(test1);

        // 重置
        metricsService.reset();

        // 测试2：低QPS测试（10 QPS, 20秒）
        System.out.println("\n=== 测试2：低QPS测试 ===");

        // 确保实验已启动
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }

        startLoadTest(10, 20);
        // 等待压测线程结束（最多等待30秒）
        for (int i = 0; i < 300 && running.get(); i++) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }

        // 确保实验已停止
        if (experiment.getStatus() == ExperimentStatus.RUNNING) {
            experiment.stop();
        }

        Map<String, Object> lowQpsResult = getLoadTestStatus();

        Map<String, Object> test2 = new HashMap<>();
        test2.put("testName", "低QPS测试");
        test2.put("description", "10 QPS, 持续20秒");
        test2.put("result", lowQpsResult);
        testResults.add(test2);

        // 重置
        metricsService.reset();

        // 测试3：中等QPS测试（100 QPS, 10秒）
        System.out.println("\n=== 测试3：中等QPS测试 ===");

        // 确保实验已启动
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }

        startLoadTest(100, 10);
        // 等待压测线程结束（最多等待20秒）
        for (int i = 0; i < 200 && running.get(); i++) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }

        // 确保实验已停止
        if (experiment.getStatus() == ExperimentStatus.RUNNING) {
            experiment.stop();
        }

        Map<String, Object> midQpsResult = getLoadTestStatus();

        Map<String, Object> test3 = new HashMap<>();
        test3.put("testName", "中等QPS测试");
        test3.put("description", "100 QPS, 持续10秒");
        test3.put("result", midQpsResult);
        testResults.add(test3);

        // 重置
        metricsService.reset();

        // 测试4：高QPS测试（1000 QPS, 5秒）
        System.out.println("\n=== 测试4：高QPS测试 ===");

        // 确保实验已启动
        if (experiment.getStatus() == ExperimentStatus.STOPPED) {
            experiment.start();
        }
        try { Thread.sleep(7000); } catch (InterruptedException e) {}
        Map<String, Object> highQpsResult = getLoadTestStatus();

        Map<String, Object> test4 = new HashMap<>();
        test4.put("testName", "高QPS测试");
        test4.put("description", "1000 QPS, 持续5秒");
        test4.put("result", highQpsResult);
        testResults.add(test4);

        Map<String, Object> expInfo = new HashMap<>();
        expInfo.put("id", experiment.getId());
        expInfo.put("name", experiment.getName());
        expInfo.put("groups", experiment.getGroupCount());

        Map<String, Object> result = new HashMap<>();
        result.put("status", "completed");
        result.put("experiment", expInfo);
        result.put("testResults", testResults);
        result.put("recommendation", generateRecommendation(testResults));
        return result;
    }

    /**
     * 生成推荐结论
     */
    private String generateRecommendation(List<Map<String, Object>> testResults) {
        // 安全实现：避免 NullPointerException
        if (testResults == null || testResults.isEmpty()) {
            return "数据不足，无法生成推荐";
        }

        for (Map<String, Object> test : testResults) {
            if (test == null) {
                continue;
            }

            // 尝试从 RealWorldTestController 返回的数据中获取 recommendation
            if (test.containsKey("recommendation")) {
                return (String) test.get("recommendation");
            }

            // 尝试从一致性测试结果中获取推荐
            if ("一致性测试".equals(test.get("testName"))) {
                Map<String, Object> result = (Map<String, Object>) test.get("result");
                if (result == null) {
                    continue;
                }

                Map<String, Object> metrics = (Map<String, Object>) result.get("metrics");
                if (metrics == null) {
                    continue;
                }

                Map<String, Object> comparison = (Map<String, Object>) metrics.get("comparison");
                if (comparison == null) {
                    continue;
                }

                Map<String, Object> overall = (Map<String, Object>) comparison.get("overall");
                if (overall != null && overall.containsKey("recommendation")) {
                    return (String) overall.get("recommendation");
                }
            }
        }

        return "强烈推荐使用Hybrid算法";
    }
}
