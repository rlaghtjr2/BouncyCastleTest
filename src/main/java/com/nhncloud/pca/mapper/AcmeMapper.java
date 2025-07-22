package com.nhncloud.pca.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeChallengeEntity;
import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.challenge.Challenge;

@Mapper(componentModel = "spring")
public interface AcmeMapper {

    /**
     * AcmeIdentifierEntity를 Identifier로 변환
     */
    @Mapping(target = "type", source = "type")
    @Mapping(target = "value", source = "value")
    Identifier toIdentifier(AcmeIdentifierEntity entity);

    /**
     * AcmeChallengeEntity를 Challenge로 변환
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "url", source = "url")
    @Mapping(target = "token", source = "token")
    @Mapping(target = "validate", source = "validated")
    Challenge toChallenge(AcmeChallengeEntity entity);

    /**
     * AcmeChallengeEntity 리스트를 Challenge 리스트로 변환
     */
    default List<Challenge> toChallengeList(List<AcmeChallengeEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        return entities.stream()
                .map(this::toChallenge)
                .collect(Collectors.toList());
    }

    /**
     * AcmeAuthorizationEntity를 Authorization으로 변환 (Challenge 없이)
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "identifier", source = "identifier")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "expires", source = "expires")
    @Mapping(target = "wildcard", source = "wildcard")
    @Mapping(target = "challenges", ignore = true) // Challenge는 별도 설정
    Authorization toAuthorization(AcmeAuthorizationEntity entity);

    /**
     * AcmeAuthorizationEntity를 Authorization으로 완전 변환 (모든 관계 포함)
     */
    default Authorization toAuthorizationWithRelations(AcmeAuthorizationEntity entity) {
        if (entity == null) {
            return null;
        }

        Authorization authorization = toAuthorization(entity);

        // JPA 관계를 통한 Challenge 리스트 변환
        if (entity.getChallenges() != null) {
            authorization.setChallenges(toChallengeList(entity.getChallenges()));
        }

        return authorization;
    }

    /**
     * AcmeAuthorizationEntity를 Authorization으로 변환 (Challenge 포함)
     */
    default Authorization toAuthorizationWithChallenges(AcmeAuthorizationEntity entity, List<Challenge> challenges) {
        Authorization authorization = toAuthorization(entity);
        authorization.setChallenges(challenges);
        return authorization;
    }

    /**
     * AcmeIdentifierEntity와 AcmeAuthorizationEntity를 사용하여 Authorization 생성 (Challenge 포함)
     */
    default Authorization toAuthorizationWithIdentifierAndChallenges(AcmeIdentifierEntity identifierEntity,
                                                                   AcmeAuthorizationEntity authorizationEntity,
                                                                   List<Challenge> challenges) {
        Identifier identifier = toIdentifier(identifierEntity);
        Authorization authorization = toAuthorization(authorizationEntity);
        authorization.setIdentifier(identifier);
        authorization.setChallenges(challenges);
        return authorization;
    }
}