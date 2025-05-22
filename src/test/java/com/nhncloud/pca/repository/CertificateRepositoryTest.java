package com.nhncloud.pca.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.nhncloud.pca.CommonTestUtil;
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
        // Save CertificateEntity
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();

        CertificateEntity savedCertificate = certificateRepository.save(certificate);
        // Find the entity by ID
        CertificateEntity foundCert = certificateRepository.findById(savedCertificate.getId()).orElse(null);

        // Verify the entity is found correctly
        assertNotNull(foundCert);
        assertEquals(savedCertificate.getId(), foundCert.getId());
    }

    @Test
    void testSave() {
        // Create a new CertificateEntity
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();

        CertificateEntity savedCertificate = certificateRepository.save(certificate);

        // Verify the entity is saved correctly
        assertNotNull(savedCertificate);
        assertNotNull(savedCertificate.getId());
//        assertEquals(123456L, savedCertificate.getSignedCa().getId());
        assertEquals(CommonTestUtil.ROOT_CA_CERT_PEM, savedCertificate.getCertificatePem());
        assertEquals(CommonTestUtil.ROOT_CA_KEY_PEM, savedCertificate.getPrivateKeyPem());
    }
}
