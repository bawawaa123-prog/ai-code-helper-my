package com.yupi.aicodehelper.model.vo;

import lombok.Data;

@Data
public class LoginUserVO {

    private Long id;

    private String userAccount;

    private String userName;

    private String avatarUrl;

    private String userRole;
}
