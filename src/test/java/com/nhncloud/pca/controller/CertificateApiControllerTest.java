package com.nhncloud.pca.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForUpdateCert;
import com.nhncloud.pca.service.CertificateService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class CertificateApiControllerTest {

    @MockitoBean
    private CertificateService certificateService;

    @Autowired
    private MockMvc mockMvc;


    @Test
    public void test_인증서_조회() throws Exception {
        CertificateInfo certificateInfo = CommonTestUtil.createTestRootCaCertificateInfo();
        ResponseBodyForReadCert result = ResponseBodyForReadCert.builder()
            .commonName(certificateInfo.getSubjectInfo().getCommonName())
            .serialNumber(certificateInfo.getSerialNumber())
            .notAfterDateTime(certificateInfo.getNotAfterDateTime())
            .notBeforeDateTime(certificateInfo.getNotBeforeDateTime())
            .publicKeyAlgorithm(certificateInfo.getPublicKeyAlgorithm())
            .certificatePem(certificateInfo.getCertificatePem())
            .chainCertificatePem(certificateInfo.getChainCertificatePem())
            .signatureAlgorithm(certificateInfo.getSignatureAlgorithm())
            .build();

        when(certificateService.getCert(any(), any())).thenReturn(result);

        mockMvc.perform(get("/ca/1/cert/1")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.commonName").value(CommonTestUtil.TEST_SUBJECT_INFO_COMMON_NAME));
    }

    @Test
    public void test_인증서_활성화() throws Exception {
        ResponseBodyForUpdateCert responseBodyForUpdateCert = CommonTestUtil.createTestCertificateResult_UpdateCert();
        responseBodyForUpdateCert.getCertificateInfo().setStatus(CertificateStatus.ACTIVE);

        when(certificateService.activateCert(any(), any())).thenReturn(responseBodyForUpdateCert);

        mockMvc.perform(post("/ca/1/cert/1/activate")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.certificateInfo.status").value(CaStatus.ACTIVE.toString()));
    }

    @Test
    public void test_인증서_비활성화() throws Exception {
        ResponseBodyForUpdateCert responseBodyForUpdateCert = CommonTestUtil.createTestCertificateResult_UpdateCert();
        responseBodyForUpdateCert.getCertificateInfo().setStatus(CertificateStatus.DISABLED);

        when(certificateService.disableCert(any(), any())).thenReturn(responseBodyForUpdateCert);

        mockMvc.perform(post("/ca/1/cert/1/disable")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.certificateInfo.status").value(CaStatus.DISABLED.toString()));
    }

    @Test
    public void test_인증서_삭제예정() throws Exception {
        ResponseBodyForUpdateCert responseBodyForUpdateCert = CommonTestUtil.createTestCertificateResult_UpdateCert();
        responseBodyForUpdateCert.getCertificateInfo().setStatus(CertificateStatus.DELETE_SCHEDULED);

        when(certificateService.setCertDeletion(any(), any())).thenReturn(responseBodyForUpdateCert);

        mockMvc.perform(post("/ca/1/cert/1/delete")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.certificateInfo.status").value(CaStatus.DELETE_SCHEDULED.toString()));
    }

    @Test
    public void test_인증서_삭제예정_취소() throws Exception {
        ResponseBodyForUpdateCert responseBodyForUpdateCert = CommonTestUtil.createTestCertificateResult_UpdateCert();
        responseBodyForUpdateCert.getCertificateInfo().setStatus(CertificateStatus.ACTIVE);

        when(certificateService.unsetCertDeletion(any(), any())).thenReturn(responseBodyForUpdateCert);

        mockMvc.perform(post("/ca/1/cert/1/recover")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.certificateInfo.status").value(CaStatus.ACTIVE.toString()));
    }

    @Test
    public void test_인증서_삭제() throws Exception {
        ResponseBodyForUpdateCert responseBodyForUpdateCert = CommonTestUtil.createTestCertificateResult_UpdateCert();
        responseBodyForUpdateCert.getCertificateInfo().setStatus(CertificateStatus.DELETED);

        when(certificateService.removeCert(any(), any())).thenReturn(responseBodyForUpdateCert);

        mockMvc.perform(delete("/ca/1/cert/1")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.certificateInfo.status").value(CaStatus.DELETED.toString()));
    }
}
