package com.nhncloud.pca.model.certificate;

import lombok.Builder;
import lombok.Data;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.nhncloud.pca.constant.CaStatus;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.subject.SubjectInfo;

@Data
@Builder
public class CertificateInfo {
    private Long certificateId;
    private String serialNumber;

    private String commonName;
    private String country;
    private String stateOrProvince;
    private String locality;
    private String organizationalUnit;
    private String organization;
    private String emailAddress;

    private String issuer;

    private LocalDateTime notBeforeDateTime;
    private LocalDateTime notAfterDateTime;

    private String certificatePem;
    private String chainCertificatePem;
    private String privateKeyPem;

    private String publicKeyAlgorithm;
    private String signatureAlgorithm;

    private CaStatus status;

    public static CertificateInfo of(SubjectInfo subjectInfo, X509Certificate certificate, KeyInfo keyInfo, String certPem, String privateKeyPem, CaStatus status) {
        return CertificateInfo.builder()
            .serialNumber(certificate.getSerialNumber().toString())
            .commonName(subjectInfo.getCommonName())
            .country(subjectInfo.getCountry())
            .locality(subjectInfo.getLocality())
            .stateOrProvince(subjectInfo.getStateOrProvince())
            .locality(subjectInfo.getLocality())
            .organization(subjectInfo.getOrganization())
            .organizationalUnit(subjectInfo.getOrganizationalUnit())
            .emailAddress(subjectInfo.getEmailAddress())
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

}
