package com.nhncloud.pca.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.constant.ca.CaType;
import com.nhncloud.pca.entity.CaEntity;

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
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        ca.setId(null);

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
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        ca.setId(null);
        // Save the entity
        CaEntity savedCa = caRepository.save(ca);

        // Verify the entity is saved correctly
        assertNotNull(savedCa);
        assertNotNull(savedCa.getId());
        assertEquals("Test CA", savedCa.getName());
        assertEquals(CaType.ROOT.getType(), savedCa.getType());
    }
}
