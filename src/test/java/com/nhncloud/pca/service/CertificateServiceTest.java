package com.nhncloud.pca.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CaMapperImpl;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.mapper.CertificateMapperImpl;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForUpdateCert;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.CertificateRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CertificateServiceTest {

    @Mock
    CertificateRepository certificateRepository;

    @Mock
    CaRepository caRepository;

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
    public void test_generateLeaf인증서_정상_생성() throws Exception {
        CertificateInfo certificateInfo = CommonTestUtil.createTestRootCaCertificateInfo();
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        certificate.setCa(ca);

        when(certificateRepository.findByCa_Id(any())).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any())).thenReturn(new CertificateEntity());

        ResponseBodyForCreateCert result = service.generateCert(CommonTestUtil.createTestCertificateRequestBody(), 1L);
        assertNotNull(result);

        System.out.println(result);
    }

    @Test
    public void test_인증서_조회() {
        CertificateEntity cert = CommonTestUtil.createTestCertificateEntity();
        when(certificateRepository.findByIdAndSignedCaIdAndStatusNot(any(), any(), any())).thenReturn(Optional.of(cert));

        ResponseBodyForReadCert result = service.getCert(1L, 1L);
        assertNotNull(result);
        assertEquals(result.getCommonName(), CommonTestUtil.TEST_SUBJECT_INFO_COMMON_NAME);
        assertEquals(result.getCertificatePem(), cert.getCertificatePem());
    }

    @Test
    public void test_인증서_활성화() {
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        certificate.setStatus(CertificateStatus.DISABLED);

        CertificateEntity returnCert = CommonTestUtil.createTestCertificateEntity();

        returnCert.setStatus(CertificateStatus.ACTIVE);

        when(certificateRepository.findByIdAndStatus(any(), any())).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any())).thenReturn(returnCert);

        ResponseBodyForUpdateCert result = service.activateCert(1L, 1L);

        assertNotNull(result);
        assertEquals(result.getCertificateInfo().getStatus(), CertificateStatus.ACTIVE);
    }

    @Test
    public void test_인증서_비활성화() {
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        certificate.setStatus(CertificateStatus.ACTIVE);

        CertificateEntity returnCert = CommonTestUtil.createTestCertificateEntity();

        returnCert.setStatus(CertificateStatus.DISABLED);

        when(certificateRepository.findByIdAndStatus(any(), any())).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any())).thenReturn(returnCert);

        ResponseBodyForUpdateCert result = service.disableCert(1L, 1L);

        assertNotNull(result);
        assertEquals(result.getCertificateInfo().getStatus(), CertificateStatus.DISABLED);
    }
}
