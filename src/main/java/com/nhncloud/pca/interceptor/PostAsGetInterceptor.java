package com.nhncloud.pca.interceptor;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PostAsGetInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ((HttpMethod.GET.matches(method) || HttpMethod.POST.matches(method)) && request.getInputStream().available() == 0) {
            return true;
        }
        throw new MethodNotAllowedException("malformed request", null);
    }
}
