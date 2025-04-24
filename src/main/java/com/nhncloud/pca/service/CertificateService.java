package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;

public interface CertificateService {
    CertificateResult generateRootCertificate(RequestBodyForCreateCA requestBody) throws Exception;

    CertificateResult generateIntermediateCertificate(RequestBodyForCreateCA requestBody) throws Exception;
}
