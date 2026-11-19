package com.wzu.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wzu.aispringboot.enumclass.UserStatus;
import com.wzu.aispringboot.enumclass.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

//CREATE TABLE `user` (
//        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
//        `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
//        `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
//        `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
//        `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
//        `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
//        `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
//        `gender` tinyint DEFAULT NULL COMMENT '性别 0:未知 1:男 2:女',
//        `birthday` date DEFAULT NULL COMMENT '生日',
//        `user_type` tinyint DEFAULT '1' COMMENT '用户类型 1:普通用户 2:管理员',
//        `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
//        `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
//        `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
//PRIMARY KEY (`id`) USING BTREE,
//UNIQUE KEY `username` (`username`) USING BTREE,
//UNIQUE KEY `email` (`email`) USING BTREE,
//UNIQUE KEY `phone` (`phone`) USING BTREE,
//KEY `idx_username` (`username`) USING BTREE,
//KEY `idx_user_type` (`user_type`) USING BTREE
//) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';

//数据库用户实体类
// 用户实体类
@Data
@TableName("user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // 用户ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户名
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    // 邮箱
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    // 手机号
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    // 密码
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 255, message = "密码长度必须在6到255个字符之间")
    private String password;

    // 昵称
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    // 头像
    @Size(max = 255, message = "头像路径长度不能超过255个字符")
    private String avatar;

    // 性别
    private Integer gender;

    // 生日
    private LocalDate birthday;

    // 用户类型 1:普通用户 2:管理员
    @TableField("user_type")
    private Integer userType;

    // 状态 0:禁用 1:正常
    private Integer status;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 是否为普通用户
     */
    public boolean isUser() {
        return UserType.USER.getCode().equals(this.userType);
    }

    /**
     * 是否为正常状态
     */
    public boolean isActive() {
        return UserStatus.NORMAL.getCode().equals(this.status);
    }

    /**
     * 是否被禁用
     */
    public boolean isDisabled() {
        return UserStatus.DISABLED.getCode().equals(this.status);
    }

    /**
     * 获取显示名称（优先显示昵称，否则显示用户名）
     */
    public String getDisplayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname : username;
    }

    /**
     * 获取用户类型显示名称
     */
    public String getUserTypeDisplayName() {
        try {
            return UserType.fromCode(userType).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    /**
     * 获取用户状态显示名称
     */
    public String getStatusDisplayName() {
        try {
            return UserStatus.fromCode(status).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
