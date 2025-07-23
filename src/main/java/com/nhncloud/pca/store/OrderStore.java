package com.nhncloud.pca.store;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.entity.CaEntity;
import com.nhncloud.pca.entity.CertificateEntity;
import com.nhncloud.pca.entity.acme.AcmeAccountEntity;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;
import com.nhncloud.pca.entity.acme.AcmeOrderEntity;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.order.Order;
import com.nhncloud.pca.repository.CaRepository;
import com.nhncloud.pca.repository.acme.AcmeAccountRepository;
import com.nhncloud.pca.repository.acme.AcmeAuthorizationRepository;
import com.nhncloud.pca.repository.acme.AcmeIdentifierRepository;
import com.nhncloud.pca.repository.acme.AcmeOrderRepository;
import com.nhncloud.pca.util.BouncyCastleUtil;

@Component
public class OrderStore {

    private static final Logger log = LoggerFactory.getLogger(OrderStore.class);

    private final AcmeOrderRepository acmeOrderRepository;
    private final AcmeIdentifierRepository acmeIdentifierRepository;
    private final AcmeAuthorizationRepository acmeAuthorizationRepository;
    private final AcmeAccountRepository acmeAccountRepository;
    private final CaRepository caRepository;

    public OrderStore(AcmeOrderRepository acmeOrderRepository,
                      AcmeIdentifierRepository acmeIdentifierRepository,
                      AcmeAuthorizationRepository acmeAuthorizationRepository,
                      AcmeAccountRepository acmeAccountRepository,
                      CaRepository caRepository) {
        this.acmeOrderRepository = acmeOrderRepository;
        this.acmeIdentifierRepository = acmeIdentifierRepository;
        this.acmeAuthorizationRepository = acmeAuthorizationRepository;
        this.acmeAccountRepository = acmeAccountRepository;
        this.caRepository = caRepository;
    }

    /**
     * Order를 DB에 저장하는 메서드 (Order만 저장)
     */
    public Order createOrderWithDatabase(Long accountId, List<Identifier> identifiers,
                                         List<Authorization> authorizations, String baseUrl) {
        try {
            // 1. Account Entity 조회
            Optional<AcmeAccountEntity> accountEntity = acmeAccountRepository.findById(accountId);
            if (accountEntity.isEmpty()) {
                throw new RuntimeException("Account not found with ID: " + accountId);
            }

            // 2. DB에 Order Entity 저장 (Account Entity 참조)
            AcmeOrderEntity orderEntity = AcmeOrderEntity.builder()
                .account(accountEntity.get()) // Account Entity 직접 참조
                .status(OrderStatus.PENDING)
                .expires(LocalDateTime.now().plusMinutes(5)) // 5분 후 만료
                .build();

            AcmeOrderEntity savedOrder = acmeOrderRepository.save(orderEntity);

            // 3. Order 객체 생성 (finalize URL은 동적으로 생성)
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

    public Order getOrder(Long id) {
        Optional<AcmeOrderEntity> orderEntity = acmeOrderRepository.findById(id); // Long 직접 사용

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

                authzEntity.ifPresent(
                    authz -> {
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
                );
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
    }

    public Order findOrderByAuthzId(String authzId) {
        // Authorization과 Order 간의 관계를 DB에서 조회해야 하는 경우
        // 현재는 메모리 기반이었으므로 단순히 null 반환
        // 필요시 추후 DB 스키마에 관계 추가 필요
        return null;
    }

    public boolean isDomainAuthorized(Long orderId, String domain) {
        // 1단계: CA Certificate 도메인 매칭 체크 (우선)
        if (checkCaCertificateDomainMatch(orderId, domain)) {
            return true;
        }

        // 2단계: ACME Authorization 체크 (보조)
        if (checkAcmeAuthorizationDomainMatch(orderId, domain)) {
            return true;
        }

        return false;
    }

    /**
     * 도메인이 인증되었는지 확인하고, 매칭되는 Certificate Entity를 반환
     */
    public Optional<CertificateEntity> getAuthorizedCertificate(Long orderId, String domain) {
        // 1단계: CA Certificate 도메인 매칭 체크 및 매칭되는 Certificate 반환
        Optional<CertificateEntity> matchedCert = findMatchingCaCertificate(orderId, domain);
        if (matchedCert.isPresent()) {
            log.debug("[getAuthorizedCertificate] Found matching CA certificate ID: {} for domain: {}",
                matchedCert.get().getId(), domain);
            return matchedCert;
        }

        // 2단계: ACME Authorization 체크 (Certificate 없음)
//        if (checkAcmeAuthorizationDomainMatch(orderId, domain)) {
//            log.debug("[getAuthorizedCertificate] Domain authorized via ACME Authorization (no certificate)");
//            return Optional.empty(); // ACME Authorization으로 인증되었지만 Certificate는 없음
//        }

        return Optional.empty();
    }

    /**
     * CA Certificate의 도메인과 요청 도메인이 매칭되는 Certificate Entity를 반환
     */
    private Optional<CertificateEntity> findMatchingCaCertificate(Long orderId, String domain) {
        List<CertificateEntity> certificates = getCertificatesFromOrder(orderId);
        if (certificates.isEmpty()) {
            return Optional.empty();
        }

        for (CertificateEntity cert : certificates) {
            if (isDomainMatchInCertificate(cert, domain)) {
                return Optional.of(cert);
            }
        }

        return Optional.empty();
    }

    /**
     * CA Certificate의 도메인과 요청 도메인 매칭 확인
     */
    private boolean checkCaCertificateDomainMatch(Long orderId, String domain) {
        return findMatchingCaCertificate(orderId, domain).isPresent();
    }

    /**
     * ACME Authorization의 도메인과 요청 도메인 매칭 확인
     */
    private boolean checkAcmeAuthorizationDomainMatch(Long orderId, String domain) {
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

    /**
     * Order ID로부터 연관된 CA Certificate 목록을 조회
     */
    private List<CertificateEntity> getCertificatesFromOrder(Long orderId) {
        Optional<AcmeOrderEntity> orderEntity = acmeOrderRepository.findById(orderId);
        if (orderEntity.isEmpty()) {
            return List.of();
        }

        AcmeAccountEntity accountEntity = orderEntity.get().getAccount();
        if (accountEntity == null) {
            return List.of();
        }

        CaEntity caEntity = accountEntity.getCa();
        if (caEntity == null) {
            return List.of();
        }

        List<CertificateEntity> certificates = caEntity.getCertificates();
        return certificates != null ? certificates : List.of();
    }

    /**
     * Certificate에서 CSR 파싱 domain과 매칭 여부 확인 (개선된 버전)
     */
    private boolean isDomainMatchInCertificate(CertificateEntity cert, String domain) {
        log.debug("[isDomainMatchInCertificate] Checking certificate for domain: " + domain);

        // 1. Subject 필드에서 CN(Common Name) 추출 및 매칭
        String subject = cert.getSubject();
        if (subject != null && subject.contains("CN=")) {
            String cnValue = BouncyCastleUtil.parseDnWithBouncyCastle(subject).getCommonName();
            if (cnValue != null && domain.equalsIgnoreCase(cnValue)) {
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

    public void finalizeOrder(Long orderId, Long certificateId) {
        Optional<AcmeOrderEntity> orderEntity = acmeOrderRepository.findById(orderId); // Long 직접 사용
        if (orderEntity.isPresent()) {
            AcmeOrderEntity entity = orderEntity.get();
            entity.setStatus(OrderStatus.VALID);
            entity.setCertificateId(certificateId.toString()); // Long을 String으로 변환하여 저장
            acmeOrderRepository.save(entity);

        }
    }
}