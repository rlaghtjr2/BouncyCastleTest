package com.nhncloud.pca.model.response.ca;

import lombok.Builder;
import lombok.Data;

import com.nhncloud.pca.model.ca.CaInfo;

@Data
@Builder
public class ResponseBodyForUpdateCA {
    private CaInfo caInfo;
}
