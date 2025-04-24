package com.nhncloud.pca.service;

import org.junit.jupiter.api.Test;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.model.certificate.CaCertificateInfo;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CertificateServiceTest {

    private CertificateServiceImpl service = new CertificateServiceImpl();

    @Test
    public void test_rootCA_정상_생성() {
        CertificateResult result = service.generateRootCertificate(CommonTestUtil.createTestCertificateRequestBody());

        System.out.println("result = " + result);
        assertThat(result).isNotNull();
    }

    @Test
    public void test_rootCA_잘못된_키_알고리즘() {
        RequestBodyForCreateCA requestBody = CommonTestUtil.createTestCertificateRequestBody();
        requestBody.getKeyInfo().setAlgorithm("INVALID_ALGORITHM");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            // 예외를 발생시킬 코드
            service.generateRootCertificate(requestBody);
        });

        assertEquals("Wrong Algorithm", exception.getMessage());
    }

    @Test
    public void test_generateIntermediateCertificateCsr() {
        CaCertificateInfo caCertificateInfo = service.generateIntermediateCaCsr(CommonTestUtil.createTestCertificateRequestBody());
        assertNotNull(caCertificateInfo);
    }

    @Test
    public void test_generateIntermediateCertificate() throws Exception {
        CertificateResult result = service.generateIntermediateCertificate(CommonTestUtil.createTestCertificateRequestBody());
        assertNotNull(result);
        assertNotNull(result.getCaCertificateInfo().getCertificatePem());
        assertNotNull(result.getCaCertificateInfo().getPrivateKeyPem());
        System.out.println(result);
    }
}
