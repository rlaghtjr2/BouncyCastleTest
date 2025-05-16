package com.nhncloud.pca.model.certificate;

import lombok.Builder;
import lombok.Data;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.model.key.KeyInfo;
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

    public static CertificateInfo of(SubjectInfo subjectInfo, X509Certificate certificate, KeyInfo keyInfo, String certPem, String privateKeyPem, CertificateStatus status) {
        return CertificateInfo.builder()
            .serialNumber(CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray()))
            .subjectInfo(subjectInfo)
            .issuer(certificate.getIssuerX500Principal().getName())
            .notBeforeDateTime(certificate.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
            .notAfterDateTime(certificate.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
            .certificatePem(certPem)
            .privateKeyPem(privateKeyPem)
            .publicKeyAlgorithm(keyInfo.getAlgorithm() + keyInfo.getKeySize())
            .signatureAlgorithm(certificate.getSigAlgName())
            .status(status)
            .build();
    }

    public static CertificateDto toCertificateDto(CertificateInfo certificateInfo) {
        return CertificateDto.builder()
            .id(certificateInfo.getCertificateId())
            .subject(certificateInfo.getSubjectInfo().toDistinguishedName())
            .keyAlgorithm(certificateInfo.getPublicKeyAlgorithm())
            .signingAlgorithm(certificateInfo.getSignatureAlgorithm())
            .certificatePem(certificateInfo.getCertificatePem())
            .privateKeyPem(certificateInfo.getPrivateKeyPem())
            .notBefore(certificateInfo.getNotBeforeDateTime())
            .notAfter(certificateInfo.getNotAfterDateTime())
            .status(certificateInfo.getStatus())
            .build();
    }
}
