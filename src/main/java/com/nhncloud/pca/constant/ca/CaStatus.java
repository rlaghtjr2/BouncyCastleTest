package com.nhncloud.pca.constant.ca;

public enum CaStatus {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED"),
    EXPIRED("EXPIRED"),
    DELETE_SCHEDULED("DELETE_SCHEDULED"),
    DELETED("DELETED");

    private final String status;

    CaStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
