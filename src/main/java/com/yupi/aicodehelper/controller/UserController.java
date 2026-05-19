package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.common.ResultUtils;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.model.dto.user.UserLoginRequest;
import com.yupi.aicodehelper.model.dto.user.UserRegisterRequest;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.LoginUserVO;
import com.yupi.aicodehelper.model.vo.UserLoginVO;
import com.yupi.aicodehelper.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        Long userId = userService.userRegister(
                userRegisterRequest.getUserAccount(),
                userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword()
        );
        return ResultUtils.success(userId);
    }

    @PostMapping("/login")
    public BaseResponse<UserLoginVO> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        UserLoginVO userLoginVO = userService.userLogin(
                userLoginRequest.getUserAccount(),
                userLoginRequest.getUserPassword()
        );
        return ResultUtils.success(userLoginVO);
    }

    @GetMapping("/me")
    public BaseResponse<LoginUserVO> getLoginUser() {
        User loginUser = LoginUserHolder.get();
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
}
