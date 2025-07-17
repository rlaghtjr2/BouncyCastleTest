package com.nhncloud.pca.interceptor;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.model.acme.JwsRequest;
import com.nhncloud.pca.store.AccountStore;
import com.nhncloud.pca.store.NonceStore;
import com.nhncloud.pca.util.JwsUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class NonceValidationInterceptor implements HandlerInterceptor {

    @Autowired
    private NonceStore nonceStore;

    @Autowired
    private AccountStore accountStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            // CachingRequestWrapper에서 캐시된 body 가져오기
            if (!(request instanceof CachingRequestWrapper)) {
                // 캐시되지 않은 경우, 원본 요청 처리
                return true;
            }

            CachingRequestWrapper wrapper = (CachingRequestWrapper) request;
            String requestBody = wrapper.getCachedBody();

            // null, 빈 문자열, 공백만 있는 경우 허용 (POST-as-GET 요청)
            if (requestBody == null || requestBody.trim().isEmpty()) {
                return true;
            }

            // "{}" 빈 객체인 경우도 허용
            if ("{}".equals(requestBody.trim())) {
                return true;
            }

            // JWS 요청 파싱
            JwsRequest jwsRequest;
            try {
                jwsRequest = objectMapper.readValue(requestBody, JwsRequest.class);
            } catch (Exception e) {
                // JSON 파싱 실패 시 에러 응답
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:malformed\",\"detail\":\"Invalid JSON format\"}");
                return false;
            }

            // JWS 파싱 및 검증 (POST-as-GET 요청도 처리)
            return processJwsRequest(jwsRequest, request, response);

        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:malformed\",\"detail\":\"Invalid JSON\"}");
            return false;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:malformed\",\"detail\":\"JWS parsing failed\"}");
            return false;
        }
    }

    private boolean processJwsRequest(JwsRequest jwsRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // JWS 파싱 및 검증
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(jwsRequest, accountStore);

            // 결과를 request attribute에 저장
            request.setAttribute("jwsParseResult", result);

            // nonce 검증
            String nonce = (String) result.protectedHeader.get("nonce");
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

            // JWS 요청도 저장 (필요한 경우)
            request.setAttribute("jwsRequest", jwsRequest);

            return true;

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"type\":\"urn:ietf:params:acme:error:malformed\",\"detail\":\"JWS parsing failed\"}");
            return false;
        }
    }
}