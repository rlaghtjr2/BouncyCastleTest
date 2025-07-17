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

    // JSON 필드 내부 값으로 검색 (MySQL의 경우) - Native Query 사용
    @Query(value = "SELECT * FROM ACME_ACCOUNT WHERE JSON_CONTAINS(contact, JSON_QUOTE(?1))", nativeQuery = true)
    List<AcmeAccountEntity> findByContactEmail(@Param("email") String email);

    // 특정 연락처 타입으로 검색 - Native Query 사용
    @Query(value = "SELECT * FROM ACME_ACCOUNT WHERE JSON_SEARCH(contact, 'one', ?1) IS NOT NULL", nativeQuery = true)
    List<AcmeAccountEntity> findByContactType(@Param("contactType") String contactType);
}