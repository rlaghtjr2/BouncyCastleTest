package com.nhncloud.pca.model.response.certificate;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.model.certificate.CertificateInfo;

@Data
@Builder
public class ResponseBodyForUpdateCert {
    private CertificateInfo certificateInfo;
}
