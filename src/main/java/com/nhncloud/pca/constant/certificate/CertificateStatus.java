package com.nhncloud.pca.constant.certificate;

public enum CertificateStatus {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED"),
    EXPIRED("EXPIRED"),
    DELETE_SCHEDULED("DELETE_SCHEDULED"),
    DELETED("DELETED");

    private final String status;

    CertificateStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
