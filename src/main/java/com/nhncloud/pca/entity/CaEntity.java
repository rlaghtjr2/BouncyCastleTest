package com.nhncloud.pca.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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

    @OneToOne(mappedBy = "ca", cascade = CascadeType.ALL)
    CertificateEntity certificate;

}
