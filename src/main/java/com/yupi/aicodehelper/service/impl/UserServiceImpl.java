package com.yupi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.aicodehelper.auth.JwtUtils;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.mapper.UserMapper;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.LoginUserVO;
import com.yupi.aicodehelper.model.vo.UserLoginVO;
import com.yupi.aicodehelper.service.UserService;
import jakarta.annotation.Resource;
import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final int MIN_ACCOUNT_LENGTH = 4;

    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final Duration LOGIN_USER_CACHE_TTL = Duration.ofDays(7);

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        if (!StringUtils.hasText(userAccount) || !StringUtils.hasText(userPassword) || !StringUtils.hasText(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        if (userAccount.length() < MIN_ACCOUNT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于 4 位");
        }
        if (userPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于 8 位");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        long count = lambdaQuery()
                .eq(User::getUserAccount, userAccount)
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(passwordEncoder.encode(userPassword));
        boolean saved = save(user);
        if (!saved || user.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }
        return user.getId();
    }

    @Override
    public UserLoginVO userLogin(String userAccount, String userPassword) {
        if (!StringUtils.hasText(userAccount) || !StringUtils.hasText(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        User user = lambdaQuery()
                .eq(User::getUserAccount, userAccount)
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (!passwordEncoder.matches(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        User safetyUser = getSafetyUser(user);
        cacheLoginUser(safetyUser);

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setToken(jwtUtils.generateToken(user.getId()));
        userLoginVO.setUser(getLoginUserVO(safetyUser));
        return userLoginVO;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserName(user.getUserName());
        loginUserVO.setAvatarUrl(user.getAvatarUrl());
        loginUserVO.setUserRole(user.getUserRole());
        return loginUserVO;
    }

    @Override
    public User getSafetyUser(User user) {
        if (user == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setUserName(user.getUserName());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setUpdateTime(user.getUpdateTime());
        safetyUser.setIsDelete(user.getIsDelete());
        return safetyUser;
    }

    private void cacheLoginUser(User safetyUser) {
        redisTemplate.opsForValue().set(
                LOGIN_USER_KEY_PREFIX + safetyUser.getId(),
                safetyUser,
                LOGIN_USER_CACHE_TTL
        );
    }
}
