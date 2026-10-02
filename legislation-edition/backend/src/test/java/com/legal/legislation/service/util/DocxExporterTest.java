package com.legal.legislation.service.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Docx 导出 smoke test:不打开 Word,只校验返回非空字节流且头是 PK(zip = docx)。
 */
class DocxExporterTest {

    @Test
    void export_minimalReport_returnsValidDocxBytes() {
        DocxExporter exporter = new DocxExporter();
        byte[] bytes = exporter.export(
            "测试报告",
            List.of(
                Map.of("type", "h1", "text", "一级标题"),
                Map.of("type", "p",  "text", "正文段落"),
                Map.of("type", "kv", "data", Map.of("指标A", 1, "指标B", "OK")),
                Map.of("type", "table", "headers", List.of("列1", "列2"),
                       "rows", List.of(List.of("a", "b"), List.of("c", "d")))
            )
        );
        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "docx 不应为空");
        // zip 魔数 PK\x03\x04
        assertEquals(0x50, bytes[0] & 0xFF);
        assertEquals(0x4B, bytes[1] & 0xFF);
    }

    @Test
    void export_emptySections_returnsHeaderOnly() {
        DocxExporter exporter = new DocxExporter();
        byte[] bytes = exporter.export("无章节", List.of());
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}
