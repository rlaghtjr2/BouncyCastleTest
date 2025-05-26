package com.nhncloud.pca.service;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Service;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.mapper.CaMapper;
import com.nhncloud.pca.mapper.CertificateMapper;
import com.nhncloud.pca.model.ca.CaDto;
import com.nhncloud.pca.model.certificate.CertificateDto;
import com.nhncloud.pca.model.certificate.CertificateExtension;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.csr.CsrInfo;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCertList;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForUpdateCert;
import com.nhncloud.pca.model.subject.SubjectInfo;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.CertificateRepository;
import com.nhncloud.pca.util.BouncyCastleUtil;
import com.nhncloud.pca.util.CertificateUtil;

@Service
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CaRepository caRepository;

    private final CertificateMapper certificateMapper;
    private final CaMapper caMapper;

    public CertificateServiceImpl(CertificateRepository certificateRepository, CaRepository caRepository, CertificateMapper certificateMapper, CaMapper caMapper) {
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
    public ResponseBodyForCreateCert generateCert(RequestBodyForCreateCert requestBody, Long caId) throws Exception {
        log.info("generateCert() = {}, caId = {}", requestBody, caId);

        //1. 인증서 생성에 사용할 Key 만들기
        KeyPair keyPair = generateKeyPair(requestBody.getKeyInfo());

        //2. 정보 세팅
        SubjectInfo subjectInfo = requestBody.getSubjectInfo();

        CertificateEntity upperCertEntity = certificateRepository.findByCa_Id(caId).orElseThrow(() -> new RuntimeException("Certificate not found"));
        CertificateDto upperCertDto = certificateMapper.toDto(upperCertEntity);
        CaDto upperCaDto = caMapper.toDto(upperCertEntity.getCa());
        if (upperCaDto.getStatus() != CaStatus.ACTIVE) {
            // findBy..에서 처리할 수 도 있지만, 다른 Exception을 줘야히지 않을까?싶어 보류
            throw new RuntimeException("Upper CA is not ACTIVE");
        }

        PrivateKey upperPrivateKey = CertificateUtil.parsePrivateKey(upperCertDto.getPrivateKeyPem());
        X509Certificate upperCertificate = BouncyCastleUtil.parseCertificate(upperCertDto.getCertificatePem());

        PrivateKey signingKey = upperPrivateKey;
        X500Name subjectName = new X500Name(subjectInfo.toDistinguishedName());
        X500Name issuerName = new X500Name(upperCertDto.getSubject());

        //3. CSR 파일 생성
        CsrInfo csrInfo = CertificateUtil.generateCsr(requestBody, keyPair);
        String csrPem = csrInfo.getCsrPem();
        PKCS10CertificationRequest csr = BouncyCastleUtil.parseCsr(csrPem);

        // 4. Builder 생성
        X509v3CertificateBuilder certBuilder = BouncyCastleUtil.getCertificateBuilder(
            issuerName,
            subjectName,
            requestBody.getPeriod(),
            csr.getSubjectPublicKeyInfo()
        );

        // 5. Extension 넣기
        try {
            List<CertificateExtension> extensions = new ArrayList<>();

            // 기본 확장 필드
            extensions.add(new CertificateExtension(Extension.basicConstraints, false, new BasicConstraints(false)));
            extensions.add(new CertificateExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.keyAgreement)));
            certBuilder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            // Subject Key Identifier
            extensions.add(new CertificateExtension(Extension.subjectKeyIdentifier, false,
                new JcaX509ExtensionUtils().createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo())
            ));
            // Authority Key Identifier 추가
            extensions.add(new CertificateExtension(Extension.authorityKeyIdentifier, false,
                new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(upperCertificate.getPublicKey())
            ));
            BouncyCastleUtil.setCertificateExtensions(certBuilder, extensions);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to add extensions", e);
        }

        // 6. 인증서 생성 - siningKey로  sign
        X509Certificate certificate = BouncyCastleUtil.getX509Certificate(certBuilder, signingKey);

        // 7. 생성한 인증서 PEM 변환
        String certificatePem = CertificateUtil.toPemString(certificate);
        String privateKeyPem = CertificateUtil.toPemString(keyPair.getPrivate());

        // 8. DB에 저장
        // 8-1 인증서 정보
        CertificateDto certificateDto = CertificateDto.builder()
            .csr(csrPem)
            .keyAlgorithm(requestBody.getKeyInfo().getAlgorithm())
            .signingAlgorithm("SHA256withRSA")
            .certificatePem(certificatePem)
            .privateKeyPem(privateKeyPem)
            .status(CertificateStatus.ACTIVE)
            .signedCaId(upperCertEntity.getSignedCaId())
            .creationUser("HOSEOK")
            .creationDatetime(LocalDateTime.now())
            .build();
        certificateDto.setX509Certificate(certificate);
        CertificateEntity certificateEntity = certificateMapper.toEntity(certificateDto);

        certificateRepository.save(certificateEntity);


        // 9. Return 객체 생성
        ResponseBodyForCreateCert result = ResponseBodyForCreateCert.builder()
            .serialNo(CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray()))
            .certificatePem(certificatePem)
            .privateKeyPem(privateKeyPem)
            .issuer(upperCertDto.getCertificatePem())
            .build();

        return result;
    }

    @Override
    public ResponseBodyForReadCert getCert(Long caId, Long certId) {
        log.info("getCert() = {}, {}", caId, certId);
        CertificateEntity certificateEntity = certificateRepository.findByIdAndSignedCaIdAndStatusNot(certId, String.valueOf(caId), CertificateStatus.DELETED)
            .orElseThrow(() -> new RuntimeException("Certificate not found"));
        CertificateDto certificateDto = certificateMapper.toDto(certificateEntity);

        X509Certificate certificate = BouncyCastleUtil.parseCertificate(certificateDto.getCertificatePem());

        CertificateInfo certificateInfo = CertificateInfo.fromCertificateDtoAndCertificate(certificateDto, certificate);

        ResponseBodyForReadCert result = ResponseBodyForReadCert.builder()
            .commonName(certificateInfo.getSubjectInfo().getCommonName())
            .serialNumber(certificateInfo.getSerialNumber())
            .notAfterDateTime(certificateInfo.getNotAfterDateTime())
            .notBeforeDateTime(certificateInfo.getNotBeforeDateTime())
            .publicKeyAlgorithm(certificateInfo.getPublicKeyAlgorithm())
            .certificatePem(certificateInfo.getCertificatePem())
            .chainCertificatePem(certificateInfo.getChainCertificatePem())
            .signatureAlgorithm(certificateInfo.getSignatureAlgorithm())
            .build();

        return result;
    }

    @Override
    public ResponseBodyForReadCertList getCertList(Long caId) {
        log.info("getCertList() = {}", caId);
        List<CertificateEntity> certificateEntities = certificateRepository.findBySignedCaIdAndCaIsNullAndStatusNot(String.valueOf(caId), CertificateStatus.DELETED).orElseThrow(() -> new RuntimeException("CA not found"));

        List<String> certSerialNumberList = certificateEntities.stream()
            .map(certificateEntity -> {
                CertificateDto certificateDto = certificateMapper.toDto(certificateEntity);

                X509Certificate certificate = BouncyCastleUtil.parseCertificate(certificateDto.getCertificatePem());
                return CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray());
            })
            .collect(Collectors.toList());

        return ResponseBodyForReadCertList.builder()
            .listCerts(certSerialNumberList)
            .build();
    }

    @Override
    public ResponseBodyForUpdateCert activateCert(Long caId, Long certId) {
        log.info("activateCert. caId = {}, certId = {}", caId, certId);
        CertificateEntity certificateEntity = certificateRepository.findByIdAndStatus(certId, CertificateStatus.DISABLED)
            .orElseThrow(() -> new RuntimeException("Certificate not found"));

        certificateEntity.setStatus(CertificateStatus.ACTIVE);

        CertificateEntity saveCertificateEntity = certificateRepository.save(certificateEntity);

        CertificateDto certificateDto = certificateMapper.toDto(saveCertificateEntity);
        CertificateInfo certificateInfo = CertificateInfo.fromCertificateDtoAndCertificate(certificateDto, BouncyCastleUtil.parseCertificate(certificateDto.getCertificatePem()));
        ResponseBodyForUpdateCert result = ResponseBodyForUpdateCert.builder()
            .certificateInfo(certificateInfo)
            .build();
        return result;
    }

    @Override
    public ResponseBodyForUpdateCert disableCert(Long caId, Long certId) {
        log.info("disableCert. caId = {}, certId = {}", caId, certId);
        CertificateEntity certificateEntity = certificateRepository.findByIdAndStatus(certId, CertificateStatus.ACTIVE)
            .orElseThrow(() -> new RuntimeException("Certificate not found"));

        certificateEntity.setStatus(CertificateStatus.DISABLED);

        CertificateEntity saveCertificateEntity = certificateRepository.save(certificateEntity);

        CertificateDto certificateDto = certificateMapper.toDto(saveCertificateEntity);
        CertificateInfo certificateInfo = CertificateInfo.fromCertificateDtoAndCertificate(certificateDto, BouncyCastleUtil.parseCertificate(certificateDto.getCertificatePem()));
        ResponseBodyForUpdateCert result = ResponseBodyForUpdateCert.builder()
            .certificateInfo(certificateInfo)
            .build();
        return result;
    }

    private KeyPair generateKeyPair(KeyInfo keyInfo) {
        // Algorithm과 provider BC(Bouncy Castle) 지정
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance(keyInfo.getAlgorithm(), "BC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Wrong Algorithm");
        } catch (NoSuchProviderException e) {
            //BC를 넣고있기때문에 발생하지 않을것같음..
            throw new RuntimeException("No Such Provider");
        }
        // 키 사이즈 지정
        keyGen.initialize(keyInfo.getKeySize());
        return keyGen.generateKeyPair();

    }
}
