package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.CaCreateResult;
import com.nhncloud.pca.model.response.CertificateCreateResult;

public interface CertificateService {
    CaCreateResult generateCa(RequestBodyForCreateCA requestBody, String caType, Long caId) throws Exception;

    CertificateCreateResult generateCert(RequestBodyForCreateCert requestBody, Long caId) throws Exception;
}
