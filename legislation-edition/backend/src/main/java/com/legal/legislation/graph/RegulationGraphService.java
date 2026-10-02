package com.legal.legislation.graph;

import com.legal.legislation.entity.Regulation;
import com.legal.legislation.entity.RegulationRelation;

import java.util.List;
import java.util.Map;

/**
 * 法规知识图谱服务接口
 *
 * 实现要点（参照 KnowledgeGraphService）：
 *  - 同时写 MySQL（合规检索）和 Neo4j（图谱可视化）；
 *  - RegulationRelation 同步到 Neo4j 关系边；
 *  - 支持按 rootRegulationId 取子图（限深度）。
 */
public interface RegulationGraphService {

    /**
     * 同步单条法规到 Neo4j。
     */
    void upsertRegulation(Regulation regulation);

    /**
     * 同步一条法规关系边到 Neo4j。
     */
    void upsertRelation(RegulationRelation rel);

    /**
     * 查询某条法规的 N 度子图（节点 + 关系）。
     *
     * @param rootRegulationId 起点法规 ID
     * @param depth 深度限制，1..3
     */
    Map<String, Object> getSubgraph(Long rootRegulationId, int depth);

    /**
     * 找出所有受某条上位法规影响的下级法规（清理模块的核心入口）。
     */
    List<Long> findImpactedRegulations(Long superiorId);

    /**
     * 删除法规节点及其全部关系。
     */
    void deleteRegulation(Long regulationId);
}