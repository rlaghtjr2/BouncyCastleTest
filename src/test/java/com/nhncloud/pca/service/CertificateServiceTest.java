package com.nhncloud.pca.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.constant.CaType;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CaMapperImpl;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.mapper.CertificateMapperImpl;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.CertificateRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CertificateServiceTest {

    @Mock
    CertificateRepository certificateRepository;

    @Mock
    CaRepository caRepository;

    @Mock
    CertificateMapper certificateMapper;

    @InjectMocks
    private CertificateServiceImpl service;

    @BeforeEach
    public void setting() {
        CaMapper caMapper = new CaMapperImpl();
        CertificateMapper certificateMapper = new CertificateMapperImpl();
        service = new CertificateServiceImpl(
            certificateRepository,
            caRepository,
            certificateMapper,
            caMapper
        );
    }

    @Test
    public void test_rootCA_정상_생성() {
        when(caRepository.save(any())).thenReturn(new CaEntity());

        CertificateResult result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), "ROOT", null);

        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void test_rootCA_잘못된_키_알고리즘() {
        RequestBodyForCreateCA requestBody = CommonTestUtil.createTestCertificateRequestBody();
        requestBody.getKeyInfo().setAlgorithm("INVALID_ALGORITHM");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            // 예외를 발생시킬 코드
            service.generateCa(requestBody, "ROOT", null);
        });

        assertEquals("Wrong Algorithm", exception.getMessage());
    }

    @Test
    public void test_generateIntermediateCa_정상_생성() {
        CertificateInfo certificateInfo = CommonTestUtil.createTestRootCaCertificateInfo();
        CaEntity ca = new CaEntity();
        ca.setName("TEST_CA_NAME");
        ca.setType(CaType.ROOT.getType());
        CertificateEntity certificate = new CertificateEntity();
        certificate.setCertificatePem(certificateInfo.getCertificatePem());
        certificate.setPrivateKeyPem(certificateInfo.getPrivateKeyPem());
        certificate.setSignedCa(ca);
        ca.setSignedCertificates(List.of(certificate));

        when(caRepository.save(any())).thenReturn(new CaEntity());
        when(certificateRepository.findByCaId(any())).thenReturn(Optional.of(certificate));
        CertificateResult result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), "SUB", 1L);

        System.out.println(result);
        assertNotNull(result);
    }
}
