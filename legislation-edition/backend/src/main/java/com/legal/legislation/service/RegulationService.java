package com.legal.legislation.service;

import com.legal.legislation.entity.Regulation;

import java.util.List;
import java.util.Map;

public interface RegulationService {

    Task<?> list(int page, int size, String regulationType, String status, String regionCode);

    /** 关键词全文检索：regulation_name + digest + full_text */
    Task<?> search(String keyword, int page, int size);

    Task<Regulation> getDetail(Long id);

    Task<Boolean> upsert(Regulation reg);

    /** 上下位 / 引用关系 */
    Task<Map<String, Object>> getRelations(Long id, int depth);
}