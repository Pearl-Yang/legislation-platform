package com.legal.legislation.audit;

import java.lang.annotation.*;

/**
 * 审计注解:打在 controller / service 方法上,自动记录到审计日志。
 *
 * 用法:
 *   @Audited(action = "CREATE", resource = "regulation", description = "新增法规")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    /** 动作: CREATE / UPDATE / DELETE / QUERY / EXPORT / LOGIN / LOGOUT */
    String action();
    /** 资源对象: regulation / project / draft / review / opinion ... */
    String resource() default "";
    /** 备注(可空) */
    String description() default "";
}
