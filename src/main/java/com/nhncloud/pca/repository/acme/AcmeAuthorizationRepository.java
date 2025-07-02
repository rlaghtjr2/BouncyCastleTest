package com.nhncloud.pca.repository.acme;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;

@Repository
public interface AcmeAuthorizationRepository extends JpaRepository<AcmeAuthorizationEntity, Long> {

    List<AcmeAuthorizationEntity> findByOrderId(Long orderId);

    List<AcmeAuthorizationEntity> findByOrderIdAndStatus(Long orderId, AuthorizationStatus status);

    Optional<AcmeAuthorizationEntity> findByIdAndOrderId(Long id, Long orderId);

    List<AcmeAuthorizationEntity> findByIdentifierId(Long identifierId);

    List<AcmeAuthorizationEntity> findByStatus(AuthorizationStatus status);

    // 만료된 인증 조회
    List<AcmeAuthorizationEntity> findByExpiresBefore(LocalDateTime now);

    // 와일드카드 인증 조회
    List<AcmeAuthorizationEntity> findByWildcardTrue();
}