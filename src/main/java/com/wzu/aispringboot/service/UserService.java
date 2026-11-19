package com.wzu.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzu.aispringboot.DTO.command.UserLoginCommandDTO;
import com.wzu.aispringboot.DTO.response.UserLoginResponseDTO;
import com.wzu.aispringboot.common.Result;
import com.wzu.aispringboot.entity.User;
import com.wzu.aispringboot.enumclass.UserStatus;
import com.wzu.aispringboot.exception.BusinessException;
import com.wzu.aispringboot.mapper.UserMapper;
import com.wzu.aispringboot.service.convert.UserConvert;
import com.wzu.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserLoginResponseDTO login(UserLoginCommandDTO userLoginCommandDTO) {
        //构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userLoginCommandDTO.getUsername())
                .or()
                .eq(User::getEmail, userLoginCommandDTO.getUsername());
        //调用mybatis-plus的api查询用户
        User user = userMapper.selectOne(queryWrapper);
        System.out.println(user);

        // 判断用户是否存在
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        //验证密码
        String inputPassword = userLoginCommandDTO.getPassword().trim();//去掉密码首尾空格
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        //检查用户状态（是否被禁用），使用枚举类UserStatus
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        //生成Jwt token
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        System.out.println(token);
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);

        //返回登录成功响应
        return UserConvert.entityToLoginResponse(token, userInfo);
    }
}
