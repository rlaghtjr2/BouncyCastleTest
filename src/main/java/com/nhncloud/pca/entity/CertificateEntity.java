package com.nhncloud.pca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.nhncloud.pca.constant.CaStatus;

@Entity
@Getter
@Setter
@Table(name = "PCA_CERTIFICATE")
public class CertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long certificateId;

    @OneToOne
    @JoinColumn(name = "ca_id", nullable = true)
    CaEntity ca;

    @Enumerated(EnumType.STRING)
    CaStatus status;

    @ManyToOne
    @JoinColumn(name = "signed_ca_id")
    CaEntity signedCa;

    @Column
    String certificatePem;

    @Column
    String privateKeyPem;
}
