package com.nhncloud.pca.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificateCreateResult {
    private String certificatePem;
    private String chainCertificatePem;
    private String privateKeyPem;
    private String serialNo;
    private String ocspResponder;
    private String issuer;
}
