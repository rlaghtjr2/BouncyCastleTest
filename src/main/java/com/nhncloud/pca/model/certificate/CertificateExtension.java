package com.nhncloud.pca.model.certificate;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;

@Data
@AllArgsConstructor
public class CertificateExtension {
    private ASN1ObjectIdentifier name;
    private boolean isCritical;
    private ASN1Encodable value;

}
