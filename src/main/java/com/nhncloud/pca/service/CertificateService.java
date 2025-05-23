package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.ca.RequestBodyForUpdateCA;
import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCertList;

public interface CertificateService {
    ResponseBodyForCreateCA generateCa(RequestBodyForCreateCA requestBody, String caType, Long caId) throws Exception;

    ResponseBodyForCreateCert generateCert(RequestBodyForCreateCert requestBody, Long caId) throws Exception;

    ResponseBodyForReadCAList getCaList(int page);

    ResponseBodyForReadCA getCA(Long caId);

    ResponseBodyForUpdateCA updateCA(Long caId, RequestBodyForUpdateCA requestBody);

    ResponseBodyForReadChainCA getCAChain(Long caId);

    ResponseBodyForReadCert getCert(Long caId, Long certId);

    ResponseBodyForReadCertList getCertList(Long caId);

    ResponseBodyForUpdateCA deleteCa(Long caId);

    ResponseBodyForUpdateCA removeCert(Long caId);

    ResponseBodyForUpdateCA activateCa(Long caId);

    ResponseBodyForUpdateCA disableCa(Long caId);
}
