package com.nhncloud.pca.model.response.ca;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CertificateInfo;

@Data
@Builder
public class ResponseBodyForCreateCA {
    private CaInfo caInfo;

    private CertificateInfo certificateInfo;

    private CaStatus status;

    private LocalDateTime creationDatetime;
    private String creationUser;
    private LocalDateTime lastChangeDatetime;
    private String lastChangeUser;

}
