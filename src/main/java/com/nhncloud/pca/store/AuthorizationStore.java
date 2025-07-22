package com.nhncloud.pca.store;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeChallengeEntity;
import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;
import com.nhncloud.pca.mapper.AcmeMapper;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.repository.acme.AcmeAuthorizationRepository;
import com.nhncloud.pca.repository.acme.AcmeChallengeRepository;
import com.nhncloud.pca.repository.acme.AcmeIdentifierRepository;

@Component
public class AuthorizationStore {

    private final AcmeAuthorizationRepository acmeAuthorizationRepository;
    private final AcmeIdentifierRepository acmeIdentifierRepository;
    private final AcmeChallengeRepository acmeChallengeRepository;
    private final AcmeMapper acmeMapper;

    public AuthorizationStore(AcmeAuthorizationRepository acmeAuthorizationRepository,
                              AcmeIdentifierRepository acmeIdentifierRepository,
                              AcmeChallengeRepository acmeChallengeRepository,
                              AcmeMapper acmeMapper) {
        this.acmeAuthorizationRepository = acmeAuthorizationRepository;
        this.acmeIdentifierRepository = acmeIdentifierRepository;
        this.acmeChallengeRepository = acmeChallengeRepository;
        this.acmeMapper = acmeMapper;
    }

    /**
     * AcmeIdentifierEntity를 직접 받아서 Authorization Entity를 생성하는 메서드 (DB 접근 최적화)
     */
    public AcmeAuthorizationEntity createAuthorizationWithIdentifierEntity(AcmeIdentifierEntity identifierEntity) {
        try {
            // 1. DB에 Authorization Entity 저장
            AcmeAuthorizationEntity authzEntity = AcmeAuthorizationEntity.builder()
                .identifier(identifierEntity) // Identifier Entity 직접 참조
                .status(AuthorizationStatus.PENDING)
                .expires(LocalDateTime.now().plusHours(1)) // 1시간 후 만료
                .build();

            // 2. 저장된 Entity를 바로 반환 (DB 접근 최적화)
            return acmeAuthorizationRepository.save(authzEntity);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save authorization to database: " + e.getMessage(), e);
        }
    }

    /**
     * Authorization 조회 (모든 관계 포함 - 리팩토링된 간결한 버전)
     */
    public Authorization getAuthorization(Long id) {
        return acmeAuthorizationRepository.findById(id)
            .map(acmeMapper::toAuthorizationWithRelations)
            .orElse(null);
    }

    public void markValid(Long id) {
        try {
            Optional<AcmeAuthorizationEntity> authzEntity = acmeAuthorizationRepository.findById(id);
            if (authzEntity.isPresent()) {
                AcmeAuthorizationEntity entity = authzEntity.get();
                entity.setStatus(AuthorizationStatus.VALID);
                acmeAuthorizationRepository.save(entity);
            }
        } catch (Exception e) {
            // 무시
        }
    }

    /**
     * Challenge ID로 Authorization 조회 (순환 참조 방지를 위해 Challenge 미포함 - 리팩토링된 간결한 버전)
     */
    public Authorization findAuthorizationByChallengeId(String challengeId) {
        try {
            Long challengeIdLong = Long.parseLong(challengeId);
            return acmeChallengeRepository.findById(challengeIdLong)
                .map(AcmeChallengeEntity::getAuthorization)
                .map(acmeMapper::toAuthorization) // Challenge 없는 버전 사용 (순환 참조 방지)
                .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
