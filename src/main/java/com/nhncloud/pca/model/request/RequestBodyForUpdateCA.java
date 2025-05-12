package com.nhncloud.pca.model.request;

import lombok.Data;

import com.nhncloud.pca.constant.CaStatus;

@Data
public class RequestBodyForUpdateCA {
    private CaStatus status;
}
