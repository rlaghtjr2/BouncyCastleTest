package com.nhncloud.pca.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.nhncloud.pca.interceptor.PostAsGetInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {
    private final PostAsGetInterceptor postAsGetInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(postAsGetInterceptor)
                .addPathPatterns("/acme/directory")
                .addPathPatterns("/acme/newNonce")
                .order(1);
    }
}
