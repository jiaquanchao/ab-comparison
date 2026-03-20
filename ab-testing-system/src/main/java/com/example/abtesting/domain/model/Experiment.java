package com.example.abtesting.domain.model;

import java.util.*;

/**
 * 实验聚合根
 * 
 * DDD设计：实验是一个聚合根，包含多个分组和标签维度
 */
public class Experiment {
    private final String id;
    private final String name;
    private final List<Group> groups;
    private final TagSchema tagSchema;
    private ExperimentStatus status;
    
    public Experiment(String id, String name, List<Group> groups, TagSchema tagSchema) {
        this.id = id;
        this.name = name;
        this.groups = Collections.unmodifiableList(new ArrayList<>(groups));
        this.tagSchema = tagSchema;
        this.status = ExperimentStatus.CREATED;
        
        validate();
    }
    
    private void validate() {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("实验ID不能为空");
        }
        
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("实验名称不能为空");
        }
        
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个分组");
        }
        
        // 验证分组比例总和为100%
        double totalRatio = groups.stream().mapToDouble(Group::getTargetRatio).sum();
        if (Math.abs(totalRatio - 1.0) > 0.001) {
            throw new IllegalArgumentException(
                String.format("分组比例总和必须为100%%，当前为: %.2f%%", totalRatio * 100)
            );
        }
        
        // 验证分组名称唯一
        Set<String> names = new HashSet<>();
        for (Group group : groups) {
            if (!names.add(group.getName())) {
                throw new IllegalArgumentException("分组名称重复: " + group.getName());
            }
        }
    }
    
    public void start() {
        // 允许从 CREATED 或 STOPPED 状态启动
        if (status != ExperimentStatus.CREATED && status != ExperimentStatus.STOPPED) {
            throw new IllegalStateException("实验状态不允许启动，当前状态: " + status);
        }
        this.status = ExperimentStatus.RUNNING;
    }
    
    public void stop() {
        if (status != ExperimentStatus.RUNNING) {
            throw new IllegalStateException("实验状态不允许停止，当前状态: " + status);
        }
        this.status = ExperimentStatus.STOPPED;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public List<Group> getGroups() { return groups; }
    public TagSchema getTagSchema() { return tagSchema; }
    public ExperimentStatus getStatus() { return status; }
    public int getGroupCount() { return groups.size(); }
    
    public Group getGroup(String name) {
        return groups.stream()
            .filter(g -> g.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    public Group getGroup(int index) {
        if (index < 0 || index >= groups.size()) {
            throw new IndexOutOfBoundsException("分组索引超出范围: " + index);
        }
        return groups.get(index);
    }
    
    /**
     * 根据分组名称获取索引
     */
    public int getGroupIndex(String groupName) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getName().equals(groupName)) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public String toString() {
        return String.format("Experiment{id='%s', name='%s', groups=%d, status=%s}", 
            id, name, groups.size(), status);
    }
}
