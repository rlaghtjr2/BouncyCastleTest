package com.example.vault.demo;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Date;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

public class BouncyCastleService {

    public void generateCsr() throws NoSuchAlgorithmException, NoSuchProviderException, IOException, OperatorCreationException {
        // Bouncy Castle Provider 등록
        Security.addProvider(new BouncyCastleProvider());

        // RSA KeyPair 생성
        // Algorithm과 provider BC(Bouncy Castle) 지정
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        // 키 사이즈 지정
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // CSR 주체 정보 입력
        X500Name subject = new X500Name(
            "CN=hoseok.com, O=Hoseok Cloud, OU=Deployment Develop Department, C=KR, ST=Gyeonggi-do, L=Pangyo, E=hoseok.kim@abcd.com"
        );

        // 확장 필드 설정
        ExtensionsGenerator extGen = new ExtensionsGenerator();

        // Basic Constraints (CA)
        extGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

        // Key Usage
        extGen.addExtension(Extension.keyUsage, true, new KeyUsage(
            KeyUsage.cRLSign | KeyUsage.keyCertSign));
        // Subject Key Identifier
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        extGen.addExtension(Extension.subjectKeyIdentifier, false,
            extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));

        Extensions extensions = extGen.generate();

        // Attribute로 X.509 v3 extensions 추가
        ASN1EncodableVector attr = new ASN1EncodableVector();
        attr.add(new Attribute(
            PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
            new DERSet(extensions)));

        // CSR Builder (JcaPKCS10CertificationRequestBuilder 사용)
        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        builder.setAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);

        // 서명자
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(keyPair.getPrivate());

        // CSR 생성
        PKCS10CertificationRequest csr = builder.build(signer);

        // PEM으로 저장
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter("csr_with_extensions.csr"))) {
            writer.writeObject(csr);
            System.out.println("확장 필드 포함 CSR 생성 완료!");
        }
    }

    public void generateRootCA () throws NoSuchAlgorithmException, NoSuchProviderException, CertIOException, OperatorCreationException {
        Security.addProvider(new BouncyCastleProvider());

        // RSA KeyPair 생성
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(4096);
        KeyPair keyPair = keyGen.generateKeyPair();

        // CA 주체 이름
        X500Name issuer = new X500Name("CN=HoseokRootCA, O=Hoseok Cloud, C=KR, OU=Deployment Develop Department, ST=Gyeonggi-do, L=Pangyo, E=hoseok.kim@abcd.com");

        // 유효 기간 설정
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L); // 하루 전부터
        Date notAfter = new Date(System.currentTimeMillis() + (3650L * 24 * 60 * 60 * 1000)); // 10년

        // 시리얼 넘버
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        // X.509 인증서 빌더
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, issuer, keyPair.getPublic()
        );

        // 확장 필드 추가
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true)); // CA: true
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(
            KeyUsage.keyCertSign | KeyUsage.cRLSign
        ));

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
            extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));


        // 서명자
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(keyPair.getPrivate());

        // 인증서 생성
        X509CertificateHolder holder = certBuilder.build(signer);

        // PEM 파일로 저장
        try (JcaPEMWriter certWriter = new JcaPEMWriter(new FileWriter("rootCA.crt"))) {
            certWriter.writeObject(holder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (JcaPEMWriter keyWriter = new JcaPEMWriter(new FileWriter("rootCA.key"))) {
            keyWriter.writeObject(keyPair.getPrivate());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Root CA 인증서 및 키 생성 완료");
    }

    public void getBouncyCastleProviderAlgorithm(){
        // Provider 등록
        Security.addProvider(new BouncyCastleProvider());

        System.out.println("=====================");
        Provider bcProvider = Security.getProvider("BC");
        if (bcProvider != null) {
            bcProvider.keySet().stream()
                .filter(k -> k.toString().startsWith("KeyPairGenerator"))
                .forEach(System.out::println);
        } else {
            System.out.println("BC Provider not found!");
        }
    }
}
