package com.legal.legislation.service;

import com.legal.legislation.entity.LegislativeProject;

import java.util.Map;

public interface LegislativeProjectService {
    Task<LegislativeProject> create(LegislativeProject project);

    Task<Map<String, Object>> getDetail(Long id);

    Task<?> list(int page, int size, String status, String projectType);

    Task<?> dashboard();

    Task<Boolean> update(Long id, LegislativeProject project);

    Task<Boolean> delete(Long id);
}