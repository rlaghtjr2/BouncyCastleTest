package com.nhncloud.pca.model.ca;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.model.request.RequestBodyForCreateCA;

@Data
@Builder
public class CaInfo {
    private String name;
    private Long caId;
    private String type;

    public static CaInfo of(RequestBodyForCreateCA request, String type) {
        return CaInfo.builder()
            .name(request.getName())
            .caId(1234L)
            .type(type)
            .build();
    }
}
