package com.nhncloud.pca.model.ca;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.ca.CaStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaDto {
    private Long id;
    private Long toastProjectId;
    private String name;
    private CaStatus status;
    private LocalDateTime deletionDatetime;
    private String creationUser;
    private LocalDateTime creationDatetime;
    private String lastChangeUser;
    private LocalDateTime lastChangeDatetime;
}
