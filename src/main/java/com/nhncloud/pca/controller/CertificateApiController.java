package com.nhncloud.pca.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.common.response.ApiResponse;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.service.CertificateService;

@Slf4j
@RestController
@RequestMapping("/ca")
public class CertificateApiController {
    private final CertificateService certificateService;

    public CertificateApiController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createCa(@RequestBody RequestBodyForCreateCA requestBody, @RequestParam("caType") String caType, @RequestParam("caId") Long caId) {
        CertificateResult result;
        try {
            result = certificateService.generateCa(requestBody, caType, caId);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
