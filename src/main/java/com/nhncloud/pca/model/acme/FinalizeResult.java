package com.nhncloud.pca.model.acme;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinalizeResult {
    private final String status;
    private final String replayNonce;
}
