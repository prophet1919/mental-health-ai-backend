package com.wzu.aispringboot.controller;


import com.auth0.jwt.interfaces.DecodedJWT;
import com.wzu.aispringboot.DTO.command.UserLoginCommandDTO;
import com.wzu.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.wzu.aispringboot.DTO.response.UserLoginResponseDTO;
import com.wzu.aispringboot.common.Result;
import com.wzu.aispringboot.service.UserService;
import com.wzu.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")//POST请求,登录接口
public class User {
    @Resource
    private UserService userService;

    //用户登录接口
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

    //用户注册接口
    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO userRegisterCommandDTO){
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(userRegisterCommandDTO);
        return Result.ok(result);
    }

    //获取用户详情接口
    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser(){
        //如何从token中获取用户ID
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        //调用service层获取用户详情
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.getUserById(userId);
        return Result.ok(result);
    }
}
