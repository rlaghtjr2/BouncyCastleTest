package com.nhncloud.pca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CertificateEntity;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {
    Optional<CertificateEntity> findByCa_Id(Long caId);

    Optional<CertificateEntity> findByIdAndStatusNot(Long certificateId, CertificateStatus status);

    Optional<List<CertificateEntity>> findBySignedCertificateIdAndCaIsNullAndStatusNot(String signedCaCaId, CertificateStatus status);

    Optional<CertificateEntity> findByIdAndStatus(Long certificateId, CertificateStatus status);

    Optional<CertificateEntity> findByIdAndCa_Id(Long certificateId, Long caId);
}
