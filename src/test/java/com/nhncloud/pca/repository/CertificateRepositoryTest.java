package com.nhncloud.pca.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.constant.ca.CaType;
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CertificateRepositoryTest {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CaRepository caRepository;

    @Test
    void test_findId() {
        // Save CaEntity and CertificateEntity
        CaEntity ca = new CaEntity();
        ca.setName("TEST_CA_NAME");
        ca.setType(CaType.ROOT.getType());
        ca.setStatus(CaStatus.ACTIVE);
        ca.setCreationUser("HOSEOK");
        ca.setCreationDatetime(LocalDateTime.now());

        CertificateEntity certificate = new CertificateEntity();
        certificate.setCsr("TEST_CSR");
        certificate.setSubject("TEST_SUBJECT");
        certificate.setKeyAlgorithm("RSA");
        certificate.setSigningAlgorithm("SHA256withRSA");
        certificate.setCertificatePem("TEST_CERTIFICATE_PEM");
        certificate.setPrivateKeyPem("TEST_PRIVATE_KEY_PEM");
        certificate.setNotBefore(LocalDateTime.now().toString());
        certificate.setNotAfter(LocalDateTime.now().plusDays(365).toString());
        certificate.setStatus(CertificateStatus.ACTIVE);
        certificate.setCreationUser("HOSEOK");
        certificate.setCreationDatetime(LocalDateTime.now());

        certificate.setSignedCa(ca);
        certificate.setCa(ca);

        ca.setSignedCertificates(new ArrayList<>(List.of(certificate)));
        ca.setCertificate(certificate);
        // Save the entity
        caRepository.save(ca);

        ca.setSignedCa(ca);

        CaEntity savedCa = caRepository.save(ca);
        // Find the entity by ID
        CertificateEntity foundCert = certificateRepository.findById(savedCa.getCertificate().getId()).orElse(null);

        // Verify the entity is found correctly
        assertNotNull(foundCert);
        assertEquals(savedCa.getCertificate().getId(), foundCert.getId());
    }

    @Test
    void testSave() {
        // Create a new CertificateEntity
        CaEntity caEntity = new CaEntity();
        caEntity.setId(123456L);
        caEntity.setType(CaType.ROOT.getType());
        caEntity.setName("TEST_CA_NAME");

        CertificateEntity certificate = new CertificateEntity();
        certificate.setSignedCa(caEntity);
        certificate.setCertificatePem("TEST_CERTIFICATE_PEM");
        certificate.setPrivateKeyPem("TEST_PRIVATE_KEY_PEM");
        caEntity.setSignedCertificates(List.of(certificate));
        // Save the entity
        CertificateEntity savedCertificate = certificateRepository.save(certificate);

        // Verify the entity is saved correctly
        assertNotNull(savedCertificate);
        assertNotNull(savedCertificate.getId());
        assertEquals(123456L, savedCertificate.getSignedCa().getId());
        assertEquals("TEST_CERTIFICATE_PEM", savedCertificate.getCertificatePem());
        assertEquals("TEST_PRIVATE_KEY_PEM", savedCertificate.getPrivateKeyPem());
    }
}
