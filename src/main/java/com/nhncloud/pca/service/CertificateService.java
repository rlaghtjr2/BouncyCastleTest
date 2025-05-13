package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.RequestBodyForCreateCert;
import com.nhncloud.pca.model.request.RequestBodyForUpdateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;

public interface CertificateService {
    ResponseBodyForCreateCA generateCa(RequestBodyForCreateCA requestBody, String caType, Long caId) throws Exception;

    ResponseBodyForCreateCert generateCert(RequestBodyForCreateCert requestBody, Long caId) throws Exception;

    ResponseBodyForReadCA getCA(Long caId);

    ResponseBodyForUpdateCA updateCA(Long caId, RequestBodyForUpdateCA requestBody);

    ResponseBodyForReadChainCA getCAChain(Long caId);

    ResponseBodyForReadCert getCert(Long caId, Long certId);
}
