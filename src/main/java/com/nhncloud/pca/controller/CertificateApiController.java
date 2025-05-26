package com.nhncloud.pca.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.common.response.ApiResponse;
import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCertList;
import com.nhncloud.pca.service.CertificateService;

@Slf4j
@RestController
@RequestMapping("/ca/{caId}/cert")
public class CertificateApiController {
    private final CertificateService certificateService;

    public CertificateApiController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createCert(@RequestBody RequestBodyForCreateCert requestBody, @PathVariable("caId") Long caId) {
        ResponseBodyForCreateCert result;
        try {
            result = certificateService.generateCert(requestBody, caId);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getCertList(@PathVariable("caId") Long caId) {
        ResponseBodyForReadCertList result = certificateService.getCertList(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{certId}")
    public ResponseEntity<ApiResponse> getCert(@PathVariable("caId") Long caId, @PathVariable("certId") Long certId) {
        ResponseBodyForReadCert result = certificateService.getCert(caId, certId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
