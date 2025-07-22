package com.nhncloud.pca.entity.acme;

import java.time.LocalDateTime;
import java.util.List;

import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.converter.OrderStatusConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

    // Account와 N:1 관계 - 하나의 Account는 여러 Order를 가질 수 있음
    @ManyToOne
    @JoinColumn(name = "acme_account_id", nullable = false)
    private AcmeAccountEntity account;

    @Column(name = "certificate_id", length = 255)
    private String certificateId;

    @Column(name = "status", nullable = false, length = 64)
    @Convert(converter = OrderStatusConverter.class)
    private OrderStatus status;

    @Column(name = "expires")
    private LocalDateTime expires;

    @Column(name = "not_before")
    private LocalDateTime notBefore;

    @Column(name = "not_after")
    private LocalDateTime notAfter;

    // 1:N 관계 - 하나의 Order는 여러 Identifier를 가질 수 있음
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcmeIdentifierEntity> identifiers;

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