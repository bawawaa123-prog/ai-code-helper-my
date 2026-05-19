package com.yupi.aicodehelper.auth;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.model.entity.User;

public final class LoginUserHolder {

    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();

    private LoginUserHolder() {
    }

    public static void set(User user) {
        USER_HOLDER.set(user);
    }

    public static User get() {
        User user = USER_HOLDER.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        return user;
    }

    public static void remove() {
        USER_HOLDER.remove();
    }
}
