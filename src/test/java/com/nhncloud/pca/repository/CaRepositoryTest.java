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
public class CaRepositoryTest {

    @Autowired
    private CaRepository caRepository;

    @Test
    void testSave() {
        // Create a new CaEntity
        CaEntity ca = new CaEntity();
        ca.setName("TEST_CA_NAME");
        ca.setType(CaType.ROOT.getType());
        CertificateEntity certificate = new CertificateEntity();
        certificate.setCertificatePem("TEST_CERTIFICATE_PEM");
        certificate.setPrivateKeyPem("TEST_PRIVATE_KEY_PEM");
        certificate.setSignedCa(ca);
        ca.setSignedCertificates(List.of(certificate));
        // Save the entity
        CaEntity savedCa = caRepository.save(ca);

        // Verify the entity is saved correctly
        assertNotNull(savedCa);
        assertNotNull(savedCa.getCaId());
        assertEquals("TEST_CA_NAME", savedCa.getName());
        assertEquals(CaType.ROOT.getType(), savedCa.getType());
    }
}
