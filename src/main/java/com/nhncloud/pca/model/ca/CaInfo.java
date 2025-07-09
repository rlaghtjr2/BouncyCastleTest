package com.nhncloud.pca.model.ca;

import com.nhncloud.pca.constant.ca.CaStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaInfo {
    private String name;
    private Long id;
    private Long toastProjectId;
    private CaStatus status;

    public static CaInfo fromCaDto(CaDto caDto) {
        return CaInfo.builder()
            .id(caDto.getId())
            .name(caDto.getName())
            .toastProjectId(caDto.getToastProjectId())
            .status(caDto.getStatus())
            .build();
    }
}
