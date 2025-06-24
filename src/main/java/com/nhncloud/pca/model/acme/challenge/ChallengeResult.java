package com.nhncloud.pca.model.acme.challenge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChallengeResult {
    private final String type;
    private final String url;
    private final String status;
    private final String token;
    private final String replayNonce;
    private final String upLink;
}
