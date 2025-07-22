package com.nhncloud.pca.entity.acme;

import java.time.LocalDateTime;
import java.util.List;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.converter.AuthorizationStatusConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

    @OneToOne
    @JoinColumn(name = "identifier_id", nullable = false)
    private AcmeIdentifierEntity identifier;

    @Column(name = "status", nullable = false, length = 64)
    @Convert(converter = AuthorizationStatusConverter.class)
    private AuthorizationStatus status;

    @Column(name = "expires")
    private LocalDateTime expires;

    @Column(name = "wildcard")
    @Builder.Default
    private Boolean wildcard = false;

    // 1:N 관계 - 하나의 Authorization은 여러 Challenge를 가질 수 있음
    @OneToMany(mappedBy = "authorization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcmeChallengeEntity> challenges;

    @Column(name = "creation_datetime", nullable = false)
    private LocalDateTime creationDatetime;

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