package com.nhncloud.pca.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.nhncloud.pca.interceptor.JwsValidationInterceptor;
import com.nhncloud.pca.interceptor.NonceValidationInterceptor;
import com.nhncloud.pca.interceptor.PostAsGetInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {
    private final PostAsGetInterceptor postAsGetInterceptor;
    private final JwsValidationInterceptor jwsValidationInterceptor;
    private final NonceValidationInterceptor nonceValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // PostAsGetInterceptor - HTTP 메소드 검증
        registry.addInterceptor(postAsGetInterceptor)
                .addPathPatterns("/acme/directory")
                .addPathPatterns("/acme/new-nonce")
                .order(1);

        // JwsValidationInterceptor - JWS 파싱 및 토큰 검증 (서명 검증)
        registry.addInterceptor(jwsValidationInterceptor)
                .addPathPatterns("/acme/new-account")
                .addPathPatterns("/acme/new-order")
                .addPathPatterns("/acme/authz/*")
                .addPathPatterns("/acme/challenge/*")
                .addPathPatterns("/acme/order/*/finalize")
                .addPathPatterns("/acme/order/*")
                .order(2);

        // NonceValidationInterceptor - nonce 검증
        registry.addInterceptor(nonceValidationInterceptor)
                .addPathPatterns("/acme/new-account")
                .addPathPatterns("/acme/new-order")
                .addPathPatterns("/acme/authz/*")
                .addPathPatterns("/acme/challenge/*")
                .addPathPatterns("/acme/order/*/finalize")
                .addPathPatterns("/acme/order/*")
                .order(3);
    }
}
