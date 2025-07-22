package com.nhncloud.pca.store;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeChallengeEntity;
import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.challenge.Challenge;
import com.nhncloud.pca.repository.acme.AcmeAuthorizationRepository;
import com.nhncloud.pca.repository.acme.AcmeChallengeRepository;
import com.nhncloud.pca.repository.acme.AcmeIdentifierRepository;

@Component
public class AuthorizationStore {

    private final AcmeAuthorizationRepository acmeAuthorizationRepository;
    private final AcmeIdentifierRepository acmeIdentifierRepository;
    private final AcmeChallengeRepository acmeChallengeRepository;

    public AuthorizationStore(AcmeAuthorizationRepository acmeAuthorizationRepository,
                             AcmeIdentifierRepository acmeIdentifierRepository,
                             AcmeChallengeRepository acmeChallengeRepository) {
        this.acmeAuthorizationRepository = acmeAuthorizationRepository;
        this.acmeIdentifierRepository = acmeIdentifierRepository;
        this.acmeChallengeRepository = acmeChallengeRepository;
    }

    /**
     * Authorization을 DB에 저장하는 메서드 (Identifier Entity 참조 사용)
     */
    public Authorization createAuthorizationWithDatabase(Identifier identifier, List<Challenge> challenges) {
        try {
            // 1. Identifier의 type과 value로 DB에서 해당 Entity 조회
            List<AcmeIdentifierEntity> identifierEntities =
                acmeIdentifierRepository.findByTypeAndValue(identifier.getType(), identifier.getValue());

            if (identifierEntities.isEmpty()) {
                throw new RuntimeException("Identifier not found in database: " + identifier.getType() + "=" + identifier.getValue());
            }

            // 가장 최근에 생성된 Identifier Entity 사용 (여러 개가 있을 경우)
            AcmeIdentifierEntity identifierEntity = identifierEntities.get(identifierEntities.size() - 1);

            // 2. DB에 Authorization Entity 저장 (Identifier Entity 참조 사용)
            AcmeAuthorizationEntity authzEntity = AcmeAuthorizationEntity.builder()
                    .identifier(identifierEntity) // Identifier Entity 직접 참조
                    .status(AuthorizationStatus.PENDING)
                    .expires(LocalDateTime.now().plusHours(1)) // 1시간 후 만료
                    .build();

            AcmeAuthorizationEntity savedAuthz = acmeAuthorizationRepository.save(authzEntity);

            // 3. Authorization 객체 생성 (기존 로직과 호환성 유지)
            Authorization authorization = Authorization.builder()
                    .id(savedAuthz.getId().toString())
                    .identifier(identifier)
                    .status(savedAuthz.getStatus())
                    .expires(savedAuthz.getExpires())
                    .wildcard(savedAuthz.getWildcard())
                    .challenges(challenges)
                    .build();

            return authorization;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save authorization to database: " + e.getMessage(), e);
        }
    }

    /**
     * AcmeIdentifierEntity를 직접 받아서 Authorization을 생성하는 메서드 (더 명확한 방식)
     * 저장된 AcmeAuthorizationEntity도 함께 반환
     */
    public AuthorizationWithEntity createAuthorizationWithIdentifierEntity(AcmeIdentifierEntity identifierEntity,
                                                               List<Challenge> challenges) {
        try {
            // 1. DB에 Authorization Entity 저장
            AcmeAuthorizationEntity authzEntity = AcmeAuthorizationEntity.builder()
                    .identifier(identifierEntity) // Identifier Entity 직접 참조
                    .status(AuthorizationStatus.PENDING)
                    .expires(LocalDateTime.now().plusHours(1)) // 1시간 후 만료
                    .build();

            AcmeAuthorizationEntity savedAuthz = acmeAuthorizationRepository.save(authzEntity);

            // 2. Authorization 객체 생성
            Identifier identifier = Identifier.builder()
                    .type(identifierEntity.getType())
                    .value(identifierEntity.getValue())
                    .build();

            Authorization authorization = Authorization.builder()
                    .id(savedAuthz.getId().toString())
                    .identifier(identifier)
                    .status(savedAuthz.getStatus())
                    .expires(savedAuthz.getExpires())
                    .wildcard(savedAuthz.getWildcard())
                    .challenges(challenges)
                    .build();

            // 3. Authorization과 Entity 모두 반환
            return new AuthorizationWithEntity(authorization, savedAuthz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save authorization to database: " + e.getMessage(), e);
        }
    }

    /**
     * Authorization과 저장된 Entity를 함께 담는 클래스
     */
    public static class AuthorizationWithEntity {
        private final Authorization authorization;
        private final AcmeAuthorizationEntity entity;

        public AuthorizationWithEntity(Authorization authorization, AcmeAuthorizationEntity entity) {
            this.authorization = authorization;
            this.entity = entity;
        }

        public Authorization getAuthorization() {
            return authorization;
        }

        public AcmeAuthorizationEntity getEntity() {
            return entity;
        }
    }

    public Authorization getAuthorization(String id) {
        try {
            Long authzId = Long.parseLong(id);
            Optional<AcmeAuthorizationEntity> authzEntity = acmeAuthorizationRepository.findById(authzId);

            if (authzEntity.isPresent()) {
                AcmeAuthorizationEntity entity = authzEntity.get();

                // JPA 관계를 통해 Identifier 조회
                Identifier identifier = null;
                if (entity.getIdentifier() != null) {
                    identifier = Identifier.builder()
                            .type(entity.getIdentifier().getType())
                            .value(entity.getIdentifier().getValue())
                            .build();
                }

                // JPA 관계를 통해 Challenge들 조회
                List<Challenge> challenges = new ArrayList<>();
                if (entity.getChallenges() != null && !entity.getChallenges().isEmpty()) {
                    challenges = entity.getChallenges().stream()
                            .map(challengeEntity -> {
                                Challenge.ChallengeBuilder builder = Challenge.builder()
                                        .id(challengeEntity.getId().toString())
                                        .type(challengeEntity.getType())
                                        .status(challengeEntity.getStatus())
                                        .url(challengeEntity.getUrl())
                                        .token(challengeEntity.getToken());

                                // validated 필드가 null이 아닌 경우에만 매핑
                                if (challengeEntity.getValidated() != null) {
                                    builder.validate(challengeEntity.getValidated());
                                }

                                return builder.build();
                            })
                            .collect(Collectors.toList());
                }

                return Authorization.builder()
                    .id(entity.getId().toString())
                    .identifier(identifier) // JPA 관계를 통해 조회한 Identifier
                    .status(entity.getStatus())
                    .expires(entity.getExpires())
                    .wildcard(entity.getWildcard())
                    .challenges(challenges) // JPA 관계를 통해 조회한 Challenge들
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
        try {
            Long challengeIdLong = Long.parseLong(challengeId);

            // 1. Challenge Entity 조회
            Optional<AcmeChallengeEntity> challengeEntity = acmeChallengeRepository.findById(challengeIdLong);

            if (challengeEntity.isPresent() && challengeEntity.get().getAuthorization() != null) {
                // 2. JPA 관계를 통해 Authorization Entity 조회
                AcmeAuthorizationEntity authzEntity = challengeEntity.get().getAuthorization();

                // 3. Authorization 모델로 변환
                Identifier identifier = null;
                if (authzEntity.getIdentifier() != null) {
                    identifier = Identifier.builder()
                            .type(authzEntity.getIdentifier().getType())
                            .value(authzEntity.getIdentifier().getValue())
                            .build();
                }

                // Challenge 정보는 포함하지 않음 (순환 참조 방지)
                return Authorization.builder()
                        .id(authzEntity.getId().toString())
                        .identifier(identifier)
                        .status(authzEntity.getStatus())
                        .expires(authzEntity.getExpires())
                        .wildcard(authzEntity.getWildcard())
                        .challenges(new ArrayList<>()) // 빈 리스트로 설정 (순환 참조 방지)
                        .build();
            }

            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
