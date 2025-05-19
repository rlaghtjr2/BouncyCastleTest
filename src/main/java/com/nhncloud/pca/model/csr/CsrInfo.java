package com.nhncloud.pca.model.csr;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CsrInfo {
    private String csrPem;
    private String privateKeyPem;
}
