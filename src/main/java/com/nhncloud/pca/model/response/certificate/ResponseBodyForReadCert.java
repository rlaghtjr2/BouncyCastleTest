package com.nhncloud.pca.model.response.certificate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseBodyForReadCert {
    private String commonName;
    private String serialNumber;

    private LocalDateTime notAfterDateTime;
    private LocalDateTime notBeforeDateTime;

    private String publicKeyAlgorithm;

    private String certificatePem;
    private String chainCertificatePem;

    private String signatureAlgorithm;
}
