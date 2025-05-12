package com.nhncloud.pca.model.response;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.model.ca.CaInfo;

@Data
@Builder
public class CaUpdateResult {
    private CaInfo caInfo;
}
