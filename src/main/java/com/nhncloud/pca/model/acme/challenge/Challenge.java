package com.nhncloud.pca.model.acme.challenge;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.acme.ChallengeStatus;
import com.nhncloud.pca.constant.acme.ChallengeType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Challenge {
    private Long id;
    private String url;
    @Builder.Default
    private ChallengeType type = ChallengeType.HTTP_01;
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.PENDING;
    private String token;
    @Builder.Default
    private LocalDateTime validate = LocalDateTime.now().plusMinutes(10);
}
