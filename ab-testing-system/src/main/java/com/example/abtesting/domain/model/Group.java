package com.example.abtesting.domain.model;

/**
 * 分组实体
 * 
 * DDD设计：分组是实验聚合内的实体
 */
public class Group {
    private final String name;
    private final double targetRatio; // 目标比例（0-1）
    private final String description;
    
    public Group(String name, double targetRatio, String description) {
        this.name = name;
        this.targetRatio = targetRatio;
        this.description = description;
        
        validate();
    }
    
    private void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }
        if (targetRatio < 0 || targetRatio > 1) {
            throw new IllegalArgumentException(
                String.format("目标比例必须在0-1之间，当前为: %.2f", targetRatio)
            );
        }
    }
    
    public String getName() { return name; }
    public double getTargetRatio() { return targetRatio; }
    public String getDescription() { return description; }
    
    /**
     * 获取目标百分比
     */
    public double getTargetPercentage() {
        return targetRatio * 100;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return name.equals(group.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Group{name='%s', target=%.1f%%}", name, getTargetPercentage());
    }
}
