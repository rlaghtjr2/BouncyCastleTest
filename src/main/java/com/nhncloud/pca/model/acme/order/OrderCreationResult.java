package com.nhncloud.pca.model.acme.order;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.nhncloud.pca.model.acme.authorization.Authorization;

@Data
@Builder
public class OrderCreationResult {
    private final Order order;
    private final List<Authorization> authzs;
    private final String replayNonce;
    private final String originalNonce;
}
