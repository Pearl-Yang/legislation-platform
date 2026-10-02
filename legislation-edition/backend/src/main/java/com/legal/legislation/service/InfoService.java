package com.legal.legislation.service;

import java.util.List;
import java.util.Map;

public interface InfoService {

    Task<List<Map<String, Object>>> news(String category, int page, int size);

    Task<Map<String, Object>> newsDetail(Long id);

    Task<List<Map<String, Object>>> regulationIndex(String domain, String regionCode);

    Task<List<Map<String, Object>>> policyInterpretations();

    Task<List<Map<String, Object>>> academicLiterature();

    Task<List<Map<String, Object>>> bulletin();

    Task<Map<String, Object>> dashboard();

    Task<Map<String, Object>> dashboardChart();

    Task<Long> subscribe(Map<String, Object> body);

    Task<Boolean> unsubscribe(Long id);

    Task<List<Map<String, Object>>> subscriptions(Long userId);

    Task<List<Map<String, Object>>> recommend(Long materialId);

    /** 各省法规地图分布(供 ECharts 地图) */
    Task<List<Map<String, Object>>> regulationMap();
}