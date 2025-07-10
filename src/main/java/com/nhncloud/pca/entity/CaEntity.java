package com.nhncloud.pca.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.nhncloud.pca.constant.ca.CaStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "CA")
public class CaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "toast_project_id", nullable = false)
    Long toastProjectId;

    @Column(name = "name", nullable = false, length = 256)
    String name;

    @Column(name = "status", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    CaStatus status;

    @Column(name = "deletion_datetime")
    LocalDateTime deletionDatetime;

    @Column(name = "creation_user", nullable = false, length = 64)
    String creationUser;

    @Column(name = "creation_datetime", nullable = false)
    LocalDateTime creationDatetime;

    @Column(name = "last_change_user", length = 64)
    String lastChangeUser;

    @Column(name = "last_change_datetime")
    LocalDateTime lastChangeDatetime;

    @OneToMany(mappedBy = "ca", cascade = CascadeType.ALL)
    List<CertificateEntity> certificates;

    public CaEntity(Long id) {
        this.id = id;
    }

    public CaEntity(Long id, Long toastProjectId) {
        this.id = id;
        this.toastProjectId = toastProjectId;
    }
}
