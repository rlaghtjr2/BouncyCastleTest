package com.nhncloud.pca.model.ca;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.constant.ca.CaStatus;

@Data
@Builder
public class CaInfo {
    private String name;
    private Long id;
    private String type;
    private CaStatus status;

    public static CaInfo fromCaDto(CaDto caDto) {
        return CaInfo.builder()
            .id(caDto.getId())
            .name(caDto.getName())
            .type(caDto.getType())
            .status(caDto.getStatus())
            .build();
    }
}
