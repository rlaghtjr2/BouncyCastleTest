package com.nhncloud.pca.entity.acme;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.acme.ChallengeStatus;
import com.nhncloud.pca.constant.acme.ChallengeType;
import com.nhncloud.pca.converter.ChallengeStatusConverter;
import com.nhncloud.pca.converter.ChallengeTypeConverter;
import com.nhncloud.pca.converter.ProblemConverter;
import com.nhncloud.pca.model.acme.Problem;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ACME_CHALLENGE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcmeChallengeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "authorization_id", nullable = false)
    private AcmeAuthorizationEntity authorization;

    @Column(name = "type", nullable = false, length = 20)
    @Convert(converter = ChallengeTypeConverter.class)
    private ChallengeType type;

    @Column(name = "status", nullable = false, length = 20)
    @Convert(converter = ChallengeStatusConverter.class)
    private ChallengeStatus status;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Column(name = "validated")
    private LocalDateTime validated;

    @Column(name = "error", columnDefinition = "JSON")
    @Convert(converter = ProblemConverter.class)
    private Problem error;

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