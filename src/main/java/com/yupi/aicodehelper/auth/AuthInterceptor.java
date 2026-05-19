package com.yupi.aicodehelper.auth;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final Duration LOGIN_USER_CACHE_TTL = Duration.ofDays(7);

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }

        Long userId;
        try {
            userId = jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态无效");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态无效");
        }

        String cacheKey = UserService.LOGIN_USER_KEY_PREFIX + userId;
        Object cachedUser = getCachedValueSafely(cacheKey);
        User loginUser = convertCachedUser(cachedUser);
        if (loginUser != null) {
            LoginUserHolder.set(loginUser);
            return true;
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在或未登录");
        }

        User safetyUser = userService.getSafetyUser(user);
        redisTemplate.opsForValue().set(cacheKey, safetyUser, LOGIN_USER_CACHE_TTL);
        LoginUserHolder.set(safetyUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserHolder.remove();
    }

    private User convertCachedUser(Object cachedUser) {
        if (cachedUser instanceof User user) {
            return user;
        }
        return null;
    }

    private Object getCachedValueSafely(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (SerializationException e) {
            redisTemplate.delete(cacheKey);
            return null;
        }
    }
}
