package com.nhncloud.pca.repository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.nhncloud.pca.constant.CaType;
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
    void testSave() {
        // Create a new CertificateEntity
        CaEntity caEntity = new CaEntity();
        caEntity.setCaId(123456L);
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
        assertNotNull(savedCertificate.getCertificateId());
        assertEquals(123456L, savedCertificate.getSignedCa().getCaId());
        assertEquals("TEST_CERTIFICATE_PEM", savedCertificate.getCertificatePem());
        assertEquals("TEST_PRIVATE_KEY_PEM", savedCertificate.getPrivateKeyPem());
    }
}
