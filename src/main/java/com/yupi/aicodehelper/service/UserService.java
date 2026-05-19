package com.yupi.aicodehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.LoginUserVO;
import com.yupi.aicodehelper.model.vo.UserLoginVO;

public interface UserService extends IService<User> {

    String LOGIN_USER_KEY_PREFIX = "login:user:";

    Long userRegister(String userAccount, String userPassword, String checkPassword);

    UserLoginVO userLogin(String userAccount, String userPassword);

    LoginUserVO getLoginUserVO(User user);

    User getSafetyUser(User user);
}
