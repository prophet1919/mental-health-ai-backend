package com.wzu.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRegisterCommandDTO {

    // 校验用户名
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    // 校验邮箱
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 100, message = "邮箱长度必须小于等于100个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-]+$", message = "邮箱格式错误")
    private String email;

    // 校验昵称
    @Size(max = 50, message = "昵称长度必须小于等于50个字符")
    private String nickname;

    // 校验手机号
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    // 校验密码
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50之间")
    private String password;

    // 校验确认密码
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;


    private Integer gender;
    private LocalDateTime birthday;
    private Integer userType = 1;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
