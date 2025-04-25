package com.nhncloud.pca.repository;

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
public class CaRepositoryTest {

    @Autowired
    private CaRepository caRepository;

    @Test
    void testFindById() {
        // Create a new Ca
        CaEntity caEntity = new CaEntity();
        caEntity.setType(CaType.ROOT.getType());
        caEntity.setName("TEST_CA_NAME");

        CertificateEntity certificate = new CertificateEntity();
        certificate.setCa(caEntity);
        certificate.setCertificatePem("TEST_CERTIFICATE_PEM");
        certificate.setPrivateKeyPem("TEST_PRIVATE_KEY_PEM");
        certificate.setSignedCaId(123456L);
        caEntity.setCertificate(certificate);

        // Save the entity
        CaEntity savedCa = caRepository.save(caEntity);

        // Find the entity by ID
        CaEntity foundCa = caRepository.findById(savedCa.getCaId()).orElse(null);

        // Verify the entity is found correctly
        assertNotNull(foundCa);
        assertEquals(savedCa.getCaId(), foundCa.getCaId());
    }

    @Test
    void testFindByCaIdAndType() {
        // Create a new Ca
        CaEntity caEntity = new CaEntity();
        caEntity.setType(CaType.ROOT.getType());
        caEntity.setName("TEST_CA_NAME");

        CertificateEntity certificate = new CertificateEntity();
        certificate.setCa(caEntity);
        certificate.setCertificatePem("TEST_CERTIFICATE_PEM");
        certificate.setPrivateKeyPem("TEST_PRIVATE_KEY_PEM");
        certificate.setSignedCaId(123456L);
        caEntity.setCertificate(certificate);

        // Save the entity
        CaEntity savedCa = caRepository.save(caEntity);

        // Find the entity by ID and type
        CaEntity foundCa = caRepository.findByCaIdAndType(savedCa.getCaId(), CaType.ROOT.getType());

        // Verify the entity is found correctly
        assertNotNull(foundCa);
        assertEquals(savedCa.getCaId(), foundCa.getCaId());
    }

    @Test
    void testSave() {
        // Create a new CaEntity
        CaEntity ca = new CaEntity();
        ca.setName("TEST_CA_NAME");
        ca.setType(CaType.ROOT.getType());

        // Save the entity
        CaEntity savedCa = caRepository.save(ca);

        // Verify the entity is saved correctly
        assertNotNull(savedCa);
        assertNotNull(savedCa.getCaId());
        assertEquals("TEST_CA_NAME", savedCa.getName());
        assertEquals(CaType.ROOT.getType(), savedCa.getType());
    }
}
