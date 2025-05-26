package com.nhncloud.pca.service;

import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;

public interface CaService {
    ResponseBodyForCreateCA generateCa(RequestBodyForCreateCA requestBody, String caType, Long caId) throws Exception;

    ResponseBodyForReadCAList getCaList(int page);

    ResponseBodyForReadCA getCA(Long caId);

    ResponseBodyForReadChainCA getCAChain(Long caId);

    ResponseBodyForUpdateCA setCaDeletion(Long caId);

    ResponseBodyForUpdateCA unsetCaDeletion(Long caId);

    ResponseBodyForUpdateCA removeCert(Long caId);

    ResponseBodyForUpdateCA activateCa(Long caId);

    ResponseBodyForUpdateCA disableCa(Long caId);
}
