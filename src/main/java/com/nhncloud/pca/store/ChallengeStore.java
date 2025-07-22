package com.nhncloud.pca.store;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.ChallengeStatus;
import com.nhncloud.pca.constant.acme.ChallengeType;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeChallengeEntity;
import com.nhncloud.pca.model.acme.challenge.Challenge;
import com.nhncloud.pca.repository.acme.AcmeChallengeRepository;

@Component
public class ChallengeStore {
    private final AcmeChallengeRepository acmeChallengeRepository;

    public ChallengeStore(AcmeChallengeRepository acmeChallengeRepository) {
        this.acmeChallengeRepository = acmeChallengeRepository;
    }

    /**
     * AcmeAuthorizationEntity를 직접 받아서 Challenge를 생성하는 메서드
     */
    public Challenge createChallengeWithAuthorizationEntity(AcmeAuthorizationEntity authorizationEntity, String baseUrl) {
        try {
            String token = UUID.randomUUID().toString();

            // 1. DB에 Challenge Entity 저장 (Authorization Entity 직접 참조)
            AcmeChallengeEntity challengeEntity = AcmeChallengeEntity.builder()
                .authorization(authorizationEntity) // Authorization Entity 직접 참조
                .type(ChallengeType.HTTP_01) // 기본 타입
                .status(ChallengeStatus.PENDING)
                .url(baseUrl + "/acme/challenge/") // 임시 URL, 저장 후 업데이트
                .token(token)
                .build();

            AcmeChallengeEntity savedChallenge = acmeChallengeRepository.save(challengeEntity);

            // 2. DB에서 생성된 ID로 URL 업데이트
            savedChallenge.setUrl(baseUrl + "/acme/challenge/" + savedChallenge.getId());
            acmeChallengeRepository.save(savedChallenge);

            // 3. Challenge 객체 생성
            Challenge challenge = Challenge.builder()
                .id(savedChallenge.getId()) // Long id 직접 사용
                .type(savedChallenge.getType())
                .status(savedChallenge.getStatus())
                .url(savedChallenge.getUrl())
                .token(savedChallenge.getToken())
                .build();

            return challenge;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save challenge to database: " + e.getMessage(), e);
        }
    }

    public Challenge getChallenge(Long id) {
        try {
            Optional<AcmeChallengeEntity> challengeEntity = acmeChallengeRepository.findById(id);

            if (challengeEntity.isPresent()) {
                AcmeChallengeEntity entity = challengeEntity.get();
                return Challenge.builder()
                    .id(entity.getId()) // Long id 직접 사용
                    .type(entity.getType())
                    .status(entity.getStatus())
                    .url(entity.getUrl())
                    .token(entity.getToken())
                    .build();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void markValid(Long id) {
        Optional<AcmeChallengeEntity> challengeEntity = acmeChallengeRepository.findById(id);
        challengeEntity.ifPresent(acmeChallengeEntity -> {
            acmeChallengeEntity.setStatus(ChallengeStatus.VALID);
            acmeChallengeRepository.save(acmeChallengeEntity);
        });
    }
}
