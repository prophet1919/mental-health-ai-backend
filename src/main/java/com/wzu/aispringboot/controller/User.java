package com.wzu.aispringboot.controller;


import com.wzu.aispringboot.DTO.command.UserLoginCommandDTO;
import com.wzu.aispringboot.DTO.response.UserLoginResponseDTO;
import com.wzu.aispringboot.common.Result;
import com.wzu.aispringboot.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")//POST请求,登录接口
public class User {
    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO userLoginCommandDTO){
        // 校验登录命令DTO是否符合userLoginCommandDTO类的注解要求(NotBlank,Size注解)
        System.out.println(userLoginCommandDTO.getUsername());
        System.out.println(userLoginCommandDTO.getPassword());
        // 调用服务层UserService的login方法,传入登录命令DTO
        UserLoginResponseDTO result = userService.login(userLoginCommandDTO);
        System.out.println(result);
        return Result.ok(result);
    }
}
