package com.nhncloud.pca.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.nhncloud.pca.model.certificate.CertificateInfo;

@Data
@Builder
public class CertificateReadResult {
    private String commonName;
    private String serialNumber;

    private LocalDateTime notAfterDateTime;
    private LocalDateTime notBeforeDateTime;

    private String publicKeyAlgorithm;

    private String certificatePem;
    private String chainCertificatePem;

    private String signatureAlgorithm;

    public static CertificateReadResult of(CertificateInfo certificateInfo) {
        return CertificateReadResult.builder()
            .commonName(certificateInfo.getCommonName())
            .serialNumber(certificateInfo.getSerialNumber())
            .notAfterDateTime(certificateInfo.getNotAfterDateTime())
            .notBeforeDateTime(certificateInfo.getNotBeforeDateTime())
            .publicKeyAlgorithm(certificateInfo.getPublicKeyAlgorithm())
            .certificatePem(certificateInfo.getCertificatePem())
            .chainCertificatePem(certificateInfo.getChainCertificatePem())
            .signatureAlgorithm(certificateInfo.getSignatureAlgorithm())
            .build();
    }
}
