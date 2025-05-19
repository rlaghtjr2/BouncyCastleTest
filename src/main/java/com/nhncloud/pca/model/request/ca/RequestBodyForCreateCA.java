package com.nhncloud.pca.model.request.ca;

import lombok.Data;

import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;

@Data
public class RequestBodyForCreateCA extends RequestBodyForCreateCert {
    private String name;
    private String description;
}
