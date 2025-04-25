package com.nhncloud.pca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "PCA_CERTIFICATE")
public class CertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long certificateId;

    @OneToOne
    @JoinColumn(name = "ca_id")
    CaEntity ca;

    Long signedCaId;

    @Column
    String certificatePem;

    @Column
    String privateKeyPem;
}
