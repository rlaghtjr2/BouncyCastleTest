package com.nhncloud.pca.model.response.certificate;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResponseBodyForReadCertList {
    private List<String> listCerts;
}
