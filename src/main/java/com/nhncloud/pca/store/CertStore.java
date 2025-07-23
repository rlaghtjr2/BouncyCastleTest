package com.nhncloud.pca.store;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.repository.CertificateRepository;

@Component
public class CertStore {

    private final CertificateRepository certificateRepository;

    public CertStore(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    public Long save(X509Certificate cert) {
        try {
            // X509Certificate를 PEM 문자열로 변환
            String pemCert = convertToPem(cert);

            // DB에 저장 (기존 CertificateEntity 활용)
            CertificateEntity certEntity = new CertificateEntity();
            certEntity.setCertificatePem(pemCert);

            CertificateEntity saved = certificateRepository.save(certEntity);

            // 실제 DB에서 생성된 Long ID를 반환
            return saved.getId();

        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate to database: " + e.getMessage(), e);
        }
    }

    public X509Certificate get(Long id) {
        try {
            Optional<CertificateEntity> certEntity = certificateRepository.findById(id); // Long 직접 사용

            if (certEntity.isPresent()) {
                String pemCert = certEntity.get().getCertificatePem();
                return convertFromPem(pemCert);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get certificate from database: " + e.getMessage(), e);
        }
    }

    /**
     * X509Certificate를 PEM 문자열로 변환
     */
    private String convertToPem(X509Certificate cert) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("-----BEGIN CERTIFICATE-----\n");
            sb.append(java.util.Base64.getEncoder().encodeToString(cert.getEncoded()));
            sb.append("\n-----END CERTIFICATE-----\n");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert certificate to PEM", e);
        }
    }

    /**
     * PEM 문자열을 X509Certificate로 변환
     */
    private X509Certificate convertFromPem(String pemCert) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(pemCert.getBytes()));
        } catch (CertificateException e) {
            throw new RuntimeException("Failed to convert PEM to certificate", e);
        }
    }
}
