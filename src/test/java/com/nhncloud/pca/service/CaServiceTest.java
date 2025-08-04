package com.nhncloud.pca.service;

import java.time.LocalDateTime;
import java.util.Arrays;
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
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CaMapperImpl;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.mapper.CertificateMapperImpl;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.CertificateRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaServiceTest {

    @Mock
    CertificateRepository certificateRepository;

    @Mock
    CaRepository caRepository;

    @InjectMocks
    private CaServiceImpl service;

    @BeforeEach
    public void setting() {
        CaMapper caMapper = new CaMapperImpl();
        CertificateMapper certificateMapper = new CertificateMapperImpl();
        service = new CaServiceImpl(
            certificateRepository,
            caRepository,
            certificateMapper,
            caMapper
        );
    }

    @Test
    public void test_rootCA_정상_생성() {
        // CaEntity Mock 설정
        CaEntity caEntity = CommonTestUtil.createTestCaEntity();
        when(caRepository.save(any())).thenReturn(caEntity);

        // CertificateEntity Mock 설정 (generateCa에서 certificate 저장)
        CertificateEntity certificateEntity = CommonTestUtil.createTestCertificateEntity();
        when(certificateRepository.save(any())).thenReturn(certificateEntity);

        ResponseBodyForCreateCA result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), null);

        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void test_rootCA_잘못된_키_알고리즘() {
        RequestBodyForCreateCA requestBody = CommonTestUtil.createTestCertificateRequestBody();
        requestBody.getCertificateRequest().getKeyInfo().setAlgorithm("INVALID_ALGORITHM");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            // 예외를 발생시킬 코드
            service.generateCa(requestBody, null);
        });

        assertEquals("Invalid Algorithm or Key Size", exception.getMessage());
    }

    @Test
    public void test_generateIntermediateCa_정상_생성() {
        // Upper CA 설정 (certificateId=1L에 해당하는 certificate)
        CertificateEntity upperCertificate = CommonTestUtil.createTestCertificateEntity();
        upperCertificate.setId(1L);

        CaEntity upperCa = CommonTestUtil.createTestCaEntity();
        upperCa.setCertificates(Arrays.asList(upperCertificate)); // certificates 설정
        upperCertificate.setCa(upperCa);

        // 새로 생성될 CA/Certificate Mock 설정
        CaEntity newCa = CommonTestUtil.createTestCaEntity();
        newCa.setId(2L);

        CertificateEntity newCertificate = CommonTestUtil.createTestCertificateEntity();
        newCertificate.setId(2L);

        // Mock 설정
        when(caRepository.save(any())).thenReturn(newCa);
        when(certificateRepository.save(any())).thenReturn(newCertificate);

        ResponseBodyForCreateCA result = service.generateCa(CommonTestUtil.createTestCertificateRequestBody(), 1L);

        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    public void test_CA_조회() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        ca.setCertificates(Arrays.asList(certificate));
        when(caRepository.findByIdAndStatusNot(any(), any())).thenReturn(Optional.of(ca));

        ResponseBodyForReadCA result = service.getCA(1L);
        assertNotNull(result);
        assertEquals(result.getCaInfo().getName(), CommonTestUtil.TEST_CA_INFO_NAME);
        assertEquals(result.getCaInfo().getToastProjectId(), CommonTestUtil.TEST_CA_INFO_TOAST_PROJECT_ID);
        assertEquals(result.getCaInfo().getId(), CommonTestUtil.TEST_CA_INFO_ID);
    }

    @Test
    public void test_ChainCA_조회() {
        CertificateEntity cert = CommonTestUtil.createTestCertificateEntity();
        CertificateEntity rootCert = CommonTestUtil.createTestCertificateEntity();
        CaEntity rootCa = CommonTestUtil.createTestCaEntity();
        rootCa.setCertificates(Arrays.asList(rootCert));
        rootCert.setCa(rootCa);

        when(certificateRepository.findByCa_Id(any())).thenReturn(Optional.of(cert));
        when(certificateRepository.findById(any())).thenReturn(Optional.of(cert));
        String chainCert = String.join("\n", Collections.nCopies(4, CommonTestUtil.ROOT_CA_CERT_PEM));
        ResponseBodyForReadChainCA result = service.getCAChain(1L);
        assertNotNull(result);
        assertEquals(result.getData(), chainCert);
    }

    @Test
    public void test_CA_리스트_조회() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        ca.setCertificates(Arrays.asList(certificate));
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

        ResponseBodyForUpdateCA result = service.setCaDeletion(1L);

        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.DELETE_SCHEDULED);
    }

    @Test
    public void test_CA_삭제_예정_취소() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        ca.setStatus(CaStatus.DELETE_SCHEDULED);
        ca.setDeletionDatetime(LocalDateTime.now());
        CaEntity saveCa = CommonTestUtil.createTestCaEntity();
        saveCa.setStatus(CaStatus.ACTIVE);

        when(caRepository.findByIdAndStatus(any(), any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(saveCa);

        ResponseBodyForUpdateCA result = service.unsetCaDeletion(1L);

        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.ACTIVE);
    }

    @Test
    public void test_CA_즉시_삭제() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        ca.setCertificates(Arrays.asList(certificate));
        ca.setStatus(CaStatus.DELETE_SCHEDULED);

        CaEntity returnCa = CommonTestUtil.createTestCaEntity();
        CertificateEntity returnCert = CommonTestUtil.createTestCertificateEntity();

        returnCa.setStatus(CaStatus.DELETED);
        returnCert.setStatus(CertificateStatus.DELETED);

        returnCa.setCertificates(Arrays.asList(returnCert));

        when(caRepository.findByIdAndStatus(any(), any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(returnCa);

        ResponseBodyForUpdateCA result = service.removeCert(1L);

        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.DELETED);
    }

    @Test
    public void test_CA_활성화() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        ca.setCertificates(Arrays.asList(certificate));
        ca.setStatus(CaStatus.DISABLED);

        CaEntity returnCa = CommonTestUtil.createTestCaEntity();
        CertificateEntity returnCert = CommonTestUtil.createTestCertificateEntity();

        returnCa.setStatus(CaStatus.ACTIVE);
        returnCert.setStatus(CertificateStatus.ACTIVE);

        returnCa.setCertificates(Arrays.asList(returnCert));

        when(caRepository.findByIdAndStatus(any(), any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(returnCa);

        ResponseBodyForUpdateCA result = service.activateCa(1L); // 수정: removeCert → activateCa

        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.ACTIVE);
    }

    @Test
    public void test_CA_비활성화() {
        CaEntity ca = CommonTestUtil.createTestCaEntity();
        CertificateEntity certificate = CommonTestUtil.createTestCertificateEntity();
        ca.setCertificates(Arrays.asList(certificate));
        ca.setStatus(CaStatus.ACTIVE);

        CaEntity returnCa = CommonTestUtil.createTestCaEntity();
        CertificateEntity returnCert = CommonTestUtil.createTestCertificateEntity();

        returnCa.setStatus(CaStatus.DISABLED);
        returnCert.setStatus(CertificateStatus.DISABLED);

        returnCa.setCertificates(Arrays.asList(returnCert));

        when(caRepository.findByIdAndStatus(any(), any())).thenReturn(Optional.of(ca));
        when(caRepository.save(any())).thenReturn(returnCa);

        ResponseBodyForUpdateCA result = service.disableCa(1L); // 수정: removeCert → disableCa

        assertNotNull(result);
        assertEquals(result.getCaInfo().getStatus(), CaStatus.DISABLED);
    }
}
