package com.nhncloud.pca;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.CaType;
import com.nhncloud.pca.model.ca.CaInfo;
import com.nhncloud.pca.model.certificate.CaCertificateInfo;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.request.RequestBodyForCreateRootCA;
import com.nhncloud.pca.model.response.CertificateResult;
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
    public static final String TEST_CERTIFICATE_INFO_PUBLIC_KEY_ALGORITHM = "RSA";
    public static final String TEST_CERTIFICATE_INFO_SIGNATURE_ALGORITHM = "SHA256withRSA";

    public static final Long TEST_CA_INFO_ID = 1L;
    public static final String TEST_CA_INFO_NAME = "Test CA";
    public static final String TEST_CA_INFO_TYPE = CaType.ROOT.getType();

    public static final String TEST_CERTIFICATE_INFO_STATUS = "ACTIVE";
    public static final String TEST_CERTIFICATE_INFO_CREATION_DATETIME = "2023-10-01T00:00:00";

    public static RequestBodyForCreateRootCA createTestCertificateRequestBody() {
        RequestBodyForCreateRootCA requestBody = new RequestBodyForCreateRootCA();
        requestBody.setName(TEST_CERTIFICATE_NAME);
        requestBody.setPeriod(TEST_CERTIFICATE_PERIOD);
        requestBody.setKeyInfo(createTestKeyInfo());
        requestBody.setSubjectInfo(createTestSubjectInfo());
        return requestBody;
    }

    public static KeyInfo createTestKeyInfo() {
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.setAlgorithm(TEST_KEY_INFO_ALGORITHM);
        keyInfo.setKeySize(TEST_KEY_INFO_KEY_SIZE);
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

    public static CaCertificateInfo createTestCaCertificateInfo() {
        CaCertificateInfo caCertificateInfo = CaCertificateInfo.builder().
            caCertificateId(TEST_CERTIFICATE_INFO_ID).serialNumber(TEST_CERTIFICATE_INFO_SERIAL_NUMBER).commonName(TEST_CERTIFICATE_INFO_COMMON_NAME).
            country(TEST_CERTIFICATE_INFO_COUNTRY).locality(TEST_CERTIFICATE_INFO_LOCALITY).stateProvince(TEST_CERTIFICATE_INFO_STATE_PROVINCE).
            organization(TEST_CERTIFICATE_INFO_ORGANIZATION).issuer(TEST_CERTIFICATE_INFO_ISSUER).notBeforeDateTime(TEST_CERTIFICATE_INFO_NOT_BEFORE).
            notAfterDateTime(TEST_CERTIFICATE_INFO_NOT_AFTER).certificatePem(TEST_CERTIFICATE_INFO_CERTIFICATE_PEM).
            chainCertificatePem(TEST_CERTIFICATE_INFO_CHAIN_CERTIFICATE_PEM).publicKeyAlgorithm(TEST_CERTIFICATE_INFO_PUBLIC_KEY_ALGORITHM).
            signatureAlgorithm(TEST_CERTIFICATE_INFO_SIGNATURE_ALGORITHM).build();
        return caCertificateInfo;
    }

    public static CaInfo createTestCaInfo() {
        CaInfo caInfo = CaInfo.builder().
            caId(TEST_CA_INFO_ID).name(TEST_CA_INFO_NAME).caType(TEST_CA_INFO_TYPE).build();
        return caInfo;
    }

    public static CertificateResult createTestCertificateResult() {
        CertificateResult certificateResult = CertificateResult.builder().
            caInfo(createTestCaInfo()).caCertificateInfo(createTestCaCertificateInfo()).status(TEST_CERTIFICATE_INFO_STATUS).
            creationDatetime(LocalDateTime.parse(TEST_CERTIFICATE_INFO_CREATION_DATETIME)).creationUser("testUser").
            lastChangeDatetime(LocalDateTime.parse(TEST_CERTIFICATE_INFO_CREATION_DATETIME)).lastChangeUser("testUser").build();
        return certificateResult;
    }
}
