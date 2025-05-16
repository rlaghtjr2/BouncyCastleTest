package com.nhncloud.pca.model.ca;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.model.certificate.CertificateDto;

@Data
@Builder
public class CaDto {
    private Long id;
    private CertificateDto signedCa;
    private String name;
    private String type;
    private CaStatus status;
    private String creationUser;
    private LocalDateTime creationDatetime;
    private String lastChangeUser;
    private LocalDateTime lastChangeDatetime;
}
