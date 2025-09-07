package com.xcw.picturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.xcw.picturebackend.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)//默认使用jdk动态代理，设置为true使用cglib动态代理
public class PictureBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PictureBackendApplication.class, args);
    }
}
