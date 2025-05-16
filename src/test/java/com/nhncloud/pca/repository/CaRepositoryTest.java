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
public class CaRepositoryTest {

    @Autowired
    private CaRepository caRepository;

    @Test
    void testFindId() {
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
        CaEntity foundCa = caRepository.findById(savedCa.getId()).orElse(null);

        // Verify the entity is found correctly
        assertNotNull(foundCa);
        assertEquals(savedCa.getId(), foundCa.getId());
    }

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
        assertNotNull(savedCa.getId());
        assertEquals("TEST_CA_NAME", savedCa.getName());
        assertEquals(CaType.ROOT.getType(), savedCa.getType());
    }
}
