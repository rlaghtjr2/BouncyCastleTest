package com.nhncloud.pca.entity.acme;

import java.time.LocalDateTime;
import java.util.List;

import com.nhncloud.pca.constant.acme.AccountStatus;
import com.nhncloud.pca.converter.AccountStatusConverter;
import com.nhncloud.pca.converter.ContactInfoConverter;
import com.nhncloud.pca.converter.ExternalAccountBindingConverter;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.model.acme.ExternalAccountBinding;

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
@Table(name = "ACME_ACCOUNT")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcmeAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "ca_id", nullable = false)
    private CaEntity ca;

    @Column(name = "status", nullable = false, length = 20)
    @Convert(converter = AccountStatusConverter.class)
    private AccountStatus status;

    @Column(name = "contact", columnDefinition = "TEXT")
    @Convert(converter = ContactInfoConverter.class)
    private List<String> contact;

    @Column(name = "terms_of_service_agreed")
    @Builder.Default
    private Boolean termsOfServiceAgreed = false;

    @Column(name = "external_account_binding", columnDefinition = "JSON")
    @Convert(converter = ExternalAccountBindingConverter.class)
    private ExternalAccountBinding externalAccountBinding;

    @Column(name = "private_key", nullable = false, columnDefinition = "TEXT")
    private String privateKey;

    // 1:N 관계 - 하나의 Account는 여러 Order를 가질 수 있음
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcmeOrderEntity> orders;

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