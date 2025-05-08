package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;

public interface CertificateService {
    CertificateResult generateCa(RequestBodyForCreateCA requestBody, String caType, Long caId) throws Exception;
}
