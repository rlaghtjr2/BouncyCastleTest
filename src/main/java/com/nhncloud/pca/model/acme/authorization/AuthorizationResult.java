package com.nhncloud.pca.model.acme.authorization;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.nhncloud.pca.model.acme.Identifier;

@Data
@Builder
public class AuthorizationResult {
    private final Identifier identifier;
    private final String status;
    private final Instant expires;
    private final List<Map<String, String>> challenges;
    private final String replayNonce;
    private final String upLink;  // nullable
}
