package com.nhncloud.pca.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CaCertificateInfo;

@Data
@Builder
public class CertificateResult {
    private CaInfo caInfo;

    private CaCertificateInfo caCertificateInfo;

    private String status;

    private LocalDateTime creationDatetime;
    private String creationUser;
    private LocalDateTime lastChangeDatetime;
    private String lastChangeUser;

    public static CertificateResult of(CaInfo caInfo, CaCertificateInfo caCertInfo, String caStatus) {
        return CertificateResult.builder()
            .caInfo(caInfo)
            .caCertificateInfo(caCertInfo)
            .status(caStatus)
            .creationDatetime(LocalDateTime.now())
            .creationUser("hoseok")
            .build();
    }
}
