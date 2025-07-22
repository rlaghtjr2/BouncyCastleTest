package com.nhncloud.pca.store;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;
import com.nhncloud.pca.entity.acme.AcmeOrderEntity;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.order.Order;
import com.nhncloud.pca.repository.acme.AcmeAuthorizationRepository;
import com.nhncloud.pca.repository.acme.AcmeIdentifierRepository;
import com.nhncloud.pca.repository.acme.AcmeOrderRepository;

@Component
public class OrderStore {

    private final AcmeOrderRepository acmeOrderRepository;
    private final AcmeIdentifierRepository acmeIdentifierRepository;
    private final AcmeAuthorizationRepository acmeAuthorizationRepository;

    public OrderStore(AcmeOrderRepository acmeOrderRepository,
                      AcmeIdentifierRepository acmeIdentifierRepository,
                      AcmeAuthorizationRepository acmeAuthorizationRepository) {
        this.acmeOrderRepository = acmeOrderRepository;
        this.acmeIdentifierRepository = acmeIdentifierRepository;
        this.acmeAuthorizationRepository = acmeAuthorizationRepository;
    }

    /**
     * Order를 DB에 저장하는 메서드 (Order만 저장)
     */
    public Order createOrderWithDatabase(Long accountId, List<Identifier> identifiers,
                                         List<Authorization> authorizations, String baseUrl) {
        try {
            // 1. DB에 Order Entity 저장
            AcmeOrderEntity orderEntity = AcmeOrderEntity.builder()
                .accountId(accountId)
                .status(OrderStatus.PENDING)
                .expires(LocalDateTime.now().plusMinutes(5)) // 5분 후 만료
                .build();

            AcmeOrderEntity savedOrder = acmeOrderRepository.save(orderEntity);

            // 2. Order 객체 생성 (finalize URL은 동적으로 생성)
            String finalizeUrl = baseUrl + "/acme/order/" + savedOrder.getId() + "/finalize";

            Order order = Order.builder()
                .id(savedOrder.getId()) // Long id 직접 사용
                .status(savedOrder.getStatus())
                .identifiers(identifiers) // 원래 Identifier 객체들 사용
                .authorizations(authorizations)
                .expires(savedOrder.getExpires())
                .finalize(finalizeUrl)
                .build();

            return order;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save order to database: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 Order에 Identifier들을 저장하는 메서드 (저장된 Entity들 반환)
     */
    public List<AcmeIdentifierEntity> saveIdentifiersForOrder(Long orderId, List<Identifier> identifiers) {
        // Order Entity 조회
        Optional<AcmeOrderEntity> orderEntity = acmeOrderRepository.findById(orderId);
        if (orderEntity.isEmpty()) {
            throw new RuntimeException("Order not found with ID: " + orderId);
        }

        AcmeOrderEntity order = orderEntity.get();

        // Identifier들을 DB에 저장 (Order Entity 참조 사용)
        List<AcmeIdentifierEntity> identifierEntities = identifiers.stream()
            .map(identifier -> AcmeIdentifierEntity.builder()
                .order(order) // Order Entity 직접 참조
                .type(identifier.getType())
                .value(identifier.getValue())
                .build())
            .collect(Collectors.toList());

        List<AcmeIdentifierEntity> savedIdentifiers = acmeIdentifierRepository.saveAll(identifierEntities);
        return savedIdentifiers; // 저장된 Entity들 반환
    }

    public Order getOrder(String id) {
        try {
            Long orderId = Long.parseLong(id);
            Optional<AcmeOrderEntity> orderEntity = acmeOrderRepository.findById(orderId);

            if (orderEntity.isPresent()) {
                AcmeOrderEntity entity = orderEntity.get();

                // 1. JPA 관계를 통해 Identifier들 조회
                List<Identifier> identifiers = entity.getIdentifiers().stream()
                    .map(identifierEntity -> Identifier.builder()
                        .type(identifierEntity.getType())
                        .value(identifierEntity.getValue())
                        .build())
                    .collect(Collectors.toList());

                // 2. 각 Identifier에 대해 연관된 Authorization 조회
                List<Authorization> authorizations = new ArrayList<>();
                for (AcmeIdentifierEntity identifierEntity : entity.getIdentifiers()) {
                    Optional<AcmeAuthorizationEntity> authzEntity =
                        acmeAuthorizationRepository.findByIdentifier(identifierEntity);

                    if (authzEntity.isPresent()) {
                        AcmeAuthorizationEntity authz = authzEntity.get();

                        // Identifier 정보 생성
                        Identifier identifier = Identifier.builder()
                            .type(identifierEntity.getType())
                            .value(identifierEntity.getValue())
                            .build();

                        // Authorization 객체 생성
                        Authorization authorization = Authorization.builder()
                            .id(authz.getId()) // Long id 직접 사용
                            .identifier(identifier)
                            .status(authz.getStatus())
                            .expires(authz.getExpires())
                            .wildcard(authz.getWildcard())
                            .challenges(new ArrayList<>()) // Challenge는 필요시 별도 조회
                            .build();

                        authorizations.add(authorization);
                    }
                }

                // 동적으로 finalize URL 생성
                String finalizeUrl = "/acme/finalize/" + entity.getId();

                return Order.builder()
                    .id(entity.getId()) // Long id 직접 사용
                    .status(entity.getStatus())
                    .identifiers(identifiers) // JPA 관계를 통해 조회한 Identifier들
                    .authorizations(authorizations) // JPA 관계를 통해 조회한 Authorization들
                    .expires(entity.getExpires())
                    .finalize(finalizeUrl)
                    .certificate(entity.getCertificateId() != null ? "/acme/certificate/" + entity.getCertificateId() : null)
                    .build();
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Order findOrderByAuthzId(String authzId) {
        // Authorization과 Order 간의 관계를 DB에서 조회해야 하는 경우
        // 현재는 메모리 기반이었으므로 단순히 null 반환
        // 필요시 추후 DB 스키마에 관계 추가 필요
        return null;
    }

    public boolean isDomainAuthorized(String orderId, String domain) {
        Order order = getOrder(orderId);
        if (order == null || order.getAuthorizations() == null || order.getAuthorizations().isEmpty()) {
            return false;
        }

        for (Authorization authz : order.getAuthorizations()) {
            if (authz != null &&
                domain.equalsIgnoreCase(authz.getIdentifier().getValue()) &&
                authz.getStatus().equals(AuthorizationStatus.VALID)) {
                return true;
            }
        }

        return false;
    }

    public void markReadyIfAllAuthzValid(Order order, AuthorizationStore authzStore) {
        boolean allValid = order.getAuthorizations().stream()
            .allMatch(authz -> authz != null && authz.getStatus().equals(AuthorizationStatus.VALID));

        if (allValid && order.getStatus().equals(OrderStatus.PENDING)) {
            try {
                Long orderId = order.getId(); // Order의 Long id 직접 사용
                Optional<AcmeOrderEntity> orderEntity = acmeOrderRepository.findById(orderId);
                if (orderEntity.isPresent()) {
                    AcmeOrderEntity entity = orderEntity.get();
                    entity.setStatus(OrderStatus.READY);
                    acmeOrderRepository.save(entity);
                    order.setStatus(OrderStatus.READY);
                }
            } catch (Exception e) {
                // 무시
            }
        }
    }

    public void finalizeOrder(String orderId, String certificateId, PKCS10CertificationRequest csr, String pemCertificate) {
        try {
            Long id = Long.parseLong(orderId);
            Optional<AcmeOrderEntity> orderEntity = acmeOrderRepository.findById(id);
            if (orderEntity.isPresent()) {
                AcmeOrderEntity entity = orderEntity.get();
                entity.setStatus(OrderStatus.VALID);
                entity.setCertificateId(certificateId);
                acmeOrderRepository.save(entity);
            }
        } catch (NumberFormatException e) {
            // 무시
        }
    }
}