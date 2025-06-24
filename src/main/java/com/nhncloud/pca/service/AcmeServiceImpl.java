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
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.model.acme.CertificateResult;
import com.nhncloud.pca.model.acme.Directory;
import com.nhncloud.pca.model.acme.FinalizeResult;
import com.nhncloud.pca.model.acme.Identifier;
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
import com.nhncloud.pca.util.JwsUtils;
import com.nimbusds.jose.jwk.RSAKey;

@Service
public class AcmeServiceImpl implements AcmeService {
    private final NonceStore nonceStore;
    private final AccountStore accountStore;
    private final ChallengeStore challengeStore;
    private final AuthorizationStore authorizationStore;
    private final OrderStore orderStore;
    private final CertStore certStore;

    public AcmeServiceImpl(NonceStore nonceStore, AccountStore accountStore, ChallengeStore challengeStore, AuthorizationStore authorizationStore, OrderStore orderStore, CertStore certStore) {
        this.nonceStore = nonceStore;
        this.accountStore = accountStore;
        this.challengeStore = challengeStore;
        this.authorizationStore = authorizationStore;
        this.orderStore = orderStore;
        this.certStore = certStore;
    }

    @Override
    public Directory getDirectory(HttpServletRequest request) {
        return new Directory(request.getServerName(), request.getServerPort());
    }

    @Override
    public String getNonce() {
        return nonceStore.generateNonce();
    }

    @Override
    public AccountCreationResult createAccount(Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            // 1. JWS 파싱 및 서명 검증
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(request, accountStore);
            String nonce = result.protectedHeader.get("nonce").toString();
            if (!nonceStore.consumeNonce(nonce)) {
                throw new RuntimeException("The request did not include a valid nonce.");
            }

            Map<String, Object> payloadMap = result.payload;
            RSAKey jwk = result.accountKey;

            // 2. 새로운 계정 ID 및 URL 생성
            String accountId = UUID.randomUUID().toString();
            String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
            String accountUrl = baseUrl + "/acme/acct/" + accountId;

            // 3. 계정 키 저장
            accountStore.saveAccount(accountUrl, jwk);

            // 4. 응답 객체 생성
            String replayNonce = nonceStore.generateNonce();
            List<String> contact = (List<String>) payloadMap.getOrDefault("contact", List.of());

            return AccountCreationResult.builder()
                .accountUrl(accountUrl)
                .contact(contact)
                .replayNonce(replayNonce)
                .build();
        } catch (SecurityException e) {
            throw new SecurityException("Signature verification failed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OrderCreationResult createOrder(Map<String, String> request, String baseUrl) {
        try {
            // JWS 검증
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(request, accountStore);

            // Nonce 확인
            String nonce = result.protectedHeader.get("nonce").toString();
            if (!nonceStore.consumeNonce(nonce)) {
                throw new RuntimeException("Invalid nonce");
            }

            // identifiers 파싱
            Map<String, Object> payloadMap = result.payload;
            List<Identifier> identifiers = new ObjectMapper().convertValue(
                payloadMap.get("identifiers"), new TypeReference<List<Identifier>>() {
                }
            );
            // Authz + Challenge 생성
            List<Authorization> authzs = new ArrayList<>();
            for (Identifier identifier : identifiers) {
                Challenge challenge = challengeStore.createChallenge(baseUrl);
                Authorization authz = authorizationStore.createAuthorization(identifier, List.of(challenge));
                authzs.add(authz);
            }

            // Order 생성
            Order order = orderStore.createOrder(identifiers, authzs, baseUrl);
            String replayNonce = nonceStore.generateNonce();

            return OrderCreationResult.builder()
                .order(order)
                .authzs(authzs)
                .replayNonce(replayNonce)
                .originalNonce(nonce)
                .build();

        } catch (SecurityException e) {
            throw new SecurityException("Signature verification failed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthorizationResult getAuthorization(String id, String baseUrl) {
        Authorization authz = authorizationStore.getAuthorization(id);

        if (authz == null) {
            throw new RuntimeException("Authorization not found");
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
            Order parentOrder = orderStore.findOrderByAuthzId(authz.getId());
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
    public ChallengeResult triggerChallenge(String id, Map<String, String> jwsRequest, String baseUrl) {
        JwsUtils.JwsParseResult result = null;
        try {
            result = JwsUtils.parseAndVerifyJws(jwsRequest, accountStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String nonce = result.protectedHeader.get("nonce").toString();

        if (!nonceStore.consumeNonce(nonce)) {
            throw new RuntimeException("Invalid nonce");
        }

        Challenge challenge = challengeStore.getChallenge(id);
        if (challenge == null) {
            throw new RuntimeException("Challenge not found");
        }

        // Challenge 및 Authorization 상태 갱신
        challengeStore.markValid(id);
        Authorization authz = authorizationStore.findAuthorizationByChallengeId(id);
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
    public FinalizeResult finalizeOrder(String orderId, Map<String, String> jwsRequest) {
        // 1. JWS 파싱 및 검증
        try {
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(jwsRequest, accountStore);

            Map<String, Object> payloadMap = result.payload;

            // 2. CSR 디코딩
            String csrBase64Url = (String) payloadMap.get("csr");
            byte[] csrBytes = Base64.getUrlDecoder().decode(csrBase64Url);
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);

            // 3. 도메인 추출
            String domain = BouncyCastleUtil.extractCommonName(csr);

            // 4. 도메인 인증 여부 확인
            if (!orderStore.isDomainAuthorized(orderId, domain)) {
                throw new RuntimeException("Domain not authorized");
            }

            // 5. 인증서 생성 및 저장
            X509Certificate certificate = BouncyCastleUtil.generateSelfSignedCert(csr);
            String certId = UUID.randomUUID().toString();
            certStore.save(certId, certificate);

            // 6. order finalize
            String pemCert = CertificateUtil.toPemString(certificate);
            orderStore.finalizeOrder(orderId, certId, csr, pemCert);

            return FinalizeResult.builder()
                .status("valid")
                .replayNonce(nonceStore.generateNonce())
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to finalize order", e);
        }
    }

    @Override
    public OrderQueryResult getOrder(String orderId, String baseUrl) {
        Order order = orderStore.getOrder(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
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
    public CertificateResult getCertificate(String certificateId) {
        // 인증서는 Chain으로 구성되어 있으므로, 단일 인증서가 아닌 체인으로 반환
        // 아직 DB 연동이 안되서, 그냥 새로 발급된 인증서 2개 붙이는 형식으로 진행
        X509Certificate certificate = certStore.get(certificateId);
        if (certificate == null) {
            throw new RuntimeException("Certificate not found");
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
}
