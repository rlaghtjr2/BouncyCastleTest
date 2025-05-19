package com.nhncloud.pca.model.certificate;

import lombok.Builder;
import lombok.Data;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.model.subject.SubjectInfo;
import com.nhncloud.pca.util.CertificateUtil;

@Data
@Builder
public class CertificateInfo {
    private Long certificateId;
    private String serialNumber;

    private SubjectInfo subjectInfo;

    private String issuer;

    private LocalDateTime notBeforeDateTime;
    private LocalDateTime notAfterDateTime;

    private String certificatePem;
    private String chainCertificatePem;
    private String privateKeyPem;

    private String publicKeyAlgorithm;
    private String signatureAlgorithm;

    private CertificateStatus status;

    public static CertificateInfo fromCertificateDtoAndCertificate(CertificateDto certificateDto, X509Certificate certificate) {
        return CertificateInfo.builder()
            .certificateId(certificateDto.getId())
            .serialNumber(CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray()))
            .subjectInfo(CertificateUtil.parseDnWithBouncyCastle(certificateDto.getSubject()))
            .issuer(certificate.getIssuerX500Principal().getName())
            .publicKeyAlgorithm(certificateDto.getKeyAlgorithm())
            .signatureAlgorithm(certificateDto.getSigningAlgorithm())
            .certificatePem(certificateDto.getCertificatePem())
            .privateKeyPem(certificateDto.getPrivateKeyPem())
            .notBeforeDateTime(certificateDto.getNotBefore())
            .notAfterDateTime(certificateDto.getNotAfter())
            .status(certificateDto.getStatus())
            .build();
    }
}
