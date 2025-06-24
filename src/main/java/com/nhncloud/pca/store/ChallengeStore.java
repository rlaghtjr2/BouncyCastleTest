package com.nhncloud.pca.store;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.ChallengeStatus;
import com.nhncloud.pca.model.acme.challenge.Challenge;

@Component
public class ChallengeStore {
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public Challenge createChallenge(String baseUrl) {
        String challengeId = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();
        Challenge challenge = Challenge.builder()
            .id(challengeId)
            .token(token)
            .url(baseUrl + "/acme/challenge/" + challengeId)
            .build();
        challenges.put(challengeId, challenge);
        return challenge;
    }

    public Challenge getChallenge(String id) {
        return challenges.get(id);
    }

    public void markValid(String id) {
        Challenge challenge = challenges.get(id);
        if (challenge != null) {
            challenge.getStatus().equals(ChallengeStatus.VALID);
        }
    }
}
