package com.nhncloud.pca.constant;

public enum CaStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DELETED("DELETED");

    private final String status;

    CaStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
