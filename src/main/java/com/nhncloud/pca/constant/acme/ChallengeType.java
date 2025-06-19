package com.nhncloud.pca.constant.acme;

public enum ChallengeType {
    HTTP_01("http-01"),
    DNS_01("dns-01"),
    TLS_ALPN_01("tls-alpn-01");

    private final String type;

    ChallengeType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
