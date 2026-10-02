package com.legal.legislation.audit;

import com.legal.legislation.notify.ConsoleNotificationServiceImpl;
import com.legal.legislation.notify.NotifyMessage;
import com.legal.legislation.notify.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计 AOP:
 *  - 拦截 @Audited 注解的方法
 *  - 记录 user / action / resource / costMs / status
 *  - 失败也记录
 *
 * 当前实现: 写到内存日志(ConsoleNotificationServiceImpl 转发)+ 计划落 audit_log 表
 * 异步执行,不阻塞业务。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final NotifyService notifyService;

    @Around("@annotation(com.legal.legislation.audit.Audited)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long t0 = System.currentTimeMillis();
        Audited ann = ((MethodSignature) pjp.getSignature()).getMethod().getAnnotation(Audited.class);
        AuditLog.AuditLogBuilder b = AuditLog.builder()
            .action(ann.action())
            .resource(ann.resource())
            .description(ann.description())
            .createdAt(LocalDateTime.now());
        // 上下文(可能不在 Web 环境)
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                var req = attrs.getRequest();
                b.method(req.getMethod()).path(req.getRequestURI());
                b.ip(req.getRemoteAddr()).userAgent(req.getHeader("User-Agent"));
                String uid = req.getHeader("X-User-Id");
                if (uid != null) b.userId(Long.parseLong(uid));
            }
        } catch (Exception ignored) {}

        int status = 0;
        String err = null;
        Object ret = null;
        try {
            ret = pjp.proceed();
            return ret;
        } catch (Throwable ex) {
            status = 1;
            err = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            b.costMs(System.currentTimeMillis() - t0)
             .status(status)
             .errorMessage(err);
            try {
                writeAsync(b.build());
            } catch (Exception ex) {
                log.warn("审计记录失败: {}", ex.getMessage());
            }
        }
    }

    @Async
    void writeAsync(AuditLog audit) {
        // 通过 NotifyService 推一条 AUDIT 通知,既给管理员看,也方便后续接 sink
        String body = String.format("[%s] %s %s %sms user=%d ip=%s err=%s",
            audit.getAction(), audit.getResource(), audit.getPath(),
            audit.getCostMs(), audit.getUserId(), audit.getIp(), audit.getErrorMessage());
        log.info("AUDIT {}", body);
        try {
            notifyService.send(new NotifyMessage(
                audit.getUserId(),
                "AUDIT",
                "操作审计",
                body,
                audit.getResource(),
                null,
                audit.getCreatedAt()
            ));
        } catch (Exception ignored) { /* 通知失败不影响业务 */ }
    }
}
