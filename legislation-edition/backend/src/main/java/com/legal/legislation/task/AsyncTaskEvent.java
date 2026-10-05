package com.legal.legislation.task;

import org.springframework.context.ApplicationEvent;

/**
 * 异步执行任务事件。
 *
 * <p>业务 Service 在自身事务提交后发布此类,
 * 监听器收到后在新线程里执行耗时操作(如生成建议、跑指标计算)。
 *
 * <p>设计动机:避免在 {@code @Transactional} 方法内调用同类 {@code @Async} 方法时
 * 因 Spring proxy 失效而退化为同步执行的问题。
 */
public class AsyncTaskEvent extends ApplicationEvent {

    public enum Kind { CLEANUP_RUN, EVALUATION_RUN }

    private final Kind   kind;
    private final Long   taskId;

    public AsyncTaskEvent(Object source, Kind kind, Long taskId) {
        super(source);
        this.kind = kind;
        this.taskId = taskId;
    }

    public Kind getKind()  { return kind; }
    public Long getTaskId() { return taskId; }
}
