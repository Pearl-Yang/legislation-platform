package com.legal.legislation.service.util;

import com.legal.legislation.service.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Docx 报告导出工具(基于 POI,纯本地实现,不引 poi-tl 也能跑)。
 *
 * 用法:
 *   byte[] bytes = docxExporter.exportReport("标题", sections);
 *   sections: List&lt;Map&lt;String,Object&gt;&gt; [{type:"h1"|"h2"|"p"|"table", ...}]
 */
@Slf4j
@Service
public class DocxExporter {

    /** 把一个 ReportStructure 渲染成 .docx 字节流 */
    public byte[] export(String title, List<Map<String, Object>> sections) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 页面边距
            CTPageMar mar = doc.getDocument().getBody().addNewSectPr().addNewPgMar();
            mar.setLeft(BigInteger.valueOf(1440));
            mar.setRight(BigInteger.valueOf(1440));
            mar.setTop(BigInteger.valueOf(1440));
            mar.setBottom(BigInteger.valueOf(1440));

            // 标题
            if (title != null && !title.isBlank()) {
                XWPFParagraph p = doc.createParagraph();
                p.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun r = p.createRun();
                r.setText(title);
                r.setBold(true);
                r.setFontSize(20);
                r.setFontFamily("SimSun");
                doc.createParagraph(); // 空行
            }

            for (Map<String, Object> sec : sections) {
                String type = String.valueOf(sec.getOrDefault("type", "p"));
                switch (type) {
                    case "h1" -> writeHeading(doc, String.valueOf(sec.get("text")), 16, true);
                    case "h2" -> writeHeading(doc, String.valueOf(sec.get("text")), 14, true);
                    case "h3" -> writeHeading(doc, String.valueOf(sec.get("text")), 12, true);
                    case "p"  -> writeParagraph(doc, String.valueOf(sec.get("text")));
                    case "kv" -> writeKvTable(doc, (Map<String, Object>) sec.get("data"));
                    case "table" -> writeTable(doc, (List<List<String>>) sec.get("rows"), (List<String>) sec.get("headers"));
                    case "blank" -> doc.createParagraph();
                    default -> writeParagraph(doc, String.valueOf(sec.get("text")));
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            log.error("Docx 导出失败", ex);
            return new byte[0];
        }
    }

    private void writeHeading(XWPFDocument doc, String text, int size, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text == null ? "" : text);
        r.setBold(bold);
        r.setFontSize(size);
        r.setFontFamily("SimSun");
    }

    private void writeParagraph(XWPFDocument doc, String text) {
        if (text == null) return;
        // 多行支持
        for (String line : text.split("\n")) {
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationFirstLine(480); // 2 字符首行缩进
            XWPFRun r = p.createRun();
            r.setText(line);
            r.setFontSize(12);
            r.setFontFamily("SimSun");
        }
    }

    @SuppressWarnings("unchecked")
    private void writeKvTable(XWPFDocument doc, Map<String, Object> data) {
        if (data == null || data.isEmpty()) return;
        XWPFTable table = doc.createTable(data.size(), 2);
        table.setWidth("100%");
        int i = 0;
        for (Map.Entry<String, Object> e : data.entrySet()) {
            table.getRow(i).getCell(0).setText(e.getKey());
            table.getRow(i).getCell(1).setText(String.valueOf(e.getValue()));
            i++;
        }
        doc.createParagraph();
    }

    @SuppressWarnings("unchecked")
    private void writeTable(XWPFDocument doc, List<List<String>> rows, List<String> headers) {
        if (rows == null || rows.isEmpty()) return;
        int cols = headers != null ? headers.size() : Math.max(1, rows.get(0).size());
        XWPFTable table = doc.createTable(rows.size() + (headers != null ? 1 : 0), cols);
        table.setWidth("100%");
        int start = 0;
        if (headers != null) {
            for (int c = 0; c < cols; c++) {
                XWPFTableCell cell = table.getRow(0).getCell(c);
                cell.setText(c < headers.size() ? headers.get(c) : "");
                for (XWPFParagraph p : cell.getParagraphs()) {
                    for (XWPFRun r : p.getRuns()) {
                        r.setBold(true);
                    }
                }
            }
            start = 1;
        }
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < cols; c++) {
                table.getRow(start + r).getCell(c).setText(c < row.size() ? row.get(c) : "");
            }
        }
        doc.createParagraph();
    }

    /** 包装一个错误返回,让 controller 拿不到 byte[] 时也能返回空字节 */
    public Task<byte[]> safe(String title, List<Map<String, Object>> sections) {
        return Task.ok(export(title, sections));
    }
}
