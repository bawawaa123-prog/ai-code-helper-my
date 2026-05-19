package com.yupi.aicodehelper.model.dto.user;

import lombok.Data;

@Data
public class UserLoginRequest {

    private String userAccount;

    private String userPassword;
}
