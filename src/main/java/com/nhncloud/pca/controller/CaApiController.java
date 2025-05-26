package com.nhncloud.pca.controller;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.common.response.ApiResponse;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.service.CaService;

@Slf4j
@RestController
@RequestMapping("/ca")
public class CaApiController {
    private final CaService caService;

    public CaApiController(CaService caService) {
        this.caService = caService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createCa(@RequestBody RequestBodyForCreateCA requestBody, @RequestParam("caType") String caType, @Nullable @RequestParam("caId") Long caId) {
        ResponseBodyForCreateCA result;
        try {
            result = caService.generateCa(requestBody, caType, caId);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(50000, e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getCAList(@Nullable @RequestParam(defaultValue = "0") int page) {
        ResponseBodyForReadCAList result = caService.getCaList(page);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{caId}")
    public ResponseEntity<ApiResponse> getCA(@PathVariable("caId") Long caId) {
        ResponseBodyForReadCA result = caService.getCA(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{caId}/chain")
    public ResponseEntity<ApiResponse> getCAChain(@PathVariable("caId") Long caId) {
        ResponseBodyForReadChainCA result = caService.getCAChain(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{caId}/delete")
    public ResponseEntity<ApiResponse> setCaDeletion(@PathVariable("caId") Long caId) {
        ResponseBodyForUpdateCA result = caService.setCaDeletion(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{caId}/recover")
    public ResponseEntity<ApiResponse> unsetCaDeletion(@PathVariable("caId") Long caId) {
        ResponseBodyForUpdateCA result = caService.unsetCaDeletion(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{caId}")
    public ResponseEntity<ApiResponse> removeCa(@PathVariable("caId") Long caId) {
        ResponseBodyForUpdateCA result = caService.removeCert(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{caId}/activate")
    public ResponseEntity<ApiResponse> activateCa(@PathVariable("caId") Long caId) {
        ResponseBodyForUpdateCA result = caService.activateCa(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{caId}/disable")
    public ResponseEntity<ApiResponse> disableCa(@PathVariable("caId") Long caId) {
        ResponseBodyForUpdateCA result = caService.disableCa(caId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
