package com.nhncloud.pca.service;

import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.constant.ca.CaType;
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
import com.nhncloud.pca.model.csr.CsrInfo;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.ca.RequestBodyForUpdateCA;
import com.nhncloud.pca.model.request.certificate.RequestBodyForCreateCert;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCAList;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadChainCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForCreateCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCert;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForReadCertList;
import com.nhncloud.pca.model.subject.SubjectInfo;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.CertificateRepository;
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
    public ResponseBodyForCreateCA generateCa(RequestBodyForCreateCA requestBody, String caType, Long caId) {
        log.info("generateCa() = {}, caType = {}", requestBody, caType);

        //1. 인증서 생성에 사용할 Key 만들기
        KeyPair keyPair = generateKeyPair(requestBody.getKeyInfo());

        //2. CSR 파일 생성
        CsrInfo csrInfo = generateCsr(requestBody, keyPair);
        String csrPem = csrInfo.getCsrPem();
        PKCS10CertificationRequest csr = CertificateUtil.parseCsr(csrPem);

        //3. 정보 세팅
        SubjectInfo subjectInfo = requestBody.getSubjectInfo();

        X500Name issuerName = new X500Name(subjectInfo.toDistinguishedName());
        X500Name subjectName = new X500Name(subjectInfo.toDistinguishedName());

        PrivateKey signingKey = keyPair.getPrivate();

        X509Certificate upperCertificate = null;
        if (caType.equals(CaType.SUB.getType())) {
            //3. Intermediate경우 signingKey와 Issuer가 달라짐
            CertificateEntity upperCa = certificateRepository.findByCa_Id(caId).orElseThrow(() -> new RuntimeException("CA not found"));
            String upperPrivateKeyPem = upperCa.getPrivateKeyPem();
            String upperCertificatePem = upperCa.getCertificatePem();
            PrivateKey upperPrivateKey = CertificateUtil.parsePrivateKey(upperPrivateKeyPem);
            upperCertificate = CertificateUtil.parseCertificate(upperCertificatePem);

            signingKey = upperPrivateKey;
            issuerName = getReversedDistinguishedName(upperCertificate.getSubjectX500Principal());

        }

        // 4. Builder 생성
        X509v3CertificateBuilder certBuilder = getCertificateBuilder(
            issuerName,
            subjectName,
            requestBody.getPeriod(),
            csr.getSubjectPublicKeyInfo()
        );

        // 5. Extension 넣기
        try {
            List<CertificateExtension> extensions = new ArrayList<>();

            // 기본 확장 필드
            extensions.add(new CertificateExtension(Extension.basicConstraints, true, new BasicConstraints(true)));
            extensions.add(new CertificateExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)));

            // Subject Key Identifier
            extensions.add(new CertificateExtension(
                Extension.subjectKeyIdentifier,
                false,
                new JcaX509ExtensionUtils().createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo())
            ));
            // Intermediate일 경우 Authority Key Identifier 추가
            if (caType.equals(CaType.SUB.getType())) {
                extensions.add(new CertificateExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(upperCertificate.getPublicKey())
                ));
            }

            CertificateUtil.setCertificateExtensions(certBuilder, extensions);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to add extensions", e);
        }

        // 6. 인증서 생성 - signing Key로  sign
        X509Certificate certificate = getX509Certificate(certBuilder, signingKey);

        // 7. 생성한 인증서 PEM 변환
        String certificatePem = CertificateUtil.toPemString(certificate);
        String privateKeyPem = CertificateUtil.toPemString(keyPair.getPrivate());


        // 8. DB에 저장
        // 만든 CA Dto -> Entity
        CaDto caDto = CaDto.builder()
            .name(requestBody.getName())
            .type(caType)
            .status(CaStatus.ACTIVE)
            .creationUser("HOSEOK")
            .creationDatetime(LocalDateTime.now())
            .build();
        CaEntity caEntity = caMapper.toEntity(caDto);

        // 상위CA
        CaEntity upperCaEntity = caEntity;
        if (caType.equals(CaType.SUB.getType())) {
            // SUB인경우 상위CA를 DB에서 불러옴
            upperCaEntity = caRepository.findById(caId).orElseThrow(() -> new RuntimeException("CA not found"));
        }

        // 만들어진 인증서 정보 Certificate Dto -> Entity
        CertificateDto certificateDto = CertificateDto.builder()
            .csr(csrPem)
            .status(CertificateStatus.ACTIVE)
            .certificatePem(certificatePem)
            .privateKeyPem(privateKeyPem)
            .creationUser("HOSEOK")
            .creationDatetime(LocalDateTime.now())
            .build();
        certificateDto.setX509Certificate(certificate);
        CertificateEntity certificateEntity = certificateMapper.toEntity(certificateDto);

        // 상위 CA에 만들어진 인증서 정보 저장
        if (upperCaEntity.getSignedCertificates() == null) {
            upperCaEntity.setSignedCertificates(new ArrayList<>());
        }
        upperCaEntity.getSignedCertificates().add(certificateEntity);

        certificateEntity.setSignedCa(upperCaEntity);
        // CA 정보 저장
        certificateEntity.setCa(caEntity);
        caRepository.save(caEntity);

        // 9. Return type 정의
        // 9-1 Ca 정보
        CaInfo caInfo = CaInfo.fromCaDto(caMapper.toDto(caEntity));

        // 9-2 인증서 정보
        CertificateInfo caCertificateInfo = CertificateInfo.fromCertificateDtoAndCertificate(certificateMapper.toDto(certificateEntity), certificate);
        caCertificateInfo.setSerialNumber(CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray()));
        caCertificateInfo.setIssuer(issuerName.toString());

        // 9. Return값
        ResponseBodyForCreateCA result = ResponseBodyForCreateCA.builder()
            .caInfo(caInfo)
            .certificateInfo(caCertificateInfo)
            .status(caInfo.getStatus())
            .creationDatetime(caDto.getCreationDatetime())
            .creationUser(caDto.getCreationUser())
            .build();

        return result;
    }

    @Override
    public ResponseBodyForCreateCert generateCert(RequestBodyForCreateCert requestBody, Long caId) throws Exception {
        log.info("generateCert() = {}, caId = {}", requestBody, caId);

        //1. 인증서 생성에 사용할 Key 만들기
        KeyPair keyPair = generateKeyPair(requestBody.getKeyInfo());

        //2. CSR 파일 생성
        CsrInfo csrInfo = generateCsr(requestBody, keyPair);
        String csrPem = csrInfo.getCsrPem();
        PKCS10CertificationRequest csr = CertificateUtil.parseCsr(csrPem);

        //3. 정보 세팅
        SubjectInfo subjectInfo = requestBody.getSubjectInfo();

        CertificateEntity upperCert = certificateRepository.findByCa_Id(caId).orElseThrow(() -> new RuntimeException("Certificate not found"));
        String upperPrivateKeyPem = upperCert.getPrivateKeyPem();
        String upperCertificatePem = upperCert.getCertificatePem();
        PrivateKey upperPrivateKey = CertificateUtil.parsePrivateKey(upperPrivateKeyPem);
        X509Certificate upperCertificate = CertificateUtil.parseCertificate(upperCertificatePem);

        PrivateKey signingKey = upperPrivateKey;
        X500Name subjectName = new X500Name(subjectInfo.toDistinguishedName());
        X500Name issuerName = getReversedDistinguishedName(upperCertificate.getSubjectX500Principal());


        // 4. Builder 생성
        X509v3CertificateBuilder certBuilder = getCertificateBuilder(
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
            CertificateUtil.setCertificateExtensions(certBuilder, extensions);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to add extensions", e);
        }

        // 6. 인증서 생성 - siningKey로  sign
        X509Certificate certificate = getX509Certificate(certBuilder, signingKey);

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
            .creationUser("HOSEOK")
            .creationDatetime(LocalDateTime.now())
            .build();
        certificateDto.setX509Certificate(certificate);
        CertificateEntity certificateEntity = certificateMapper.toEntity(certificateDto);

        // 8-2 CA 정보
        CaEntity caEntity = caRepository.findById(caId).orElseThrow(() -> new RuntimeException("CA not found"));

        caEntity.getSignedCertificates().add(certificateEntity);
        certificateEntity.setSignedCa(caEntity);
        caRepository.save(caEntity);

        // 9. Return 객체 생성
        ResponseBodyForCreateCert result = ResponseBodyForCreateCert.builder()
            .serialNo(CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray()))
            .certificatePem(certificatePem)
            .privateKeyPem(privateKeyPem)
            .issuer(upperCertificatePem)
            .build();

        return result;
    }

    @Override
    public ResponseBodyForReadCAList getCaList(int page) {
        log.info("getCaList()");
        PageRequest pageRequest = PageRequest.of(page, 10);

        Page<CaEntity> caEntities = caRepository.findAll(pageRequest);

        List<ResponseBodyForReadCA> caInfoList = caEntities.getContent().stream()
            .map(caEntity -> {
                CaDto caDto = caMapper.toDto(caEntity);
                CaInfo caInfo = CaInfo.fromCaDto(caDto);

                CertificateDto certificateDto = certificateMapper.toDto(caEntity.getCertificate());
                CertificateInfo caCertificateInfo = getCertificateInfoByCertificateDto(certificateDto);

                return ResponseBodyForReadCA.builder().
                    caInfo(caInfo)
                    .certificateInfo(caCertificateInfo)
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
        CaEntity caEntity = caRepository.findById(caId).orElseThrow(() -> new RuntimeException("CA not found"));
        CaDto caDto = caMapper.toDto(caEntity);
        CaInfo caInfo = CaInfo.fromCaDto(caDto);

        CertificateDto certificateDto = certificateMapper.toDto(caEntity.getCertificate());
        CertificateInfo caCertificateInfo = getCertificateInfoByCertificateDto(certificateDto);

        ResponseBodyForReadCA result = ResponseBodyForReadCA.builder().
            caInfo(caInfo)
            .certificateInfo(caCertificateInfo)
            .status(caInfo.getStatus())
            .creationDatetime(caDto.getCreationDatetime())
            .creationUser(caDto.getCreationUser())
            .build();

        return result;
    }

    @Override
    public ResponseBodyForUpdateCA updateCA(Long caId, RequestBodyForUpdateCA requestBody) {
        log.info("updateCA() = {}", caId);
        CaEntity caEntity = caRepository.findById(caId).orElseThrow(() -> new RuntimeException("CA not found"));
        caEntity.setStatus(requestBody.getStatus());

        CaEntity saveEntity = caRepository.save(caEntity);

        CaDto caDto = caMapper.toDto(saveEntity);
        CaInfo caInfo = CaInfo.fromCaDto(caDto);

        ResponseBodyForUpdateCA result = ResponseBodyForUpdateCA.builder()
            .caInfo(caInfo)
            .build();

        return result;
    }

    @Override
    public ResponseBodyForReadChainCA getCAChain(Long caId) {
        log.info("getCAChain() = {}", caId);
        CertificateEntity certificateEntity = certificateRepository.findByCa_Id(caId).orElseThrow(() -> new RuntimeException("CA not found"));

        List<String> chainPems = buildCaChain(certificateEntity.getSignedCa().getCertificate());

        return ResponseBodyForReadChainCA.builder()
            .data(chainPems.stream()
                .map(String::trim)
                .collect(Collectors.joining("\n")))
            .build();
    }

    @Override
    public ResponseBodyForReadCert getCert(Long caId, Long certId) {
        log.info("getCert() = {}, {}", caId, certId);
        CertificateEntity certificateEntity = certificateRepository.findByIdAndSignedCa_Id(certId, caId)
            .orElseThrow(() -> new RuntimeException("Certificate not found"));
        CertificateDto certificateDto = certificateMapper.toDto(certificateEntity);

        X509Certificate certificate = CertificateUtil.parseCertificate(certificateDto.getCertificatePem());

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
        List<CertificateEntity> certificateEntities = certificateRepository.findBySignedCa_IdAndCaIsNull(caId).orElseThrow(() -> new RuntimeException("CA not found"));

        List<String> certSerialNumberList = certificateEntities.stream()
            .map(certificateEntity -> {
                CertificateDto certificateDto = certificateMapper.toDto(certificateEntity);

                X509Certificate certificate = CertificateUtil.parseCertificate(certificateDto.getCertificatePem());
                return CertificateUtil.formatSerialNumber(certificate.getSerialNumber().toByteArray());
            })
            .collect(Collectors.toList());

        return ResponseBodyForReadCertList.builder()
            .listCerts(certSerialNumberList)
            .build();
    }

    private List<String> buildCaChain(CertificateEntity certificateEntity) {
        List<String> chain = new ArrayList<>();

        while (!isSelfSigned(certificateEntity)) {
            chain.add(certificateEntity.getCertificatePem());
            certificateEntity = certificateEntity.getSignedCa().getCertificate();
        }

        // 루트 CA도 포함
        chain.add(certificateEntity.getCertificatePem());

        return chain;
    }

    private boolean isSelfSigned(CertificateEntity cert) {
        return cert.getCa().getId().equals(cert.getSignedCa().getId());
    }

    public CsrInfo generateCsr(RequestBodyForCreateCert requestBody, KeyPair keyPair) {
        log.info("generateCsr() = {}", requestBody);

        SubjectInfo subjectInfo = requestBody.getSubjectInfo();

        // Subject 이름 설정
        X500Name subject = new X500Name(subjectInfo.toDistinguishedName());

        // CSR 빌더
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        PKCS10CertificationRequestBuilder csrBuilder =
            new PKCS10CertificationRequestBuilder(subject, subjectPublicKeyInfo);

        ContentSigner signer = null;
        try {
            signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new RuntimeException("new JcaContentSignerBuilder() = [OperatorCreationException]");
        }

        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        // PEM 문자열로 변환
        String csrPem = CertificateUtil.toPemString(csr);
        String privateKeyPem = CertificateUtil.toPemString(keyPair.getPrivate());

        CsrInfo csrInfo = CsrInfo.builder()
            .csrPem(csrPem)
            .privateKeyPem(privateKeyPem).build();

        return csrInfo;
    }

    private X509Certificate getX509Certificate(X509v3CertificateBuilder certBuilder, PrivateKey privateKey) {
        // 서명자 생성
        ContentSigner signer;
        try {
            signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(privateKey);
        } catch (OperatorCreationException e) {
            throw new RuntimeException("new JcaContentSignerBuilder() = [OperatorCreationException]");
        }

        X509CertificateHolder holder = certBuilder.build(signer);
        X509Certificate certificate;
        try {
            certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        } catch (CertificateException e) {
            throw new RuntimeException("new JcaX509CertificateConverter() = [CertificateException]");
        }
        return certificate;
    }

    private X509v3CertificateBuilder getCertificateBuilder(X500Name issuerName, X500Name subjectName, Integer period, SubjectPublicKeyInfo subjectPublicKeyInfo) {
        // 유효 기간, 시리얼 등 설정
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + (period * 24L * 60 * 60 * 1000));  // 기간 (일 단위)

        return new X509v3CertificateBuilder(
            issuerName,
            serial,
            notBefore,
            notAfter,
            subjectName,
            subjectPublicKeyInfo
        );

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

    public X500Name getReversedDistinguishedName(X500Principal principal) {
        if (principal == null) return null;

        X500Name original = new X500Name(principal.getName("RFC2253"));
        RDN[] rdns = original.getRDNs();

        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        for (int i = rdns.length - 1; i >= 0; i--) {
            builder.addRDN(rdns[i].getFirst().getType(), rdns[i].getFirst().getValue());
        }

        return builder.build();
    }

    private CertificateInfo getCertificateInfoByCertificateDto(CertificateDto certificateDto) {
        X509Certificate certificate = CertificateUtil.parseCertificate(certificateDto.getCertificatePem());

        CertificateInfo caCertificateInfo = CertificateInfo.fromCertificateDtoAndCertificate(
            certificateDto,
            certificate
        );

        caCertificateInfo.setCertificateId(certificateDto.getId());

        return caCertificateInfo;
    }
}
