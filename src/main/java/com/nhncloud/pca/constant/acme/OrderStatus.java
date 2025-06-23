package com.nhncloud.pca.constant.acme;

public enum OrderStatus {
    PENDING("pending"),
    READY("ready"),
    PROCESSING("processing"),
    VALID("valid"),
    INVALID("invalid");

    private final String status;

    OrderStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
