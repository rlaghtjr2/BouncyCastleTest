package com.nhncloud.pca.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
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
     * AcmeAuthorizationEntity를 Authorization으로 변환 (Challenge 없이)
     */
    @Mapping(target = "id", expression = "java(entity.getId().toString())")
    @Mapping(target = "identifier", source = "identifier")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "expires", source = "expires")
    @Mapping(target = "wildcard", source = "wildcard")
    @Mapping(target = "challenges", ignore = true) // Challenge는 별도 설정
    Authorization toAuthorization(AcmeAuthorizationEntity entity);

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