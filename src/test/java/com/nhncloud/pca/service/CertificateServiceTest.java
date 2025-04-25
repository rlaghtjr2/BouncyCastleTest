package com.nhncloud.pca.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.CertificateRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CertificateServiceTest {

    @Mock
    CertificateRepository certificateRepository;
    @Mock
    CaRepository caRepository;

    @Mock
    CertificateMapper certificateMapper;
    @Mock
    CaMapper caMapper;

    private CertificateServiceImpl service = new CertificateServiceImpl(certificateRepository, caRepository, certificateMapper, caMapper);

    @Test
    public void test_rootCA_정상_생성() {

        when(certificateRepository.save(any())).thenReturn(1);

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
        CertificateInfo certificateInfo = service.generateIntermediateCaCsr(CommonTestUtil.createTestCertificateRequestBody());
        assertNotNull(certificateInfo);
    }

    @Test
    public void test_generateIntermediateCertificate() throws Exception {
        CertificateResult result = service.generateIntermediateCertificate(CommonTestUtil.createTestCertificateRequestBody());
        assertNotNull(result);
        assertNotNull(result.getCertificateInfo().getCertificatePem());
        assertNotNull(result.getCertificateInfo().getPrivateKeyPem());
        System.out.println(result);
    }
}
