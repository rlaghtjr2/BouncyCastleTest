package com.nhncloud.pca.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.service.CertificateService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class CertificateApiControllerTest {

    @Mock
    private CertificateService certificateService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetCertificate() throws Exception {

        CertificateResult certificateResult = CommonTestUtil.createTestCertificateResult();

        when(certificateService.generateRootCertificate(any())).thenReturn(certificateResult);

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
        mockMvc.perform(post("/certificate/root")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.header.isSuccessful").value(true))
            .andExpect(jsonPath("$.body.caCertificateInfo.caCertificateId").value("1234"));
    }
}
