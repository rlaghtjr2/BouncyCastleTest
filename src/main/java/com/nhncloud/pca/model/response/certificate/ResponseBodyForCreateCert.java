package com.nhncloud.pca.model.response.certificate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseBodyForCreateCert {
    private Long certId;
    private String certificatePem;
    private String chainCertificatePem;
    private String privateKeyPem;
    private String serialNo;
    private String ocspResponder;
    private String issuer;
}
