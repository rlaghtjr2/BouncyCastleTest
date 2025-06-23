package com.nhncloud.pca.constant.acme;

public enum AuthorizationStatus {
    VALID("valid"),
    INVALID("invalid"),
    DEACTIVATED("deactivated"),
    EXPIRED("expired"),
    REVOKED("revoked"),
    PENDING("pending");

    private final String status;

    AuthorizationStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
