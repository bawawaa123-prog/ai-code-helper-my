package com.yupi.aicodehelper.model.vo;

import lombok.Data;

@Data
public class UserLoginVO {

    private String token;

    private LoginUserVO user;
}
