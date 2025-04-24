package com.nhncloud.pca.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
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
import org.springframework.stereotype.Service;

import com.nhncloud.pca.constant.CaStatus;
import com.nhncloud.pca.constant.CaType;
import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CaCertificateInfo;
import com.nhncloud.pca.model.certificate.CertificateExtension;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.request.RequestBodyForCreateCA;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.model.subject.SubjectInfo;
import com.nhncloud.pca.util.CertificateUtil;

@Service
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    public CertificateServiceImpl() {
        initializeBouncyCastle();
    }

    public void initializeBouncyCastle() {
        // 매번 BouncyCastleProvider를 추가하지 않도록 체크
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }


    @Override
    public CertificateResult generateRootCertificate(RequestBodyForCreateCA requestBody) {
        log.info("generateRootCertificate() = {}", requestBody);

        // 1. 정보 세팅
        KeyInfo keyInfo = requestBody.getKeyInfo();
        SubjectInfo subjectInfo = requestBody.getSubjectInfo();

        KeyPair keyPair = generateKeyPair(keyInfo);

        // publicKey → SubjectPublicKeyInfo로 변환
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X500Name issuer = new X500Name(subjectInfo.toDistinguishedName());

        CertificateResult result = issueCertificate(
            requestBody,
            issuer,
            subjectPublicKeyInfo,
            keyPair.getPrivate(),
            CaType.ROOT,
            CertificateUtil.toPemString(keyPair.getPrivate()),
            null
        );

        return result;
    }


    @Override
    public CertificateResult generateIntermediateCertificate(RequestBodyForCreateCA requestBody) throws Exception {
        CaCertificateInfo intermediateCaCertificateInfo = generateIntermediateCaCsr(requestBody);

        //Todo DB Select 필요
        String rootPrivateKeyPem = Files.readString(Paths.get("src/main/resources/cert/rootCA.key"));
        String rootCertificatePem = Files.readString(Paths.get("src/main/resources/cert/rootCA.crt"));

        // 정보 세팅
        String csrPem = intermediateCaCertificateInfo.getCertificatePem();

        PrivateKey rootPrivateKey = CertificateUtil.parsePrivateKey(rootPrivateKeyPem);
        X509Certificate rootCertificate = CertificateUtil.parseCertificate(rootCertificatePem);
        PKCS10CertificationRequest csr = CertificateUtil.parseCsr(csrPem);


        CertificateResult result = issueCertificate(
            requestBody,
            new X500Name(rootCertificate.getSubjectX500Principal().getName()),
            csr.getSubjectPublicKeyInfo(),
            rootPrivateKey,
            CaType.SUB,
            intermediateCaCertificateInfo.getPrivateKeyPem(),
            rootCertificate
        );

        return result;
    }

    public CaCertificateInfo generateIntermediateCaCsr(RequestBodyForCreateCA requestBody) {
        // Intermediate용 KeyPair 생성
        log.info("generateIntermediateCaCsr() = {}", requestBody);

        KeyInfo keyInfo = requestBody.getKeyInfo();
        SubjectInfo subjectInfo = requestBody.getSubjectInfo();

        KeyPair keyPair = generateKeyPair(keyInfo);

        // Subject 이름 설정
        X500Name subject = new X500Name(subjectInfo.toDistinguishedName());

        // CSR 확장 생성
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        try {
            extGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));  // CA: true
            extGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } catch (IOException e) {
            throw new RuntimeException("new ExtensionsGenerator() = [IOException]");
        }

        Extensions extensions = extGen.generate();

        // CSR 빌더
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        PKCS10CertificationRequestBuilder csrBuilder =
            new PKCS10CertificationRequestBuilder(subject, subjectPublicKeyInfo);

        csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);

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

        CaCertificateInfo certificateInfo = CaCertificateInfo.builder()
            .certificatePem(csrPem)
            .privateKeyPem(privateKeyPem).build();

        return certificateInfo;
    }

    public CertificateResult issueCertificate(RequestBodyForCreateCA requestBody, X500Name issuer, SubjectPublicKeyInfo subjectPublicKeyInfo, PrivateKey signingKey, CaType caType, String issuerPrivateKeyPem, X509Certificate issuerCert) {
        SubjectInfo subjectInfo = requestBody.getSubjectInfo();
        KeyInfo keyInfo = requestBody.getKeyInfo();

        // 1. 인증서 빌더 생성
        X509v3CertificateBuilder certBuilder = getCertificateBuilder(
            issuer,
            requestBody.getPeriod(),
            new X500Name(subjectInfo.toDistinguishedName()),
            subjectPublicKeyInfo
        );

        // 2. 확장 필드 추가
        try {
            List<CertificateExtension> extensions = new ArrayList<>();

            // 기본 확장 필드
            extensions.add(new CertificateExtension(Extension.basicConstraints, true, new BasicConstraints(true)));
            extensions.add(new CertificateExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)));

            // Subject Key Identifier
            extensions.add(new CertificateExtension(
                Extension.subjectKeyIdentifier,
                false,
                new JcaX509ExtensionUtils().createSubjectKeyIdentifier(subjectPublicKeyInfo)
            ));

            // Intermediate일 경우 Authority Key Identifier 추가
            if (caType == CaType.SUB && issuerCert != null) {
                extensions.add(new CertificateExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(issuerCert.getPublicKey())
                ));
            }
            CertificateUtil.setCertificateExtensions(certBuilder, extensions);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to add extensions", e);
        }

        // 3. 인증서 생성
        X509Certificate certificate = getX509Certificate(certBuilder, signingKey);

        // 4. PEM 변환
        String certificatePem = CertificateUtil.toPemString(certificate);
        String privateKeyPem = issuerPrivateKeyPem;

        // 5. Return 객체 생성
        CaInfo caInfo = CaInfo.of(requestBody, caType.getType());

        CaCertificateInfo caCertificateInfo = CaCertificateInfo.of(
            subjectInfo,
            certificate,
            keyInfo,
            certificatePem,
            privateKeyPem
        );

        return CertificateResult.of(
            caInfo,
            caCertificateInfo,
            CaStatus.ACTIVE.getStatus()
        );
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

    private X509v3CertificateBuilder getCertificateBuilder(X500Name issuer, Integer period, X500Name subject, SubjectPublicKeyInfo subjectPublicKeyInfo) {
        // 유효 기간, 시리얼 등 설정
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + (period * 24L * 60 * 60 * 1000));  // 기간 (일 단위)

        return new X509v3CertificateBuilder(
            issuer,
            serial,
            notBefore,
            notAfter,
            subject,
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
}
