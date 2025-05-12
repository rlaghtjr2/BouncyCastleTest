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
import com.nhncloud.pca.constant.CaStatus;
import com.nhncloud.pca.constant.CaType;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CaMapperImpl;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.mapper.CertificateMapperImpl;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CaCreateResult;
import com.nhncloud.pca.model.response.CaReadResult;
import com.nhncloud.pca.model.response.CaUpdateResult;
import com.nhncloud.pca.model.response.CertificateCreateResult;
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

        CaCreateResult result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), "ROOT", null);

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
        when(certificateRepository.findBySignedCa_CaId(any())).thenReturn(Optional.of(certificate));
        CaCreateResult result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), "SUB", 1L);

        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void test_generateLeaf인증서_정상_생성() throws Exception {
        CertificateInfo certificateInfo = CommonTestUtil.createTestRootCaCertificateInfo();
        CaEntity ca = new CaEntity();
        ca.setName("TEST_CA_NAME");
        ca.setType(CaType.ROOT.getType());
        CertificateEntity certificate = new CertificateEntity();
        certificate.setCertificatePem(certificateInfo.getCertificatePem());
        certificate.setPrivateKeyPem(certificateInfo.getPrivateKeyPem());
        certificate.setSignedCa(ca);
        ca.setSignedCertificates(List.of(certificate));

        when(certificateRepository.findBySignedCa_CaId(any())).thenReturn(Optional.of(certificate));

        CertificateCreateResult result = service.generateCert(CommonTestUtil.createTestCertificateRequestBody(), 1L);
        assertNotNull(result);

        System.out.println(result);
    }

    @Test
    public void test_인증서_조회() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        when(caRepository.findById(any())).thenReturn(Optional.of(ca));

        CaReadResult result = service.getCA(1L);
        assertNotNull(result);
        assertEquals(result.getCaInfo().getName(), CommonTestUtil.TEST_CA_INFO_NAME);
        assertEquals(result.getCaInfo().getType(), CaType.ROOT.getType());
        assertEquals(result.getCaInfo().getCaId(), CommonTestUtil.TEST_CA_INFO_ID);
    }

    @Test
    public void test_CA_업데이트_성공() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CaEntity saveCa = CommonTestUtil.createTestCaEntity();
        saveCa.setStatus(CaStatus.INACTIVE);
        when(caRepository.findById(any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(saveCa);

        CaUpdateResult result = service.updateCA(1L, CommonTestUtil.createTestCertificateRequestBodyForUpdate());
        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.INACTIVE);
    }
}
