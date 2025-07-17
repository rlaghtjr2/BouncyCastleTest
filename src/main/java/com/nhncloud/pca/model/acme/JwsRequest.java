package com.nhncloud.pca.model.acme;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JWS (JSON Web Signature) 요청을 나타내는 클래스
 */
public class JwsRequest {

    @JsonProperty("protected")
    private String protectedHeader;

    @JsonProperty("payload")
    private String payload;

    @JsonProperty("signature")
    private String signature;

    public JwsRequest() {
    }

    public JwsRequest(String protectedHeader, String payload, String signature) {
        this.protectedHeader = protectedHeader;
        this.payload = payload;
        this.signature = signature;
    }

    public String getProtectedHeader() {
        return protectedHeader;
    }

    public void setProtectedHeader(String protectedHeader) {
        this.protectedHeader = protectedHeader;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "JwsRequest{" +
                "protectedHeader='" + protectedHeader + '\'' +
                ", payload='" + payload + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}