package com.nhncloud.pca.model.acme;

import java.util.Map;

import com.nimbusds.jose.jwk.RSAKey;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwsParseResult {

    /**
     * JWS Protected Header (파싱된 JSON 객체)
     */
    private Map<String, Object> protectedHeader;

    /**
     * JWS Payload (파싱된 JSON 객체)
     */
    private Map<String, Object> payload;

    /**
     * Account의 RSA Key
     */
    private RSAKey accountKey;
}