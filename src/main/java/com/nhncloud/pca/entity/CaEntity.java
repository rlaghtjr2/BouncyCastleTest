package com.nhncloud.pca.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import com.nhncloud.pca.constant.CaStatus;

@Entity
@Getter
@Setter
@Table(name = "PCA_CA")
public class CaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long caId;

    @Column
    String name;

    @Column
    String type;

    @Enumerated(EnumType.STRING)
    CaStatus status;

    @OneToMany(mappedBy = "signedCa", cascade = CascadeType.ALL)
    List<CertificateEntity> signedCertificates;

    @OneToOne(mappedBy = "ca", cascade = CascadeType.ALL)
    CertificateEntity certificate;
}
