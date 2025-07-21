package com.nhncloud.pca.repository.acme;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;
import com.nhncloud.pca.entity.acme.AcmeOrderEntity;

@Repository
public interface AcmeIdentifierRepository extends JpaRepository<AcmeIdentifierEntity, Long> {

    // Order Entity를 직접 사용하는 메서드들 (권장)
    List<AcmeIdentifierEntity> findByOrder(AcmeOrderEntity order);

    List<AcmeIdentifierEntity> findByOrderAndType(AcmeOrderEntity order, String type);

    // 기존 orderId 기반 메서드들 (호환성을 위해 유지, JPA가 자동으로 order.id로 처리)
    List<AcmeIdentifierEntity> findByOrderId(Long orderId);

    List<AcmeIdentifierEntity> findByType(String type);

    List<AcmeIdentifierEntity> findByValue(String value);

    List<AcmeIdentifierEntity> findByTypeAndValue(String type, String value);
}