package com.nhncloud.pca.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.model.TestDto;
import com.nhncloud.pca.model.ca.CaDto;

@Mapper(componentModel = "spring")
public interface CaMapper {
    List<TestDto> toDtoList(List<CaEntity> testEntities);

    @Mapping(target = "signedCa", ignore = true)
    CaDto toDto(CaEntity entity);

    @Mapping(target = "signedCa", ignore = true)
    CaEntity toEntity(CaDto dto);
}
