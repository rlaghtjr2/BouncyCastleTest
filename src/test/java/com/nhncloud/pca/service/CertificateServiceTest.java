package com.nhncloud.pca.service;

import java.util.ArrayList;
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
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
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

        ResponseBodyForCreateCA result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), "ROOT", null);

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
        ca.setSignedCertificates(new ArrayList<>(List.of(certificate)));


        when(caRepository.save(any())).thenReturn(new CaEntity());
        when(certificateRepository.findByCa_CaId(any())).thenReturn(Optional.of(certificate));
        when(caRepository.findById(any())).thenReturn(Optional.of(ca));
        ResponseBodyForCreateCA result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), "SUB", 1L);

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
        ca.setSignedCertificates(new ArrayList<>(List.of(certificate)));

        when(certificateRepository.findByCa_CaId(any())).thenReturn(Optional.of(certificate));
        when(caRepository.findById(any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(new CaEntity());

        ResponseBodyForCreateCert result = service.generateCert(CommonTestUtil.createTestCertificateRequestBody(), 1L);
        assertNotNull(result);

        System.out.println(result);
    }

    @Test
    public void test_CA_조회() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        when(caRepository.findById(any())).thenReturn(Optional.of(ca));

        ResponseBodyForReadCA result = service.getCA(1L);
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

        ResponseBodyForUpdateCA result = service.updateCA(1L, CommonTestUtil.createTestCertificateRequestBodyForUpdate());
        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.INACTIVE);
    }

    @Test
    public void test_ChainCA_조회() {
        CertificateEntity cert = CommonTestUtil.createTestCertificateEntity();
        CertificateEntity rootCert = CommonTestUtil.createTestCertificateEntity();
        CaEntity rootCa = CommonTestUtil.createTestCaEntity();
        rootCa.setCertificate(rootCert);
        rootCert.setCa(rootCa);
        rootCert.setSignedCa(rootCa);
        cert.setSignedCa(rootCa);


        when(certificateRepository.findByCa_CaId(any())).thenReturn(Optional.of(cert));

        String chainCert = CommonTestUtil.ROOT_CA_CERT_PEM;
        ResponseBodyForReadChainCA result = service.getCAChain(1L);
        assertNotNull(result);
        assertEquals(result.getData(), chainCert);
    }

    @Test
    public void test_인증서_조회() {
        CertificateEntity cert = CommonTestUtil.createTestCertificateEntity();
        when(certificateRepository.findByCertificateIdAndSignedCa_CaId(any(), any())).thenReturn(Optional.of(cert));

        ResponseBodyForReadCert result = service.getCert(1L, 1L);
        assertNotNull(result);
        assertEquals(result.getCommonName(), CommonTestUtil.TEST_SUBJECT_INFO_COMMON_NAME);
        assertEquals(result.getCertificatePem(), cert.getCertificatePem());
    }
}
