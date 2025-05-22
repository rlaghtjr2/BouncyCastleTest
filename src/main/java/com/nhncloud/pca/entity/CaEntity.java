package com.nhncloud.pca.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.ca.CaStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "PCA_CA")
public class CaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    @JoinColumn(name = "signed_ca_id", nullable = true)
    CaEntity signedCa;

    @Column
    String name;

    @Column
    String type;

    @Enumerated(EnumType.STRING)
    CaStatus status;

    @Column
    String creationUser;

    @Column
    LocalDateTime creationDatetime;

    @Column
    String lastChangeUser;

    @Column
    LocalDateTime lastChangeDatetime;

    @OneToOne(mappedBy = "ca", cascade = CascadeType.ALL)
    CertificateEntity certificate;

    public CaEntity(Long id) {
        this.id = id;
    }
}
