package com.nhncloud.pca.repository.acme;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhncloud.pca.constant.acme.AccountStatus;
import com.nhncloud.pca.entity.acme.AcmeAccountEntity;

@Repository
public interface AcmeAccountRepository extends JpaRepository<AcmeAccountEntity, Long> {

    Optional<AcmeAccountEntity> findByIdAndStatus(Long id, AccountStatus status);

    List<AcmeAccountEntity> findByStatus(AccountStatus status);

    // JSON 필드 내부 값으로 검색 (MySQL의 경우)
    @Query("SELECT a FROM AcmeAccountEntity a WHERE JSON_EXTRACT(a.contact, '$[*]') LIKE %:email%")
    List<AcmeAccountEntity> findByContactEmail(@Param("email") String email);

    // 특정 연락처 타입으로 검색
    @Query("SELECT a FROM AcmeAccountEntity a WHERE JSON_EXTRACT(a.contact, '$[*]') LIKE %:contactType%")
    List<AcmeAccountEntity> findByContactType(@Param("contactType") String contactType);
}