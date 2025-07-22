package com.nhncloud.pca.repository.acme;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;

@Repository
public interface AcmeAuthorizationRepository extends JpaRepository<AcmeAuthorizationEntity, Long> {

    // Identifier Entity를 직접 사용하는 메서드들 (권장)
    Optional<AcmeAuthorizationEntity> findByIdentifier(AcmeIdentifierEntity identifier);

    List<AcmeAuthorizationEntity> findByIdentifierAndStatus(AcmeIdentifierEntity identifier, AuthorizationStatus status);

    // 기존 identifierId 기반 메서드들 (호환성을 위해 유지, JPA가 자동으로 identifier.id로 처리)
    List<AcmeAuthorizationEntity> findByIdentifierId(Long identifierId);

    List<AcmeAuthorizationEntity> findByStatus(AuthorizationStatus status);

    // 만료된 인증 조회
    List<AcmeAuthorizationEntity> findByExpiresBefore(LocalDateTime now);

    // 와일드카드 인증 조회
    List<AcmeAuthorizationEntity> findByWildcardTrue();
}