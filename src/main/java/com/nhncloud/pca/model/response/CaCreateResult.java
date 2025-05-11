package com.nhncloud.pca.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CertificateInfo;

@Data
@Builder
public class CaCreateResult {
    private CaInfo caInfo;

    private CertificateInfo certificateInfo;

    private String status;

    private LocalDateTime creationDatetime;
    private String creationUser;
    private LocalDateTime lastChangeDatetime;
    private String lastChangeUser;

    public static CaCreateResult of(CaInfo caInfo, CertificateInfo caCertInfo, String caStatus) {
        return CaCreateResult.builder()
            .caInfo(caInfo)
            .certificateInfo(caCertInfo)
            .status(caStatus)
            .creationDatetime(LocalDateTime.now())
            .creationUser("hoseok")
            .build();
    }
}
