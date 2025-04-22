package com.nhncloud.pca.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;

import com.nhncloud.pca.constant.CaStatus;
import com.nhncloud.pca.constant.CaType;
import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CaCertificateInfo;
import com.nhncloud.pca.model.certificate.CertificateExtension;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.request.RequestBodyForCreateRootCA;
import com.nhncloud.pca.model.response.CertificateResult;
import com.nhncloud.pca.model.subject.SubjectInfo;

@Service
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    public CertificateServiceImpl() {
        initializeBouncyCastle();
    }

    public void initializeBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }


    @Override
    public CertificateResult generateRootCertificate(RequestBodyForCreateRootCA requestBody) {
        log.info("generateRootCertificate() = {}", requestBody);

        KeyInfo keyInfo = requestBody.getKeyInfo();
        SubjectInfo subjectInfo = requestBody.getSubjectInfo();

        KeyPair keyPair;

        try {
            keyPair = generateKeyPair(keyInfo);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Wrong Algorithm");
        } catch (NoSuchProviderException e) {
            //BC를 넣고있기때문에 발생하지 않을것같음..
            throw new RuntimeException("No Such Provider");
        }

        X500Name issuer = new X500Name(subjectInfo.toDistinguishedName());

        // 유효 기간 설정
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + (requestBody.getPeriod() * 24 * 60 * 60 * 1000));  //유효 기간 (일)

        // 시리얼 넘버
        //Rand? 어떻게 해야하지 ?
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        // X.509 인증서 빌더
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, issuer, keyPair.getPublic()
        );

        // 확장 필드 추가
        try {
            // CA: true
            CertificateExtension caExtension = new CertificateExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            CertificateExtension keyUsageExtension = new CertificateExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.keyCertSign | KeyUsage.cRLSign
            ));
            CertificateExtension subjectKeyIdentifier = new CertificateExtension(Extension.subjectKeyIdentifier, false,
                new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));
            List<CertificateExtension> extensions = List.of(caExtension, keyUsageExtension, subjectKeyIdentifier);

            setCertificateExtensions(certBuilder, extensions);

        } catch (NoSuchAlgorithmException e) {
            // Default로 SHA1을 넣고 있어서 발생하진 않을것같음..
            throw new RuntimeException("new JcaX509ExtensionUtils() = [No Such Algorithm]");
        }

        // 인증서 Signer 생성 (SelfSign)
        ContentSigner signer = null;
        try {
            signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }

        // 인증서 생성
        X509CertificateHolder holder = certBuilder.build(signer);
        X509Certificate certificate = null;
        try {
            certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        } catch (CertificateException e) {
            throw new RuntimeException("new JcaX509CertificateConverter() = [CertificateException]");
        }

        // 인증서와 키를 PEM 문자열로 변환
        StringWriter certWriter = new StringWriter();
        StringWriter keyWriter = new StringWriter();

        try (JcaPEMWriter pemCertWriter = new JcaPEMWriter(certWriter);
             JcaPEMWriter pemKeyWriter = new JcaPEMWriter(keyWriter)) {

            pemCertWriter.writeObject(holder);
            pemCertWriter.flush();

            pemKeyWriter.writeObject(keyPair.getPrivate());
            pemKeyWriter.flush();

        } catch (IOException e) {
            throw new RuntimeException("PEM 변환 중 IO 예외", e);
        }

        //결과값 세팅
        CaInfo caInfo = CaInfo.builder()
            .name(requestBody.getName())
            .caId(1234L)
            .caType(CaType.ROOT.getType())
            .build();

        CaCertificateInfo caCertificateInfo = CaCertificateInfo.builder()
            .caCertificateId(1234L)
            .serialNumber(certificate.getSerialNumber().toString())
            .commonName(subjectInfo.getCommonName())
            .country(subjectInfo.getCountry())
            .locality(subjectInfo.getLocality())
            .stateProvince(subjectInfo.getStateOrProvince())
            .organization(subjectInfo.getOrganization())
            .issuer(certificate.getIssuerX500Principal().getName())
            .notBeforeDateTime(certificate.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
            .notAfterDateTime(certificate.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
            .certificatePem(certWriter.toString())
            .chainCertificatePem(certWriter.toString())
            .publicKeyAlgorithm(keyInfo.getAlgorithm())
            .signatureAlgorithm(certificate.getSigAlgName())
            .build();

        CertificateResult result = CertificateResult.builder()
            .caInfo(caInfo)
            .caCertificateInfo(caCertificateInfo)
            .status(CaStatus.ACTIVE.getStatus())
            .creationDatetime(LocalDateTime.now())
            .creationUser("hoseok")
            .build();

        return result;
    }

    private void setCertificateExtensions(JcaX509v3CertificateBuilder certBuilder, List<CertificateExtension> extensions) {
        for (CertificateExtension extension : extensions) {
            try {
                certBuilder.addExtension(extension.getName(), extension.isCritical(), extension.getValue());
            } catch (CertIOException e) {
                throw new RuntimeException("setCertificateExtensions() = [CertIOException]");
            }
        }
    }

    private KeyPair generateKeyPair(KeyInfo keyInfo) throws NoSuchAlgorithmException, NoSuchProviderException {
        // Algorithm과 provider BC(Bouncy Castle) 지정
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyInfo.getAlgorithm(), "BC");
        // 키 사이즈 지정
        keyGen.initialize(keyInfo.getKeySize());
        return keyGen.generateKeyPair();
    }
}
