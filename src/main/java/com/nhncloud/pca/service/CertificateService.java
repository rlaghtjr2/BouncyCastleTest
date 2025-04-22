package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.RequestBodyForCreateRootCA;
import com.nhncloud.pca.model.response.CertificateResult;

public interface CertificateService {
    CertificateResult generateRootCertificate(RequestBodyForCreateRootCA requestBody) throws Exception;
}
