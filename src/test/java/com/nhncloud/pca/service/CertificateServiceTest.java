package com.nhncloud.pca.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.nhncloud.pca.CommonTestUtil;
import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.constant.ca.CaType;
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CaMapperImpl;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.mapper.CertificateMapperImpl;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
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
        CaEntity caEntity = new CaEntity();
        caEntity.setId(1L);
        when(caRepository.save(any())).thenReturn(caEntity);

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

        CaEntity ca = CommonTestUtil.createTestCaEntity();

        CertificateEntity certificate = new CertificateEntity();
        certificate.setStatus(CertificateStatus.ACTIVE);
        certificate.setCertificatePem(certificateInfo.getCertificatePem());
        certificate.setPrivateKeyPem(certificateInfo.getPrivateKeyPem());
        certificate.setCa(ca);
//        ca.setSignedCertificates(new ArrayList<>(List.of(certificate)));


        when(caRepository.save(any())).thenReturn(ca);
        when(certificateRepository.findByCa_Id(any())).thenReturn(Optional.of(certificate));
        ResponseBodyForCreateCA result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), "INTERMEDIATE", 1L);

        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void test_generateLeaf인증서_정상_생성() throws Exception {
        CertificateInfo certificateInfo = CommonTestUtil.createTestRootCaCertificateInfo();
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        certificate.setCa(ca);
//        ca.setSignedCertificates(new ArrayList<>(List.of(certificate)));

        when(certificateRepository.findByCa_Id(any())).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any())).thenReturn(new CertificateEntity());

        ResponseBodyForCreateCert result = service.generateCert(CommonTestUtil.createTestCertificateRequestBody(), 1L);
        assertNotNull(result);

        System.out.println(result);
    }

    @Test
    public void test_CA_조회() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        ca.setCertificate(certificate);
        when(caRepository.findByIdAndStatusNot(any(), any())).thenReturn(Optional.of(ca));

        ResponseBodyForReadCA result = service.getCA(1L);
        assertNotNull(result);
        assertEquals(result.getCaInfo().getName(), CommonTestUtil.TEST_CA_INFO_NAME);
        assertEquals(result.getCaInfo().getType(), CaType.ROOT.getType());
        assertEquals(result.getCaInfo().getId(), CommonTestUtil.TEST_CA_INFO_ID);
    }

    @Test
    public void test_CA_업데이트_성공() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CaEntity saveCa = CommonTestUtil.createTestCaEntity();
        saveCa.setStatus(CaStatus.DISABLED);
        when(caRepository.findById(any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(saveCa);

        ResponseBodyForUpdateCA result = service.updateCA(1L, CommonTestUtil.createTestCertificateRequestBodyForUpdate());
        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.DISABLED);
    }

    @Test
    public void test_ChainCA_조회() {
        CertificateEntity cert = CommonTestUtil.createTestCertificateEntity();
        CertificateEntity rootCert = CommonTestUtil.createTestCertificateEntity();
        CaEntity rootCa = CommonTestUtil.createTestCaEntity();
        rootCa.setCertificate(rootCert);
        rootCert.setCa(rootCa);

        when(certificateRepository.findByCa_Id(any())).thenReturn(Optional.of(cert));

        String chainCert = String.join("\n", Collections.nCopies(4, CommonTestUtil.ROOT_CA_CERT_PEM));
        ResponseBodyForReadChainCA result = service.getCAChain(1L);
        assertNotNull(result);
        assertEquals(result.getData(), chainCert);
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
    public void test_CA_리스트_조회() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        ca.setCertificate(certificate);
        Pageable pageable = PageRequest.of(0, 10);
        List<CaEntity> caList = List.of(ca);
        Page<CaEntity> page = new PageImpl<>(caList, pageable, caList.size());

        when(caRepository.findByStatusNot(any(), any(Pageable.class))).thenReturn(page);

        ResponseBodyForReadCAList result = service.getCaList(0);
        assertEquals(1, result.getTotalCnt());
        assertEquals(1, result.getTotalPageNo());
        assertEquals(1, result.getCurrentPageNo());
        assertEquals(1, result.getCaInfoList().size());
        assertEquals(result.getCaInfoList().get(0).getCaInfo().getName(), CommonTestUtil.TEST_CA_INFO_NAME);
    }

    @Test
    public void test_CA_삭제_예정() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CaEntity saveCa = CommonTestUtil.createTestCaEntity();
        saveCa.setStatus(CaStatus.DELETE_SCHEDULED);
        saveCa.setDeletionDatetime(LocalDateTime.now());

        when(caRepository.findById(any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(saveCa);

        ResponseBodyForUpdateCA result = service.deleteCa(1L);

        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.DELETE_SCHEDULED);
    }
}
