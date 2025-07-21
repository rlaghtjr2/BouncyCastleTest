package com.nhncloud.pca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.constant.acme.ChallengeStatus;
import com.nhncloud.pca.constant.acme.ChallengeType;
import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.constant.acme.ProblemType;
import com.nhncloud.pca.exception.AcmeProblemException;
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
import com.nhncloud.pca.store.AccountStore;
import com.nhncloud.pca.store.AuthorizationStore;
import com.nhncloud.pca.store.CertStore;
import com.nhncloud.pca.store.ChallengeStore;
import com.nhncloud.pca.store.NonceStore;
import com.nhncloud.pca.store.OrderStore;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

@ExtendWith(MockitoExtension.class)
public class AcmeServiceTest {

    @Mock
    NonceStore nonceStore;

    @Mock
    AccountStore accountStore;

    @Mock
    ChallengeStore challengeStore;

    @Mock
    AuthorizationStore authorizationStore;

    @Mock
    OrderStore orderStore;

    @Mock
    CertStore certStore;

    @InjectMocks
    private AcmeServiceImpl service;

    @BeforeEach
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
        service = new AcmeServiceImpl(nonceStore, accountStore, challengeStore, authorizationStore, orderStore, certStore);
    }

    @Test
    public void testGetDirectory() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8443);

        // Implement the test logic for getDirectory method
        Directory directory = service.getDirectory(mockRequest);

        assertNotNull(directory);
        assertEquals(directory.getNewNonce(), "https://localhost:8443/acme/new-nonce");
        assertEquals(directory.getNewAccount(), "https://localhost:8443/acme/new-account");
        assertEquals(directory.getNewOrder(), "https://localhost:8443/acme/new-order");
        assertEquals(directory.getNewAuthz(), "https://localhost:8443/acme/new-authz");
        assertEquals(directory.getRevokecert(), "https://localhost:8443/acme/revoke-cert");
        assertEquals(directory.getKeyChange(), "https://localhost:8443/acme/key-change");
    }

    @Test
    public void testCreateAccount() {
        // 2. JWS 요청 구성 (JwsRequest 객체 사용)
        JwsRequest jwsRequest = new JwsRequest("fakeProtected", "fakePayload", "fakeSignature");

        // 3. Mock HttpServletRequest
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("https");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8443);

        // 4. NonceStore mock
        when(nonceStore.generateNonce()).thenReturn("mocked-nonce");

        // 5. JwsParseResult 생성 (실제 객체 생성)
        Map<String, Object> protectedHeader = Map.of(
            "nonce", "mocked-nonce"
        );
        Map<String, Object> payload = Map.of(
            "contact", List.of("mailto:hosoek.kim@nhn.com"),
            "termsOfServiceAgreed", true
        );
        RSAKey accountKey = new RSAKey.Builder(
            new Base64URL("somerandomModulusBase64url"),
            new Base64URL("AQAB")  // 일반적인 공개 지수 65537
        )
            .keyID("mock-key-id")
            .build();

        JwsParseResult mockResult = new JwsParseResult(protectedHeader, payload, accountKey);

        // 6. Mock HttpServletRequest attribute 설정
        mockRequest.setAttribute("jwsParseResult", mockResult);

        // 7. 서비스 호출
        AccountCreationResult result = service.createAccount(jwsRequest, mockRequest);

        // 8. 결과 검증
        assertTrue(result.getAccountUrl().contains("https://localhost:8443/acme/acct/"));
        assertEquals(List.of("mailto:hosoek.kim@nhn.com"), result.getContact());
        assertEquals("mocked-nonce", result.getReplayNonce());
    }

    @Test
    public void testCreateOrder() {
        // ----- JWS 요청 시뮬레이션 -----
        JwsRequest jwsRequest = new JwsRequest("base64-protected", "base64-payload", "base64-signature");

        String nonce = "mocked-nonce";
        String baseUrl = "https://localhost:8443";

        // ----- JWS 파싱 결과 구성 (kid 추가) -----
        Map<String, Object> protectedHeader = Map.of(
            "nonce", nonce,
            "kid", "https://localhost:8443/acme/acct/123"
        );
        Map<String, Object> payload = Map.of("identifiers", List.of(Map.of("type", "dns", "value", "example.com")));

        RSAKey mockKey = new RSAKey.Builder(new Base64URL("mock-n"), new Base64URL("AQAB"))
            .keyID("mock-key-id")
            .build();

        JwsParseResult mockResult = new JwsParseResult(protectedHeader, payload, mockKey);

        // ----- Mock 설정 -----
        when(nonceStore.generateNonce()).thenReturn("new-nonce");

        // ----- Challenge, Authz, Order 생성 -----
        Identifier identifier = new Identifier("dns", "example.com");
        Challenge mockChallenge = Challenge.builder()
            .id("456") // 숫자 형태의 ID
            .type(ChallengeType.HTTP_01)
            .url(baseUrl + "/acme/challenge/456")
            .token("tok-abc")
            .status(ChallengeStatus.PENDING)
            .build();

        Authorization mockAuthz = Authorization.builder()
            .id("789") // 숫자 형태의 ID
            .identifier(identifier)
            .status(AuthorizationStatus.PENDING)
            .challenges(List.of(mockChallenge))
            .build();

        // OrderStore의 createOrderWithDatabase Mock 설정
        Order mockOrder = Order.builder()
            .id("123")
            .status(OrderStatus.PENDING)
            .identifiers(List.of(identifier))
            .authorizations(List.of(mockAuthz))
            .expires(LocalDateTime.now().plusMinutes(5))
            .finalize(baseUrl + "/acme/finalize/123")
            .build();

        when(orderStore.createOrderWithDatabase(eq(123L), anyList(), anyList(), eq(baseUrl))).thenReturn(mockOrder);

        // AuthorizationStore의 createAuthorizationWithDatabase Mock 설정
        Authorization mockAuthzWithoutChallenge = Authorization.builder()
            .id("789")
            .identifier(identifier)
            .status(AuthorizationStatus.PENDING)
            .challenges(new ArrayList<>()) // 빈 목록
            .build();
        when(authorizationStore.createAuthorizationWithDatabase(eq(identifier), anyList()))
            .thenReturn(mockAuthzWithoutChallenge);

        // ChallengeStore의 createChallengeWithDatabase Mock 설정
        when(challengeStore.createChallengeWithDatabase(eq(789L), eq(baseUrl))).thenReturn(mockChallenge);

        // ----- Mock HttpServletRequest -----
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("jwsParseResult", mockResult);

        // ----- 서비스 호출 -----
        OrderCreationResult result = service.createOrder(jwsRequest, baseUrl, mockRequest);

        // ----- 검증 -----
        assertEquals("123", result.getOrder().getId()); // DB에서 생성된 ID 사용
        assertEquals(1, result.getAuthzs().size());

        // DB 저장 메서드들이 호출되었는지 검증
        verify(orderStore, times(1)).createOrderWithDatabase(eq(123L), anyList(), anyList(), eq(baseUrl));
        verify(authorizationStore, times(1)).createAuthorizationWithDatabase(eq(identifier), anyList());
        verify(challengeStore, times(1)).createChallengeWithDatabase(eq(789L), eq(baseUrl));
    }

    @Test
    public void testGetAuthorization_성공() {
        // ----- 테스트 데이터 준비 -----
        String authzId = "authz-test-123";
        String baseUrl = "https://localhost:8443";

        Identifier identifier = new Identifier("dns", "example.com");

        Challenge challenge = Challenge.builder()
            .id("challenge-123")
            .type(ChallengeType.HTTP_01)
            .url(baseUrl + "/acme/challenge/challenge-123")
            .token("tok-abc")
            .status(ChallengeStatus.PENDING)
            .build();

        Authorization authorization = Authorization.builder()
            .id(authzId)
            .identifier(identifier)
            .status(AuthorizationStatus.PENDING)
            .challenges(List.of(challenge))
            .build();

        // ----- Mock 설정 -----
        when(authorizationStore.getAuthorization(authzId)).thenReturn(authorization);
        when(nonceStore.generateNonce()).thenReturn("test-nonce");

        // ----- 서비스 호출 -----
        AuthorizationResult result = service.getAuthorization(authzId, baseUrl);

        // ----- 검증 -----
        assertEquals(identifier, result.getIdentifier());
        assertEquals("pending", result.getStatus());
        assertEquals("test-nonce", result.getReplayNonce());
    }

    @Test
    public void testGetAuthorization_notFound() {
        // ----- 테스트 데이터 준비 -----
        String authzId = "non-existent-authz";
        String baseUrl = "https://localhost:8443";

        // ----- Mock 설정 -----
        when(authorizationStore.getAuthorization(authzId)).thenReturn(null);
        when(nonceStore.generateNonce()).thenReturn("test-nonce");

        // ----- 예외 발생 검증 -----
        AcmeProblemException exception = assertThrows(AcmeProblemException.class, () -> {
            service.getAuthorization(authzId, baseUrl);
        });

        assertEquals(ProblemType.MALFORMED, exception.getProblemType());
        assertTrue(exception.getDetail().contains("Authorization"));
    }

    @Test
    void triggerChallenge_success() {
        // ----- 테스트 데이터 준비 -----
        String challengeId = "challenge-123";
        String baseUrl = "https://localhost:8443";
        JwsRequest jwsRequest = new JwsRequest("protected", "payload", "signature");

        // ----- JWS 파싱 결과 구성 -----
        Map<String, Object> protectedHeader = Map.of("nonce", "test-nonce");
        Map<String, Object> payload = Map.of("keyAuthorization", "test-key-auth");
        RSAKey mockKey = new RSAKey.Builder(new Base64URL("mock-n"), new Base64URL("AQAB"))
            .keyID("mock-key-id")
            .build();

        JwsParseResult mockResult = new JwsParseResult(protectedHeader, payload, mockKey);

        // ----- Challenge 구성 -----
        Challenge challenge = Challenge.builder()
            .id(challengeId)
            .type(ChallengeType.HTTP_01)
            .url(baseUrl + "/acme/challenge/" + challengeId)
            .token("tok-abc")
            .status(ChallengeStatus.PENDING)
            .build();

        // ----- Authorization 구성 -----
        Authorization authorization = Authorization.builder()
            .id("authz-123")
            .identifier(new Identifier("dns", "example.com"))
            .status(AuthorizationStatus.PENDING)
            .challenges(List.of(challenge))
            .build();

        // ----- Mock 설정 -----
        when(challengeStore.getChallenge(challengeId)).thenReturn(challenge);
        when(authorizationStore.findAuthorizationByChallengeId(challengeId)).thenReturn(authorization);
        when(nonceStore.generateNonce()).thenReturn("new-nonce");

        // ----- Mock HttpServletRequest -----
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("jwsParseResult", mockResult);

        // ----- 서비스 호출 -----
        ChallengeResult challengeResult = service.triggerChallenge(challengeId, jwsRequest, baseUrl, mockRequest);

        // ----- 검증 -----
        assertEquals(ChallengeType.HTTP_01.getType(), challengeResult.getType());
        assertEquals("new-nonce", challengeResult.getReplayNonce());
        assertEquals(baseUrl + "/acme/challenge/" + challengeId, challengeResult.getUrl());
    }

    @Disabled
    @Test
    void finalizeOrder_success() throws Exception {
        // ----- 테스트 데이터 준비 -----
        String orderId = "order-123";
        JwsRequest jwsRequest = new JwsRequest("protected", "payload", "signature");

        // ----- JWS 파싱 결과 구성 -----
        String mockCsrBase64 = createFakeCsrBase64();
        Map<String, Object> protectedHeader = Map.of("nonce", "test-nonce");
        Map<String, Object> payload = Map.of("csr", mockCsrBase64);
        RSAKey mockKey = new RSAKey.Builder(new Base64URL("mock-n"), new Base64URL("AQAB"))
            .keyID("mock-key-id")
            .build();

        JwsParseResult mockResult = new JwsParseResult(protectedHeader, payload, mockKey);

        // ----- Order 구성 -----
        Order order = Order.builder()
            .id(orderId)
            .status(OrderStatus.READY)
            .build();

        // ----- Mock 설정 -----
        when(orderStore.getOrder(orderId)).thenReturn(order);
        when(orderStore.isDomainAuthorized(orderId, "test.com")).thenReturn(true);
        when(nonceStore.generateNonce()).thenReturn("new-nonce");

        // ----- Mock HttpServletRequest -----
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setAttribute("jwsParseResult", mockResult);

        // ----- 서비스 호출 -----
        FinalizeResult result = service.finalizeOrder(orderId, jwsRequest, mockRequest);

        // ----- 검증 -----
        assertEquals("new-nonce", result.getReplayNonce());
    }

    @Test
    void getCertificate_success() throws Exception {
        // ----- 테스트 데이터 준비 -----
        String certId = "cert-123";
        String baseUrl = "https://localhost:8443";

        // ----- Mock 설정 -----
        X509Certificate testCert = generateTestCert();
        when(certStore.get(certId)).thenReturn(testCert);

        // ----- 서비스 호출 -----
        CertificateResult result = service.getCertificate(certId, baseUrl);

        // ----- 검증 -----
        assertNotNull(result);
        assertNotNull(result.getPemChain());
        assertTrue(result.getPemChain().contains("-----BEGIN CERTIFICATE-----"));
        assertTrue(result.getPemChain().contains("-----END CERTIFICATE-----"));
    }

    @Test
    void getCertificate_notFound() {
        // ----- 테스트 데이터 준비 -----
        String certId = "non-existent-cert";
        String baseUrl = "https://localhost:8443";

        // ----- Mock 설정 -----
        when(certStore.get(certId)).thenReturn(null);

        // ----- 예외 발생 검증 -----
        AcmeProblemException exception = assertThrows(AcmeProblemException.class, () -> {
            service.getCertificate(certId, baseUrl);
        });

        assertEquals(ProblemType.SERVER_INTERNAL, exception.getProblemType());
        assertTrue(exception.getDetail().contains("Certificate"));
    }

    private X509Certificate generateTestCert() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name("CN=test.com");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        java.util.Date notBefore = new java.util.Date();
        java.util.Date notAfter = new java.util.Date(notBefore.getTime() + 365 * 24 * 60 * 60 * 1000L);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
            subject, serial, notBefore, notAfter, subject, keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private String createFakeCsrBase64() {
        // 실제 Base64 인코딩된 CSR 대신 테스트용 가짜 데이터 사용
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            X500Principal subject = new X500Principal("CN=test.com");
            PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
            PKCS10CertificationRequest csr = builder.build(signer);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(csr.getEncoded());
        } catch (Exception e) {
            // 실패 시 가짜 데이터 반환
            return "fake-csr-base64-data";
        }
    }
}
