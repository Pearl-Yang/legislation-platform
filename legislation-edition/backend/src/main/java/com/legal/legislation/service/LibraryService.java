package com.legal.legislation.service;

import com.legal.legislation.entity.LibraryMaterial;
import com.legal.legislation.entity.LibraryTag;
import com.legal.legislation.entity.MaterialNote;

import java.util.List;
import java.util.Map;

public interface LibraryService {

    Task<?> list(int page, int size, String materialType, String regionCode, String keyword);

    Task<LibraryMaterial> create(LibraryMaterial material);

    Task<LibraryMaterial> update(Long id, LibraryMaterial material);

    Task<Boolean> delete(Long id);

    Task<?> search(String keyword, String materialType, int page, int size);

    Task<Map<String, Object>> detail(Long id);

    Task<Integer> favorite(Long id, Long userId);

    Task<Integer> unFavorite(Long id, Long userId);

    Task<List<LibraryMaterial>> myFavorites(Long userId);

    Task<MaterialNote> addNote(Long materialId, Long userId, String content, String highlightedText);

    Task<List<MaterialNote>> listNotes(Long materialId);

    Task<List<LibraryMaterial>> related(Long id);

    Task<Integer> batchImport(List<LibraryMaterial> materials);

    Task<List<LibraryTag>> listTags();

    /** 搜索建议(根据关键词返回建议词 / 相关资料) */
    Task<List<String>> suggest(String keyword, int limit);
}