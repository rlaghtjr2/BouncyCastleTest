package com.nhncloud.pca.model.ca;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;

@Data
@Builder
public class CaInfo {
    private String name;
    private Long id;
    private String type;
    private CaStatus status;

    public static CaInfo of(RequestBodyForCreateCA request, String type, CaStatus status) {
        return CaInfo.builder()
            .name(request.getName())
            .type(type)
            .status(status)
            .build();
    }

    public static CaInfo fromCaDto(CaDto caDto) {
        return CaInfo.builder()
            .id(caDto.getId())
            .name(caDto.getName())
            .type(caDto.getType())
            .status(caDto.getStatus())
            .build();
    }

    public static CaDto toCaDto(CaInfo caInfo) {
        return CaDto.builder()
            .id(caInfo.getId())
            .name(caInfo.getName())
            .type(caInfo.getType())
            .status(caInfo.getStatus())
            .build();
    }
}
