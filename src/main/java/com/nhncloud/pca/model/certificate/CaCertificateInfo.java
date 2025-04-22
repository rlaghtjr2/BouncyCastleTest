package com.nhncloud.pca.model.certificate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CaCertificateInfo {
    private Long caCertificateId;
    private String serialNumber;

    private String commonName;
    private String country;
    private String locality;
    private String stateProvince;
    private String organization;

    private String issuer;

    private LocalDateTime notBeforeDateTime;
    private LocalDateTime notAfterDateTime;

    private String certificatePem;
    private String chainCertificatePem;

    private String publicKeyAlgorithm;
    private String signatureAlgorithm;
}
