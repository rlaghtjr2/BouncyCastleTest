package com.nhncloud.pca.model.response.ca;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResponseBodyForReadCAList {
    private List<ResponseBodyForReadCA> caInfoList;
    private Long totalCnt;
    private Long totalPageNo;
    private Long currentPageNo;
}
