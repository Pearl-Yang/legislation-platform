package com.legal.legislation.controller;

import com.legal.legislation.crawler.CrawlerReport;
import com.legal.legislation.crawler.CrawlerRunner;
import com.legal.legislation.crawler.GovCnSource;
import com.legal.legislation.crawler.MojGovCnSource;
import com.legal.legislation.crawler.RegulationSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 法规爬虫管理 Controller。
 *
 * 仅管理员可调用(权限注解后续在 SecurityConfig 收紧)。
 */
@RestController
@RequestMapping("/admin/crawler")
@RequiredArgsConstructor
@Tag(name = "Admin-爬虫管理", description = "gov.cn / moj.gov.cn 法规抓取,带限流 / 重试 / 断点续抓")
public class CrawlerAdminController {

    private final CrawlerRunner runner;
    private final GovCnSource   govCn;
    private final MojGovCnSource moj;

    @Operation(summary = "触发 gov.cn 爬取(同步,慢)")
    @PostMapping("/run/gov-cn")
    public Map<String, Object> runGovCn(
        @RequestParam(defaultValue = "1")  int fromPage,
        @RequestParam(defaultValue = "3")  int toPage) {
        CrawlerReport r = runner.run(govCn, fromPage, toPage);
        return toMap(r);
    }

    @Operation(summary = "触发 moj.gov.cn 爬取(同步,慢)")
    @PostMapping("/run/moj")
    public Map<String, Object> runMoj(
        @RequestParam(defaultValue = "1")  int fromPage,
        @RequestParam(defaultValue = "3")  int toPage) {
        CrawlerReport r = runner.run(moj, fromPage, toPage);
        return toMap(r);
    }

    @Operation(summary = "查看各 source 状态")
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "checkpoints", runner.checkpointStore().all()
        );
    }

    private Map<String, Object> toMap(CrawlerReport r) {
        Map<String, Object> m = new HashMap<>();
        m.put("source",     r.source);
        m.put("pages",      r.pages);
        m.put("detailUrls", r.detailUrls);
        m.put("saved",      r.saved);
        m.put("failed",     r.failed);
        m.put("elapsedMs",  r.elapsedMs);
        m.put("error",      r.error);
        return m;
    }
}
