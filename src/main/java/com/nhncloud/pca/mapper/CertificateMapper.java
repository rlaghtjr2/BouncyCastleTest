package com.nhncloud.pca.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.model.certificate.CertificateInfo;

@Mapper(componentModel = "spring")
public interface CertificateMapper {
    List<CertificateInfo> toDtoList(List<CertificateEntity> entities);

    CertificateInfo toDto(CertificateEntity entity);

    @Mapping(target = "certificateId", ignore = true)
    CertificateEntity toEntity(CertificateInfo dto);
}
