package com.nhncloud.pca.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.nhncloud.pca.interceptor.CachingRequestFilter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/.well-known/acme-challenge/**")
            .addResourceLocations("file:/var/www/html/.well-known/acme-challenge/");
    }

    @Bean
    public FilterRegistrationBean<CachingRequestFilter> cachingRequestFilter() {
        FilterRegistrationBean<CachingRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CachingRequestFilter());
        registration.addUrlPatterns("/acme/*");
        registration.setOrder(1);
        registration.setName("CachingRequestFilter");
        return registration;
    }
}