package com.nhncloud.pca;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.nhncloud.pca.constant.ca.CaStatus;
import com.nhncloud.pca.constant.ca.CaType;
import com.nhncloud.pca.constant.certificate.CertificateStatus;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CertificateInfo;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.request.ca.RequestBodyForCreateCA;
import com.nhncloud.pca.model.request.ca.RequestBodyForUpdateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForCreateCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForReadCA;
import com.nhncloud.pca.model.response.ca.ResponseBodyForUpdateCA;
import com.nhncloud.pca.model.response.certificate.ResponseBodyForUpdateCert;
import com.nhncloud.pca.model.subject.SubjectInfo;

public class CommonTestUtil {
    public static final String TEST_CERTIFICATE_NAME = "TEST_CERTIFICATE_NAME";
    public static final Integer TEST_CERTIFICATE_PERIOD = 3650;
    public static final String TEST_KEY_INFO_ALGORITHM = "RSA";
    public static final Integer TEST_KEY_INFO_KEY_SIZE = 2048;
    public static final String TEST_SUBJECT_INFO_COUNTRY = "KR";
    public static final String TEST_SUBJECT_INFO_ORGANIZATION = "NHN Cloud";
    public static final String TEST_SUBJECT_INFO_ORGANIZATIONAL_UNIT = "Deployment Development";
    public static final String TEST_SUBJECT_INFO_COMMON_NAME = "TEST_SUBJECT_INFO_COMMON_NAME";
    public static final String TEST_SUBJECT_INFO_LOCALITY = "Pangyo";
    public static final String TEST_SUBJECT_INFO_STATE = "Gyeonggi-do";

    public static final Long TEST_CERTIFICATE_INFO_ID = 1L;
    public static final String TEST_CERTIFICATE_INFO_SERIAL_NUMBER = "1234567890";
    public static final String TEST_CERTIFICATE_INFO_COMMON_NAME = "example.com";
    public static final String TEST_CERTIFICATE_INFO_COUNTRY = "US";
    public static final String TEST_CERTIFICATE_INFO_LOCALITY = "San Francisco";
    public static final String TEST_CERTIFICATE_INFO_STATE_PROVINCE = "CA";
    public static final String TEST_CERTIFICATE_INFO_ORGANIZATION = "Example Inc.";
    public static final String TEST_CERTIFICATE_INFO_ISSUER = "Example CA";
    public static final LocalDateTime TEST_CERTIFICATE_INFO_NOT_BEFORE = LocalDateTime.parse("2023-10-01T00:00:00");
    public static final LocalDateTime TEST_CERTIFICATE_INFO_NOT_AFTER = LocalDateTime.parse("2024-10-01T00:00:00");
    public static final String TEST_CERTIFICATE_INFO_CERTIFICATE_PEM = "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----";
    public static final String TEST_CERTIFICATE_INFO_CHAIN_CERTIFICATE_PEM = "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----";
    public static final String TEST_CERTIFICATE_CSR = "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----";

    public static final String TEST_CERTIFICATE_INFO_PUBLIC_KEY_ALGORITHM = "RSA";
    public static final String TEST_CERTIFICATE_INFO_SIGNATURE_ALGORITHM = "SHA256withRSA";

    public static final Long TEST_CA_INFO_ID = 1L;
    public static final String TEST_CA_INFO_NAME = "Test CA";
    public static final String TEST_CA_INFO_TYPE = CaType.ROOT.getType();

    public static final CaStatus TEST_CERTIFICATE_INFO_STATUS = CaStatus.ACTIVE;
    public static final String TEST_CERTIFICATE_INFO_CREATION_DATETIME = "2023-10-01T00:00:00";

    public static final String ROOT_CA_CERT_PEM;
    public static final String ROOT_CA_KEY_PEM;

    static {
        try {
            ROOT_CA_CERT_PEM = Files.readString(Paths.get("src/test/resources/certs/rootCA.crt"));
            ROOT_CA_KEY_PEM = Files.readString(Paths.get("src/test/resources/certs/rootCA.key"));
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load root CA files: " + e.getMessage());
        }
    }

    public static RequestBodyForCreateCA createTestCertificateRequestBody() {
        RequestBodyForCreateCA requestBody = new RequestBodyForCreateCA();
        requestBody.setName(TEST_CERTIFICATE_NAME);
        requestBody.setPeriod(TEST_CERTIFICATE_PERIOD);
        requestBody.setKeyInfo(createTestKeyInfo());
        requestBody.setSubjectInfo(createTestSubjectInfo());
        return requestBody;
    }

    public static KeyInfo createTestKeyInfo() {
        KeyInfo keyInfo = KeyInfo.builder()
            .algorithm(TEST_KEY_INFO_ALGORITHM)
            .keySize(TEST_KEY_INFO_KEY_SIZE)
            .build();
        return keyInfo;
    }

    public static SubjectInfo createTestSubjectInfo() {
        SubjectInfo subjectInfo = new SubjectInfo();
        subjectInfo.setCountry(TEST_SUBJECT_INFO_COUNTRY);
        subjectInfo.setOrganization(TEST_SUBJECT_INFO_ORGANIZATION);
        subjectInfo.setOrganizationalUnit(TEST_SUBJECT_INFO_ORGANIZATIONAL_UNIT);
        subjectInfo.setCommonName(TEST_SUBJECT_INFO_COMMON_NAME);
        subjectInfo.setLocality(TEST_SUBJECT_INFO_LOCALITY);
        subjectInfo.setStateOrProvince(TEST_SUBJECT_INFO_STATE);
        return subjectInfo;
    }

    public static CertificateInfo createTestRootCaCertificateInfo() {
        CertificateInfo certificateInfo = CertificateInfo.builder()
            .certificateId(TEST_CERTIFICATE_INFO_ID)
            .serialNumber(TEST_CERTIFICATE_INFO_SERIAL_NUMBER)
            .subjectInfo(createTestSubjectInfo())
            .issuer(TEST_CERTIFICATE_INFO_ISSUER)
            .notBeforeDateTime(TEST_CERTIFICATE_INFO_NOT_BEFORE)
            .notAfterDateTime(TEST_CERTIFICATE_INFO_NOT_AFTER)
            .certificatePem(ROOT_CA_CERT_PEM)
            .chainCertificatePem(TEST_CERTIFICATE_INFO_CHAIN_CERTIFICATE_PEM)
            .privateKeyPem(ROOT_CA_KEY_PEM)
            .publicKeyAlgorithm(TEST_CERTIFICATE_INFO_PUBLIC_KEY_ALGORITHM)
            .signatureAlgorithm(TEST_CERTIFICATE_INFO_SIGNATURE_ALGORITHM)
            .status(CertificateStatus.ACTIVE)
            .build();
        return certificateInfo;
    }

    public static CaInfo createTestCaInfo_Root() {
        CaInfo caInfo = CaInfo.builder()
            .id(TEST_CA_INFO_ID)
            .name(TEST_CA_INFO_NAME)
            .type(CaType.ROOT.getType())
            .status(CaStatus.ACTIVE)
            .build();
        return caInfo;
    }

    public static ResponseBodyForCreateCA createTestCertificateResult_Root() {
        ResponseBodyForCreateCA responseBodyForCreateCA = ResponseBodyForCreateCA.builder()
            .caInfo(createTestCaInfo_Root())
            .certificateInfo(createTestRootCaCertificateInfo())
            .status(CaStatus.ACTIVE)
            .creationDatetime(LocalDateTime.now())
            .creationUser("HOSEOK")
            .build();

        return responseBodyForCreateCA;
    }

    public static CaInfo createTestCaInfo_Intermediate() {
        CaInfo caInfo = CaInfo.builder()
            .id(TEST_CA_INFO_ID)
            .name(TEST_CA_INFO_NAME)
            .type(CaType.INTERMEDIATE.getType())
            .status(CaStatus.ACTIVE)
            .build();
        return caInfo;
    }

    public static ResponseBodyForCreateCA createTestCertificateResult_Intermediate() {
        ResponseBodyForCreateCA responseBodyForCreateCA = ResponseBodyForCreateCA.builder()
            .caInfo(createTestCaInfo_Intermediate())
            .certificateInfo(createTestRootCaCertificateInfo())
            .status(CaStatus.ACTIVE)
            .creationDatetime(LocalDateTime.now())
            .creationUser("HOSEOK")
            .build();

        return responseBodyForCreateCA;
    }

    public static ResponseBodyForReadCA createTestCertificateResult_Read() {
        ResponseBodyForReadCA caCreateResult = ResponseBodyForReadCA.builder().
            caInfo(createTestCaInfo_Intermediate())
            .certificateInfo(createTestRootCaCertificateInfo())
            .status(CaStatus.ACTIVE)
            .creationDatetime(LocalDateTime.now())
            .creationUser("HOSEOK")
            .build();
        return caCreateResult;
    }

    public static CaEntity createTestCaEntity() {
        CaEntity caEntity = new CaEntity();
        caEntity.setId(TEST_CA_INFO_ID);
        caEntity.setName(TEST_CA_INFO_NAME);
        caEntity.setType(TEST_CA_INFO_TYPE);
        caEntity.setStatus(CaStatus.ACTIVE);
        caEntity.setCreationUser("HOSEOK");
        caEntity.setCreationDatetime(LocalDateTime.now());

        return caEntity;
    }

    public static CertificateEntity createTestCertificateEntity() {
        CertificateEntity certificateEntity = new CertificateEntity();
        certificateEntity.setId(TEST_CERTIFICATE_INFO_ID);
        certificateEntity.setSubject(createTestSubjectInfo().toDistinguishedName());
        certificateEntity.setCsr(TEST_CERTIFICATE_CSR);
        certificateEntity.setKeyAlgorithm(TEST_KEY_INFO_ALGORITHM);
        certificateEntity.setSigningAlgorithm(TEST_CERTIFICATE_INFO_SIGNATURE_ALGORITHM);
        certificateEntity.setNotBefore(TEST_CERTIFICATE_INFO_NOT_BEFORE.toString());
        certificateEntity.setNotAfter(TEST_CERTIFICATE_INFO_NOT_AFTER.toString());
        certificateEntity.setCertificatePem(TEST_CERTIFICATE_INFO_CERTIFICATE_PEM);
        certificateEntity.setCertificatePem(ROOT_CA_CERT_PEM);
        certificateEntity.setPrivateKeyPem(ROOT_CA_KEY_PEM);
        certificateEntity.setSignedCaId("1,2,3,4");
        certificateEntity.setStatus(CertificateStatus.ACTIVE);
        certificateEntity.setCreationUser("HOSEOK");
        certificateEntity.setCreationDatetime(LocalDateTime.now());
        return certificateEntity;
    }

    public static ResponseBodyForUpdateCA createTestCertificateResult_Update() {
        ResponseBodyForUpdateCA responseBodyForUpdateCA = ResponseBodyForUpdateCA.builder()
            .caInfo(createTestCaInfo_Intermediate())
            .build();

        return responseBodyForUpdateCA;
    }

    public static ResponseBodyForUpdateCert createTestCertificateResult_UpdateCert() {
        ResponseBodyForUpdateCert responseBodyForUpdateCert = ResponseBodyForUpdateCert.builder()
            .certificateInfo(createTestRootCaCertificateInfo())
            .build();

        return responseBodyForUpdateCert;
    }

    public static X509Certificate generateSelfSignedCertificate() {
        Security.addProvider(new BouncyCastleProvider());
        // KeyPair 생성
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // 인증서 정보
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 1000L * 60); // 1분 전
        Date notAfter = new Date(now + 1000L * 60 * 60 * 24 * 365); // 1년 후

        X500Name issuer = new X500Name("CN=Test CA, O=Test Org, C=KR");
        BigInteger serial = BigInteger.valueOf(now);

        // 인증서 빌드
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, issuer, keyPair.getPublic()
        );

        // 서명 알고리즘
        ContentSigner signer = null;
        try {
            signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }

        X509CertificateHolder certHolder = certBuilder.build(signer);

        // X509Certificate 변환
        try {
            return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static RequestBodyForUpdateCA createTestCertificateRequestBodyForUpdate() {
        RequestBodyForUpdateCA requestBody = new RequestBodyForUpdateCA();
        requestBody.setStatus(CaStatus.DISABLED);
        return requestBody;
    }
}
