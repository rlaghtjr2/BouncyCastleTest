package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCertList;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForUpdateCert;

public interface CertificateService {
    ResponseBodyForCreateCert generateCert(RequestBodyForCreateCert requestBody, Long caId) throws Exception;

    ResponseBodyForReadCert getCert(Long caId, Long certId);

    ResponseBodyForReadCertList getCertList(Long caId);

    ResponseBodyForUpdateCert activateCert(Long caId, Long certId);

    ResponseBodyForUpdateCert disableCert(Long caId, Long certId);

    ResponseBodyForUpdateCert setCertDeletion(Long caId, Long certId);

    ResponseBodyForUpdateCert unsetCertDeletion(Long caId, Long certId);

    ResponseBodyForUpdateCert removeCert(Long caId, Long certId);
}
