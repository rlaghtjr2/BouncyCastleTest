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

        // GET과 HEAD는 body가 없어야 함
        if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)) {
            return request.getInputStream().available() == 0;
        }

        // POST는 body가 있어야 함 (또는 없어도 허용)
        if (HttpMethod.POST.matches(method)) {
            return true;
        }

        throw new MethodNotAllowedException("malformed request", null);
    }
}
