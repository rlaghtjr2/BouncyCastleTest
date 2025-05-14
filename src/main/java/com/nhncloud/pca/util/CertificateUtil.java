package com.nhncloud.pca.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import com.nhncloud.pca.model.certificate.CertificateExtension;
import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.subject.SubjectInfo;

public class CertificateUtil {
    public static String toPemString(Object pemObject) {
        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(pemObject);
        } catch (IOException e) {
            throw new UncheckedIOException("PEM 변환 실패", e);
        }
        return stringWriter.toString();
    }

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

    public static PrivateKey parsePrivateKey(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (obj instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) obj).getPrivate();
            } else if (obj instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) obj);
            } else {
                throw new IllegalArgumentException("지원하지 않는 Private Key 형식");
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

    public static String formatSerialNumber(byte[] bytes) {
        // 양수 보장을 위한 leading zero 제거
        if (bytes.length > 1 && bytes[0] == 0x00) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public static KeyInfo getKeyInfo(X509Certificate certificate) {
        String algorithm = certificate.getPublicKey().getAlgorithm();
        RSAPublicKey rsaKey = (RSAPublicKey) certificate.getPublicKey();
        int keySize = rsaKey.getModulus().bitLength();
        KeyInfo keyInfo = KeyInfo.builder()
            .algorithm(algorithm)
            .keySize(keySize)
            .build();

        return keyInfo;
    }
}
