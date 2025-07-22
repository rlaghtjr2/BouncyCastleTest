package com.nhncloud.pca.service;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.constant.acme.ProblemType;
import com.nhncloud.pca.entity.acme.AcmeAuthorizationEntity;
import com.nhncloud.pca.entity.acme.AcmeIdentifierEntity;
import com.nhncloud.pca.exception.AcmeProblemException;
import com.nhncloud.pca.mapper.AcmeMapper;
import com.nhncloud.pca.model.acme.CertificateResult;
import com.nhncloud.pca.model.acme.Directory;
import com.nhncloud.pca.model.acme.FinalizeResult;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.JwsParseResult;
import com.nhncloud.pca.model.acme.JwsRequest;
import com.nhncloud.pca.model.acme.account.AccountCreationResult;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.authorization.AuthorizationResult;
import com.nhncloud.pca.model.acme.challenge.Challenge;
import com.nhncloud.pca.model.acme.challenge.ChallengeResult;
import com.nhncloud.pca.model.acme.order.Order;
import com.nhncloud.pca.model.acme.order.OrderCreationResult;
import com.nhncloud.pca.model.acme.order.OrderQueryResult;
import com.nhncloud.pca.store.AccountStore;
import com.nhncloud.pca.store.AuthorizationStore;
import com.nhncloud.pca.store.CertStore;
import com.nhncloud.pca.store.ChallengeStore;
import com.nhncloud.pca.store.NonceStore;
import com.nhncloud.pca.store.OrderStore;
import com.nhncloud.pca.util.BouncyCastleUtil;
import com.nhncloud.pca.util.CertificateUtil;
import com.nimbusds.jose.jwk.RSAKey;

@Service
public class AcmeServiceImpl implements AcmeService {
    private final NonceStore nonceStore;
    private final AccountStore accountStore;
    private final ChallengeStore challengeStore;
    private final AuthorizationStore authorizationStore;
    private final OrderStore orderStore;
    private final CertStore certStore;
    private final AcmeMapper acmeMapper;

    public AcmeServiceImpl(NonceStore nonceStore, AccountStore accountStore, ChallengeStore challengeStore,
                           AuthorizationStore authorizationStore, OrderStore orderStore, CertStore certStore,
                           AcmeMapper acmeMapper) {
        this.nonceStore = nonceStore;
        this.accountStore = accountStore;
        this.challengeStore = challengeStore;
        this.authorizationStore = authorizationStore;
        this.orderStore = orderStore;
        this.certStore = certStore;
        this.acmeMapper = acmeMapper;
    }

    @Override
    public Directory getDirectory(HttpServletRequest request) {
        return new Directory(request.getServerName(), request.getServerPort());
    }

    @Override
    public AccountCreationResult createAccount(JwsRequest jwsRequest, HttpServletRequest httpRequest) {
        // Interceptor에서 이미 JWS 파싱 및 nonce 검증 완료
        JwsParseResult result = (JwsParseResult) httpRequest.getAttribute("jwsParseResult");

        Map<String, Object> payloadMap = result.getPayload();
        RSAKey accountKey = result.getAccountKey();

        // 2. 새로운 계정 ID 및 URL 생성
        String accountId = UUID.randomUUID().toString();
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        String accountUrl = baseUrl + "/acme/acct/" + accountId;

        // 3. 계정 키는 별도 서비스에서 DB에 저장됨 (메모리 저장 제거)

        // 4. 응답 객체 생성
        String replayNonce = nonceStore.generateNonce();
        List<String> contact = (List<String>) payloadMap.getOrDefault("contact", List.of());

        return AccountCreationResult.builder()
            .accountUrl(accountUrl)
            .contact(contact)
            .replayNonce(replayNonce)
            .build();
    }

    @Override
    public OrderCreationResult createOrder(JwsRequest jwsRequest, String baseUrl, HttpServletRequest httpRequest) {
        // Interceptor에서 이미 JWS 파싱 및 nonce 검증 완료
        JwsParseResult result = (JwsParseResult) httpRequest.getAttribute("jwsParseResult");

        // identifiers 파싱
        Map<String, Object> payloadMap = result.getPayload();
        List<Identifier> identifiers = new ObjectMapper().convertValue(
            payloadMap.get("identifiers"), new TypeReference<List<Identifier>>() {
            }
        );

        // kid에서 account ID 추출 (kid는 URL 형태: https://localhost:8443/acme/acct/3)
        String kid = (String) result.getProtectedHeader().get("kid");
        Long accountId = extractAccountIdFromKid(kid);

        // 1단계: Order를 DB에 저장
        Order order = orderStore.createOrderWithDatabase(accountId, identifiers, new ArrayList<>(), baseUrl);

        // 2단계: Identifier들을 DB에 저장 (Order와 연결) - 저장된 Entity들 반환받음
        List<AcmeIdentifierEntity> savedIdentifierEntities =
            orderStore.saveIdentifiersForOrder(order.getId(), identifiers); // Long id 직접 사용

        // 3단계: Authorization을 DB에 저장 후 Challenge도 DB에 저장 (저장된 Identifier Entity 활용)
        List<Authorization> savedAuthzs = new ArrayList<>();
        for (AcmeIdentifierEntity identifierEntity : savedIdentifierEntities) {
            // Authorization Entity를 DB에 저장 (불필요한 DB 조회 제거)
            AcmeAuthorizationEntity authorizationEntity = authorizationStore.createAuthorizationWithIdentifierEntity(identifierEntity);

            // Challenge를 DB에 저장 (저장된 Authorization Entity 직접 사용)
            Challenge challenge = challengeStore.createChallengeWithAuthorizationEntity(authorizationEntity, baseUrl);

            // Mapper를 사용한 Entity → 객체 변환 (간결화)
            Authorization authorization = acmeMapper.toAuthorizationWithIdentifierAndChallenges(
                identifierEntity, authorizationEntity, List.of(challenge));

            savedAuthzs.add(authorization);
        }

        String replayNonce = nonceStore.generateNonce();

        return OrderCreationResult.builder()
            .order(order)
            .authzs(savedAuthzs) // DB에 저장된 Authorization 사용
            .replayNonce(replayNonce)
            .originalNonce(result.getProtectedHeader().get("nonce").toString())
            .build();
    }

    @Override
    public AuthorizationResult getAuthorization(Long id, String baseUrl) {
        Authorization authz = authorizationStore.getAuthorization(id);

        if (authz == null) {
            throw new AcmeProblemException(ProblemType.MALFORMED, "Authorization resource with ID '" + id + "' was not found",
                HttpStatus.NOT_FOUND, nonceStore.generateNonce());
        }

        // 챌린지 리스트
        List<Map<String, String>> challenges = authz.getChallenges().stream()
            .map(challenge -> Map.of(
                "type", challenge.getType().getType(),
                "status", challenge.getStatus().getStatus(),
                "url", challenge.getUrl(),
                "token", challenge.getToken()
            ))
            .collect(Collectors.toList());

        // 부모 Order가 있는 경우 (status == valid)
        String upLink = null;
        if (authz.getStatus().equals(AuthorizationStatus.VALID)) {
            Order parentOrder = orderStore.findOrderByAuthzId(authz.getId().toString());
            if (parentOrder != null) {
                upLink = baseUrl + "/acme/order/" + parentOrder.getId();
            }
        }

        return AuthorizationResult.builder()
            .identifier(authz.getIdentifier())
            .status(authz.getStatus().getStatus())
            .expires(authz.getExpires().atZone(ZoneOffset.UTC).toInstant())
            .challenges(challenges)
            .replayNonce(nonceStore.generateNonce())
            .upLink(upLink)
            .build();
    }

    @Override
    public ChallengeResult triggerChallenge(Long id, JwsRequest jwsRequest, String baseUrl, HttpServletRequest httpRequest) {
        Challenge challenge = challengeStore.getChallenge(id);
        if (challenge == null) {
            throw new AcmeProblemException(ProblemType.MALFORMED, "Challenge resource with ID '" + id + "' was not found",
                HttpStatus.NOT_FOUND, nonceStore.generateNonce());
        }

        // Challenge 및 Authorization 상태 갱신
        challengeStore.markValid(id);
        Authorization authz = authorizationStore.findAuthorizationByChallengeId(id); // toString() 제거
        authorizationStore.markValid(authz.getId());

        // 응답 데이터
        String challengeUrl = baseUrl + "/acme/challenge/" + id;
        String authzUrl = baseUrl + "/acme/authz/" + authz.getId();

        return ChallengeResult.builder()
            .type("http-01")
            .url(challengeUrl)
            .status("valid")
            .token(challenge.getToken())
            .replayNonce(nonceStore.generateNonce())
            .upLink(authzUrl)
            .build();
    }

    @Override
    public FinalizeResult finalizeOrder(String orderId, JwsRequest jwsRequest, HttpServletRequest httpRequest) {
        // Interceptor에서 이미 JWS 파싱 및 nonce 검증 완료
        JwsParseResult result = (JwsParseResult) httpRequest.getAttribute("jwsParseResult");

        Map<String, Object> payloadMap = result.getPayload();

        // 2. CSR 디코딩
        String csrBase64Url = (String) payloadMap.get("csr");
        byte[] csrBytes = Base64.getUrlDecoder().decode(csrBase64Url);
        PKCS10CertificationRequest csr = null;
        try {
            csr = new PKCS10CertificationRequest(csrBytes);
        } catch (IOException e) {
            throw new RuntimeException("csr decoding failed", e);
        }

        // 3. 도메인 추출
        String domain = null;
        try {
            domain = BouncyCastleUtil.extractCommonName(csr);
        } catch (Exception e) {
            throw new RuntimeException("domain extraction failed", e);
        }

        // 4. 도메인 인증 여부 확인
        if (!orderStore.isDomainAuthorized(orderId, domain)) {
            throw new AcmeProblemException(ProblemType.MALFORMED, "domain '" + domain + "' is not authorized for order '" + orderId + "'",
                HttpStatus.NOT_FOUND, nonceStore.generateNonce());
        }

        // 5. 인증서 생성 및 저장
        X509Certificate certificate = null;
        try {
            certificate = BouncyCastleUtil.generateSelfSignedCert(csr);
        } catch (Exception e) {
            throw new RuntimeException("certificate generation failed", e);
        }
        String certId = UUID.randomUUID().toString();
        certStore.save(certId, certificate);

        // 6. order finalize
        String pemCert = CertificateUtil.toPemString(certificate);
        orderStore.finalizeOrder(orderId, certId, csr, pemCert);

        return FinalizeResult.builder()
            .status("valid")
            .replayNonce(nonceStore.generateNonce())
            .build();
    }

    @Override
    public OrderQueryResult getOrder(String orderId, String baseUrl) {
        Order order = orderStore.getOrder(orderId);
        if (order == null) {
            throw new AcmeProblemException(ProblemType.MALFORMED, "Order resource with ID '" + orderId + "' was not found",
                HttpStatus.NOT_FOUND, nonceStore.generateNonce());
        }

        // Authorization 상태 확인 → Order 상태 갱신
        orderStore.markReadyIfAllAuthzValid(order, authorizationStore);

        Map<String, Object> body = new HashMap<>();
        body.put("status", order.getStatus().getStatus());
        body.put("expires", order.getExpires().atZone(ZoneOffset.UTC).toInstant());
        body.put("identifiers", order.getIdentifiers());
        body.put("authorizations", order.getAuthorizations().stream()
            .map(authz -> baseUrl + "/acme/authz/" + authz.getId())
            .collect(Collectors.toList()));
        body.put("finalize", order.getFinalize());

        if (order.getStatus().equals(OrderStatus.VALID)) {
            body.put("certificate", baseUrl + order.getCertificate());
        }

        return OrderQueryResult.builder()
            .body(body)
            .replayNonce(nonceStore.generateNonce())
            .build();
    }

    @Override
    public CertificateResult getCertificate(String certificateId, String baseUrl) {
        X509Certificate certificate = certStore.get(certificateId);

        if (certificate == null) {
            throw new AcmeProblemException(ProblemType.SERVER_INTERNAL, "Certificate data not found",
                HttpStatus.INTERNAL_SERVER_ERROR, nonceStore.generateNonce());
        }

        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(certificate); // 체인 시뮬레이션 (2회 반복)
            pemWriter.writeObject(certificate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return CertificateResult.builder()
            .pemChain(writer.toString())
            .replayNonce(nonceStore.generateNonce())
            .build();
    }

    /**
     * kid URL에서 account id 추출
     * 예: https://localhost:8443/acme/acct/3 -> 3
     */
    private Long extractAccountIdFromKid(String kid) {
        if (kid == null) {
            throw new IllegalArgumentException("kid is required");
        }

        try {
            // kid가 숫자인 경우 (기존 방식 호환)
            return Long.parseLong(kid);
        } catch (NumberFormatException e) {
            // kid가 URL인 경우 마지막 부분에서 account id 추출
            if (kid.contains("/acct/")) {
                String[] parts = kid.split("/acct/");
                if (parts.length >= 2) {
                    String accountIdStr = parts[1];
                    // 추가 경로나 쿼리 파라미터가 있는 경우 제거
                    if (accountIdStr.contains("/")) {
                        accountIdStr = accountIdStr.substring(0, accountIdStr.indexOf("/"));
                    }
                    if (accountIdStr.contains("?")) {
                        accountIdStr = accountIdStr.substring(0, accountIdStr.indexOf("?"));
                    }
                    return Long.parseLong(accountIdStr);
                }
            }

            // URL에서 마지막 숫자 추출 (일반적인 경우)
            String[] urlParts = kid.split("/");
            if (urlParts.length > 0) {
                String lastPart = urlParts[urlParts.length - 1];
                // 쿼리 파라미터가 있는 경우 제거
                if (lastPart.contains("?")) {
                    lastPart = lastPart.substring(0, lastPart.indexOf("?"));
                }
                return Long.parseLong(lastPart);
            }

            throw new IllegalArgumentException("Cannot extract account id from kid: " + kid);
        }
    }
}
