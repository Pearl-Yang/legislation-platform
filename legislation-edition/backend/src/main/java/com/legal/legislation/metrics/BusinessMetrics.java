package com.legal.legislation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 业务指标统一收集。
 *
 * 暴露给 Prometheus / Grafana 看板:
 *  - legislation_project_total              : 立法项目计数
 *  - legislation_draft_generated_total      : 草案生成次数
 *  - legislation_review_total               : 审查次数
 *  - legislation_cleanup_decision_total     : 清理决策(标签 action=accept/reject/defer)
 *  - legislation_evaluation_total           : 评估次数
 *  - legislation_consultation_opinion_total : 意见提交次数
 *  - legislation_crawler_fetch_total        : 抓取次数(标签 source/status)
 *  - legislation_qwen_latency_seconds       : Qwen 调用耗时直方图
 *
 * 命名遵循 Micrometer 约定: <namespace>_<name>_<unit>
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    private final Counter projectCounter;
    private final Counter draftGeneratedCounter;
    private final Counter reviewCounter;
    private final Counter cleanupDecisionCounter;
    private final Counter evaluationCounter;
    private final Counter consultationOpinionCounter;
    private final Counter crawlerFetchCounter;

    private final Timer qwenLatencyTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.projectCounter = Counter.builder("legislation.project.total")
                .description("立法项目创建计数")
                .register(registry);
        this.draftGeneratedCounter = Counter.builder("legislation.draft.generated.total")
                .description("草案 AI 生成次数")
                .register(registry);
        this.reviewCounter = Counter.builder("legislation.review.total")
                .description("审查执行次数")
                .register(registry);
        this.cleanupDecisionCounter = Counter.builder("legislation.cleanup.decision.total")
                .description("清理决策数(accept/reject/defer)")
                .register(registry);
        this.evaluationCounter = Counter.builder("legislation.evaluation.total")
                .description("评估任务执行数")
                .register(registry);
        this.consultationOpinionCounter = Counter.builder("legislation.consultation.opinion.total")
                .description("意见提交数")
                .register(registry);
        this.crawlerFetchCounter = Counter.builder("legislation.crawler.fetch.total")
                .description("爬虫抓取次数")
                .register(registry);

        this.qwenLatencyTimer = Timer.builder("legislation.qwen.latency")
                .description("Qwen LLM 调用耗时")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void incProject() { projectCounter.increment(); }

    public void incDraftGenerated() { draftGeneratedCounter.increment(); }

    public void incReview() { reviewCounter.increment(); }

    public void incCleanupDecision(String action) {
        registry.counter("legislation.cleanup.decision.total", "action", action == null ? "unknown" : action).increment();
    }

    public void incEvaluation() { evaluationCounter.increment(); }

    public void incConsultationOpinion() { consultationOpinionCounter.increment(); }

    public void incCrawlerFetch(String source, String status) {
        registry.counter("legislation.crawler.fetch.total",
                "source", source == null ? "unknown" : source,
                "status", status == null ? "unknown" : status).increment();
    }

    public void recordQwenLatency(long nanos) {
        qwenLatencyTimer.record(nanos, TimeUnit.NANOSECONDS);
    }

    public MeterRegistry getRegistry() { return registry; }
}