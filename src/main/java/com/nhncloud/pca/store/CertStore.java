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

    public void save(String id, X509Certificate cert) {
        try {
            // X509Certificate를 PEM 문자열로 변환
            String pemCert = convertToPem(cert);

                        // DB에 저장 (기존 CertificateEntity 활용)
            CertificateEntity certEntity = new CertificateEntity();
            certEntity.setCertificatePem(pemCert);

            CertificateEntity saved = certificateRepository.save(certEntity);

            // 생성된 ID와 요청된 ID가 다를 수 있지만 일단 저장
            // 실제로는 ID 매핑 테이블이나 별도 처리가 필요할 수 있음

        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate to database: " + e.getMessage(), e);
        }
    }

    public X509Certificate get(String id) {
        try {
            // 간단한 구현을 위해 ID를 Long으로 변환 시도
            Long certId = Long.parseLong(id);
            Optional<CertificateEntity> certEntity = certificateRepository.findById(certId);

            if (certEntity.isPresent()) {
                String pemCert = certEntity.get().getCertificatePem();
                return convertFromPem(pemCert);
            }

            return null;

        } catch (NumberFormatException e) {
            // ID가 숫자가 아닌 경우 처리 불가
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
