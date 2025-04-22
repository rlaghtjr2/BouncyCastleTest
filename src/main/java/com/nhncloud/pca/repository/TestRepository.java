package com.nhncloud.pca.repository;

import com.nhncloud.pca.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<TestEntity, Long> {

}
