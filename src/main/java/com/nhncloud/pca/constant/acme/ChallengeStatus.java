package com.nhncloud.pca.constant.acme;

public enum ChallengeStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    VALID("valid"),
    INVALID("invalid");

    private final String status;

    ChallengeStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
