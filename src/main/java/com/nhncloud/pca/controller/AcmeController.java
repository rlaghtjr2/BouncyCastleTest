package com.nhncloud.pca.controller;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.nhncloud.pca.model.acme.CertificateResult;
import com.nhncloud.pca.model.acme.Directory;
import com.nhncloud.pca.model.acme.FinalizeResult;
import com.nhncloud.pca.model.acme.JwsRequest;
import com.nhncloud.pca.model.acme.account.AccountCreationResult;
import com.nhncloud.pca.model.acme.authorization.Authorization;
import com.nhncloud.pca.model.acme.authorization.AuthorizationResult;
import com.nhncloud.pca.model.acme.challenge.ChallengeResult;
import com.nhncloud.pca.model.acme.order.Order;
import com.nhncloud.pca.model.acme.order.OrderCreationResult;
import com.nhncloud.pca.model.acme.order.OrderQueryResult;
import com.nhncloud.pca.service.AcmeService;
import com.nhncloud.pca.store.NonceStore;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/acme")
@Slf4j
public class AcmeController {
    private final AcmeService acmeService;
    private final NonceStore nonceStore;

    public AcmeController(AcmeService acmeService, NonceStore nonceStore) {
        this.acmeService = acmeService;
        this.nonceStore = nonceStore;
    }

    // /directory
    @RequestMapping(value = "/directory", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Directory> getDirectory(HttpServletRequest request) {
        Directory directory = acmeService.getDirectory(request);
        return ResponseEntity.ok(directory);
    }

    // /new-nonce
    @RequestMapping(value = "/new-nonce", method = RequestMethod.HEAD)
    public ResponseEntity<Void> newNonce() {
        // NonceStore에서 nonce 생성 및 저장
        String nonce = nonceStore.generateNonce();

        return ResponseEntity.noContent()
            .header("Replay-Nonce", nonce)
            .header("Cache-Control", "no-store")
            .build();
    }

    // /new-account
    @PostMapping("/new-account")
    public ResponseEntity<Map<String, Object>> newAccount(@RequestBody JwsRequest jwsRequest,
                                                          HttpServletRequest httpRequest) {
        try {
            // 1. JWS 파싱 및 서명 검증
            AccountCreationResult result = acmeService.createAccount(jwsRequest, httpRequest);

            // 4. 응답 생성
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", result.getAccountUrl()); // kid 역할
            headers.set("Replay-Nonce", result.getReplayNonce());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "valid");
            response.put("contact", result.getContact());
            response.put("orders", result.getAccountUrl() + "/orders");

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
    public ResponseEntity<?> newOrder(@RequestBody JwsRequest jwsRequest,
                                      HttpServletRequest request) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

            OrderCreationResult result = acmeService.createOrder(jwsRequest, baseUrl, request);

            Order order = result.getOrder();
            List<Authorization> authzs = result.getAuthzs();

            Map<String, Object> response = Map.of(
                "status", order.getStatus().getStatus(),
                "expires", order.getExpires().atZone(ZoneOffset.UTC).toInstant(),
                "identifiers", order.getIdentifiers(),
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
    public ResponseEntity<?> getAuthorization(@PathVariable Long id, HttpServletRequest req) {
        String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
        AuthorizationResult result = acmeService.getAuthorization(id, baseUrl);

        Map<String, Object> response = Map.of(
            "identifier", result.getIdentifier(),
            "status", result.getStatus(),
            "expires", result.getExpires(),
            "challenges", result.getChallenges()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Replay-Nonce", nonceStore.generateNonce());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // status가 valid인 경우에만 Link 헤더 추가
        if (result.getUpLink() != null) {
            headers.add("Link", "<" + result.getUpLink() + ">; rel=\"up\"");
        }
        log.info("----Authz----");
        response.forEach((key, value) -> log.info("{}: {}", key, value));

        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }

    @PostMapping("/challenge/{id}")
    public ResponseEntity<?> triggerChallenge(@PathVariable String id,
                                              @RequestBody JwsRequest jwsRequest,
                                              HttpServletRequest req) {
        try {
            String baseUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
            ChallengeResult result = acmeService.triggerChallenge(id, jwsRequest, baseUrl, req);

            Map<String, Object> response = Map.of(
                "type", result.getType(),
                "url", result.getUrl(),
                "status", result.getStatus(),
                "token", result.getToken()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.set("Replay-Nonce", result.getReplayNonce());
            headers.set("Link", "<" + result.getUpLink() + ">; rel=\"up\"");
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
                                           @RequestBody JwsRequest jwsRequest,
                                           HttpServletRequest request
    ) {
        try {
            FinalizeResult result = acmeService.finalizeOrder(orderId, jwsRequest, request);

            Map<String, Object> response = Map.of(
                "status", result.getStatus()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Replay-Nonce", result.getReplayNonce());
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("----Finalize----");
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

    @PostMapping("/order/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId, HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        OrderQueryResult result = acmeService.getOrder(orderId, baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Replay-Nonce", result.getReplayNonce());
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("----Order----");
        result.getBody().forEach((key, value) -> log.info("{}: {}", key, value));

        return new ResponseEntity<>(result.getBody(), headers, HttpStatus.OK);
    }

    @PostMapping("/certificate/{id}")
    public ResponseEntity<String> getCertificate(@PathVariable String id) {
        CertificateResult result = acmeService.getCertificate(id, "");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Replay-Nonce", result.getReplayNonce());
        headers.setContentType(MediaType.valueOf("application/pem-certificate-chain"));

        log.info("----Certificate----");
        log.info("PEM Chain: {}", result.getPemChain());

        return new ResponseEntity<>(result.getPemChain(), headers, HttpStatus.OK);
    }
}
