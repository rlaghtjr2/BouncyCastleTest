package com.nhncloud.pca.model.acme;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificateResult {
    private final String pemChain;
    private final String replayNonce;
}
