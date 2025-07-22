package com.nhncloud.pca.repository.acme;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhncloud.pca.constant.acme.ChallengeStatus;
import com.nhncloud.pca.constant.acme.ChallengeType;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeChallengeEntity;

@Repository
public interface AcmeChallengeRepository extends JpaRepository<AcmeChallengeEntity, Long> {

    // Authorization Entity를 직접 사용하는 메서드들 (권장)
    List<AcmeChallengeEntity> findByAuthorization(AcmeAuthorizationEntity authorization);

    List<AcmeChallengeEntity> findByAuthorizationAndStatus(AcmeAuthorizationEntity authorization, ChallengeStatus status);

    List<AcmeChallengeEntity> findByAuthorizationAndType(AcmeAuthorizationEntity authorization, ChallengeType type);

    Optional<AcmeChallengeEntity> findByIdAndAuthorization(Long id, AcmeAuthorizationEntity authorization);

    // 기존 authorizationId 기반 메서드들 (호환성을 위해 유지, JPA가 자동으로 authorization.id로 처리)
    List<AcmeChallengeEntity> findByAuthorizationId(Long authorizationId);

    List<AcmeChallengeEntity> findByAuthorizationIdAndStatus(Long authorizationId, ChallengeStatus status);

    Optional<AcmeChallengeEntity> findByIdAndAuthorizationId(Long id, Long authorizationId);

    List<AcmeChallengeEntity> findByType(ChallengeType type);

    List<AcmeChallengeEntity> findByStatus(ChallengeStatus status);

    List<AcmeChallengeEntity> findByTypeAndStatus(ChallengeType type, ChallengeStatus status);

    // 토큰으로 챌린지 조회
    Optional<AcmeChallengeEntity> findByToken(String token);

    // 검증된 챌린지 조회
    List<AcmeChallengeEntity> findByValidatedIsNotNull();

    // 검증되지 않은 챌린지 조회
    List<AcmeChallengeEntity> findByValidatedIsNull();

    // 특정 기간 내 생성된 챌린지 조회
    List<AcmeChallengeEntity> findByCreationDatetimeBetween(LocalDateTime startDate, LocalDateTime endDate);
}