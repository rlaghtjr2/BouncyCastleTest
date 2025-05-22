package com.nhncloud.pca.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.entity.CaEntity;

public interface CaRepository extends JpaRepository<CaEntity, Long> {
    Page<CaEntity> findByStatusNot(CaStatus status, Pageable pageable);

    Optional<CaEntity> findByIdAndStatusNot(Long id, CaStatus status);
}
