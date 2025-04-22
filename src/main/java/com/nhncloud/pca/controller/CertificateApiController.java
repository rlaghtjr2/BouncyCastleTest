package com.nhncloud.pca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.common.response.ApiResponse;
import com.nhncloud.pca.model.request.RequestBodyForCreateRootCA;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.service.CertificateService;

@RestController
@RequestMapping("/certificate")
public class CertificateApiController {
    private final CertificateService certificateService;

    public CertificateApiController(CertificateService certificateService) {
        this.certificateService = certificateService;

    }

    @PostMapping("/root")
    public ResponseEntity<ApiResponse> createRootCertificate(@RequestBody RequestBodyForCreateRootCA requestBody) {
        CertificateResult result;
        try {
            result = certificateService.generateRootCertificate(requestBody);
        } catch (Exception e) {
            //Exception 세분화 필요
            //Handler ? 혹은 Controller에서 Exception 마다 처리?
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
