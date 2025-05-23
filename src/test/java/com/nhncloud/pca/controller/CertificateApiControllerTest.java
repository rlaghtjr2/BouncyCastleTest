package com.nhncloud.pca.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.service.CertificateService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    public void test_root_CA_생성() throws Exception {

        ResponseBodyForCreateCA responseBodyForCreateCA = CommonTestUtil.createTestCertificateResult_Root();

        when(certificateService.generateCa(any(), any(), any())).thenReturn(responseBodyForCreateCA);

        String body = "{\n" +
            "  \"name\": \"ROOT CA NAME\",\n" +
            "  \"description\": \"ROOT CA DESCRIPTION\",\n" +
            "  \"keyInfo\": {\n" +
            "    \"algorithm\": \"RSA\",\n" +
            "    \"keySize\": 2048\n" +
            "  },\n" +
            "  \"period\": 3650,\n" +
            "  \"altName\": [],\n" +
            "  \"ip\": [],\n" +
            "  \"subjectInfo\": {\n" +
            "    \"commonName\": \"ROOT CA\",\n" +
            "    \"country\": \"KR\",\n" +
            "    \"stateOrProvince\": \"Gyeonggi-do\",\n" +
            "    \"locality\": \"Pangyo\",\n" +
            "    \"organization\": \"NHN Cloud\",\n" +
            "    \"organizationalUnit\": \"Deployment Development\",\n" +
            "    \"emailAddress\": \"hoseok.kim@nhn.com\"\n" +
            "  }\n" +
            "}";
        mockMvc.perform(post("/ca")
                .param("caType", "ROOT")
                .param("caId", "1234")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfo.name").value(CommonTestUtil.TEST_CA_INFO_NAME));
    }

    @Test
    public void test_intermediate_CA_생성() throws Exception {

        ResponseBodyForCreateCA responseBodyForCreateCA = CommonTestUtil.createTestCertificateResult_Intermediate();
        when(certificateService.generateCa(any(), any(), any())).thenReturn(responseBodyForCreateCA);

        String body = "{\n" +
            "  \"name\": \"Intermediate CA NAME\",\n" +
            "  \"description\": \"Intermediate CA DESCRIPTION\",\n" +
            "  \"keyInfo\": {\n" +
            "    \"algorithm\": \"RSA\",\n" +
            "    \"keySize\": 2048\n" +
            "  },\n" +
            "  \"period\": 3650,\n" +
            "  \"altName\": [],\n" +
            "  \"ip\": [],\n" +
            "  \"subjectInfo\": {\n" +
            "    \"commonName\": \"Intermediate CA\",\n" +
            "    \"country\": \"KR\",\n" +
            "    \"stateOrProvince\": \"Gyeonggi-do\",\n" +
            "    \"locality\": \"Pangyo\",\n" +
            "    \"organization\": \"NHN Cloud\",\n" +
            "    \"organizationalUnit\": \"Deployment Development\",\n" +
            "    \"emailAddress\": \"hoseok.kim@nhn.com\"\n" +
            "  }\n" +
            "}";
        mockMvc.perform(post("/ca")
                .param("caType", "INTERMEDIATE")
                .param("caId", "1")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo.subjectInfo.commonName").value(CommonTestUtil.TEST_SUBJECT_INFO_COMMON_NAME));
    }

    @Test
    public void test_CA_조회() throws Exception {

        ResponseBodyForReadCA caCreateResult = CommonTestUtil.createTestCertificateResult_Read();
        when(certificateService.getCA(any())).thenReturn(caCreateResult);

        mockMvc.perform(get("/ca/1")
                .param("caType", "INTERMEDIATE")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo.subjectInfo.commonName").value(CommonTestUtil.TEST_SUBJECT_INFO_COMMON_NAME));
    }

    @Test
    public void test_CA_상태변경() throws Exception {

        ResponseBodyForUpdateCA responseBodyForUpdateCA = CommonTestUtil.createTestCertificateResult_Update();
        when(certificateService.updateCA(any(), any())).thenReturn(responseBodyForUpdateCA);

        String body = "{\n" +
            "  \"status\": \"DISABLED\"\n" +
            "  }\n" +
            "}";
        mockMvc.perform(put("/ca/1")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfo.status").value(CaStatus.DISABLED.toString()));
    }

    @Test
    public void test_CA_체인_조회() throws Exception {
        String chainCert = CommonTestUtil.ROOT_CA_KEY_PEM + "\n" + CommonTestUtil.TEST_CERTIFICATE_INFO_CERTIFICATE_PEM;

        ResponseBodyForReadChainCA responseBodyForReadChainCA = ResponseBodyForReadChainCA.builder().data(chainCert).build();
        when(certificateService.getCAChain(any())).thenReturn(responseBodyForReadChainCA);

        mockMvc.perform(get("/ca/1/chain")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.data").value(chainCert));
    }

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
    public void test_CA_리스트_조회() throws Exception {
        ResponseBodyForReadCA caCreateResult = CommonTestUtil.createTestCertificateResult_Read();
        ResponseBodyForReadCAList responseBodyForReadCAList = ResponseBodyForReadCAList.builder()
            .caInfoList(List.of(caCreateResult))
            .build();

        when(certificateService.getCaList(anyInt())).thenReturn(responseBodyForReadCAList);

        mockMvc.perform(get("/ca")
                .param("page", "0")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfoList").isArray())
            .andExpect(jsonPath("$.body.caInfoList").isNotEmpty())
            .andExpect(jsonPath("$.body.caInfoList[0].caInfo.name").value(CommonTestUtil.TEST_CA_INFO_NAME));
    }

    @Test
    public void test_CA_삭제_예정() throws Exception {
        ResponseBodyForUpdateCA responseBodyForUpdateCA = CommonTestUtil.createTestCertificateResult_Update();
        responseBodyForUpdateCA.getCaInfo().setStatus(CaStatus.DELETE_SCHEDULED);

        when(certificateService.setCaDeletion(any())).thenReturn(responseBodyForUpdateCA);

        mockMvc.perform(post("/ca/1/delete")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.caInfo.status").value(CaStatus.DELETE_SCHEDULED.toString()));
    }

    @Test
    public void test_CA_삭제() throws Exception {
        ResponseBodyForUpdateCA responseBodyForUpdateCA = CommonTestUtil.createTestCertificateResult_Update();
        responseBodyForUpdateCA.getCaInfo().setStatus(CaStatus.DELETED);

        when(certificateService.removeCert(any())).thenReturn(responseBodyForUpdateCA);

        mockMvc.perform(delete("/ca/1")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.caInfo.status").value(CaStatus.DELETED.toString()));
    }

    @Test
    public void test_CA_활성화() throws Exception {
        ResponseBodyForUpdateCA responseBodyForUpdateCA = CommonTestUtil.createTestCertificateResult_Update();
        responseBodyForUpdateCA.getCaInfo().setStatus(CaStatus.ACTIVE);

        when(certificateService.activateCa(any())).thenReturn(responseBodyForUpdateCA);

        mockMvc.perform(post("/ca/1/activate")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.caInfo.status").value(CaStatus.ACTIVE.toString()));
    }

    @Test
    public void test_CA_비활성화() throws Exception {
        ResponseBodyForUpdateCA responseBodyForUpdateCA = CommonTestUtil.createTestCertificateResult_Update();
        responseBodyForUpdateCA.getCaInfo().setStatus(CaStatus.DISABLED);

        when(certificateService.disableCa(any())).thenReturn(responseBodyForUpdateCA);

        mockMvc.perform(post("/ca/1/disable")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfo").isNotEmpty())
            .andExpect(jsonPath("$.body.caInfo.status").value(CaStatus.DISABLED.toString()));
    }
}
