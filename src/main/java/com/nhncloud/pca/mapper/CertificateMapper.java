package com.nhncloud.pca.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.model.certificate.CertificateDto;
import com.nhncloud.pca.model.certificate.CertificateInfo;

@Mapper(componentModel = "spring")
public interface CertificateMapper {
    List<CertificateInfo> toDtoList(List<CertificateEntity> entities);

    CertificateDto toDto(CertificateEntity entity);

    CertificateEntity toEntity(CertificateDto dto);
}
