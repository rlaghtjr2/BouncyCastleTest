package com.nhncloud.pca.model.ca;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.model.request.RequestBodyForCreateCA;

@Data
@Builder
public class CaInfo {
    private String name;
    private Long caId;
    private String caType;

    public static CaInfo of(RequestBodyForCreateCA request, String caType) {
        return CaInfo.builder()
            .name(request.getName())
            .caId(1234L)
            .caType(caType)
            .build();
    }
}
