package com.legal.legislation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.legal.legislation.entity.LibraryMaterial;
import com.legal.legislation.entity.LibraryTag;
import com.legal.legislation.entity.MaterialNote;
import com.legal.legislation.entity.MaterialTag;
import com.legal.legislation.mapper.LibraryMaterialMapper;
import com.legal.legislation.mapper.LibraryTagMapper;
import com.legal.legislation.mapper.MaterialNoteMapper;
import com.legal.legislation.mapper.MaterialTagMapper;
import com.legal.legislation.service.LibraryService;
import com.legal.legislation.service.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 立法资料库 Service
 *
 * 收藏当前实现：把 userId + materialId 组合写到 material_note 的 content 里做去重。
 * 后续可单独建 favorite 表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryServiceImpl implements LibraryService {

    private final LibraryMaterialMapper materialMapper;
    private final LibraryTagMapper      tagMapper;
    private final MaterialTagMapper     materialTagMapper;
    private final MaterialNoteMapper    noteMapper;

    @Override
    public Task<?> list(int page, int size, String materialType, String regionCode, String keyword) {
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        if (materialType != null) qw.eq("material_type", materialType);
        if (regionCode   != null) qw.eq("region_code",   regionCode);
        if (keyword      != null && !keyword.isBlank()) {
            qw.and(w -> w.like("title",     keyword)
                         .or().like("digest",   keyword)
                         .or().like("keywords", keyword));
        }
        qw.orderByDesc("created_at");
        return Task.ok(materialMapper.selectPage(new Page<>(page, size), qw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<LibraryMaterial> create(LibraryMaterial material) {
        if (material.getTitle() == null || material.getTitle().isBlank()) {
            return Task.error("资料标题不能为空");
        }
        if (material.getMaterialType() == null) {
            return Task.error("资料类型不能为空");
        }
        if (material.getReferenceCount() == null) material.setReferenceCount(0);
        if (material.getViewCount()      == null) material.setViewCount(0);
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.insert(material);
        return Task.ok(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<LibraryMaterial> update(Long id, LibraryMaterial material) {
        LibraryMaterial exist = materialMapper.selectById(id);
        if (exist == null) return Task.error("资料不存在");
        material.setId(id);
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);
        return Task.ok(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Boolean> delete(Long id) {
        return Task.ok(materialMapper.deleteById(id) > 0);
    }

    @Override
    public Task<?> search(String keyword, String materialType, int page, int size) {
        if (keyword == null || keyword.isBlank()) return Task.error("keyword 必填");
        // 优先用 FULLTEXT 索引(MATCH AGAINST,自然语言模式)
        // MySQL 8 + ngram parser 对中文友好
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        boolean useFullText = keyword.length() >= 2;
        if (useFullText) {
            // 用 native 查(只查询需要的列 + score 排序)
            try {
                List<LibraryMaterial> hits = materialMapper.selectList(
                    qw.last("WHERE MATCH(title, digest, full_text) AGAINST('" + escapeMysql(keyword) + "' IN NATURAL LANGUAGE MODE) "
                          + (materialType != null ? "AND material_type = '" + escapeMysql(materialType) + "' " : "")
                          + "ORDER BY MATCH(title, digest, full_text) AGAINST('" + escapeMysql(keyword) + "' IN NATURAL LANGUAGE MODE) DESC "
                          + "LIMIT " + size + " OFFSET " + (page - 1) * size)
                );
                Map<String, Object> result = new HashMap<>();
                result.put("records", hits);
                result.put("total",   hits.size());
                result.put("page",    page);
                result.put("size",    size);
                result.put("mode",    "FULLTEXT");
                return Task.ok(result);
            } catch (Exception ex) {
                // FULLTEXT 失败回退到 LIKE
                useFullText = false;
            }
        }
        // LIKE 兜底
        QueryWrapper<LibraryMaterial> qw2 = new QueryWrapper<>();
        qw2.and(w -> w.like("title",     keyword)
                     .or().like("digest",   keyword)
                     .or().like("keywords", keyword)
                     .or().like("full_text", keyword));
        if (materialType != null) qw2.eq("material_type", materialType);
        qw2.orderByDesc("updated_at");
        var pageResult = materialMapper.selectPage(new Page<>(page, size), qw2);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total",   pageResult.getTotal());
        result.put("page",    page);
        result.put("size",    size);
        result.put("mode",    "LIKE");
        return Task.ok(result);
    }

    /** MySQL FULLTEXT 防注入。 */
    private String escapeMysql(String s) {
        if (s == null) return "";
        return s.replace("'", "''").replace("\\", "\\\\");
    }

    @Override
    public Task<List<String>> suggest(String keyword, int limit) {
        if (keyword == null || keyword.isBlank() || limit <= 0) return Task.ok(List.of());
        // 简化:从 title 找含 keyword 前缀 / 子串的标题,取前 N 个
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        qw.select("DISTINCT title")
          .and(w -> w.likeLeft("title", keyword).or().like("title", keyword))
          .orderByDesc("view_count")
          .last("LIMIT " + Math.min(limit, 20));
        List<LibraryMaterial> rows = materialMapper.selectList(qw);
        List<String> out = new ArrayList<>();
        for (LibraryMaterial m : rows) {
            if (m.getTitle() != null && !out.contains(m.getTitle())) {
                out.add(m.getTitle());
            }
        }
        return Task.ok(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Map<String, Object>> detail(Long id) {
        LibraryMaterial m = materialMapper.selectById(id);
        if (m == null) return Task.error("资料不存在");
        // 浏览量 +1
        m.setViewCount((m.getViewCount() == null ? 0 : m.getViewCount()) + 1);
        materialMapper.updateById(m);

        // 标签
        QueryWrapper<MaterialTag> mtQw = new QueryWrapper<>();
        mtQw.eq("material_id", id);
        List<MaterialTag> mts = materialTagMapper.selectList(mtQw);
        List<Long> tagIds = new ArrayList<>();
        for (MaterialTag mt : mts) tagIds.add(mt.getTagId());
        List<LibraryTag> tags = tagIds.isEmpty() ? new ArrayList<>() : tagMapper.selectBatchIds(tagIds);

        Map<String, Object> data = new HashMap<>();
        data.put("material", m);
        data.put("tags",     tags);
        return Task.ok(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Integer> favorite(Long id, Long userId) {
        // 占位：以 "FAV:#userId#materialId" 写入 note 实现去重
        String marker = "FAV:" + userId + ":" + id;
        QueryWrapper<MaterialNote> qw = new QueryWrapper<>();
        qw.eq("material_id", id).eq("note_content", marker);
        if (noteMapper.selectCount(qw) > 0) {
            return Task.ok(0);
        }
        MaterialNote note = new MaterialNote();
        note.setMaterialId(id);
        note.setUserId(userId);
        note.setNoteContent(marker);
        note.setHighlightedText("");
        note.setCreatedAt(LocalDateTime.now());
        noteMapper.insert(note);
        return Task.ok(1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Integer> unFavorite(Long id, Long userId) {
        QueryWrapper<MaterialNote> qw = new QueryWrapper<>();
        qw.eq("material_id", id).eq("note_content", "FAV:" + userId + ":" + id);
        int rows = noteMapper.delete(qw);
        return Task.ok(rows);
    }

    @Override
    public Task<List<LibraryMaterial>> myFavorites(Long userId) {
        QueryWrapper<MaterialNote> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).like("note_content", "FAV:");
        List<MaterialNote> notes = noteMapper.selectList(qw);
        if (notes.isEmpty()) return Task.ok(new ArrayList<>());
        List<Long> materialIds = new ArrayList<>();
        for (MaterialNote n : notes) materialIds.add(n.getMaterialId());
        return Task.ok(materialMapper.selectBatchIds(materialIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<MaterialNote> addNote(Long materialId, Long userId, String content, String highlightedText) {
        if (materialMapper.selectById(materialId) == null) return Task.error("资料不存在");
        if (content == null || content.isBlank()) return Task.error("批注内容不能为空");
        MaterialNote note = new MaterialNote();
        note.setMaterialId(materialId);
        note.setUserId(userId);
        note.setNoteContent(content);
        note.setHighlightedText(highlightedText);
        note.setCreatedAt(LocalDateTime.now());
        noteMapper.insert(note);
        return Task.ok(note);
    }

    @Override
    public Task<List<MaterialNote>> listNotes(Long materialId) {
        QueryWrapper<MaterialNote> qw = new QueryWrapper<>();
        qw.eq("material_id", materialId).orderByDesc("created_at");
        return Task.ok(noteMapper.selectList(qw));
    }

    @Override
    public Task<List<LibraryMaterial>> related(Long id) {
        LibraryMaterial m = materialMapper.selectById(id);
        if (m == null) return Task.error("资料不存在");
        QueryWrapper<LibraryMaterial> qw = new QueryWrapper<>();
        if (m.getMaterialType() != null) qw.eq("material_type", m.getMaterialType());
        if (m.getRegionCode()   != null) qw.eq("region_code",   m.getRegionCode());
        qw.ne("id", id).orderByDesc("view_count").last("LIMIT 10");
        return Task.ok(materialMapper.selectList(qw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task<Integer> batchImport(List<LibraryMaterial> materials) {
        if (materials == null || materials.isEmpty()) return Task.ok(0);
        int count = 0;
        for (LibraryMaterial m : materials) {
            if (m.getTitle() == null) continue;
            if (m.getMaterialType() == null) m.setMaterialType(LibraryMaterial.TYPE_REGULATION);
            if (m.getReferenceCount() == null) m.setReferenceCount(0);
            if (m.getViewCount()      == null) m.setViewCount(0);
            m.setCreatedAt(LocalDateTime.now());
            m.setUpdatedAt(LocalDateTime.now());
            materialMapper.insert(m);
            count++;
        }
        return Task.ok(count);
    }

    @Override
    public Task<List<LibraryTag>> listTags() {
        QueryWrapper<LibraryTag> qw = new QueryWrapper<>();
        qw.orderByDesc("usage_count");
        return Task.ok(tagMapper.selectList(qw));
    }
}