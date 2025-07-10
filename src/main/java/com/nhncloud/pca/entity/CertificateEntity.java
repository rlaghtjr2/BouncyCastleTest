package com.nhncloud.pca.entity;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.certificate.CertificateStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "CERTIFICATE")
public class CertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "ca_id", nullable = true)
    CaEntity ca;

    @Column
    String csr;

    @Column
    String subject;

    @Column
    String keyAlgorithm;

    @Column
    String signingAlgorithm;

    @Column
    String certificatePem;

    @Column
    String privateKey;

    @Column
    String notBefore;

    @Column
    String notAfter;

    @Column
    String signedCertificateId;

    @Enumerated(EnumType.STRING)
    CertificateStatus status;

    @Column
    LocalDateTime deletionDatetime;

    @Column
    String creationUser;

    @Column
    LocalDateTime creationDatetime;

    @Column
    String lastChangeUser;

    @Column
    LocalDateTime lastChangeDatetime;
}



