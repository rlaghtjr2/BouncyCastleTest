package com.nhncloud.pca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CertificateEntity;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
    Optional<CertificateEntity> findByCa_Id(Long caId);

    Optional<CertificateEntity> findByIdAndSignedCaIdAndStatusNot(Long certificateId, String caId, CertificateStatus status);

    Optional<List<CertificateEntity>> findBySignedCaIdAndCaIsNullAndStatusNot(String signedCaCaId, CertificateStatus status);
}
