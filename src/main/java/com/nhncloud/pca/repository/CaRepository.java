package com.nhncloud.pca.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhncloud.pca.entity.CaEntity;

public interface CaRepository extends JpaRepository<CaEntity, Long> {
}
