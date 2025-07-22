package com.nhncloud.pca.model.acme.authorization;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.challenge.Challenge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Authorization {
    private Long id;
    @Builder.Default
    private AuthorizationStatus status = AuthorizationStatus.PENDING;
    private Identifier identifier;
    @Builder.Default
    private LocalDateTime expires = LocalDateTime.now().plusSeconds(300);
    @Builder.Default
    private List<Challenge> challenges = new ArrayList<>();
    private boolean wildcard;
}
