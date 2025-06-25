package com.nhncloud.pca.util;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.store.AccountStore;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

public class JwsUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static class JwsParseResult {
        public final Map<String, Object> protectedHeader;
        public final Map<String, Object> payload;
        public final RSAKey accountKey;

        public JwsParseResult(Map<String, Object> protectedHeader, Map<String, Object> payload, RSAKey accountKey) {
            this.protectedHeader = protectedHeader;
            this.payload = payload;
            this.accountKey = accountKey;
        }
    }

    public static JwsParseResult parseAndVerifyJws(Map<String, String> jwsRequest, AccountStore accountStore) {
        try {
            // 1. Base64로 디코딩
            String protectedB64 = jwsRequest.get("protected");
            String payloadB64 = jwsRequest.get("payload");
            String signatureB64 = jwsRequest.get("signature");

            String protectedJson = new String(Base64.getUrlDecoder().decode(protectedB64), StandardCharsets.UTF_8);
            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);

            // 2. JSON 파싱
            Map<String, Object> protectedMap = mapper.readValue(protectedJson, Map.class);
            Map<String, Object> payloadMap = mapper.readValue(payloadJson, Map.class);

            // 3. 공개키 추출
            RSAKey jwk;
            if (protectedMap.containsKey("jwk")) {
                Map<String, Object> jwkMap = (Map<String, Object>) protectedMap.get("jwk");
                jwk = RSAKey.parse(jwkMap);
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
}
