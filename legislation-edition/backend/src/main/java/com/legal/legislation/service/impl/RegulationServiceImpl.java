package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.legislation.entity.Regulation;
import com.legal.legislation.entity.RegulationRelation;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.mapper.RegulationRelationMapper;
import com.legal.legislation.service.RegulationService;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulationServiceImpl implements RegulationService {

    private final RegulationMapper         regulationMapper;
    private final RegulationRelationMapper relationMapper;

    @Override
    public Task<?> list(int page, int size, String regulationType, String status, String regionCode) {
        QueryWrapper<Regulation> qw = new QueryWrapper<>();
        if (regulationType != null) qw.eq("regulation_type", regulationType);
        if (status         != null) qw.eq("status",          status);
        if (regionCode     != null) qw.eq("region_code",     regionCode);
        qw.orderByDesc("updated_at");
        Page<Regulation> p = regulationMapper.selectPage(new Page<>(page, size), qw);
        return Task.ok(p);
    }

    @Override
    public Task<?> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            return Task.error("keyword 必填");
        }
        QueryWrapper<Regulation> qw = new QueryWrapper<>();
        qw.and(w -> w.like("regulation_name", keyword)
                     .or().like("digest",       keyword)
                     .or().like("full_text",    keyword));
        qw.orderByDesc("updated_at");
        Page<Regulation> p = regulationMapper.selectPage(new Page<>(page, size), qw);
        return Task.ok(p);
    }

    @Override
    public Task<Regulation> getDetail(Long id) {
        Regulation r = regulationMapper.selectById(id);
        if (r == null) return Task.error("法规不存在");
        return Task.ok(r);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> upsert(Regulation reg) {
        if (reg.getRegulationName() == null || reg.getRegulationName().isBlank()) {
            return Task.error("法规名称不能为空");
        }
        if (reg.getRegulationType() == null) {
            return Task.error("法规类型不能为空");
        }
        if (reg.getStatus() == null) reg.setStatus(Regulation.STATUS_EFFECTIVE);
        if (reg.getIsFromCrawler() == null) reg.setIsFromCrawler(0);

        boolean ok;
        if (reg.getId() == null) {
            reg.setCreatedAt(LocalDateTime.now());
            reg.setUpdatedAt(LocalDateTime.now());
            regulationMapper.insert(reg);
            ok = true;
        } else {
            reg.setUpdatedAt(LocalDateTime.now());
            ok = regulationMapper.updateById(reg) > 0;
        }
        return Task.ok(ok);
    }

    @Override
    public Task<Map<String, Object>> getRelations(Long id, int depth) {
        if (depth < 1) depth = 1;
        if (depth > 3) depth = 3;
        Regulation r = regulationMapper.selectById(id);
        if (r == null) return Task.error("法规不存在");

        // 收集 source / target 双向关系
        QueryWrapper<RegulationRelation> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("source_id", id).or().eq("target_id", id));
        List<RegulationRelation> rels = relationMapper.selectList(qw);

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        nodes.add(toNode(r));

        if (!rels.isEmpty()) {
            List<Long> neighborIds = new ArrayList<>();
            for (RegulationRelation rel : rels) {
                Long otherId = rel.getSourceId().equals(id) ? rel.getTargetId() : rel.getSourceId();
                if (!neighborIds.contains(otherId)) neighborIds.add(otherId);

                Map<String, Object> edge = new HashMap<>();
                edge.put("source", rel.getSourceId());
                edge.put("target", rel.getTargetId());
                edge.put("type",   rel.getRelationType());
                edge.put("article", rel.getRelatedArticle());
                edges.add(edge);
            }
            for (Regulation n : regulationMapper.selectBatchIds(neighborIds)) {
                nodes.add(toNode(n));
            }
        }

        Map<String, Object> graph = new HashMap<>();
        graph.put("regulationId", id);
        graph.put("depth",        depth);
        graph.put("nodes",        nodes);
        graph.put("edges",        edges);
        return Task.ok(graph);
    }

    private Map<String, Object> toNode(Regulation r) {
        Map<String, Object> node = new HashMap<>();
        node.put("id",   r.getId());
        node.put("name", r.getRegulationName());
        node.put("type", r.getRegulationType());
        node.put("status", r.getStatus());
        return node;
    }
}