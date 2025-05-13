package com.nhncloud.pca.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.response.CaCreateResult;
import com.nhncloud.pca.model.response.CaReadResult;
import com.nhncloud.pca.model.response.CaUpdateResult;
import com.nhncloud.pca.model.response.CertificateReadResult;
import com.nhncloud.pca.model.response.ChainCaReadResult;
import com.nhncloud.pca.service.CertificateService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

        CaCreateResult caCreateResult = CommonTestUtil.createTestCertificateResult_Root();

        when(certificateService.generateCa(any(), any(), any())).thenReturn(caCreateResult);

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
            .andExpect(jsonPath("$.body.caInfo.name").value("TEST_CERTIFICATE_NAME"));
    }

    @Test
    public void test_intermediate_CA_생성() throws Exception {

        CaCreateResult caCreateResult = CommonTestUtil.createTestCertificateResult_Intermediate();
        when(certificateService.generateCa(any(), any(), any())).thenReturn(caCreateResult);

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
                .param("caType", "SUB")
                .param("caId", "1")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo.commonName").value("TEST_SUBJECT_INFO_COMMON_NAME"));
    }

    @Test
    public void test_CA_조회() throws Exception {

        CaReadResult caCreateResult = CommonTestUtil.createTestCertificateResult_Read();
        when(certificateService.getCA(any())).thenReturn(caCreateResult);

        mockMvc.perform(get("/ca/1")
                .param("caType", "SUB")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.certificateInfo.commonName").value("TEST_SUBJECT_INFO_COMMON_NAME"));
    }

    @Test
    public void test_CA_상태변경() throws Exception {

        CaUpdateResult caUpdateResult = CommonTestUtil.createTestCertificateResult_Update();
        when(certificateService.updateCA(any(), any())).thenReturn(caUpdateResult);

        String body = "{\n" +
            "  \"status\": \"INACTIVE\"\n" +
            "  }\n" +
            "}";
        mockMvc.perform(put("/ca/1")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caInfo.status").value("INACTIVE"));
    }

    @Test
    public void test_CA_체인_조회() throws Exception {
        String chainCert = CommonTestUtil.ROOT_CA_KEY_PEM + "\n" + CommonTestUtil.TEST_CERTIFICATE_INFO_CERTIFICATE_PEM;

        ChainCaReadResult chainCaReadResult = ChainCaReadResult.builder().data(chainCert).build();
        when(certificateService.getCAChain(any())).thenReturn(chainCaReadResult);

        mockMvc.perform(get("/ca/1/chain")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.data").value(chainCert));
    }

    @Test
    public void test_인증서_조회() throws Exception {
        CertificateInfo certificateInfo = CommonTestUtil.createTestRootCaCertificateInfo();
        CertificateReadResult result = CertificateReadResult.of(certificateInfo);
        when(certificateService.getCert(any(), any())).thenReturn(result);

        mockMvc.perform(get("/ca/1/cert/1")
                .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.commonName").value(CommonTestUtil.TEST_SUBJECT_INFO_COMMON_NAME));
    }
}
