package com.nhncloud.pca.interceptor;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.nhncloud.pca.model.acme.JwsParseResult;
import com.nhncloud.pca.store.NonceStore;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class NonceValidationInterceptor implements HandlerInterceptor {

    @Autowired
    private NonceStore nonceStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // ACME POST 요청만 처리
        if (!"POST".equals(request.getMethod())) {
            return true;
        }

        String requestURI = request.getRequestURI();
        if (!requestURI.contains("/acme/")) {
            return true;
        }

        // new-nonce 요청은 제외
        if (requestURI.endsWith("/new-nonce")) {
            return true;
        }

        // HEAD 요청은 제외
        if ("HEAD".equals(request.getMethod())) {
            return true;
        }

        try {
            // JwsValidationInterceptor에서 설정한 결과를 가져오기
            JwsParseResult result = (JwsParseResult) request.getAttribute("jwsParseResult");

            // JWS 파싱 결과가 없으면 이전 인터셉터에서 처리되지 않은 요청
            if (result == null) {
                return true;
            }

            // Nonce 검증
            Map<String, Object> protectedHeader = result.getProtectedHeader();
            String nonce = (String) protectedHeader.get("nonce");
            if (nonce == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:badNonce\",\"detail\":\"Missing nonce\"}");
                return false;
            }

            if (!nonceStore.consumeNonce(nonce)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:badNonce\",\"detail\":\"Invalid nonce\"}");
                return false;
            }

            return true;

        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:malformed\",\"detail\":\"Invalid JSON\"}");
            return false;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:malformed\",\"detail\":\"Nonce validation failed\"}");
            return false;
        }
    }
}