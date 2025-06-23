package com.nhncloud.pca.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.security.SecureRandom;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.model.acme.Authorization;
import com.nhncloud.pca.model.acme.Challenge;
import com.nhncloud.pca.model.acme.Directory;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.Order;
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

@RestController
@RequestMapping("/acme")
@Slf4j
public class AcmeController {
    private final SecureRandom secureRandom = new SecureRandom();

    private final AccountStore accountStore;
    private final ChallengeStore challengeStore;
    private final NonceStore nonceStore;
    private final OrderStore orderStore;
    private final AuthorizationStore authorizationStore;
    private final CertStore certStore;

    public AcmeController(AccountStore accountStore, ChallengeStore challengeStore, NonceStore nonceStore, OrderStore orderStore, AuthorizationStore authorizationStore, CertStore certStore) {
        this.accountStore = accountStore;
        this.challengeStore = challengeStore;
        this.nonceStore = nonceStore;
        this.orderStore = orderStore;
        this.authorizationStore = authorizationStore;
        this.certStore = certStore;
    }

    // /directory
    @RequestMapping(value = "/directory", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Directory> getDirectory(HttpServletRequest request) {
        return ResponseEntity.ok(new Directory(request.getServerName(), request.getServerPort()));
    }

    // /new-nonce
    @RequestMapping(value = "/new-nonce", method = RequestMethod.HEAD)
    public ResponseEntity<Void> newNonce() {
        String nonce = nonceStore.generateNonce();

        return ResponseEntity.noContent()
            .header("Replay-Nonce", nonce)
            .header("Cache-Control", "no-store")
            .build();
    }

    // /new-account
    @PostMapping("/new-account")
    public ResponseEntity<Map<String, Object>> newAccount(@RequestBody Map<String, String> request,
                                                          HttpServletRequest httpRequest) {
        try {
            // 1. JWS 파싱 및 서명 검증
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(request, accountStore);
            String nonce = result.protectedHeader.get("nonce").toString();
            if (!nonceStore.consumeNonce(nonce)) {
                return ResponseEntity.status(400).body(Map.of(
                    "type", "urn:ietf:params:acme:error:badRequest",
                    "detail", "The request did not include a valid nonce.",
                    "status", 400
                ));
            }
            Map<String, Object> payloadMap = result.payload;
            RSAKey jwk = result.accountKey;

            // 2. 새로운 계정 ID 및 URL 생성
            String accountId = UUID.randomUUID().toString();
            String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
            String accountUrl = baseUrl + "/acme/acct/" + accountId;

            // 3. 계정 키 저장
            accountStore.saveAccount(accountUrl, jwk);

            // 4. 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("status", "valid");
            response.put("contact", payloadMap.getOrDefault("contact", List.of()));
            response.put("orders", accountUrl + "/orders");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", accountUrl); // kid 역할
            headers.set("Replay-Nonce", nonceStore.generateNonce());
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("----New Account----");
            response.forEach((key, value) -> log.info("{}: {}", key, value));
            return new ResponseEntity<>(response, headers, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("type", "urn:ietf:params:acme:error:malformed", "detail", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(400).body(Map.of("type", "urn:ietf:params:acme:error:badSignature", "detail", "Signature verification failed"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("type", "urn:ietf:params:acme:error:serverInternal", "detail", "Internal server error"));
        }
    }

    @PostMapping("/new-order")
    public ResponseEntity<?> newOrder(@RequestBody Map<String, String> jwsRequest,
                                      HttpServletRequest request) {
        try {
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(jwsRequest, accountStore);
            String nonce = result.protectedHeader.get("nonce").toString();
            if (!nonceStore.consumeNonce(nonce)) {
                return ResponseEntity.status(400).body(Map.of(
                    "type", "urn:ietf:params:acme:error:badRequest",
                    "detail", "The request did not include a valid nonce.",
                    "status", 400
                ));
            }
            Map<String, Object> payloadMap = result.payload;

            List<Identifier> identifiers = new ObjectMapper().convertValue(
                payloadMap.get("identifiers"),
                new TypeReference<List<Identifier>>() {
                }
            );
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            // authzs 리스트를 채워야 하므로 각각에 대해 Authorization과 Challenge 생성
            List<Authorization> authzs = new ArrayList<>();

            for (Identifier identifier : identifiers) {
                // 3-1. Challenge 생성
                // 여러개 일 수 있지만, 일단 http-01만 지원
                Challenge challenge = challengeStore.createChallenge(baseUrl);

                // 3-2. Authorization 생성 및 저장
                Authorization authz = authorizationStore.createAuthorization(identifier, List.of(challenge));

                authzs.add(authz);
            }

            // Order 생성 후 저장
            Order order = orderStore.createOrder(identifiers, authzs, baseUrl);

            Map<String, Object> response = Map.of(
                "status", order.getStatus().getStatus(),
                "expires", order.getExpires().atZone(ZoneOffset.UTC).toInstant(),
                "identifiers", identifiers,
                "authorizations", authzs.stream()
                    .map(authorization -> baseUrl + "/acme/authz/" + authorization.getId())
                    .collect(Collectors.toList()),
                "finalize", order.getFinalize()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Replay-Nonce", nonceStore.generateNonce());
            headers.setLocation(URI.create(baseUrl + "/acme/order/" + order.getId()));

            log.info("----New Order----");
            response.forEach((k, v) -> log.info("{}: {}", k, v));

            return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/authz/{id}")
    public ResponseEntity<?> getAuthorization(@PathVariable String id, HttpServletRequest req) {
        Authorization authz = authorizationStore.getAuthorization(id);

        if (authz == null) {
            return ResponseEntity.status(404).body(Map.of(
                "type", "urn:ietf:params:acme:error:authorizationNotFound",
                "detail", "Authorization not found",
                "status", 404
            ));
        }
        String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();

        List<Map<String, String>> challengesMap = authz.getChallenges().stream()
            .map(challenge -> {
                return Map.of(
                    "type", challenge.getType().getType(),
                    "status", challenge.getStatus().getStatus(),
                    "url", challenge.getUrl(),
                    "token", challenge.getToken()
                );
            })
            .collect(Collectors.toList());

        Map<String, Object> response = Map.of(
            "identifier", authz.getIdentifier(),
            "status", authz.getStatus().getStatus(),
            "expires", authz.getExpires().atZone(ZoneOffset.UTC).toInstant(),
            "challenges", challengesMap
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Replay-Nonce", nonceStore.generateNonce());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // status가 valid인 경우에만 Link 헤더 추가
        if ("valid".equalsIgnoreCase(authz.getStatus().toString())) {
            Order parentOrder = orderStore.findOrderByAuthzId(authz.getId());
            if (parentOrder != null) {
                headers.add("Link", "<" + baseUrl + "/acme/order/" + parentOrder.getId() + ">; rel=\"up\"");
            }
        }

        log.info("----Authz----");
        response.forEach((key, value) -> log.info("{}: {}", key, value));

        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    @PostMapping("/challenge/{id}")
    public ResponseEntity<?> triggerChallenge(@PathVariable String id,
                                              @RequestBody Map<String, String> jwsRequest,
                                              HttpServletRequest req) {
        try {
            // JWS 파싱 및 nonce 검증
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(jwsRequest, accountStore);
            String nonce = result.protectedHeader.get("nonce").toString();
            if (!nonceStore.consumeNonce(nonce)) {
                return ResponseEntity.status(400).body(Map.of(
                    "type", "urn:ietf:params:acme:error:badRequest",
                    "detail", "The request did not include a valid nonce.",
                    "status", 400
                ));
            }
            Challenge challenge = challengeStore.getChallenge(id);
            if (challenge == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "type", "urn:ietf:params:acme:error:challengeNotFound",
                    "detail", "Challenge not found",
                    "status", 404
                ));
            }

            challengeStore.markValid(id);
            String authzId = authorizationStore.findAuthorizationByChallengeId(id).getId();

            //Challenge가 valid로 변경되면 (일단 1개) Authorization도 valid로 변경
            authorizationStore.markValid(authzId);

            String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();

            Map<String, Object> response = Map.of(
                "type", "http-01",
                "url", baseUrl + "/acme/challenge/" + id,
                "status", "valid",
                "token", challenge.getToken()
            );
            String authzUrl = baseUrl + "/acme/authz/" + authzId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Replay-Nonce", nonceStore.generateNonce());
            headers.set("Link", "<" + authzUrl + ">; rel=\"up\"");
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("----Challenge----");
            response.forEach((key, value) -> log.info("{}: {}", key, value));

            return new ResponseEntity<>(response, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "type", "urn:ietf:params:acme:error:badRequest",
                "detail", e.getMessage(),
                "status", 400
            ));
        }
    }

    @PostMapping("/order/{orderId}/finalize")
    public ResponseEntity<?> finalizeOrder(@PathVariable String orderId,
                                           @RequestBody Map<String, String> jwsRequest,
                                           HttpServletRequest httpRequest) {

        try {
            // 1. JWS 파싱 및 검증
            JwsUtils.JwsParseResult result = JwsUtils.parseAndVerifyJws(jwsRequest, accountStore);
            Map<String, Object> payloadMap = result.payload;

            // 2. CSR 파싱
            String csrBase64Url = (String) payloadMap.get("csr");
            byte[] csrBytes = Base64.getUrlDecoder().decode(csrBase64Url);
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);

            // 3. CSR에서 도메인 추출 (예: CN, SAN 등)
            String domain = BouncyCastleUtil.extractCommonName(csr);
            System.out.println("CSR domain: " + domain);

            // 4. 도메인이 이미 인증된 상태인지 확인
            if (!orderStore.isDomainAuthorized(orderId, domain)) {
                return ResponseEntity.status(400).body(Map.of(
                    "type", "urn:ietf:params:acme:error:unauthorized",
                    "detail", "Domain not authorized"
                ));
            }

            // 5. 인증서 생성 (임시 self-signed 예시)
            X509Certificate certificate = BouncyCastleUtil.generateSelfSignedCert(csr);
            String certId = UUID.randomUUID().toString();
            certStore.save(certId, certificate);

            // 6. 응답 생성
            orderStore.finalizeOrder(orderId, certId, csr, CertificateUtil.toPemString(certificate)); // order 상태를 valid로 변경

            HttpHeaders headers = new HttpHeaders();
            headers.set("Replay-Nonce", nonceStore.generateNonce());
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("----Finalize----");
            return new ResponseEntity<>(Map.of(
                "status", "valid"
            ), headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "type", "urn:ietf:params:acme:error:serverInternal",
                "detail", "CSR processing failed"
            ));
        }
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderStore.getOrder(orderId);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of(
                "type", "urn:ietf:params:acme:error:orderNotFound",
                "detail", "Order not found",
                "status", 404
            ));
        }

        // 상태 최신화: Authorization들이 모두 valid면 order를 ready로
        orderStore.markReadyIfAllAuthzValid(order, authorizationStore);

        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

        Map<String, Object> response = new HashMap<>();
        response.put("status", order.getStatus().getStatus());
        response.put("expires", order.getExpires().atZone(ZoneOffset.UTC).toInstant());
        response.put("identifiers", order.getIdentifiers());
        response.put("authorizations", order.getAuthorizations().stream()
            .map(authz -> baseUrl + "/acme/authz/" + authz.getId())
            .collect(Collectors.toList()));
        response.put("finalize", order.getFinalize());
        if (order.getStatus().equals(OrderStatus.VALID)) {
            response.put("certificate", baseUrl + order.getCertificate());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Replay-Nonce", nonceStore.generateNonce());
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("----GET Order----");
        response.forEach((key, value) -> log.info("{}: {}", key, value));
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    @PostMapping("/certificate/{id}")
    public ResponseEntity<String> getCertificate(@PathVariable String id) {
        // 인증서는 Chain으로 구성되어 있으므로, 단일 인증서가 아닌 체인으로 반환
        // 아직 DB 연동이 안되서, 그냥 새로 발급된 인증서 2개 붙이는 형식으로 진행
        X509Certificate certificate = certStore.get(id);
        if (certificate == null) {
            return ResponseEntity.status(404).body("Certificate not found");
        }

        try {
            StringWriter writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
            pemWriter.writeObject(certificate);
            pemWriter.writeObject(certificate);
            pemWriter.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/pem-certificate-chain"));
            headers.set("Replay-Nonce", nonceStore.generateNonce());

            log.info("----Get Certificate----");
            log.info(writer.toString());

            return new ResponseEntity<>(writer.toString(), headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to encode certificate");
        }
    }

}
