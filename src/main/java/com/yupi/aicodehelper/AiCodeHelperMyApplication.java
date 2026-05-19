package com.yupi.aicodehelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yupi.aicodehelper.mapper")
public class AiCodeHelperMyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeHelperMyApplication.class, args);
    }

}
