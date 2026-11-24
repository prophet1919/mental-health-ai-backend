package com.wzu.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzu.aispringboot.DTO.command.UserLoginCommandDTO;
import com.wzu.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.wzu.aispringboot.DTO.response.UserLoginResponseDTO;
import com.wzu.aispringboot.entity.User;
import com.wzu.aispringboot.enumclass.UserType;
import com.wzu.aispringboot.exception.BusinessException;
import com.wzu.aispringboot.mapper.UserMapper;
import com.wzu.aispringboot.service.convert.UserConvert;
import com.wzu.aispringboot.util.JwtTokenUtil;
import jakarta.annotation.Resource;
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

        // 判断用户是否存在（不区分"用户不存在"与"密码错误"，避免账号枚举）
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        //验证密码
        String inputPassword = userLoginCommandDTO.getPassword().trim();//去掉密码首尾空格
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        //检查用户状态（是否被禁用），使用枚举类UserStatus
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        //生成Jwt token
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);

        //返回登录成功响应
        return UserConvert.entityToLoginResponse(token, userInfo);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO userRegisterCommandDTO) {
        //验证密码是否一致
        if (!userRegisterCommandDTO.getPassword().equals(userRegisterCommandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }

        //检查用户是否存在，不能重复
        LambdaQueryWrapper<User> userNameQueryWrapper = new LambdaQueryWrapper<>();
        userNameQueryWrapper.eq(User::getUsername, userRegisterCommandDTO.getUsername());
        if (userMapper.selectCount(userNameQueryWrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }
        //检查邮箱是否存在，不能重复
        LambdaQueryWrapper<User> emailQueryWrapper = new LambdaQueryWrapper<>();
        emailQueryWrapper.eq(User::getEmail, userRegisterCommandDTO.getEmail());
        if (userMapper.selectCount(emailQueryWrapper) > 0) {
            throw new BusinessException("邮箱已存在");
        }

        //用户类型是否有效
        if (!UserType.isValidCode(userRegisterCommandDTO.getUserType())) {
            throw new BusinessException("用户类型无效");
        }

        //创建用户
        String password = userRegisterCommandDTO.getPassword().trim();
        password = passwordEncoder.encode(password);
        User user = UserConvert.registerCommandToEntity(userRegisterCommandDTO, password);

        //插入数据库
        userMapper.insert(user);

        return UserConvert.entityToDetailResponse(user);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null){
            throw new BusinessException("用户不存在");
        }
        return UserConvert.entityToDetailResponse(user);
    }
}