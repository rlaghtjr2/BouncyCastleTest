package com.nhncloud.pca.model.request.ca;

import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;

import lombok.Data;

@Data
public class RequestBodyForCreateCA {
    private String name;
    private String description;
    private RequestBodyForCreateCert certificateRequest;
}
