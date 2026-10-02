package com.legal.legislation.service;

import com.legal.legislation.entity.Consultation;
import com.legal.legislation.entity.Opinion;
import com.legal.legislation.entity.OpinionReply;

import java.util.List;
import java.util.Map;

public interface ConsultationService {

    Task<?> list(int page, int size, String status);

    Task<Consultation> create(Consultation consultation);

    Task<Map<String, Object>> getDetail(Long id);

    Task<Boolean> update(Long id, Consultation consultation);

    Task<Opinion> submitOpinion(Long consultationId, Opinion opinion);

    Task<?> listOpinions(Long consultationId, int page, int size, String status, String category);

    Task<Map<String, Object>> getStatistics(Long consultationId);

    Task<Integer> classify(Long consultationId);

    Task<Integer> dedup(Long consultationId);

    /** 该征集的"关键词词云"统计(Top 50 词 + 频次) */
    Task<List<Map<String, Object>>> wordCloud(Long consultationId, int topN);

    Task<Map<String, Object>> exportReport(Long consultationId);

    Task<OpinionReply> replyOpinion(Long opinionId, String content, Long replyBy);
}