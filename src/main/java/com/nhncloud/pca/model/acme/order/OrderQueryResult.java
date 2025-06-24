package com.nhncloud.pca.model.acme.order;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OrderQueryResult {
    private final Map<String, Object> body;
    private final String replayNonce;
}
