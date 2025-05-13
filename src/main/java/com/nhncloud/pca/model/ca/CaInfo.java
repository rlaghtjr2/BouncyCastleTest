package com.nhncloud.pca.model.ca;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.constant.CaStatus;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;

@Data
@Builder
public class CaInfo {
    private String name;
    private Long caId;
    private String type;
    private CaStatus status;

    public static CaInfo of(RequestBodyForCreateCA request, String type, CaStatus status) {
        return CaInfo.builder()
            .name(request.getName())
            .type(type)
            .status(status)
            .build();
    }
}
