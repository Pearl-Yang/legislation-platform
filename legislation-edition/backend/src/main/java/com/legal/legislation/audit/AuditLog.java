package com.legal.legislation.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long          id;
    private Long          userId;
    private String        userName;
    private String        action;       // CREATE/UPDATE/DELETE/QUERY/EXPORT/LOGIN
    private String        resource;     // 资源对象
    private String        description;  // 备注
    private String        method;       // HTTP method
    private String        path;         // 访问路径
    private String        ip;
    private String        userAgent;
    private Long          costMs;       // 耗时
    private Integer       status;       // 0=成功, 1=失败
    private String        errorMessage;
    private LocalDateTime createdAt;
}
