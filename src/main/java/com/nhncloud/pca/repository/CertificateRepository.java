package com.nhncloud.pca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhncloud.pca.entity.CertificateEntity;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
    Optional<CertificateEntity> findByCa_Id(Long caId);

    Optional<CertificateEntity> findByIdAndSignedCa_Id(Long certificateId, Long caId);

    Optional<List<CertificateEntity>> findBySignedCa_IdAndCaIsNull(Long signedCaCaId);
}
