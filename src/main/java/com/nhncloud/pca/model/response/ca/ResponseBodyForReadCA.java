package com.nhncloud.pca.model.response.ca;

import java.time.LocalDateTime;
import java.util.List;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CertificateInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseBodyForReadCA {
    private CaInfo caInfo;

    private List<CertificateInfo> certificateInfoList;

    private CaStatus status;

    private LocalDateTime creationDatetime;
    private String creationUser;
    private LocalDateTime lastChangeDatetime;
    private String lastChangeUser;
}
