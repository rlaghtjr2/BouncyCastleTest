package com.nhncloud.pca.model.acme;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.acme.ChallengeStatus;
import com.nhncloud.pca.constant.acme.ChallengeType;

public class Challenge {
    private String url;
    private ChallengeType type;
    private ChallengeStatus status;
    private String token;
    private LocalDateTime validated;
}
