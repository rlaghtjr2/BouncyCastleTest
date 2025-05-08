package com.nhncloud.pca.model.request;

import lombok.Data;

@Data
public class RequestBodyForCreateCA extends RequestBodyForCreateCert {
    private String name;
    private String description;
    private Long rootCaId;
}
