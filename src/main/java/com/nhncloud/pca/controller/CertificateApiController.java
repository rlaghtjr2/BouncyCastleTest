package com.nhncloud.pca.controller;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.common.response.ApiResponse;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.CaCreateResult;
import com.nhncloud.pca.model.response.CaReadResult;
import com.nhncloud.pca.model.response.CertificateCreateResult;
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
    public ResponseEntity<ApiResponse> createCa(@RequestBody RequestBodyForCreateCA requestBody, @RequestParam("caType") String caType, @Nullable @RequestParam("caId") Long caId) {
        CaCreateResult result;
        try {
            result = certificateService.generateCa(requestBody, caType, caId);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{caId}/cert")
    public ResponseEntity<ApiResponse> createCert(@RequestBody RequestBodyForCreateCert requestBody, @PathVariable("caId") Long caId) {
        CertificateCreateResult result;
        try {
            result = certificateService.generateCert(requestBody, caId);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(ApiResponse.success(result)));
    }

    @GetMapping("/{caId}")
    public ResponseEntity<ApiResponse> getCA(@PathVariable("caId") Long caId) {
        CaReadResult result = certificateService.getCA(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
