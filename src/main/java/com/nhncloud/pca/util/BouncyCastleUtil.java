package com.nhncloud.pca.util;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import com.nhncloud.pca.model.certificate.CertificateExtension;
import com.nhncloud.pca.model.subject.SubjectInfo;

public class BouncyCastleUtil {
    public static void setCertificateExtensions(X509v3CertificateBuilder certBuilder, List<CertificateExtension> extensions) {
        for (CertificateExtension extension : extensions) {
            try {
                certBuilder.addExtension(extension.getName(), extension.isCritical(), extension.getValue());
            } catch (CertIOException e) {
                throw new RuntimeException("setCertificateExtensions() = [CertIOException]");
            }
        }
    }

    public static X509Certificate parseCertificate(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj instanceof X509CertificateHolder) {
                return new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate((X509CertificateHolder) obj);
            } else {
                throw new IllegalArgumentException("Invalid PEM format");
            }
        } catch (IOException e) {
            throw new RuntimeException("new PEMParser() = [IOException]");
        } catch (CertificateException e) {
            throw new RuntimeException("new JcaX509CertificateConverter() = [CertificateException]");
        }
    }

    public static PKCS10CertificationRequest parseCsr(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj instanceof PKCS10CertificationRequest) {
                return (PKCS10CertificationRequest) obj;
            } else {
                throw new IllegalArgumentException("CSR 형식이 아님");
            }
        } catch (IOException e) {
            throw new RuntimeException("new PEMParser() = [IOException]");
        }
    }

    public static SubjectInfo parseDnWithBouncyCastle(String dn) {
        X500Name x500Name = new X500Name(dn);
        SubjectInfo info = new SubjectInfo();

        info.setCommonName(getValue(x500Name, BCStyle.CN));
        info.setCountry(getValue(x500Name, BCStyle.C));
        info.setStateOrProvince(getValue(x500Name, BCStyle.ST));
        info.setLocality(getValue(x500Name, BCStyle.L));
        info.setOrganizationalUnit(getValue(x500Name, BCStyle.OU));
        info.setOrganization(getValue(x500Name, BCStyle.O));
        info.setEmailAddress(getValue(x500Name, BCStyle.EmailAddress));

        return info;
    }

    private static String getValue(X500Name x500Name, org.bouncycastle.asn1.ASN1ObjectIdentifier identifier) {
        RDN[] rdns = x500Name.getRDNs(identifier);
        if (rdns != null && rdns.length > 0 && rdns[0].getFirst() != null) {
            return rdns[0].getFirst().getValue().toString();
        }
        return null;
    }

    public static X509v3CertificateBuilder getCertificateBuilder(X500Name issuerName, X500Name subjectName, Integer period, SubjectPublicKeyInfo subjectPublicKeyInfo) {
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

    public static X509Certificate getX509Certificate(X509v3CertificateBuilder certBuilder, PrivateKey privateKey) {
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

    public static GeneralNames createSubjectAltNames(List<String> altNames, List<String> ips) {
        List<GeneralName> generalNames = new ArrayList<>();
        altNames.stream().forEach(altName -> {
            generalNames.add(new GeneralName(GeneralName.dNSName, altName));
        });
        ips.stream().forEach(ip -> {
            generalNames.add(new GeneralName(GeneralName.iPAddress, ip));
        });
        
        return new GeneralNames(generalNames.toArray(new GeneralName[0]));
    }
}
