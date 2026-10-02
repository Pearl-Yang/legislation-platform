package com.legal.legislation.graph;

import com.legal.legislation.entity.Regulation;
import com.legal.legislation.entity.RegulationRelation;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 法规知识图谱服务 - 双模式实现
 *
 *  1) Neo4j Driver 可用时:写入/读取都走 Neo4j(Bolt + Cypher)
 *  2) 不可用时:降级到内存 Map(用于本地开发 / 集成测试)
 *
 * 切换通过 application.yml legislation.neo4j.enabled 控制;
 * Driver Bean 由 Neo4jConfig 提供(@ConditionalOnProperty 保护)。
 */
@Slf4j
@Service
public class RegulationGraphServiceImpl implements RegulationGraphService {

    private final Driver driver;   // 可能为 null(未启用 Neo4j)
    private final Map<Long, Regulation> regulationNodes = new HashMap<>();
    private final Map<String, RegulationRelation> relationEdges = new HashMap<>();

    public RegulationGraphServiceImpl(ObjectProvider<Driver> driverProvider) {
        this.driver = driverProvider.getIfAvailable();
        if (this.driver != null) {
            log.info("[Graph] Neo4j Driver injected,graph operations will hit real Neo4j");
        } else {
            log.info("[Graph] Neo4j Driver NOT found,fallback to in-memory map");
        }
    }

    private boolean useNeo4j() { return driver != null; }

    @Override
    public void upsertRegulation(Regulation reg) {
        regulationNodes.put(reg.getId(), reg);
        if (!useNeo4j()) return;
        try (Session s = driver.session()) {
            s.run(
                "MERGE (r:Regulation {id: $id}) "
              + "SET r.name   = $name, r.type = $type, r.status = $status, "
              + "    r.region = $region, r.issuingAuthority = $issuingAuthority",
                org.neo4j.driver.Values.parameters(
                    "id", reg.getId(),
                    "name", reg.getRegulationName(),
                    "type", reg.getRegulationType(),
                    "status", reg.getStatus(),
                    "region", reg.getRegionCode(),
                    "issuingAuthority", reg.getIssuingAuthority()));
        } catch (Exception e) {
            log.warn("[Graph] upsertRegulation neo4j write failed: {}", e.getMessage());
        }
    }

    @Override
    public void upsertRelation(RegulationRelation rel) {
        String key = rel.getSourceId() + "-" + rel.getTargetId() + "-" + rel.getRelationType();
        relationEdges.put(key, rel);
        if (!useNeo4j()) return;
        try (Session s = driver.session()) {
            String type = sanitize(rel.getRelationType());
            s.run(
                "MATCH (a:Regulation {id: $sid}), (b:Regulation {id: $tid}) "
              + "MERGE (a)-[r:" + type + "]->(b) "
              + "SET r.description = $desc",
                org.neo4j.driver.Values.parameters(
                    "sid", rel.getSourceId(),
                    "tid", rel.getTargetId(),
                    "desc", rel.getDescription()));
        } catch (Exception e) {
            log.warn("[Graph] upsertRelation neo4j write failed: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getSubgraph(Long rootRegulationId, int depth) {
        if (depth < 1) depth = 1;
        if (depth > 3) depth = 3;

        // Neo4j 模式:直接 Cypher
        if (useNeo4j()) {
            try {
                return querySubgraphFromNeo4j(rootRegulationId, depth);
            } catch (Exception e) {
                log.warn("[Graph] Neo4 query failed,fallback to in-memory: {}", e.getMessage());
            }
        }

        // 内存模式:BFS
        Map<String, Object> graph = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Queue<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(rootRegulationId);
        visited.add(rootRegulationId);
        int level = 0;
        while (!queue.isEmpty() && level < depth) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                Long cur = queue.poll();
                Regulation reg = regulationNodes.get(cur);
                if (reg != null) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("id", reg.getId());
                    n.put("name", reg.getRegulationName());
                    n.put("type", reg.getRegulationType());
                    nodes.add(n);
                }
                for (RegulationRelation edge : relationEdges.values()) {
                    if (edge.getSourceId().equals(cur) && !visited.contains(edge.getTargetId())) {
                        visited.add(edge.getTargetId());
                        queue.add(edge.getTargetId());
                        Map<String, Object> e = new HashMap<>();
                        e.put("source", edge.getSourceId());
                        e.put("target", edge.getTargetId());
                        e.put("type", edge.getRelationType());
                        edges.add(e);
                    }
                }
            }
            level++;
        }
        graph.put("rootId", rootRegulationId);
        graph.put("depth", depth);
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    private Map<String, Object> querySubgraphFromNeo4j(Long root, int depth) {
        Map<String, Object> graph = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        try (Session s = driver.session()) {
            var nodeRes = s.run(
                "MATCH p = (root:Regulation {id: $rootId})-[*1.." + depth + "]-(n:Regulation) "
              + "RETURN DISTINCT n",
                org.neo4j.driver.Values.parameters("rootId", root));
            while (nodeRes.hasNext()) {
                var node = nodeRes.next().get("n").asNode();
                Map<String, Object> n = new HashMap<>();
                n.put("id", node.get("id").asLong());
                n.put("name", node.get("name").asString());
                n.put("type", node.get("type").asString());
                nodes.add(n);
            }
            var edgeRes = s.run(
                "MATCH (a:Regulation)-[r]->(b:Regulation) "
              + "WHERE a.id = $rootId OR b.id = $rootId OR "
              + "      (a)-[*1.." + (depth - 1) + "]-(:Regulation {id: $rootId}) OR "
              + "      (b)-[*1.." + (depth - 1) + "]-(:Regulation {id: $rootId}) "
              + "RETURN a.id AS sid, b.id AS tid, type(r) AS type, r.description AS desc",
                org.neo4j.driver.Values.parameters("rootId", root));
            while (edgeRes.hasNext()) {
                var rec = edgeRes.next();
                Map<String, Object> e = new HashMap<>();
                e.put("source", rec.get("sid").asLong());
                e.put("target", rec.get("tid").asLong());
                e.put("type", rec.get("type").asString());
                edges.add(e);
            }
        }
        graph.put("rootId", root);
        graph.put("depth", depth);
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    @Override
    public List<Long> findImpactedRegulations(Long superiorId) {
        List<Long> result = new ArrayList<>();
        if (useNeo4j()) {
            try (Session s = driver.session()) {
                var rs = s.run(
                    "MATCH (a:Regulation {id: $id})-[*1..2]-(b:Regulation) "
                  + "WHERE b.id <> $id "
                  + "RETURN DISTINCT b.id AS id",
                    org.neo4j.driver.Values.parameters("id", superiorId));
                while (rs.hasNext()) result.add(rs.next().get("id").asLong());
                return result;
            } catch (Exception e) {
                log.warn("[Graph] findImpactedRegulations neo4j failed: {}", e.getMessage());
            }
        }
        for (RegulationRelation edge : relationEdges.values()) {
            if (edge.getSourceId().equals(superiorId)) result.add(edge.getTargetId());
            if (edge.getTargetId().equals(superiorId)) result.add(edge.getSourceId());
        }
        return result;
    }

    @Override
    public void deleteRegulation(Long regulationId) {
        regulationNodes.remove(regulationId);
        relationEdges.entrySet().removeIf(e ->
                e.getValue().getSourceId().equals(regulationId) ||
                e.getValue().getTargetId().equals(regulationId));
        if (!useNeo4j()) return;
        try (Session s = driver.session()) {
            s.run("MATCH (r:Regulation {id: $id}) DETACH DELETE r",
                org.neo4j.driver.Values.parameters("id", regulationId));
        } catch (Exception e) {
            log.warn("[Graph] deleteRegulation neo4j failed: {}", e.getMessage());
        }
    }

    private String sanitize(String t) {
        if (t == null || t.isBlank()) return "RELATED";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (char c : t.toUpperCase().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                if (first && !Character.isLetter(c) && c != '_') sb.append('R');
                sb.append(c); first = false;
            }
        }
        return sb.length() == 0 ? "RELATED" : sb.toString();
    }
}