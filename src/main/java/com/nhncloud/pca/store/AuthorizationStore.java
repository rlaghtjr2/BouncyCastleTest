package com.nhncloud.pca.store;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.challenge.Challenge;

@Component
public class AuthorizationStore {

    private final Map<String, Authorization> authzMap = new ConcurrentHashMap<>();

    public Authorization createAuthorization(Identifier identifier, List<Challenge> challenges) {
        String authId = UUID.randomUUID().toString();

        Authorization authz = Authorization.builder()
            .id(authId)
            .identifier(identifier)
            .challenges(challenges)
            .build();
        authzMap.put(authId, authz);
        return authz;
    }

    public Authorization getAuthorization(String id) {
        return authzMap.get(id);
    }

    public void markValid(String id) {
        Authorization authz = authzMap.get(id);
        if (authz != null) {
            authz.setStatus(AuthorizationStatus.VALID);
        }
    }

    public void markInvalid(String id) {
        Authorization authz = authzMap.get(id);
        if (authz != null) {
            authz.setStatus(AuthorizationStatus.INVALID);
        }
    }

    public Authorization findAuthorizationByChallengeId(String challengeId) {
        for (Authorization authorization : authzMap.values()) {
            for (Challenge challenge : authorization.getChallenges()) {
                if (challenge.getId().equals(challengeId)) {
                    return authorization;
                }
            }
        }
        return null;
    }
}
