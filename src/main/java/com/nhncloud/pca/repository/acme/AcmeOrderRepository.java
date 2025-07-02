package com.nhncloud.pca.repository.acme;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.entity.acme.AcmeOrderEntity;

@Repository
public interface AcmeOrderRepository extends JpaRepository<AcmeOrderEntity, Long> {

    List<AcmeOrderEntity> findByAccountId(Long accountId);

    List<AcmeOrderEntity> findByAccountIdAndStatus(Long accountId, OrderStatus status);

    Optional<AcmeOrderEntity> findByIdAndAccountId(Long id, Long accountId);

    List<AcmeOrderEntity> findByStatus(OrderStatus status);

    // 만료된 주문 조회
    List<AcmeOrderEntity> findByExpiresBefore(LocalDateTime now);

    // 특정 기간 내 주문 조회
    @Query("SELECT o FROM AcmeOrderEntity o WHERE o.creationDatetime BETWEEN :startDate AND :endDate")
    List<AcmeOrderEntity> findByCreationDateBetween(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
}