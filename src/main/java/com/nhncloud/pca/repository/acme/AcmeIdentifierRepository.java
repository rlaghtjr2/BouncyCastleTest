package com.nhncloud.pca.repository.acme;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;

@Repository
public interface AcmeIdentifierRepository extends JpaRepository<AcmeIdentifierEntity, Long> {

    List<AcmeIdentifierEntity> findByOrderId(Long orderId);

    List<AcmeIdentifierEntity> findByType(String type);

    List<AcmeIdentifierEntity> findByValue(String value);

    List<AcmeIdentifierEntity> findByTypeAndValue(String type, String value);
}