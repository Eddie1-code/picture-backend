package com.xcw.picturebackend.config;

import com.xcw.picturebackend.security.SecurityProtectionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityWebMvcConfig implements WebMvcConfigurer {

    private final SecurityProtectionInterceptor securityProtectionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityProtectionInterceptor)
                .addPathPatterns("/**")
                .order(-100);
    }
}
