package com.nhncloud.pca.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.challenge.Challenge;
import com.nhncloud.pca.repository.acme.AcmeAuthorizationRepository;

@Component
public class AuthorizationStore {

    private final AcmeAuthorizationRepository acmeAuthorizationRepository;

    public AuthorizationStore(AcmeAuthorizationRepository acmeAuthorizationRepository) {
        this.acmeAuthorizationRepository = acmeAuthorizationRepository;
    }

    /**
     * Authorization을 DB에 저장하는 메서드
     */
    public Authorization createAuthorizationWithDatabase(Identifier identifier, List<Challenge> challenges) {
        try {
            // 1. DB에 Authorization Entity 저장
            AcmeAuthorizationEntity authzEntity = AcmeAuthorizationEntity.builder()
                    .identifierId(1L) // 임시값 - 나중에 Identifier Entity 연동 시 개선
                    .status(AuthorizationStatus.PENDING)
                    .expires(LocalDateTime.now().plusHours(1)) // 1시간 후 만료
                    .build();

            AcmeAuthorizationEntity savedAuthz = acmeAuthorizationRepository.save(authzEntity);

            // 2. Authorization 객체 생성 (기존 로직과 호환성 유지)
            Authorization authorization = Authorization.builder()
                    .id(savedAuthz.getId().toString())
                    .identifier(identifier)
                    .status(savedAuthz.getStatus())
                    .expires(savedAuthz.getExpires())
                    .challenges(challenges)
                    .build();

            return authorization;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save authorization to database: " + e.getMessage(), e);
        }
    }

    public Authorization getAuthorization(String id) {
        try {
            Long authzId = Long.parseLong(id);
            Optional<AcmeAuthorizationEntity> authzEntity = acmeAuthorizationRepository.findById(authzId);

            if (authzEntity.isPresent()) {
                AcmeAuthorizationEntity entity = authzEntity.get();
                return Authorization.builder()
                    .id(entity.getId().toString())
                    .status(entity.getStatus())
                    .expires(entity.getExpires())
                    .wildcard(entity.getWildcard())
                    // identifier와 challenges는 필요시 별도 조회
                    .build();
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void markValid(String id) {
        try {
            Long authzId = Long.parseLong(id);
            Optional<AcmeAuthorizationEntity> authzEntity = acmeAuthorizationRepository.findById(authzId);
            if (authzEntity.isPresent()) {
                AcmeAuthorizationEntity entity = authzEntity.get();
                entity.setStatus(AuthorizationStatus.VALID);
                acmeAuthorizationRepository.save(entity);
            }
        } catch (NumberFormatException e) {
            // 무시
        }
    }

    public void markInvalid(String id) {
        try {
            Long authzId = Long.parseLong(id);
            Optional<AcmeAuthorizationEntity> authzEntity = acmeAuthorizationRepository.findById(authzId);
            if (authzEntity.isPresent()) {
                AcmeAuthorizationEntity entity = authzEntity.get();
                entity.setStatus(AuthorizationStatus.INVALID);
                acmeAuthorizationRepository.save(entity);
            }
        } catch (NumberFormatException e) {
            // 무시
        }
    }

    public Authorization findAuthorizationByChallengeId(String challengeId) {
        // Challenge와 Authorization 간의 관계를 DB에서 조회
        // 현재는 메모리 기반이었으므로 단순 구현
        // 실제로는 Challenge Entity에서 authorizationId를 통해 조회 필요
        try {
            Long challengeIdLong = Long.parseLong(challengeId);
            // ChallengeRepository를 통해 authorizationId를 조회한 후
            // 해당 Authorization을 조회하는 로직 필요
            // 현재는 임시로 null 반환
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
