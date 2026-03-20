package com.example.abtesting.model;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 标签维度定义
 * 定义5个有意义的标签维度及其原始客群比例
 */
@Component
public class TagSchema {
    
    private final List<TagDimension> dimensions;
    
    public TagSchema() {
        dimensions = new ArrayList<>();
        
        // 1. 性别（2个枚举）
        Map<String, Double> genderValues = new LinkedHashMap<>();
        genderValues.put("男", 55.0);
        genderValues.put("女", 45.0);
        dimensions.add(new TagDimension("性别", genderValues));
        
        // 2. 省份（7个枚举）
        Map<String, Double> provinceValues = new LinkedHashMap<>();
        provinceValues.put("广东", 20.0);
        provinceValues.put("山东", 18.0);
        provinceValues.put("江苏", 15.0);
        provinceValues.put("浙江", 14.0);
        provinceValues.put("河南", 12.0);
        provinceValues.put("四川", 11.0);
        provinceValues.put("北京", 10.0);
        dimensions.add(new TagDimension("省份", provinceValues));
        
        // 3. 交易笔数（8个枚举）
        Map<String, Double> tradeValues = new LinkedHashMap<>();
        tradeValues.put("10笔以下", 20.0);
        tradeValues.put("10-20笔", 25.0);
        tradeValues.put("20-30笔", 20.0);
        tradeValues.put("30-40笔", 15.0);
        tradeValues.put("40-50笔", 8.0);
        tradeValues.put("50-60笔", 6.0);
        tradeValues.put("60-70笔", 4.0);
        tradeValues.put("70笔以上", 2.0);
        dimensions.add(new TagDimension("交易笔数", tradeValues));
        
        // 4. 年龄段（5个枚举）
        Map<String, Double> ageValues = new LinkedHashMap<>();
        ageValues.put("18岁以下", 15.0);
        ageValues.put("18-25岁", 20.0);
        ageValues.put("26-35岁", 30.0);
        ageValues.put("36-45岁", 20.0);
        ageValues.put("45岁以上", 15.0);
        dimensions.add(new TagDimension("年龄段", ageValues));
        
        // 5. 会员等级（5个枚举）
        Map<String, Double> levelValues = new LinkedHashMap<>();
        levelValues.put("普通会员", 40.0);
        levelValues.put("银牌会员", 25.0);
        levelValues.put("金牌会员", 20.0);
        levelValues.put("白金会员", 10.0);
        levelValues.put("钻石会员", 5.0);
        dimensions.add(new TagDimension("会员等级", levelValues));
    }
    
    public List<TagDimension> getDimensions() {
        return dimensions;
    }
    
    public TagDimension getDimension(int index) {
        return dimensions.get(index);
    }
    
    public int getDimensionCount() {
        return dimensions.size();
    }
    
    /**
     * 生成标签组合键（用于Redis存储）
     * 格式：性别_省份_交易笔数_年龄段_会员等级
     * 例如：0_1_2_3_4 表示 "男_山东_10-20笔_18-25岁_银牌会员"
     */
    public String getComboKey(int[] tagIndexes) {
        if (tagIndexes.length != dimensions.size()) {
            throw new IllegalArgumentException("标签数量必须为" + dimensions.size());
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tagIndexes.length; i++) {
            if (i > 0) sb.append("_");
            sb.append(tagIndexes[i]);
        }
        return sb.toString();
    }
    
    /**
     * 获取标签组合的详细描述
     * 例如：性别=男, 省份=山东, 交易笔数=10-20笔, 年龄段=18-25岁, 会员等级=银牌会员
     */
    public String getComboKeyDescription(int[] tagIndexes) {
        if (tagIndexes.length != dimensions.size()) {
            throw new IllegalArgumentException("标签数量必须为" + dimensions.size());
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tagIndexes.length; i++) {
            if (i > 0) sb.append(", ");
            TagDimension dim = dimensions.get(i);
            sb.append(dim.getName()).append("=").append(dim.getValueName(tagIndexes[i]));
        }
        return sb.toString();
    }
    
    /**
     * 获取所有标签维度的摘要信息（用于前端展示）
     */
    public Map<String, Object> getSchemaInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("dimensionCount", dimensions.size());
        
        List<Map<String, Object>> dims = new ArrayList<>();
        for (TagDimension dim : dimensions) {
            Map<String, Object> dimInfo = new LinkedHashMap<>();
            dimInfo.put("name", dim.getName());
            dimInfo.put("valueCount", dim.getValues().size());
            dimInfo.put("values", dim.getValues());
            dims.add(dimInfo);
        }
        info.put("dimensions", dims);
        
        // 计算总组合数
        long totalCombinations = dimensions.stream()
            .mapToLong(d -> d.getValues().size())
            .reduce(1, (a, b) -> a * b);
        info.put("totalCombinations", totalCombinations);
        
        return info;
    }
}
