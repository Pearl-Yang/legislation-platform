package com.legal.legislation.graph;

import com.legal.legislation.entity.Regulation;
import com.legal.legislation.entity.RegulationRelation;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.mapper.RegulationRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动期法规同步器
 *
 * 项目启动后,把 MySQL `regulation` + `regulation_relation` 全量同步到 Neo4j。
 *
 * 触发条件:Neo4jConfig 启用了 Driver Bean(即 legislation.neo4j.enabled=true)
 * 不可用时直接 skip。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Neo4jStartupSync {

    private final RegulationMapper regulationMapper;
    private final RegulationRelationMapper relationMapper;
    private final ObjectProvider<Driver> driverProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        Driver driver = driverProvider.getIfAvailable();
        if (driver == null) {
            log.info("[Neo4jStartupSync] driver bean not available,skip startup sync");
            return;
        }
        try {
            doSync(driver);
        } catch (Exception e) {
            log.warn("[Neo4jStartupSync] failed ({}),continue without blocking app", e.getMessage());
        }
    }

    private void doSync(Driver driver) {
        log.info("[Neo4jStartupSync] start syncing MySQL -> Neo4j");
        try (Session session = driver.session()) {
            session.run("CREATE CONSTRAINT regulation_id_unique IF NOT EXISTS "
                    + "FOR (r:Regulation) REQUIRE r.id IS UNIQUE");
            session.run("MATCH (n:Regulation) DETACH DELETE n");

            List<Regulation> regulations = regulationMapper.selectList(null);
            int nodeCount = 0;
            if (regulations != null) {
                for (Regulation reg : regulations) {
                    session.run(
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
                    nodeCount++;
                }
            }

            List<RegulationRelation> relations = relationMapper.selectList(null);
            int edgeCount = 0;
            if (relations != null) {
                for (RegulationRelation rel : relations) {
                    String relType = sanitize(rel.getRelationType());
                    String cypher = "MATCH (a:Regulation {id: $sid}), (b:Regulation {id: $tid}) "
                                  + "MERGE (a)-[r:" + relType + "]->(b) "
                                  + "SET r.description = $desc";
                    session.run(cypher, org.neo4j.driver.Values.parameters(
                            "sid", rel.getSourceId(),
                            "tid", rel.getTargetId(),
                            "desc", rel.getDescription()));
                    edgeCount++;
                }
            }

            log.info("[Neo4jStartupSync] synced nodes={}, edges={}", nodeCount, edgeCount);
        }
    }

    private String sanitize(String type) {
        if (type == null || type.isBlank()) return "RELATED";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (char c : type.toUpperCase().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                if (first && !Character.isLetter(c) && c != '_') sb.append('R');
                sb.append(c);
                first = false;
            }
        }
        return sb.length() == 0 ? "RELATED" : sb.toString();
    }
}