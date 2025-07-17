package com.nhncloud.pca.interceptor;

import java.io.IOException;

import org.springframework.core.annotation.Order;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Order(1)
@Slf4j
public class CachingRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // ACME POST 요청인 경우에만 wrapping
            if ("POST".equals(httpRequest.getMethod()) &&
                httpRequest.getRequestURI().startsWith("/acme/") &&
                !httpRequest.getRequestURI().contains("/directory") &&
                !httpRequest.getRequestURI().contains("/new-nonce")) {

                log.debug("Wrapping request for URI: {}", httpRequest.getRequestURI());
                CachingRequestWrapper wrappedRequest = new CachingRequestWrapper(httpRequest);
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }

        // 일반 요청은 그대로 진행
        chain.doFilter(request, response);
    }
}