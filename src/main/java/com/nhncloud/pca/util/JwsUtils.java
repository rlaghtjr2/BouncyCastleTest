package com.nhncloud.pca.util;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.model.acme.JwsParseResult;
import com.nhncloud.pca.model.acme.JwsRequest;
import com.nhncloud.pca.store.AccountStore;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

public class JwsUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static JwsParseResult parseAndVerifyJws(JwsRequest jwsRequest, AccountStore accountStore) {
        try {
            // 1. Base64로 디코딩
            String protectedB64 = jwsRequest.getProtectedHeader();
            String payloadB64 = jwsRequest.getPayload();
            String signatureB64 = jwsRequest.getSignature();

            String protectedJson = new String(Base64.getUrlDecoder().decode(protectedB64), StandardCharsets.UTF_8);

            // 2. JSON 파싱
            Map<String, Object> protectedMap = mapper.readValue(protectedJson, Map.class);

            // payload 처리 - 빈 문자열인 경우 (POST-as-GET 요청)
            Map<String, Object> payloadMap;
            if (payloadB64 == null || payloadB64.trim().isEmpty()) {
                // POST-as-GET 요청의 경우 payload가 빈 문자열
                payloadMap = new HashMap<>();
            } else {
                // 일반적인 경우 payload 디코딩 및 JSON 파싱
                String payloadJson = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
                payloadMap = mapper.readValue(payloadJson, Map.class);
            }

            // 3. 공개키 추출
            RSAKey jwk;
            if (protectedMap.containsKey("jwk")) {
                Map<String, Object> jwkMap = (Map<String, Object>) protectedMap.get("jwk");
                jwk = parseAndValidateJwk(jwkMap);
            } else if (protectedMap.containsKey("kid")) {
                String kid = (String) protectedMap.get("kid");
                jwk = accountStore.getKeyByKid(kid);
                if (jwk == null) {
                    throw new IllegalArgumentException("Unknown account KID: " + kid);
                }
            } else {
                throw new IllegalArgumentException("No jwk or kid found in protected header");
            }

            // 4. 서명 검증
            String signingInput = protectedB64 + "." + payloadB64;
            Base64URL signature = new Base64URL(signatureB64);
            JWSObject jwsObject = JWSObject.parse(signingInput + "." + signature);

            boolean verified = jwsObject.verify(new RSASSAVerifier(jwk.toRSAPublicKey()));
            if (!verified) {
                throw new SecurityException("Invalid JWS signature");
            }

            return new JwsParseResult(protectedMap, payloadMap, jwk);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("JWS Header error: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse protected or payload JSON: " + e.getMessage(), e);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse JWK: " + e.getMessage(), e);
        } catch (JOSEException e) {
            throw new RuntimeException("JWS verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * JWK Map에서 RSA Public Key만 추출하여 안전하게 파싱
     * Private Key 정보는 보안상 제거
     */
    private static RSAKey parseAndValidateJwk(Map<String, Object> jwkMap) {
        try {
            // 1. Key Type 검증
            String keyType = (String) jwkMap.get("kty");
            if (!"RSA".equals(keyType)) {
                throw new IllegalArgumentException("Only RSA keys are supported, got: " + keyType);
            }

            // 2. 필수 RSA Public Key 파라미터 검증
            String modulus = (String) jwkMap.get("n");
            String exponent = (String) jwkMap.get("e");

            if (modulus == null || modulus.trim().isEmpty()) {
                throw new IllegalArgumentException("RSA modulus (n) is required");
            }
            if (exponent == null || exponent.trim().isEmpty()) {
                throw new IllegalArgumentException("RSA public exponent (e) is required");
            }

            // 3. Public Key만 포함하는 새로운 JWK Map 생성 (Private Key 정보 제거)
            Map<String, Object> publicOnlyJwk = new java.util.HashMap<>();
            publicOnlyJwk.put("kty", keyType);
            publicOnlyJwk.put("n", modulus);
            publicOnlyJwk.put("e", exponent);

            // 4. 선택적 파라미터들도 추가 (Public Key 관련만)
            if (jwkMap.containsKey("alg")) {
                publicOnlyJwk.put("alg", jwkMap.get("alg"));
            }
            if (jwkMap.containsKey("use")) {
                publicOnlyJwk.put("use", jwkMap.get("use"));
            }
            if (jwkMap.containsKey("kid")) {
                publicOnlyJwk.put("kid", jwkMap.get("kid"));
            }

            // 5. RSAKey 생성 및 반환
            RSAKey rsaKey = RSAKey.parse(publicOnlyJwk);

            // 6. Key 크기 검증 (최소 2048비트)
            int keySize = rsaKey.toRSAPublicKey().getModulus().bitLength();
            if (keySize < 2048) {
                throw new IllegalArgumentException("RSA key size must be at least 2048 bits, got: " + keySize);
            }

            return rsaKey;

        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse RSA JWK: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate JWK: " + e.getMessage(), e);
        }
    }
}
