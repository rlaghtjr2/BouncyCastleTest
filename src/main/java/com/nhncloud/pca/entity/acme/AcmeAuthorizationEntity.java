package com.nhncloud.pca.entity.acme;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.converter.AuthorizationStatusConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ACME_AUTHORIZATION")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcmeAuthorizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "identifier_id", nullable = false)
    private Long identifierId;

    @Column(name = "status", nullable = false, length = 20)
    @Convert(converter = AuthorizationStatusConverter.class)
    private AuthorizationStatus status;

    @Column(name = "expires")
    private LocalDateTime expires;

    @Column(name = "wildcard")
    private Boolean wildcard = false;

    @Column(name = "creation_user", nullable = false, length = 100)
    private String creationUser;

    @Column(name = "creation_datetime", nullable = false)
    private LocalDateTime creationDatetime;

    @Column(name = "last_change_user", nullable = false, length = 100)
    private String lastChangeUser;

    @Column(name = "last_change_datetime", nullable = false)
    private LocalDateTime lastChangeDatetime;

    @PrePersist
    protected void onCreate() {
        creationDatetime = LocalDateTime.now();
        lastChangeDatetime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastChangeDatetime = LocalDateTime.now();
    }
}