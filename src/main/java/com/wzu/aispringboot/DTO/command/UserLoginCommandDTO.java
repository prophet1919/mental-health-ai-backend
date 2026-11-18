package com.wzu.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

//Data注解,自动生成getter和setter方法，全参构造方法，无参构造方法
@Data
public class UserLoginCommandDTO {

    @NotBlank(message = "用户名或邮箱不能为空")// 校验用户名或邮箱是否为空,提示信息
    @Size(max = 100, message = "用户名或邮箱长度不能超过100个字符")//限制用户名或邮箱长度不能超过100个字符
    private String username;
    @NotBlank(message = "密码不能为空")// 校验密码是否为空,提示信息
    @Size(max = 50, min = 6, message = "密码长度不能超过100个字符")//限制密码长度不能超过100个字符
    private String password;

}
