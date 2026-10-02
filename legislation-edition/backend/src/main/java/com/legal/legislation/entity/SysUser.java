package com.legal.legislation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户。
 *
 * 立法版登录态的最小实体:username + password_hash + role 三件套;
 * 不引入 RBAC 表,角色枚举直接落 role 字段(后续若需要细化再拆 sys_role / sys_user_role)。
 */
@Data
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "登录用户名", example = "admin")
    private String username;

    @Schema(description = "显示名", example = "系统管理员")
    private String displayName;

    @Schema(description = "BCrypt 密码哈希(对外不返回)")
    private String passwordHash;

    @Schema(description = "角色枚举:ROLE_USER / ROLE_LEADER / ROLE_REVIEWER / ROLE_EVALUATOR / ROLE_ADMIN",
        example = "ROLE_ADMIN")
    private String role;

    @Schema(description = "所属部门", example = "信息中心")
    private String department;

    @Schema(description = "是否启用:1 启用 / 0 禁用")
    private Integer isActive;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
