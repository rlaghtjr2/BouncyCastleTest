package com.nhncloud.pca.controller;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.common.response.ApiResponse;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.ca.RequestBodyForUpdateCA;
import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCertList;
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
        ResponseBodyForCreateCA result;
        try {
            result = certificateService.generateCa(requestBody, caType, caId);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{caId}/cert")
    public ResponseEntity<ApiResponse> createCert(@RequestBody RequestBodyForCreateCert requestBody, @PathVariable("caId") Long caId) {
        ResponseBodyForCreateCert result;
        try {
            result = certificateService.generateCert(requestBody, caId);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(ApiResponse.success(result)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getCAList(@Nullable @RequestParam(defaultValue = "0") int page) {
        ResponseBodyForReadCAList result = certificateService.getCaList(page);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{caId}")
    public ResponseEntity<ApiResponse> getCA(@PathVariable("caId") Long caId) {
        ResponseBodyForReadCA result = certificateService.getCA(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{caId}")
    public ResponseEntity<ApiResponse> updateCA(@PathVariable("caId") Long caId, @RequestBody RequestBodyForUpdateCA requestBody) {
        ResponseBodyForUpdateCA result = certificateService.updateCA(caId, requestBody);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{caId}/chain")
    public ResponseEntity<ApiResponse> getCAChain(@PathVariable("caId") Long caId) {
        ResponseBodyForReadChainCA result = certificateService.getCAChain(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{caId}/cert/{certId}")
    public ResponseEntity<ApiResponse> getCert(@PathVariable("caId") Long caId, @PathVariable("certId") Long certId) {
        ResponseBodyForReadCert result = certificateService.getCert(caId, certId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{caId}/cert")
    public ResponseEntity<ApiResponse> getCertList(@PathVariable("caId") Long caId) {
        ResponseBodyForReadCertList result = certificateService.getCertList(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
