package com.nhncloud.pca.service;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhncloud.pca.constant.KeyAlgorithm;
import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.model.ca.CaDto;
import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CertificateDto;
import com.nhncloud.pca.model.certificate.CertificateExtension;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.subject.SubjectInfo;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.CertificateRepository;
import com.nhncloud.pca.util.BouncyCastleUtil;
import com.nhncloud.pca.util.CertificateUtil;

@Service
@Slf4j
public class CaServiceImpl implements CaService {
    private final CertificateRepository certificateRepository;
    private final CaRepository caRepository;

    private final CertificateMapper certificateMapper;
    private final CaMapper caMapper;

    public CaServiceImpl(CertificateRepository certificateRepository, CaRepository caRepository, CertificateMapper certificateMapper, CaMapper caMapper) {
        this.certificateRepository = certificateRepository;
        this.caRepository = caRepository;
        this.certificateMapper = certificateMapper;
        this.caMapper = caMapper;
        initializeBouncyCastle();
    }

    public void initializeBouncyCastle() {
        // 매번 BouncyCastleProvider를 추가하지 않도록 체크
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    @Transactional
    public ResponseBodyForCreateCA generateCa(RequestBodyForCreateCA requestBody, Long certificateId) {
        log.info("generateCa() = {}", requestBody);

        // 1. Key 생성
        KeyPair keyPair = generateKeyPair(requestBody.getCertificateRequest().getKeyInfo());

        // 2. Subject 정보 설정 (Root CA이므로 issuer = subject)
        SubjectInfo subjectInfo = requestBody.getCertificateRequest().getSubjectInfo();
        X500Name issuerName = new X500Name(subjectInfo.toDistinguishedName());
        X500Name subjectName = new X500Name(subjectInfo.toDistinguishedName());

        // 3. CSR 생성
        String csrPem = CertificateUtil.generateCsr(subjectInfo, keyPair);
        PKCS10CertificationRequest csr = BouncyCastleUtil.parseCsr(csrPem);

        // 4. Certificate Builder 생성
        X509v3CertificateBuilder certBuilder = BouncyCastleUtil.getCertificateBuilder(
            issuerName, subjectName, requestBody.getCertificateRequest().getPeriod(), csr.getSubjectPublicKeyInfo());

        // 5. Extensions 추가
        try {
            List<CertificateExtension> extensions = createCertificateExtensions(csr, false, null);
            BouncyCastleUtil.setCertificateExtensions(certBuilder, extensions);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new RuntimeException("Failed to add extensions", e);
        }

        // 6. Certificate 생성 (Root CA이므로 자기 자신으로 서명)
        X509Certificate certificate = BouncyCastleUtil.getX509Certificate(certBuilder, keyPair.getPrivate());

        // 7. Private Key PEM 변환
        String privateKeyPem = CertificateUtil.toPemString(keyPair.getPrivate());

        // 8. CA 및 Certificate 저장 (Root CA이므로 signedCertificateId = null)
        CaEntity caEntity = saveCa(requestBody);
        CertificateEntity certificateEntity = saveCertificate(caEntity, csrPem, certificate, privateKeyPem, null);

        // 9. Response 생성
        return buildCreateCaResponse(caEntity, certificateEntity, certificate, issuerName);
    }

    @Override
    @Transactional
    public ResponseBodyForCreateCA generateIntermediateCA(RequestBodyForCreateCA requestBody, Long certificateId) {
        log.info("generateIntermediateCA() = {}", requestBody);

        // 1. Key 생성
        KeyPair keyPair = generateKeyPair(requestBody.getCertificateRequest().getKeyInfo());

        // 2. Upper Certificate 정보 조회 (Intermediate CA용)
        CertificateEntity upperCaCert = certificateRepository.findById(certificateId)
            .orElseThrow(() -> new RuntimeException("Certificate not found"));

        CaDto upperCaDto = caMapper.toDto(upperCaCert.getCa());
        if (upperCaDto.getStatus() != CaStatus.ACTIVE) {
            throw new RuntimeException("Upper CA is not ACTIVE");
        }

        X509Certificate upperCertificate = BouncyCastleUtil.parseCertificate(upperCaCert.getCertificatePem());
        PrivateKey signingKey = CertificateUtil.parsePrivateKey(upperCaCert.getPrivateKey());

        // 3. Subject 정보 설정 (Intermediate CA이므로 issuer는 upper certificate)
        SubjectInfo subjectInfo = requestBody.getCertificateRequest().getSubjectInfo();
        X500Name issuerName = new X500Name(upperCaCert.getSubject());
        X500Name subjectName = new X500Name(subjectInfo.toDistinguishedName());

        // 4. CSR 생성
        String csrPem = CertificateUtil.generateCsr(subjectInfo, keyPair);
        PKCS10CertificationRequest csr = BouncyCastleUtil.parseCsr(csrPem);


        // 5. Certificate Builder 생성
        X509v3CertificateBuilder certBuilder = BouncyCastleUtil.getCertificateBuilder(
            issuerName, subjectName, requestBody.getCertificateRequest().getPeriod(), csr.getSubjectPublicKeyInfo());

        // 6. Extensions 추가 (Intermediate용)
        try {
            List<CertificateExtension> extensions = createCertificateExtensions(csr, true, upperCertificate);
            BouncyCastleUtil.setCertificateExtensions(certBuilder, extensions);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new RuntimeException("Failed to add extensions", e);
        }

        // 7. Certificate 생성 (Upper CA로 서명)
        X509Certificate certificate = BouncyCastleUtil.getX509Certificate(certBuilder, signingKey);

        // 8. Private Key PEM 변환
        String privateKeyPem = CertificateUtil.toPemString(keyPair.getPrivate());

        // 9. SignedCertificateId 계산
        String signedCertificateId = upperCaCert.getSignedCertificateId() == null
            ? upperCaCert.getId().toString()
            : upperCaCert.getSignedCertificateId() + "," + upperCaCert.getId();

        // 10. CA 및 Certificate 저장
        CaEntity caEntity = saveCa(requestBody);
        CertificateEntity certificateEntity = saveCertificate(caEntity, csrPem, certificate, privateKeyPem, signedCertificateId);

        // 11. Response 생성
        return buildCreateCaResponse(caEntity, certificateEntity, certificate, issuerName);
    }

    @Override
    public ResponseBodyForReadCAList getCaList(int page) {
        log.info("getCaList()");
        PageRequest pageRequest = PageRequest.of(page, 10);

        Page<CaEntity> caEntities = caRepository.findByStatusNot(CaStatus.DELETED, pageRequest);

        //TODO : Return할 때 정보들 필터링 필요 (필요한 정보만)
        List<ResponseBodyForReadCA> caInfoList = caEntities.getContent().stream()
            .map(caEntity -> {
                CaDto caDto = caMapper.toDto(caEntity);
                CaInfo caInfo = CaInfo.fromCaDto(caDto);

                // 모든 certificates를 List로 변환
                List<CertificateInfo> certificateInfoList = caEntity.getCertificates().stream()
                    .map(certificate -> {
                        CertificateDto certificateDto = certificateMapper.toDto(certificate);
                        return getCertificateInfoByCertificateDto(certificateDto);
                    })
                    .collect(Collectors.toList());


                return ResponseBodyForReadCA.builder()
                    .caInfo(caInfo)
                    .certificateInfoList(certificateInfoList)
                    .status(caInfo.getStatus())
                    .creationDatetime(caDto.getCreationDatetime())
                    .creationUser(caDto.getCreationUser())
                    .build();
            })
            .collect(Collectors.toList());

        ResponseBodyForReadCAList result = ResponseBodyForReadCAList.builder()
            .caInfoList(caInfoList)
            .totalCnt(caEntities.getTotalElements())
            .totalPageNo((long) caEntities.getTotalPages())
            .currentPageNo((long) caEntities.getNumber() + 1)
            .build();

        return result;
    }

    @Override
    public ResponseBodyForReadCA getCA(Long caId) {
        log.info("getCA() = {}", caId);
        CaEntity caEntity = caRepository.findByIdAndStatusNot(caId, CaStatus.DELETED).orElseThrow(() -> new RuntimeException("CA not found"));

        CaDto caDto = caMapper.toDto(caEntity);
        CaInfo caInfo = CaInfo.fromCaDto(caDto);

        // 모든 certificates를 List로 변환
        //TODO : Return할 때 정보들 필터링 필요 (필요한 정보만)
        List<CertificateInfo> certificateInfoList = caEntity.getCertificates().stream()
            .map(certificate -> {
                CertificateDto certificateDto = certificateMapper.toDto(certificate);
                return getCertificateInfoByCertificateDto(certificateDto);
            })
            .collect(Collectors.toList());


        return ResponseBodyForReadCA.builder()
            .caInfo(caInfo)
            .certificateInfoList(certificateInfoList)
            .status(caInfo.getStatus())
            .creationDatetime(caDto.getCreationDatetime())
            .creationUser(caDto.getCreationUser())
            .build();
    }

    @Override
    public ResponseBodyForReadChainCA getCAChain(Long caId) {
        log.info("getCAChain() = {}", caId);
        CertificateEntity certificateEntity = certificateRepository.findByCa_Id(caId).orElseThrow(() -> new RuntimeException("CA not found"));
        List<Long> signedCaCertificateList = Arrays.stream(certificateEntity.getSignedCertificateId()
            .split(",")).map(Long::parseLong).toList();
        List<String> chainPems = buildCaChain(signedCaCertificateList);

        return ResponseBodyForReadChainCA.builder()
            .data(chainPems.stream()
                .map(String::trim)
                .collect(Collectors.joining("\n")))
            .build();
    }

    @Override
    public ResponseBodyForUpdateCA setCaDeletion(Long caId) {
        log.info("setCaDeletion() = {}, {}", caId);
        CaEntity caEntity = caRepository.findById(caId).orElseThrow(() -> new RuntimeException("CA not found"));
        CaDto caDto = caMapper.toDto(caEntity);

        if (caDto.getStatus() != CaStatus.ACTIVE && caDto.getStatus() != CaStatus.DISABLED) {
            // findBy..에서 처리할 수 도 있지만, 다른 Exception을 줘야히지 않을까?싶어 보류
            throw new RuntimeException("CA is not ACTIVE or DISABLED");
        }

        caEntity.setStatus(CaStatus.DELETE_SCHEDULED);
        caEntity.setDeletionDatetime(LocalDateTime.now().plusWeeks(1));
        CaEntity saveEntity = caRepository.save(caEntity);

        CaDto saveCaDto = caMapper.toDto(saveEntity);
        CaInfo caInfo = CaInfo.fromCaDto(saveCaDto);

        ResponseBodyForUpdateCA result = ResponseBodyForUpdateCA.builder()
            .caInfo(caInfo)
            .build();

        return result;
    }

    @Override
    public ResponseBodyForUpdateCA unsetCaDeletion(Long caId) {
        log.info("unsetCaDeletion() = {}", caId);
        CaEntity caEntity = caRepository.findByIdAndStatus(caId, CaStatus.DELETE_SCHEDULED).orElseThrow(() -> new RuntimeException("CA not found"));
        caEntity.setStatus(CaStatus.ACTIVE);
        caEntity.setDeletionDatetime(null);

        CaEntity saveEntity = caRepository.save(caEntity);

        CaDto saveCaDto = caMapper.toDto(saveEntity);
        CaInfo caInfo = CaInfo.fromCaDto(saveCaDto);

        ResponseBodyForUpdateCA result = ResponseBodyForUpdateCA.builder()
            .caInfo(caInfo)
            .build();

        return result;
    }

    @Override
    public ResponseBodyForUpdateCA removeCert(Long caId) {
        log.info("removeCert() = {}", caId);

        CaEntity caEntity = caRepository.findByIdAndStatus(caId, CaStatus.DELETE_SCHEDULED).orElseThrow(() -> new RuntimeException("CA not found"));
        caEntity.setStatus(CaStatus.DELETED);
        caEntity.setDeletionDatetime(LocalDateTime.now());

        // 모든 certificates를 삭제 상태로 변경
        if (caEntity.getCertificates() != null) {
            caEntity.getCertificates().forEach(cert -> {
                cert.setStatus(CertificateStatus.DELETED);
                cert.setDeletionDatetime(LocalDateTime.now());
            });
        }

        //TODO: 하위 CA 삭제 및 인증서들 삭제구현

        CaEntity saveEntity = caRepository.save(caEntity);

        CaDto saveCaDto = caMapper.toDto(saveEntity);
        CaInfo caInfo = CaInfo.fromCaDto(saveCaDto);

        ResponseBodyForUpdateCA result = ResponseBodyForUpdateCA.builder()
            .caInfo(caInfo)
            .build();
        return result;
    }

    @Override
    public ResponseBodyForUpdateCA activateCa(Long caId) {
        log.info("activateCa() = {}", caId);
        CaEntity caEntity = caRepository.findByIdAndStatus(caId, CaStatus.DISABLED).orElseThrow(() -> new RuntimeException("CA not found"));
        caEntity.setStatus(CaStatus.ACTIVE);

        // 모든 certificates를 활성화 상태로 변경
        if (caEntity.getCertificates() != null) {
            caEntity.getCertificates().forEach(cert -> {
                cert.setStatus(CertificateStatus.ACTIVE);
            });
        }

        CaEntity saveEntity = caRepository.save(caEntity);
        CaDto saveCaDto = caMapper.toDto(saveEntity);
        CaInfo caInfo = CaInfo.fromCaDto(saveCaDto);
        ResponseBodyForUpdateCA result = ResponseBodyForUpdateCA.builder()
            .caInfo(caInfo)
            .build();
        return result;
    }

    @Override
    public ResponseBodyForUpdateCA disableCa(Long caId) {
        log.info("disableCa() = {}", caId);
        CaEntity caEntity = caRepository.findByIdAndStatus(caId, CaStatus.ACTIVE).orElseThrow(() -> new RuntimeException("CA not found"));
        caEntity.setStatus(CaStatus.DISABLED);

        // 모든 certificates를 비활성화 상태로 변경
        if (caEntity.getCertificates() != null) {
            caEntity.getCertificates().forEach(cert -> {
                cert.setStatus(CertificateStatus.DISABLED);
            });
        }

        CaEntity saveEntity = caRepository.save(caEntity);
        CaDto saveCaDto = caMapper.toDto(saveEntity);
        CaInfo caInfo = CaInfo.fromCaDto(saveCaDto);
        ResponseBodyForUpdateCA result = ResponseBodyForUpdateCA.builder()
            .caInfo(caInfo)
            .build();
        return result;
    }

    private KeyPair generateKeyPair(KeyInfo keyInfo) {
        if (!KeyAlgorithm.isValidAlgorithmAndKeySize(keyInfo.getAlgorithm(), keyInfo.getKeySize())) {
            throw new RuntimeException("Invalid Algorithm or Key Size");
        }

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyInfo.getAlgorithm(), "BC");
            keyGen.initialize(keyInfo.getKeySize());
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Wrong Algorithm");
        } catch (NoSuchProviderException e) {
            throw new RuntimeException("No Such Provider");
        }

    }

    private List<String> buildCaChain(List<Long> signedCaCertificateList) {
        List<String> chain = new ArrayList<>();

        signedCaCertificateList.forEach(id -> {
            CertificateEntity certificate = certificateRepository.findById(id).orElseThrow(() -> new RuntimeException("ChainCertificate not found"));
            chain.add(certificate.getCertificatePem());
        });
        return chain;
    }

    private CertificateInfo getCertificateInfoByCertificateDto(CertificateDto certificateDto) {
        X509Certificate x509Certificate = BouncyCastleUtil.parseCertificate(certificateDto.getCertificatePem());
        return CertificateInfo.fromCertificateDtoAndCertificate(certificateDto, x509Certificate);
    }

    /**
     * Certificate Extensions 생성
     */
    private List<CertificateExtension> createCertificateExtensions(PKCS10CertificationRequest csr, boolean isIntermediate,
                                                                   X509Certificate upperCertificate) throws NoSuchAlgorithmException, CertificateEncodingException {
        List<CertificateExtension> extensions = new ArrayList<>();

        // 기본 확장 필드
        extensions.add(new CertificateExtension(
            Extension.basicConstraints,
            true,
            new BasicConstraints(true)
        ));
        extensions.add(new CertificateExtension(
            Extension.keyUsage,
            true,
            new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        ));

        // Subject Key Identifier
        extensions.add(new CertificateExtension(
            Extension.subjectKeyIdentifier,
            false,
            new JcaX509ExtensionUtils().createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo())
        ));

        // Intermediate일 경우 Authority Key Identifier 추가
        if (isIntermediate && upperCertificate != null) {
            extensions.add(new CertificateExtension(
                Extension.authorityKeyIdentifier,
                false,
                new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(upperCertificate)
            ));
        }

        return extensions;
    }

    /**
     * CA 저장
     */
    private CaEntity saveCa(RequestBodyForCreateCA requestBody) {
        CaDto caDto = CaDto.builder()
            .name(requestBody.getName())
            .toastProjectId(1L) // TODO: 실제 프로젝트 ID로 변경 필요
            .status(CaStatus.ACTIVE)
            .creationUser("HOSEOK")
            .creationDatetime(LocalDateTime.now())
            .build();
        CaEntity caEntity = caMapper.toEntity(caDto);
        return caRepository.save(caEntity);
    }

    /**
     * Certificate 저장
     */
    private CertificateEntity saveCertificate(CaEntity caEntity, String csrPem, X509Certificate certificate,
                                              String privateKeyPem, String signedCertificateId) {
        // Certificate에서 PEM 생성
        String certificatePem = CertificateUtil.toPemString(certificate);

        CertificateDto certificateDto = CertificateDto.builder()
            .ca(CaDto.builder().id(caEntity.getId()).build())
            .csr(csrPem)
            .status(CertificateStatus.ACTIVE)
            .certificatePem(certificatePem)
            .privateKey(privateKeyPem)
            .creationUser("HOSEOK")
            .creationDatetime(LocalDateTime.now())
            .signedCertificateId(signedCertificateId)
            .build();
        certificateDto.setX509Certificate(certificate);

        CertificateEntity certificateEntity = certificateMapper.toEntity(certificateDto);
        return certificateRepository.save(certificateEntity);
    }

    /**
     * ResponseBodyForCreateCA 생성
     */
    private ResponseBodyForCreateCA buildCreateCaResponse(CaEntity caEntity, CertificateEntity certificateEntity,
                                                          X509Certificate certificate, X500Name issuerName) {
        CaInfo caInfo = CaInfo.fromCaDto(caMapper.toDto(caEntity));

        CertificateInfo caCertificateInfo = CertificateInfo.fromCertificateDtoAndCertificate(
            certificateMapper.toDto(certificateEntity), certificate);
        caCertificateInfo.setSerialNumber(CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray()));
        caCertificateInfo.setIssuer(issuerName.toString());

        return ResponseBodyForCreateCA.builder()
            .caInfo(caInfo)
            .certificateInfo(caCertificateInfo)
            .status(caInfo.getStatus())
            .creationDatetime(caEntity.getCreationDatetime())
            .creationUser(caEntity.getCreationUser())
            .build();
    }
}
