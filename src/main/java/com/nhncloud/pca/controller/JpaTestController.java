package com.nhncloud.pca.controller;


import com.nhncloud.pca.mapper.TestMapper;
import com.nhncloud.pca.model.TestDto;
import com.nhncloud.pca.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JpaTestController {
    private final TestRepository testRepository;
    private final TestMapper testMapper;

    @GetMapping("/test")
    public List<TestDto> getTest() {
        return testMapper.toDtoList(testRepository.findAll());
    }
}
