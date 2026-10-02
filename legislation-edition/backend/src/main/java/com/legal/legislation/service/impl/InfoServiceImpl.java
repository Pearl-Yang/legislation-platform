package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.legislation.entity.LibraryMaterial;
import com.legal.legislation.entity.LegislativeProject;
import com.legal.legislation.entity.Regulation;
import com.legal.legislation.mapper.LegislativeProjectMapper;
import com.legal.legislation.mapper.LibraryMaterialMapper;
import com.legal.legislation.mapper.RegulationMapper;
import com.legal.legislation.service.InfoService;
import com.legal.legislation.service.LegislativeFlowService;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 信息展示 Service
 *
 * 数据源 = regulation + library_material + legislative_project
 * 没有专门的"动态"表,所以从 library_material(EXPERT_OPINION/REPORT/CASE)
 * 加上 regulation 派生"立法动态"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfoServiceImpl implements InfoService {

    private final LibraryMaterialMapper materialMapper;
    private final RegulationMapper     regulationMapper;
    private final LegislativeProjectMapper projectMapper;
    private final LegislativeFlowService flowService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Task<List<Map<String, Object>>> news(String category, int page, int size) {
        // 立法动态 = EXPERT_OPINION + REPORT + REGULATION 类型资料按发布时间倒序
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        if (category == null || category.isBlank()) {
            qw.in("material_type", LibraryMaterial.TYPE_REGULATION, LibraryMaterial.TYPE_REPORT,
                  LibraryMaterial.TYPE_EXPERT_OPINION, LibraryMaterial.TYPE_CASE);
        } else {
            switch (category) {
                case "REGULATION_NEW" -> qw.eq("material_type", LibraryMaterial.TYPE_REGULATION);
                case "INTERPRET"      -> qw.eq("material_type", LibraryMaterial.TYPE_REPORT);
                case "CASE"           -> qw.eq("material_type", LibraryMaterial.TYPE_CASE);
                case "LITERATURE"     -> qw.eq("material_type", LibraryMaterial.TYPE_EXPERT_OPINION);
                case "BULLETIN"       -> qw.in("material_type", LibraryMaterial.TYPE_REPORT, LibraryMaterial.TYPE_REGULATION);
                default               -> qw.eq("material_type", category);
            }
        }
        qw.orderByDesc("issue_date").orderByDesc("created_at");
        List<LibraryMaterial> list0 = materialMapper.selectPage(new Page<>(page, size), qw).getRecords();
        List<Map<String, Object>> result = new ArrayList<>();
        for (LibraryMaterial m : list0) result.add(toNewsItem(m, category));
        return Task.ok(result);
    }

    @Override
    public Task<Map<String, Object>> newsDetail(Long id) {
        LibraryMaterial m = materialMapper.selectById(id);
        if (m == null) return Task.error("动态不存在");
        Map<String, Object> data = toNewsItem(m, null);
        data.put("fullText", m.getFullText());
        data.put("keywords", m.getKeywords());
        return Task.ok(data);
    }

    @Override
    @Cacheable(cacheNames = "regulation:index", key = "#domain + ':' + (#regionCode ?: 'ALL')")
    public Task<List<Map<String, Object>>> regulationIndex(String domain, String regionCode) {
        QueryWrapper<Regulation> qw = new QueryWrapper<>();
        if (regionCode != null) qw.eq("region_code", regionCode);
        qw.eq("status", Regulation.STATUS_EFFECTIVE).orderByDesc("issue_date").last("LIMIT 100");
        List<Regulation> regs = regulationMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Regulation r : regs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id",       r.getId());
            item.put("name",     r.getRegulationName());
            item.put("type",     r.getRegulationType());
            item.put("authority",r.getIssuingAuthority());
            item.put("issueDate",r.getIssueDate());
            item.put("domain",   domain);
            result.add(item);
        }
        return Task.ok(result);
    }

    @Override
    public Task<List<Map<String, Object>>> policyInterpretations() {
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        qw.eq("material_type", LibraryMaterial.TYPE_REPORT)
          .orderByDesc("issue_date").last("LIMIT 50");
        List<LibraryMaterial> list0 = libraryMapper().selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LibraryMaterial m : list0) result.add(toNewsItem(m, "INTERPRET"));
        return Task.ok(result);
    }

    @Override
    public Task<List<Map<String, Object>>> academicLiterature() {
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        qw.eq("material_type", LibraryMaterial.TYPE_EXPERT_OPINION)
          .orderByDesc("issue_date").last("LIMIT 50");
        List<LibraryMaterial> list0 = libraryMapper().selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LibraryMaterial m : list0) result.add(toNewsItem(m, "LITERATURE"));
        return Task.ok(result);
    }

    @Override
    public Task<List<Map<String, Object>>> bulletin() {
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        qw.eq("material_type", LibraryMaterial.TYPE_REPORT)
          .orderByDesc("created_at").last("LIMIT 30");
        List<LibraryMaterial> list0 = libraryMapper().selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LibraryMaterial m : list0) result.add(toNewsItem(m, "BULLETIN"));
        return Task.ok(result);
    }

    @Override
    @Cacheable(cacheNames = "info:dashboard", key = "'global'")
    public Task<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        // 立法项目规模
        long totalProjects  = projectMapper.selectCount(null);
        long activeProjects = projectMapper.selectCount(
                new QueryWrapper<LegislativeProject>().eq("status", LegislativeProject.STATUS_ACTIVE));
        long publishedProjects = projectMapper.selectCount(
                new QueryWrapper<LegislativeProject>().eq("status", LegislativeProject.STATUS_PUBLISHED));
        Map<String, Object> projBoard = new HashMap<>();
        projBoard.put("total",     totalProjects);
        projBoard.put("active",    activeProjects);
        projBoard.put("published", publishedProjects);
        data.put("project", projBoard);
        // 法规数量
        data.put("regulationCount", regulationMapper.selectCount(null));
        // 资料数量
        data.put("materialCount", materialMapper.selectCount(null));
        // 即将到期(30 天)
        List<?> upcoming = flowService.getUpcomingAcrossProjects(30);
        data.put("upcomingDeadlines", upcoming.size());
        // 法规类型分布
        Map<String, Long> regTypeDist = new HashMap<>();
        for (String t : new String[]{"ADMIN_REGULATION", "DEPT_RULE", "LOCAL_RULE"}) {
            regTypeDist.put(t, regulationMapper.selectCount(
                new QueryWrapper<Regulation>().eq("regulation_type", t)));
        }
        data.put("regulationTypeDistribution", regTypeDist);
        // 法规状态分布
        Map<String, Long> regStatusDist = new HashMap<>();
        for (String s : new String[]{"EFFECTIVE", "REVISING", "OBSOLETE"}) {
            regStatusDist.put(s, regulationMapper.selectCount(
                new QueryWrapper<Regulation>().eq("status", s)));
        }
        data.put("regulationStatusDistribution", regStatusDist);
        // 近 30 天新增法规
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        long recentRegs = regulationMapper.selectCount(
            new QueryWrapper<Regulation>().ge("created_at", monthAgo));
        data.put("recentRegulationCount", recentRegs);
        return Task.ok(data);
    }

    /** 地图分布:省级 region_code -> 法规数量(供 ECharts 地图) */
    public Task<List<Map<String, Object>>> regulationMap() {
        QueryWrapper<Regulation> qw = new QueryWrapper<>();
        qw.isNotNull("region_code").ne("region_code", "000000")
          .select("region_code", "COUNT(*) AS cnt")
          .groupBy("region_code");
        // 走 native SQL(因为要 group by + count)
        List<Map<String, Object>> rows = regulationMapper.selectMaps(qw);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("name",  regionNameOf((String) r.get("region_code")));
            item.put("value", r.get("cnt"));
            item.put("code",  r.get("region_code"));
            out.add(item);
        }
        return Task.ok(out);
    }

    private String regionNameOf(String code) {
        if (code == null) return "未知";
        return switch (code) {
            case "110000" -> "北京市";
            case "120000" -> "天津市";
            case "130000" -> "河北省";
            case "140000" -> "山西省";
            case "150000" -> "内蒙古自治区";
            case "210000" -> "辽宁省";
            case "220000" -> "吉林省";
            case "230000" -> "黑龙江省";
            case "310000" -> "上海市";
            case "320000" -> "江苏省";
            case "330000" -> "浙江省";
            case "340000" -> "安徽省";
            case "350000" -> "福建省";
            case "360000" -> "江西省";
            case "370000" -> "山东省";
            case "410000" -> "河南省";
            case "420000" -> "湖北省";
            case "430000" -> "湖南省";
            case "440000" -> "广东省";
            case "450000" -> "广西壮族自治区";
            case "460000" -> "海南省";
            case "500000" -> "重庆市";
            case "510000" -> "四川省";
            case "520000" -> "贵州省";
            case "530000" -> "云南省";
            case "540000" -> "西藏自治区";
            case "610000" -> "陕西省";
            case "620000" -> "甘肃省";
            case "630000" -> "青海省";
            case "640000" -> "宁夏回族自治区";
            case "650000" -> "新疆维吾尔自治区";
            default -> code;
        };
    }

    private Task<?> aggregateProjectStats() {
        return Task.ok(new HashMap<>());
    }

    private long projectCount() {
        return projectMapper.selectCount(null);
    }

    @Override
    public Task<Map<String, Object>> dashboardChart() {
        // 折线图：近 6 个月每月发布的法规数 + 每月新增资料数
        QueryWrapper<Regulation> regQw = new QueryWrapper<>();
        regQw.orderByDesc("issue_date").last("LIMIT 200");
        List<Regulation> regs = regulationMapper.selectList(regQw);
        Map<String, Long> regByMonth = new HashMap<>();
        for (Regulation r : regs) {
            String month = r.getIssueDate() == null ? "null" : r.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            regByMonth.merge(month, 1L, Long::sum);
        }
        QueryWrapper<LibraryMaterial> matQw = new QueryWrapper<>();
        matQw.orderByDesc("created_at").last("LIMIT 200");
        List<LibraryMaterial> mats = materialMapper.selectList(matQw);
        Map<String, Long> matByMonth = new HashMap<>();
        for (LibraryMaterial m : mats) {
            String month = m.getCreatedAt() == null ? "null" : m.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            matByMonth.merge(month, 1L, Long::sum);
        }
        List<String> months = new ArrayList<>();
        months.addAll(regByMonth.keySet());
        months.addAll(matByMonth.keySet());
        months.sort(String::compareTo);
        Map<String, Object> data = new HashMap<>();
        data.put("months", months);
        data.put("regulations", regByMonth);
        data.put("materials",   matByMonth);
        return Task.ok(data);
    }

    @Override
    public Task<Long> subscribe(Map<String, Object> body) {
        // 占位:返回伪 ID
        return Task.ok(System.currentTimeMillis());
    }

    @Override
    public Task<Boolean> unsubscribe(Long id) {
        return Task.ok(true);
    }

    @Override
    public Task<List<Map<String, Object>>> subscriptions(Long userId) {
        return Task.ok(new ArrayList<>());
    }

    @Override
    public Task<List<Map<String, Object>>> recommend(Long materialId) {
        LibraryMaterial m = materialMapper.selectById(materialId);
        if (m == null) return Task.error("资料不存在");
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        qw.eq("material_type", m.getMaterialType())
          .ne("id", materialId)
          .orderByDesc("view_count").last("LIMIT 10");
        List<LibraryMaterial> recs = materialMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LibraryMaterial r : recs) result.add(toNewsItem(r, null));
        return Task.ok(result);
    }

    // ---------- helpers ----------

    private Map<String, Object> toNewsItem(LibraryMaterial m, String category) {
        Map<String, Object> item = new HashMap<>();
        item.put("id",        m.getId());
        item.put("title",     m.getTitle());
        item.put("type",      m.getMaterialType());
        item.put("category",  category);
        item.put("authority", m.getIssuingAuthority());
        item.put("digest",    m.getDigest());
        item.put("fileUrl",   m.getFileUrl());
        item.put("issueDate", m.getIssueDate());
        item.put("region",    m.getRegionCode());
        return item;
    }

    // 用 lambda 解决 field 引用
    private LibraryMaterialMapper libraryMapper() { return materialMapper; }
}