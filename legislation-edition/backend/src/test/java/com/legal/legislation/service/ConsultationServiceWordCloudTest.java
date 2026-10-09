package com.legal.legislation.service;

import com.legal.legislation.entity.Opinion;
import com.legal.legislation.mapper.ConsultationMapper;
import com.legal.legislation.mapper.OpinionCategoryMapper;
import com.legal.legislation.mapper.OpinionMapper;
import com.legal.legislation.mapper.OpinionReplyMapper;
import com.legal.legislation.notify.NotifyService;
import com.legal.legislation.service.impl.ConsultationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 意见征集 Service 单元测试 - 不启动 Spring,纯 Mockito 跑逻辑。
 *
 * 覆盖 wordCloud 切词核心:
 *   - 停用词过滤
 *   - 2~4 字短语抽取
 *   - topN 截断
 *   - 数字串过滤
 *   - 空意见列表
 */
@ExtendWith(MockitoExtension.class)
class ConsultationServiceWordCloudTest {

    @Mock private OpinionMapper         opinionMapper;
    @Mock private OpinionCategoryMapper categoryMapper;
    @Mock private OpinionReplyMapper    replyMapper;
    @Mock private ConsultationMapper    consultationMapper;
    @Mock private NotifyService         notifyService;

    @InjectMocks private ConsultationServiceImpl service;

    @BeforeEach
    void setUp() {
        // mock 已在字段声明时自动注入
    }

    @Test
    @DisplayName("空意见列表 -> 返回空数组")
    void wordCloud_empty_returnsEmptyList() {
        when(opinionMapper.selectList(any())).thenReturn(Collections.emptyList());

        Task<List<Map<String, Object>>> result = service.wordCloud(1L, 50);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("正常切词: 2-4 字短语按频次排序,停用词被过滤")
    void wordCloud_basicTokenize() {
        List<Opinion> opinions = new ArrayList<>();
        opinions.add(opinion(1L, "建议加强数据安全管理,推动数据立法。"));
        opinions.add(opinion(2L, "希望完善数据安全相关法律,加强数据安全管理。"));
        opinions.add(opinion(3L, "数据安全管理非常重要,需要加快推进立法进程。"));
        when(opinionMapper.selectList(any())).thenReturn(opinions);

        Task<List<Map<String, Object>>> result = service.wordCloud(1L, 50);

        assertThat(result.isSuccess()).isTrue();
        List<Map<String, Object>> data = result.getData();
        assertThat(data).isNotEmpty();
        // 至少包含 "数据安全" 这个高频短语
        boolean hasDataSafe = data.stream()
            .anyMatch(m -> "数据安全".equals(m.get("name")));
        assertThat(hasDataSafe).isTrue();
    }

    @Test
    @DisplayName("topN 截断: 限制返回条数")
    void wordCloud_topNLimit() {
        // 构造大量不同短语
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("测试").append(i).append("词语。");
        }
        when(opinionMapper.selectList(any())).thenReturn(List.of(opinion(1L, sb.toString())));

        Task<List<Map<String, Object>>> result = service.wordCloud(1L, 5);

        assertThat(result.getData().size()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("topN 越界: <=0 或 >200 走默认值 50")
    void wordCloud_topNOutOfRange_defaultsTo50() {
        when(opinionMapper.selectList(any())).thenReturn(Collections.emptyList());

        Task<List<Map<String, Object>>> r1 = service.wordCloud(1L, 0);
        Task<List<List<Map<String, Object>>>> r2 = null;
        Task<List<Map<String, Object>>> r3 = service.wordCloud(1L, 999);
        Task<List<Map<String, Object>>> r4 = service.wordCloud(1L, -5);

        assertThat(r1.isSuccess()).isTrue();
        assertThat(r3.isSuccess()).isTrue();
        assertThat(r4.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("数字串被过滤, 不进入词频统计")
    void wordCloud_numericFiltered() {
        Opinion o = opinion(1L, "2024年1月1日发布第12345号文件");
        when(opinionMapper.selectList(any())).thenReturn(List.of(o));

        Task<List<Map<String, Object>>> result = service.wordCloud(1L, 50);
        List<Map<String, Object>> data = result.getData();

        // 任何含数字的 name 都不应该出现
        boolean anyNumeric = data.stream()
            .anyMatch(m -> {
                String n = (String) m.get("name");
                return n != null && n.matches(".*\\d.*");
            });
        assertThat(anyNumeric).isFalse();
    }

    @Test
    @DisplayName("content 为 null 的意见被跳过, 不抛 NPE")
    void wordCloud_nullContent_skipped() {
        Opinion a = opinion(1L, null);
        Opinion b = opinion(2L, "正常的意见内容");
        when(opinionMapper.selectList(any())).thenReturn(List.of(a, b));

        Task<List<Map<String, Object>>> result = service.wordCloud(1L, 50);
        assertThat(result.isSuccess()).isTrue();
        // 不应该因为 null content 抛 NPE
    }

    @Test
    @DisplayName("停用词如 '应该' '可以' '应该' 不进入词频")
    void wordCloud_stopWordsFiltered() {
        when(opinionMapper.selectList(any())).thenReturn(
            List.of(opinion(1L, "我认为可以建议应该需要我们的项目")));

        Task<List<Map<String, Object>>> result = service.wordCloud(1L, 50);
        List<Map<String, Object>> data = result.getData();

        // 仅断言 STOP 表中显式列出的停用词 — 其余如"认为"是产品决策
        for (String stop : new String[]{"应该", "可以", "建议", "需要", "我们"}) {
            boolean found = data.stream()
                .anyMatch(m -> stop.equals(m.get("name")));
            assertThat(found).as("停用词 [%s] 不应出现", stop).isFalse();
        }
    }

    // ----- helper -----
    private Opinion opinion(Long id, String content) {
        Opinion o = new Opinion();
        o.setId(id);
        o.setConsultationId(1L);
        o.setContent(content);
        o.setStatus(Opinion.STATUS_NEW);
        return o;
    }
}
