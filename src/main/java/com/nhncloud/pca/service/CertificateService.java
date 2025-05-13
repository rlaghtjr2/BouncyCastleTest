package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.RequestBodyForCreateCert;
import com.nhncloud.pca.model.request.RequestBodyForUpdateCA;
import com.nhncloud.pca.model.response.CaCreateResult;
import com.nhncloud.pca.model.response.CaReadResult;
import com.nhncloud.pca.model.response.CaUpdateResult;
import com.nhncloud.pca.model.response.CertificateCreateResult;
import com.nhncloud.pca.model.response.CertificateReadResult;
import com.nhncloud.pca.model.response.ChainCaReadResult;

public interface CertificateService {
    CaCreateResult generateCa(RequestBodyForCreateCA requestBody, String caType, Long caId) throws Exception;

    CertificateCreateResult generateCert(RequestBodyForCreateCert requestBody, Long caId) throws Exception;

    CaReadResult getCA(Long caId);

    CaUpdateResult updateCA(Long caId, RequestBodyForUpdateCA requestBody);

    ChainCaReadResult getCAChain(Long caId);

    CertificateReadResult getCert(Long caId, Long certId);
}
