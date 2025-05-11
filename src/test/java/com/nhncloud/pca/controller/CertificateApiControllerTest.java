package com.nhncloud.pca.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.model.response.CaCreateResult;
import com.nhncloud.pca.service.CertificateService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
            .andExpect(jsonPath("$.body.certificateInfo.certificateId").value("1234"));
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
            .andExpect(jsonPath("$.body.certificateInfo.certificateId").value("1234"));
    }
}
