package com.nhncloud.pca.mapper;

import com.nhncloud.pca.entity.TestEntity;
import com.nhncloud.pca.model.TestDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TestMapper {
    List<TestDto> toDtoList(List<TestEntity> testEntities);
}
