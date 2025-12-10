package com.globalbuddy.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SemanticService {

    private static final Map<String, List<String>> SYNONYM_MAP;

    static {
        Map<String, List<String>> map = new HashMap<>();
        map.put("吃饭", Arrays.asList("吃饭", "用餐", "就餐", "餐馆", "餐饮", "烹饪", "饭堂"));
        map.put("租房", Arrays.asList("租房", "住宿", "公寓", "房源", "宿舍"));
        map.put("课程", Arrays.asList("课程", "课表", "课堂", "教学", "选课"));
        map.put("签证", Arrays.asList("签证", "移民", "入境", "海关", "居留证"));
        map.put("二手", Arrays.asList("二手", "闲置", "转卖", "交易"));
        SYNONYM_MAP = Collections.unmodifiableMap(map);
    }

    public double calculateScore(String query, String target) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(target)) {
            return 0.0;
        }
        List<String> queryTerms = expandTerms(tokenize(query));
        List<String> targetTerms = expandTerms(tokenize(target));
        if (queryTerms.isEmpty() || targetTerms.isEmpty()) {
            return 0.0;
        }
        Set<String> targetSet = new HashSet<>(targetTerms);
        int matches = 0;
        for (String term : queryTerms) {
            if (targetSet.contains(term)) {
                matches++;
            }
        }
        return queryTerms.isEmpty() ? 0.0 : (double) matches / queryTerms.size();
    }

    private List<String> tokenize(String text) {
        String normalized = text == null ? "" : text.toLowerCase();
        String[] parts = normalized.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (StringUtils.hasText(trimmed)) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private List<String> expandTerms(List<String> terms) {
        Set<String> expanded = new HashSet<>(terms);
        for (String term : terms) {
            for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
                if (entry.getValue().contains(term)) {
                    expanded.addAll(entry.getValue());
                }
            }
        }
        return new ArrayList<>(expanded);
    }
}

