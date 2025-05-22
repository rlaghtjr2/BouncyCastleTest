package com.nhncloud.pca.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.model.ca.CaDto;
import com.nhncloud.pca.model.certificate.CertificateDto;
import com.nhncloud.pca.model.certificate.CertificateInfo;

@Mapper(componentModel = "spring")
public interface CertificateMapper {
    List<CertificateInfo> toDtoList(List<CertificateEntity> entities);

    CertificateDto toDto(CertificateEntity entity);

    @Mapping(source = "ca", target = "ca", qualifiedByName = "mapToCaEntity")
    CertificateEntity toEntity(CertificateDto dto);

    @Named("mapToCaEntity")
    static CaEntity mapToCaEntity(CaDto caDto) {
        return caDto == null || caDto.getId() == null ? null : new CaEntity(caDto.getId());
    }
}
