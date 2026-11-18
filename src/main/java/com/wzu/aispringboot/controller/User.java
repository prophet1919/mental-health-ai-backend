package com.wzu.aispringboot.controller;


import com.wzu.aispringboot.DTO.command.UserLoginCommandDTO;
import com.wzu.aispringboot.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")//POST请求,登录接口
public class User {
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody UserLoginCommandDTO userLoginCommandDTO){
        // 校验登录命令DTO是否符合userLoginCommandDTO类的注解要求(NotBlank,Size注解)
        System.out.println(userLoginCommandDTO.getUsername());
        System.out.println(userLoginCommandDTO.getPassword());
        return null;
    }
}
