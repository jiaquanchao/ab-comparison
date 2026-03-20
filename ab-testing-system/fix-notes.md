# RealWorldTestController 修复指南

## 问题
所有 `new HashMap<>() { put(...);put(...); }` 语法在 Java 8 中无效

## 正确语法
```java
// 错误 ❌
return new HashMap<String, Object>() { 
    put("key1", value1); 
    put("key2", value2); 
};

// 正确 ✅
Map<String, Object> result = new HashMap<>();
result.put("key1", value1);
result.put("key2", value2);
return result;
```

## 需要修复的位置
1. 第 38 行：testAgeGroupActivity 返回
2. 第 91 行：testAgeGroupActivity 返回
3. 第 141 行：testNewUsersActivity 返回
4. 第 189 行：testAgeGroupActivity 辅助方法返回
5. 第 227 行：testNewUsersActivity 辅助方法返回
