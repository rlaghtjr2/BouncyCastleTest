package com.nhncloud.pca.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhncloud.pca.entity.CertificateEntity;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
    Optional<CertificateEntity> findByCaId(Long caId);
}
