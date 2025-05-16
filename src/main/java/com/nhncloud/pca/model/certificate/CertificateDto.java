package com.nhncloud.pca.model.certificate;

import lombok.Builder;
import lombok.Data;

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.model.ca.CaDto;

@Data
@Builder
public class CertificateDto {
    private Long id;
    private CaDto ca;
    private String csr;
    private String subject;
    private String keyAlgorithm;
    private String signingAlgorithm;
    private String certificatePem;
    private String privateKeyPem;
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;
    private CaDto signedCa;
    private CertificateStatus status;
    private String creationUser;
    private LocalDateTime creationDatetime;
    private String lastChangeUser;
    private LocalDateTime lastChangeDatetime;

    public void setX509Certificate(X509Certificate certificate) {
        this.setSubject(certificate.getSubjectX500Principal().getName());
        this.setKeyAlgorithm(certificate.getPublicKey().getAlgorithm());
        this.setSigningAlgorithm(certificate.getSigAlgName());
        this.setNotBefore(LocalDateTime.ofInstant(certificate.getNotBefore().toInstant(), ZoneId.systemDefault()));
        this.setNotAfter(LocalDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.systemDefault()));
    }
}
