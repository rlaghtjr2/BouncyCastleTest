package com.nhncloud.pca.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.common.response.ApiResponse;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.service.CertificateService;

@Slf4j
@RestController
@RequestMapping("/certificate")
public class CertificateApiController {
    private final CertificateService certificateService;

    public CertificateApiController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/root")
    public ResponseEntity<ApiResponse> createRootCertificate(@RequestBody RequestBodyForCreateCA requestBody) {
        CertificateResult result;
        try {
            result = certificateService.generateRootCertificate(requestBody);
        } catch (Exception e) {
            //Exception 세분화 필요
            //Handler ? 혹은 Controller에서 Exception 마다 처리?
            log.error("msg", e);
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/intermediate")
    public ResponseEntity<ApiResponse> createIntermediateCertificate(@RequestBody RequestBodyForCreateCA requestBody) {
        CertificateResult result;
        try {
            result = certificateService.generateIntermediateCertificate(requestBody);
        } catch (Exception e) {
            //Exception 세분화 필요
            //Handler ? 혹은 Controller에서 Exception 마다 처리?
            log.error("msg", e);
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
