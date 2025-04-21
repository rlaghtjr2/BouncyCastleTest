package com.example.vault.demo;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class BouncyCastleService {

    public void generateCsr() throws NoSuchAlgorithmException, NoSuchProviderException, IOException, OperatorCreationException {
        // Bouncy Castle Provider 등록
        Security.addProvider(new BouncyCastleProvider());

        // RSA KeyPair 생성
        KeyPair keyPair = generateKeyPair("RSA", 2048);

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

    public void generateRootCA() throws NoSuchAlgorithmException, NoSuchProviderException, CertIOException, OperatorCreationException {
        Security.addProvider(new BouncyCastleProvider());

        // RSA KeyPair 생성
        KeyPair keyPair = generateKeyPair("RSA", 4096);

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

    public void generateIntermediateCACsr() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException {
        Security.addProvider(new BouncyCastleProvider());
        // Intermediate용 KeyPair 생성
        KeyPair intermediateKeyPair = generateKeyPair("RSA", 4096);

        // 개인키 PEM 저장
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter("intermediateCA.key"))) {
            writer.writeObject(intermediateKeyPair.getPrivate());
            System.out.println("Intermediate Private Key 저장 완료");
        }

        // Subject 이름 설정
        X500Name subject = new X500Name(
            "CN=HoseokIntermediateCA, O=Hoseok Cloud, OU=Deployment Develop Department, C=KR, ST=Gyeonggi-do, L=Pangyo, E=hoseok.kim@abcd.com"
        );

        // 확장 생성
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));  // CA: true
        extGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        Extensions extensions = extGen.generate();

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(intermediateKeyPair.getPublic().getEncoded());
        // CSR 빌더
        PKCS10CertificationRequestBuilder csrBuilder =
            new PKCS10CertificationRequestBuilder(subject, subjectPublicKeyInfo);

        csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(intermediateKeyPair.getPrivate());

        PKCS10CertificationRequest csr = csrBuilder.build(signer);

        // PEM 파일로 저장
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter("intermediate.csr"))) {
            writer.writeObject(csr);
        }

        System.out.println("Intermediate CSR 파일 생성 완료");
    }

    public void getBouncyCastleProviderAlgorithm() {
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

    public void signIntermediateCACsr() throws IOException, CertificateException, NoSuchAlgorithmException, OperatorCreationException {
        Security.addProvider(new BouncyCastleProvider());

        // Root 개인키 로드
        PrivateKey rootPrivateKey = getRootCaKey("rootCA.key");

        // Root 인증서 로드
        X509Certificate rootCertificate = getRootCACertificate("rootCA.crt");

        // CSR 로드
        PKCS10CertificationRequest csr;
        try (PEMParser pemParser = new PEMParser(new FileReader("intermediate.csr"))) {
            csr = (PKCS10CertificationRequest) pemParser.readObject();
        }

        // 발급 기간 및 시리얼 설정
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60);
        Date notAfter = new Date(System.currentTimeMillis() + (10L * 365 * 24 * 60 * 60 * 1000));  // 10년

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
            new X500Name(rootCertificate.getSubjectX500Principal().getName()),
            serial,
            notBefore,
            notAfter,
            csr.getSubject(),
            csr.getSubjectPublicKeyInfo()
        );

        // 확장 필드 설정
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));  // CA: true
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
            extUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));
        certBuilder.addExtension(Extension.authorityKeyIdentifier, false,
            extUtils.createAuthorityKeyIdentifier(rootCertificate.getPublicKey()));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(rootPrivateKey);

        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

        // PEM 저장
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter("intermediateCA.crt"))) {
            writer.writeObject(issuedCert);
        }

        System.out.println("Intermediate 인증서 발급 완료");
    }

    public void generateLeafCertificateCSR(String name) throws NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        KeyPair serverKeyPair = generateKeyPair("RSA", 2048);

        // 서버용 CSR 생성
        X500Name serverSubject = new X500Name("CN=localhost, O=Hoseok Cloud, C=KR");
        SubjectPublicKeyInfo serverPublicKeyInfo = SubjectPublicKeyInfo.getInstance(serverKeyPair.getPublic().getEncoded());
        PKCS10CertificationRequestBuilder serverCSRBuilder = new PKCS10CertificationRequestBuilder(serverSubject, serverPublicKeyInfo);

        ContentSigner serverSigner = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(serverKeyPair.getPrivate());
        PKCS10CertificationRequest serverCSR = serverCSRBuilder.build(serverSigner);

        // 키 파일 저장
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(name + ".key"))) {
            writer.writeObject(serverKeyPair.getPrivate());
        }

        // CSR 파일 저장
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(name + ".csr"))) {
            writer.writeObject(serverCSR);
        }

        System.out.println("서버CSR + Key 생성 완료");

    }

    public void signLeafCertificate(String name) throws NoSuchAlgorithmException, IOException, CertificateException, OperatorCreationException {
        Security.addProvider(new BouncyCastleProvider());


        // Intermediate CA 인증서와 PrivateKey 읽기
        PEMParser pemParser = new PEMParser(new FileReader("intermediateCA.crt"));
        X509CertificateHolder caCertHolder = (X509CertificateHolder) pemParser.readObject();
        pemParser.close();

        pemParser = new PEMParser(new FileReader("intermediateCA.key"));
        Object keyObject = pemParser.readObject();
        pemParser.close();

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

        PrivateKey caPrivateKey;

        if (keyObject instanceof PEMKeyPair) {
            // KeyPair 형태일 경우
            PEMKeyPair pemKeyPair = (PEMKeyPair) keyObject;
            KeyPair kp = converter.getKeyPair(pemKeyPair);
            caPrivateKey = kp.getPrivate();
        } else if (keyObject instanceof PrivateKeyInfo) {
            // PKCS#8 형태일 경우
            caPrivateKey = converter.getPrivateKey((PrivateKeyInfo) keyObject);
        } else {
            throw new IllegalArgumentException("지원하지 않는 키 형식입니다: " + keyObject.getClass());
        }

        // CSR 읽기
        pemParser = new PEMParser(new FileReader(name + ".csr"));
        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) pemParser.readObject();
        pemParser.close();

        // 기간 설정
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + (365L * 24 * 60 * 60 * 1000)); // 1년

        // 일련번호
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        // 인증서 생성
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            caCertHolder.getSubject(),
            serial,
            notBefore,
            notAfter,
            csr.getSubject(),
            csr.getSubjectPublicKeyInfo()
        );

        // 확장 필드 추가
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true,
            new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.keyAgreement));
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
            extUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));

        AuthorityKeyIdentifier authorityKeyIdentifier = new AuthorityKeyIdentifier(caCertHolder.getSubjectPublicKeyInfo());

        certBuilder.addExtension(Extension.authorityKeyIdentifier, false,
            authorityKeyIdentifier);

        certBuilder.addExtension(Extension.extendedKeyUsage, false,
            new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));

        // 서명
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(caPrivateKey);
        X509CertificateHolder certHolder = certBuilder.build(signer);

        // X509Certificate 변환
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

        // PEM 파일로 저장
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(name + ".crt"))) {
            writer.writeObject(certificate);
        }
    }

    public void generateChainCertificate() throws IOException, CertificateException {

        String serverCertPath = "server.crt";
        String intermediateCertPath = "intermediateCA.crt";
        String rootCertPath = "rootCA.crt";
        String outputPath = "fullchain.crt";

        List<String> certPaths = List.of(serverCertPath, intermediateCertPath, rootCertPath);
        List<X509Certificate> certificates = new ArrayList<>();

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

        for (String path : certPaths) {
            try (InputStream in = new FileInputStream(path)) {
                certificates.add((X509Certificate) certFactory.generateCertificate(in));
            }
        }

        try (PemWriter writer = new PemWriter(new FileWriter(outputPath))) {
            for (X509Certificate cert : certificates) {
                PemObject pemObject = new PemObject("CERTIFICATE", cert.getEncoded());
                writer.writeObject(pemObject);
            }
            System.out.println("PEM 체인 파일 생성 완료: " + outputPath);
        }

    }

    private PrivateKey getRootCaKey(String keyPath) throws IOException {
        PrivateKey rootPrivateKey;

        try (PEMParser pemParser = new PEMParser(new FileReader(keyPath))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (object instanceof PEMKeyPair) {
                // KeyPair 형태로 들어온 경우
                rootPrivateKey = converter.getKeyPair((PEMKeyPair) object).getPrivate();
            } else if (object instanceof PrivateKeyInfo) {
                // PKCS#8 형태로 들어온 경우
                rootPrivateKey = converter.getPrivateKey((PrivateKeyInfo) object);
            } else {
                throw new IllegalArgumentException("지원하지 않는 키 형식입니다: " + object.getClass());
            }
        }

        return rootPrivateKey;
    }

    private X509Certificate getRootCACertificate(String path) throws IOException, CertificateException {
        X509Certificate rootCertificate;

        try (PEMParser pemParser = new PEMParser(new FileReader("rootCA.crt"))) {
            Object object = pemParser.readObject();
            rootCertificate = new JcaX509CertificateConverter().setProvider("BC")
                .getCertificate((X509CertificateHolder) object);
        }

        return rootCertificate;
    }

    private KeyPair generateKeyPair(String algorithm, int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        // Algorithm과 provider BC(Bouncy Castle) 지정
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm, "BC");
        // 키 사이즈 지정
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }
}
