package com.nhncloud.pca.service;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
import com.nimbusds.jose.util.Base64URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        service = new AcmeServiceImpl(nonceStore, accountStore, challengeStore, authorizationStore, orderStore, certStore);
    }

    @Test
    public void testGetDirectory() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getServerName()).thenReturn("localhost");
        when(mockRequest.getServerPort()).thenReturn(8443);

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
    public void testGetNonce() {
        // Mock the nonceStore to return a specific nonce
        String expectedNonce = "test-nonce";
        when(nonceStore.generateNonce()).thenReturn(expectedNonce);

        // Call the getNonce method
        String actualNonce = service.getNonce();

        // Verify the result
        assertEquals(expectedNonce, actualNonce);
    }

    @Test
    public void testCreateAccount() {
        // 2. JWS 요청 구성 (dummy base64url strings)
        Map<String, String> jwsRequest = Map.of(
            "protected", "fakeProtected",
            "payload", "fakePayload",
            "signature", "fakeSignature"
        );

        // 3. Mock HttpServletRequest
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("https");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8443);

        // 4. NonceStore mock
        when(nonceStore.consumeNonce("mocked-nonce")).thenReturn(true);
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

        JwsUtils.JwsParseResult mockResult = new JwsUtils.JwsParseResult(protectedHeader, payload, accountKey);

        // 6. JwsUtils.parseAndVerifyJws static mock
        try (MockedStatic<JwsUtils> mockedStatic = mockStatic(JwsUtils.class)) {
            mockedStatic.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(mockResult);

            // 7. 서비스 호출
            AccountCreationResult result = service.createAccount(jwsRequest, mockRequest);

            // 8. 결과 검증
            assertTrue(result.getAccountUrl().contains("https://localhost:8443/acme/acct/"));
            assertEquals(List.of("mailto:hosoek.kim@nhn.com"), result.getContact());
            assertEquals("mocked-nonce", result.getReplayNonce());
        }
    }

    @Test
    public void testCreateAccount_invalidNonce() {
        // 1. Dummy JWS 요청
        Map<String, String> jwsRequest = Map.of(
            "protected", "fakeProtected",
            "payload", "fakePayload",
            "signature", "fakeSignature"
        );

        // 2. Mock HttpServletRequest
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("https");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8443);

        // 3. NonceStore mock: consume 실패 시뮬레이션
        when(nonceStore.consumeNonce("mocked-nonce")).thenReturn(false);

        // 4. JwsParseResult 구성
        Map<String, Object> protectedHeader = Map.of("nonce", "mocked-nonce");
        Map<String, Object> payload = Map.of(
            "contact", List.of("mailto:hosoek.kim@nhn.com"),
            "termsOfServiceAgreed", true
        );
        RSAKey accountKey = new RSAKey.Builder(
            new Base64URL("somerandomModulusBase64url"),
            new Base64URL("AQAB")
        ).keyID("mock-key-id").build();

        JwsUtils.JwsParseResult mockResult = new JwsUtils.JwsParseResult(protectedHeader, payload, accountKey);

        // 5. Static mock 설정
        try (MockedStatic<JwsUtils> mockedStatic = mockStatic(JwsUtils.class)) {
            mockedStatic.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(mockResult);

            // 6. 예외 발생 및 메시지 검증
            AcmeProblemException ex = assertThrows(
                AcmeProblemException.class,
                () -> service.createAccount(jwsRequest, mockRequest)
            );

            assertEquals("The request did not include a valid nonce.", ex.getDetail());
            assertEquals(ProblemType.BAD_NONCE, ex.getProblemType());
        }
    }


    @Test
    public void testCreateOrder() {
        // ----- JWS 요청 시뮬레이션 -----
        Map<String, String> jwsRequest = Map.of(
            "protected", "base64-protected",
            "payload", "base64-payload",
            "signature", "base64-signature"
        );

        String nonce = "mocked-nonce";
        String baseUrl = "https://localhost:8443";

        // ----- JWS 파싱 결과 구성 -----
        Map<String, Object> protectedHeader = Map.of("nonce", nonce);
        Map<String, Object> payload = Map.of("identifiers", List.of(Map.of("type", "dns", "value", "example.com")));

        RSAKey mockKey = new RSAKey.Builder(new Base64URL("mock-n"), new Base64URL("AQAB"))
            .keyID("mock-key-id")
            .build();

        JwsUtils.JwsParseResult mockResult = new JwsUtils.JwsParseResult(protectedHeader, payload, mockKey);

        // ----- nonceStore 동작 정의 -----
        when(nonceStore.consumeNonce(nonce)).thenReturn(true);
        when(nonceStore.generateNonce()).thenReturn("new-nonce");

        // ----- Challenge, Authz, Order 생성 -----
        Identifier identifier = new Identifier("dns", "example.com");
        Challenge mockChallenge = Challenge.builder()
            .id("challenge-123")
            .type(ChallengeType.HTTP_01)
            .url(baseUrl + "/acme/challenge/challenge-123")
            .token("tok-abc")
            .status(ChallengeStatus.PENDING)
            .build();

        Authorization mockAuthz = Authorization.builder()
            .id("authz-123")
            .identifier(identifier)
            .status(AuthorizationStatus.PENDING)
            .challenges(List.of(mockChallenge))
            .build();

        when(challengeStore.createChallenge(baseUrl)).thenReturn(mockChallenge);
        when(authorizationStore.createAuthorization(eq(identifier), anyList())).thenReturn(mockAuthz);

        Order mockOrder = Order.builder()
            .id("order-abc")
            .status(OrderStatus.PENDING)
            .expires(LocalDateTime.now().plusDays(7))
            .identifiers(List.of(identifier))
            .authorizations(List.of(mockAuthz))
            .finalize(baseUrl + "/acme/finalize/order-abc")
            .build();

        when(orderStore.createOrder(any(), any(), eq(baseUrl))).thenReturn(mockOrder);

        // ----- static mock: JwsUtils.parseAndVerifyJws -----
        try (MockedStatic<JwsUtils> mockedStatic = mockStatic(JwsUtils.class)) {
            mockedStatic.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(mockResult);

            // ----- 서비스 호출 -----
            OrderCreationResult result = service.createOrder(jwsRequest, baseUrl);

            // ----- 검증 -----
            assertEquals("order-abc", result.getOrder().getId());
            assertEquals(1, result.getAuthzs().size());
            assertEquals("authz-123", result.getAuthzs().get(0).getId());
            assertEquals("new-nonce", result.getReplayNonce());
            assertEquals(nonce, result.getOriginalNonce());
        }
    }

    @Test
    public void testGetAuthorization_성공() {
        // given
        String authzId = "authz-123";
        String baseUrl = "https://localhost:8443";

        // identifier
        Identifier identifier = new Identifier("dns", "example.com");

        // challenge
        Challenge challenge = Challenge.builder()
            .id("chall-1")
            .type(ChallengeType.HTTP_01)
            .status(ChallengeStatus.PENDING)
            .url(baseUrl + "/acme/challenge/chall-1")
            .token("tok-abc")
            .build();

        // authz
        Authorization authz = Authorization.builder()
            .id(authzId)
            .identifier(identifier)
            .status(AuthorizationStatus.VALID) // important
            .expires(LocalDateTime.now().plusDays(7))
            .challenges(List.of(challenge))
            .build();

        when(authorizationStore.getAuthorization(authzId)).thenReturn(authz);
        when(nonceStore.generateNonce()).thenReturn("mock-nonce");

        Order parentOrder = Order.builder()
            .id("order-xyz")
            .build();
        when(orderStore.findOrderByAuthzId(authzId)).thenReturn(parentOrder);

        // when
        AuthorizationResult result = service.getAuthorization(authzId, baseUrl);

        // then
        assertEquals(identifier, result.getIdentifier());
        assertEquals("valid", result.getStatus());
        assertEquals(1, result.getChallenges().size());
        assertEquals("mock-nonce", result.getReplayNonce());
        assertEquals(baseUrl + "/acme/order/order-xyz", result.getUpLink());
    }

    @Test
    public void testGetAuthorization_notFound() {
        // given

        when(authorizationStore.getAuthorization("not-exist")).thenReturn(null);

        // when & then
        AcmeProblemException ex = assertThrows(
            AcmeProblemException.class,
            () -> service.getAuthorization("not-exist", "https://localhost:8443")
        );
        assertEquals(ProblemType.MALFORMED, ex.getProblemType());
        assertEquals("Authorization resource with ID 'not-exist' was not found", ex.getDetail());
    }

    @Test
    void triggerChallenge_success() {
        // GIVEN
        String challengeId = "chall-1";
        String authzId = "authz-1";
        String baseUrl = "https://localhost:8443";

        Map<String, String> jwsRequest = Map.of(
            "protected", "base64-protected",
            "payload", "base64-payload",
            "signature", "base64-signature"
        );

        Map<String, Object> protectedHeader = Map.of("nonce", "nonce-123");
        Map<String, Object> payload = Map.of();

        RSAKey mockKey = new RSAKey.Builder(new Base64URL("mock-n"), new Base64URL("AQAB")).build();
        JwsUtils.JwsParseResult result = new JwsUtils.JwsParseResult(protectedHeader, payload, mockKey);

        Challenge challenge = Challenge.builder()
            .id(challengeId)
            .type(ChallengeType.HTTP_01)
            .status(ChallengeStatus.PENDING)
            .token("tok-abc")
            .url(baseUrl + "/acme/challenge/" + challengeId)
            .build();

        Authorization authz = Authorization.builder()
            .id(authzId)
            .build();

        when(nonceStore.consumeNonce("nonce-123")).thenReturn(true);
        when(nonceStore.generateNonce()).thenReturn("new-nonce");
        when(challengeStore.getChallenge(challengeId)).thenReturn(challenge);
        when(authorizationStore.findAuthorizationByChallengeId(challengeId)).thenReturn(authz);

        try (MockedStatic<JwsUtils> staticMock = mockStatic(JwsUtils.class)) {
            staticMock.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(result);

            // WHEN
            ChallengeResult challengeResult = service.triggerChallenge(challengeId, jwsRequest, baseUrl);

            // THEN
            assertEquals("http-01", challengeResult.getType());
            assertEquals("valid", challengeResult.getStatus());
            assertEquals("tok-abc", challengeResult.getToken());
            assertEquals("new-nonce", challengeResult.getReplayNonce());
            assertEquals(baseUrl + "/acme/authz/" + authzId, challengeResult.getUpLink());

            verify(challengeStore).markValid(challengeId);
            verify(authorizationStore).markValid(authzId);
        }
    }

    @Test
    void triggerChallenge_invalidNonce() {
        // GIVEN
        String challengeId = "chall-1";
        Map<String, String> jwsRequest = Map.of(
            "protected", "base64-protected",
            "payload", "base64-payload",
            "signature", "base64-signature"
        );
        Map<String, Object> protectedHeader = Map.of("nonce", "bad-nonce");
        Map<String, Object> payload = Map.of();

        RSAKey mockKey = new RSAKey.Builder(new Base64URL("mock-n"), new Base64URL("AQAB")).build();
        JwsUtils.JwsParseResult result = new JwsUtils.JwsParseResult(protectedHeader, payload, mockKey);

        when(nonceStore.consumeNonce("bad-nonce")).thenReturn(false);


        try (MockedStatic<JwsUtils> staticMock = mockStatic(JwsUtils.class)) {
            staticMock.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(result);

            // WHEN / THEN
            AcmeProblemException ex = assertThrows(
                AcmeProblemException.class,
                () -> service.triggerChallenge(challengeId, jwsRequest, "https://localhost:8443")
            );
            assertEquals("The request did not include a valid nonce.", ex.getDetail());
            assertEquals(ProblemType.BAD_NONCE, ex.getProblemType());
        }
    }

    @Test
    void triggerChallenge_challengeNotFound() {
        // GIVEN
        String challengeId = "unknown";
        Map<String, String> jwsRequest = Map.of(
            "protected", "base64-protected",
            "payload", "base64-payload",
            "signature", "base64-signature"
        );
        Map<String, Object> protectedHeader = Map.of("nonce", "nonce-abc");
        Map<String, Object> payload = Map.of();

        RSAKey mockKey = new RSAKey.Builder(new Base64URL("mock-n"), new Base64URL("AQAB")).build();
        JwsUtils.JwsParseResult result = new JwsUtils.JwsParseResult(protectedHeader, payload, mockKey);

        when(nonceStore.consumeNonce("nonce-abc")).thenReturn(true);
        when(challengeStore.getChallenge(challengeId)).thenReturn(null);

        try (MockedStatic<JwsUtils> staticMock = mockStatic(JwsUtils.class)) {
            staticMock.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(result);

            // WHEN / THEN
            AcmeProblemException ex = assertThrows(
                AcmeProblemException.class,
                () -> service.triggerChallenge(challengeId, jwsRequest, "https://localhost:8443")
            );
            assertEquals(ProblemType.MALFORMED, ex.getProblemType());
            assertEquals("Challenge resource with ID 'unknown' was not found", ex.getDetail());
        }
    }

    @Test
    void finalizeOrder_success() throws Exception {
        // GIVEN
        String orderId = "order-123";
        String base64Csr = createFakeCsrBase64(); // 아래 함수 참고
        Map<String, String> jwsRequest = Map.of(
            "protected", "p",
            "payload", "pl",
            "signature", "s"
        );

        Map<String, Object> payload = Map.of("csr", base64Csr);
        Map<String, Object> protectedHeader = Map.of("nonce", "nonce-xyz");

        RSAKey mockKey = new RSAKey.Builder(new Base64URL("n"), new Base64URL("AQAB")).build();
        JwsUtils.JwsParseResult parseResult = new JwsUtils.JwsParseResult(protectedHeader, payload, mockKey);

        // Mock dependencies

        when(orderStore.isDomainAuthorized(orderId, "test.local")).thenReturn(true);
        when(nonceStore.generateNonce()).thenReturn("new-nonce");

        X509Certificate mockCert = mock(X509Certificate.class);

        try (
            MockedStatic<JwsUtils> jwsMock = mockStatic(JwsUtils.class);
            MockedStatic<BouncyCastleUtil> bcMock = mockStatic(BouncyCastleUtil.class);
            MockedStatic<CertificateUtil> certMock = mockStatic(CertificateUtil.class)
        ) {
            jwsMock.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(parseResult);

            bcMock.when(() -> BouncyCastleUtil.extractCommonName(any(PKCS10CertificationRequest.class)))
                .thenReturn("test.local");

            bcMock.when(() -> BouncyCastleUtil.generateSelfSignedCert(any()))
                .thenReturn(mockCert);

            certMock.when(() -> CertificateUtil.toPemString(mockCert))
                .thenReturn("-----BEGIN CERTIFICATE-----MOCK-----END CERTIFICATE-----");

            // WHEN
            FinalizeResult result = service.finalizeOrder(orderId, jwsRequest);

            // THEN
            assertEquals("valid", result.getStatus());
            assertEquals("new-nonce", result.getReplayNonce());

            verify(certStore).save(anyString(), eq(mockCert));
            verify(orderStore).finalizeOrder(eq(orderId), anyString(), any(), any());
        }
    }

    @Test
    void finalizeOrder_unauthorizedDomain() {
        // GIVEN
        String orderId = "order-unauth";
        String base64Csr = createFakeCsrBase64();
        Map<String, String> jwsRequest = Map.of("protected", "p", "payload", "pl", "signature", "s");

        Map<String, Object> payload = Map.of("csr", base64Csr);
        Map<String, Object> protectedHeader = Map.of("nonce", "n");

        RSAKey mockKey = new RSAKey.Builder(new Base64URL("n"), new Base64URL("AQAB")).build();
        JwsUtils.JwsParseResult parseResult = new JwsUtils.JwsParseResult(protectedHeader, payload, mockKey);

        try (
            MockedStatic<JwsUtils> jwsMock = mockStatic(JwsUtils.class);
            MockedStatic<BouncyCastleUtil> bcMock = mockStatic(BouncyCastleUtil.class)
        ) {
            jwsMock.when(() -> JwsUtils.parseAndVerifyJws(jwsRequest, accountStore))
                .thenReturn(parseResult);

            bcMock.when(() -> BouncyCastleUtil.extractCommonName(any(PKCS10CertificationRequest.class)))
                .thenReturn("test.local");

            // WHEN / THEN
            // WHEN / THEN
            AcmeProblemException ex = assertThrows(
                AcmeProblemException.class,
                () -> service.finalizeOrder(orderId, jwsRequest)
            );
            assertEquals(ProblemType.MALFORMED, ex.getProblemType());
            assertEquals("domain 'test.local' is not authorized for order 'order-unauth'", ex.getDetail());
        }
    }

    @Test
    void getOrder_success() {
        // GIVEN
        String orderId = "order-123";
        String baseUrl = "https://localhost:8443";

        // Mock Order
        Order mockOrder = mock(Order.class);
        Identifier identifier = new Identifier("dns", "test.local");

        Authorization authz = mock(Authorization.class);
        when(authz.getId()).thenReturn("authz-1");

        when(mockOrder.getStatus()).thenReturn(OrderStatus.READY);
        when(mockOrder.getExpires()).thenReturn(ZonedDateTime.now().plusDays(7).toLocalDateTime());
        when(mockOrder.getIdentifiers()).thenReturn(List.of(identifier));
        when(mockOrder.getAuthorizations()).thenReturn(List.of(authz));
        when(mockOrder.getFinalize()).thenReturn(baseUrl + "/acme/order/" + orderId + "/finalize");

        when(orderStore.getOrder(orderId)).thenReturn(mockOrder);
        when(nonceStore.generateNonce()).thenReturn("replay-nonce-123");

        // WHEN
        OrderQueryResult result = service.getOrder(orderId, baseUrl);

        // THEN
        assertNotNull(result);
        assertEquals("replay-nonce-123", result.getReplayNonce());

        Map<String, Object> body = result.getBody();
        assertEquals("ready", body.get("status"));  // READY 상태
        assertEquals(List.of(identifier), body.get("identifiers"));
        assertEquals(List.of(baseUrl + "/acme/authz/authz-1"), body.get("authorizations"));
        assertEquals(baseUrl + "/acme/order/" + orderId + "/finalize", body.get("finalize"));

        verify(orderStore).markReadyIfAllAuthzValid(mockOrder, authorizationStore);
    }

    @Test
    void getOrder_notFound() {
        // GIVEN
        String orderId = "invalid-order";
        String baseUrl = "https://localhost";

        when(orderStore.getOrder(orderId)).thenReturn(null);

        // WHEN / THEN
        // WHEN / THEN
        AcmeProblemException ex = assertThrows(
            AcmeProblemException.class,
            () -> service.getOrder(orderId, baseUrl)
        );
        assertEquals(ProblemType.MALFORMED, ex.getProblemType());
        assertEquals("Order resource with ID 'invalid-order' was not found", ex.getDetail());
    }

    @Test
    void getCertificate_success() throws Exception {
        // GIVEN
        String certId = "cert-123";
        X509Certificate mockCert = generateTestCert();
        when(certStore.get(certId)).thenReturn(mockCert);
        when(nonceStore.generateNonce()).thenReturn("nonce-abc");

        // WHEN
        CertificateResult result = service.getCertificate(certId);

        // THEN
        assertNotNull(result);
        assertTrue(result.getPemChain().contains("BEGIN CERTIFICATE"));
        assertEquals("nonce-abc", result.getReplayNonce());

        verify(certStore).get(certId);
        verify(nonceStore).generateNonce();
    }

    @Test
    void getCertificate_notFound() {
        // GIVEN
        String certId = "missing-cert";
        when(certStore.get(certId)).thenReturn(null);

        // WHEN / THEN
        AcmeProblemException ex = assertThrows(
            AcmeProblemException.class,
            () -> service.getCertificate(certId)
        );
        assertEquals(ProblemType.MALFORMED, ex.getProblemType());
        assertEquals("Certificate resource with ID 'missing-cert' was not found", ex.getDetail());
    }

    private X509Certificate generateTestCert() throws Exception {
        // 인증서 생성
        byte[] csrBytes = Base64.getUrlDecoder().decode(createFakeCsrBase64());
        X509Certificate cert = BouncyCastleUtil.generateSelfSignedCert(new PKCS10CertificationRequest(csrBytes));
        return cert;
    }

    // 🔧 테스트용 CSR 생성
    private String createFakeCsrBase64() {
        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name("CN=test.local");
        ContentSigner signer = null;
        try {
            signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }

        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        PKCS10CertificationRequest csr = builder.build(signer);

        byte[] derEncoded = null; // DER 형식
        try {
            derEncoded = csr.getEncoded();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(derEncoded);
    }
}
