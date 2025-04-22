package com.nhncloud.pca.model.ca;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaInfo {
    private String name;
    private Long caId;
    private String caType;
}
