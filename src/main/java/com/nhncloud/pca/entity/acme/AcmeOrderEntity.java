package com.nhncloud.pca.entity.acme;

import java.time.LocalDateTime;

import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.converter.OrderStatusConverter;

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
@Table(name = "ACME_ORDER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcmeOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "status", nullable = false, length = 20)
    @Convert(converter = OrderStatusConverter.class)
    private OrderStatus status;

    @Column(name = "expires")
    private LocalDateTime expires;

    @Column(name = "not_before")
    private LocalDateTime notBefore;

    @Column(name = "not_after")
    private LocalDateTime notAfter;

    @Column(name = "finalize_url", length = 512)
    private String finalizeUrl;

    @Column(name = "certificate_url", length = 512)
    private String certificateUrl;

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