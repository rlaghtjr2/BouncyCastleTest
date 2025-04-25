package com.nhncloud.pca.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhncloud.pca.entity.CertificateEntity;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
    CertificateEntity findByCa_CaIdAndCaType(Long caId, String caType);
}
