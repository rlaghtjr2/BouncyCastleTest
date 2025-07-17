package com.nhncloud.pca.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.constant.KeyAlgorithm;
import com.nhncloud.pca.entity.acme.AcmeAccountEntity;
import com.nhncloud.pca.model.response.AccountKeyResponse;
import com.nhncloud.pca.repository.acme.AcmeAccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountKeyServiceTest {

    @Mock
    private AcmeAccountRepository acmeAccountRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AccountKeyServiceImpl accountKeyService;

    private Long testCaId = 1L;

    @BeforeEach
    void setUp() throws Exception {
        // Mock 설정은 각 테스트 메서드에서 개별적으로 설정
    }

    // ========== RSA 알고리즘 테스트 ==========

    @Test
    @DisplayName("RSA 알고리즘으로 특정 키 크기(2048)로 Account 키 생성 및 저장")
    void testGenerateAndSaveAccountKey_RSA_KeySize2048() throws Exception {
        // Given
        KeyAlgorithm algorithm = KeyAlgorithm.RSA;
        int keySize = 2048;

        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"RSA\",\"modulus\":\"test\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // When
        AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(algorithm, keySize, testCaId);

        // Then
        assertNotNull(response);
        assertEquals("RSA", response.getKeyType());
        assertRsaKeyFields(response);

        // DB 저장 확인
        verify(objectMapper, times(1)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(1)).save(any(AcmeAccountEntity.class));
    }

    @Test
    @DisplayName("RSA 알고리즘으로 특정 키 크기(4096)로 Account 키 생성 및 저장")
    void testGenerateAndSaveAccountKey_RSA_KeySize4096() throws Exception {
        // Given
        KeyAlgorithm algorithm = KeyAlgorithm.RSA;
        int keySize = 4096;

        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"RSA\",\"modulus\":\"test\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // When
        AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(algorithm, keySize, testCaId);

        // Then
        assertNotNull(response);
        assertEquals("RSA", response.getKeyType());
        assertRsaKeyFields(response);
        assertTrue(response.getModulus().length() > 500);

        // DB 저장 확인
        verify(objectMapper, times(1)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(1)).save(any(AcmeAccountEntity.class));
    }

    // ========== EC 알고리즘 테스트 ==========

    @Test
    @DisplayName("EC 알고리즘으로 P-256 키 생성 및 저장")
    void testGenerateAndSaveAccountKey_EC_P256() throws Exception {
        // Given
        KeyAlgorithm algorithm = KeyAlgorithm.EC;
        int keySize = 256;

        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"EC\",\"curve\":\"P-256\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // When
        AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(algorithm, keySize, testCaId);

        // Then
        assertNotNull(response);
        assertEquals("EC", response.getKeyType());
        assertEquals("P-256", response.getCurve());
        assertEcKeyFields(response);

        // DB 저장 확인
        verify(objectMapper, times(1)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(1)).save(any(AcmeAccountEntity.class));
    }

    @Test
    @DisplayName("EC 알고리즘으로 P-384 키 생성 및 저장")
    void testGenerateAndSaveAccountKey_EC_P384() throws Exception {
        // Given
        KeyAlgorithm algorithm = KeyAlgorithm.EC;
        int keySize = 384;

        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"EC\",\"curve\":\"P-384\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // When
        AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(algorithm, keySize, testCaId);

        // Then
        assertNotNull(response);
        assertEquals("EC", response.getKeyType());
        assertEquals("P-384", response.getCurve());
        assertEcKeyFields(response);

        // DB 저장 확인
        verify(objectMapper, times(1)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(1)).save(any(AcmeAccountEntity.class));
    }

    @Test
    @DisplayName("EC 알고리즘으로 P-521 키 생성 및 저장")
    void testGenerateAndSaveAccountKey_EC_P521() throws Exception {
        // Given
        KeyAlgorithm algorithm = KeyAlgorithm.EC;
        int keySize = 521;

        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"EC\",\"curve\":\"P-521\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // When
        AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(algorithm, keySize, testCaId);

        // Then
        assertNotNull(response);
        assertEquals("EC", response.getKeyType());
        assertEquals("P-521", response.getCurve());
        assertEcKeyFields(response);

        // DB 저장 확인
        verify(objectMapper, times(1)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(1)).save(any(AcmeAccountEntity.class));
    }

    // ========== 유효성 검증 테스트 ==========

    @Test
    @DisplayName("여러 번 키 생성 시 서로 다른 키가 생성되는지 검증")
    void testGenerateAndSaveAccountKey_UniquenessCheck() throws Exception {
        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"RSA\",\"modulus\":\"test\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // Given & When
        AccountKeyResponse response1 = accountKeyService.generateAndSaveAccountKey(KeyAlgorithm.RSA, 2048, testCaId);
        AccountKeyResponse response2 = accountKeyService.generateAndSaveAccountKey(KeyAlgorithm.RSA, 2048, testCaId);

        // Then
        assertNotNull(response1);
        assertNotNull(response2);

        // 서로 다른 키가 생성되어야 함
        assertNotEquals(response1.getModulus(), response2.getModulus());
        assertNotEquals(response1.getPrivateExponent(), response2.getPrivateExponent());
        assertNotEquals(response1.getFirstPrimeFactor(), response2.getFirstPrimeFactor());
        assertNotEquals(response1.getSecondPrimeFactor(), response2.getSecondPrimeFactor());

        // DB 저장이 두 번 호출되었는지 확인
        verify(objectMapper, times(2)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(2)).save(any(AcmeAccountEntity.class));
    }

    // ========== 예외 처리 테스트 ==========

    @Test
    @DisplayName("지원되지 않는 RSA 키 크기로 생성 시 예외 발생")
    void testGenerateAndSaveAccountKey_UnsupportedRSAKeySize() {
        // Given
        KeyAlgorithm algorithm = KeyAlgorithm.RSA;
        int unsupportedKeySize = 512;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            accountKeyService.generateAndSaveAccountKey(algorithm, unsupportedKeySize, testCaId);
        });
    }

    @Test
    @DisplayName("지원되지 않는 EC 키 크기로 생성 시 예외 발생")
    void testGenerateAndSaveAccountKey_UnsupportedECKeySize() {
        // Given
        KeyAlgorithm algorithm = KeyAlgorithm.EC;
        int unsupportedKeySize = 128;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            accountKeyService.generateAndSaveAccountKey(algorithm, unsupportedKeySize, testCaId);
        });
    }

    @Test
    @DisplayName("JSON 변환 실패 시 예외 발생")
    void testGenerateAndSaveAccountKey_JsonProcessingException() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON processing failed") {});

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            accountKeyService.generateAndSaveAccountKey(KeyAlgorithm.RSA, 2048, testCaId);
        });
    }

    // ========== 성능 테스트 ==========

    @Test
    @DisplayName("RSA 키 생성 및 저장 성능 테스트")
    void testGenerateAndSaveAccountKey_RSA_PerformanceTest() throws Exception {
        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"RSA\",\"modulus\":\"test\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // Given
        long startTime = System.currentTimeMillis();

        // When
        AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(KeyAlgorithm.RSA, 2048, testCaId);

        // Then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertNotNull(response);
        assertTrue(duration < 10000, "RSA key generation and saving took too long: " + duration + "ms");

        // DB 저장 확인
        verify(objectMapper, times(1)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(1)).save(any(AcmeAccountEntity.class));
    }

    @Test
    @DisplayName("EC 키 생성 및 저장 성능 테스트")
    void testGenerateAndSaveAccountKey_EC_PerformanceTest() throws Exception {
        // Mock 설정
        when(objectMapper.writeValueAsString(any(AccountKeyResponse.class)))
                .thenReturn("{\"keyType\":\"EC\",\"curve\":\"P-256\"}");
        when(acmeAccountRepository.save(any(AcmeAccountEntity.class)))
                .thenReturn(new AcmeAccountEntity());

        // Given
        long startTime = System.currentTimeMillis();

        // When
        AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(KeyAlgorithm.EC, 256, testCaId);

        // Then
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertNotNull(response);
        assertTrue(duration < 5000, "EC key generation and saving took too long: " + duration + "ms");

        // DB 저장 확인
        verify(objectMapper, times(1)).writeValueAsString(any(AccountKeyResponse.class));
        verify(acmeAccountRepository, times(1)).save(any(AcmeAccountEntity.class));
    }

    // ========== 헬퍼 메소드 ==========

    private void assertRsaKeyFields(AccountKeyResponse response) {
        assertNotNull(response.getModulus());
        assertNotNull(response.getExponent());
        assertNotNull(response.getPrivateExponent());
        assertNotNull(response.getFirstPrimeFactor());
        assertNotNull(response.getSecondPrimeFactor());
        assertNotNull(response.getFirstFactorCrtExponent());
        assertNotNull(response.getSecondFactorCrtExponent());
        assertNotNull(response.getFirstCrtCoefficient());

        // EC 필드들은 null이어야 함
        assertNull(response.getCurve());
        assertNull(response.getXCoordinate());
        assertNull(response.getYCoordinate());

        // JWK 형태 검증
        assertFalse(response.getModulus().isEmpty());
        assertFalse(response.getExponent().isEmpty());
        assertFalse(response.getPrivateExponent().isEmpty());

        // exponent는 일반적으로 "AQAB" (65537)
        assertEquals("AQAB", response.getExponent());

        // Base64 URL 인코딩 검증
        assertDoesNotThrow(() -> {
            Base64.getUrlDecoder().decode(response.getModulus());
            Base64.getUrlDecoder().decode(response.getExponent());
            Base64.getUrlDecoder().decode(response.getPrivateExponent());
        });
    }

    private void assertEcKeyFields(AccountKeyResponse response) {
        assertNotNull(response.getCurve());
        assertNotNull(response.getXCoordinate());
        assertNotNull(response.getYCoordinate());
        assertNotNull(response.getPrivateExponent());

        // RSA 필드들은 null이어야 함
        assertNull(response.getModulus());
        assertNull(response.getExponent());
        assertNull(response.getFirstPrimeFactor());
        assertNull(response.getSecondPrimeFactor());
        assertNull(response.getFirstFactorCrtExponent());
        assertNull(response.getSecondFactorCrtExponent());
        assertNull(response.getFirstCrtCoefficient());

        // JWK 형태 검증
        assertFalse(response.getCurve().isEmpty());
        assertFalse(response.getXCoordinate().isEmpty());
        assertFalse(response.getYCoordinate().isEmpty());
        assertFalse(response.getPrivateExponent().isEmpty());

        // Base64 URL 인코딩 검증
        assertDoesNotThrow(() -> {
            Base64.getUrlDecoder().decode(response.getXCoordinate());
            Base64.getUrlDecoder().decode(response.getYCoordinate());
            Base64.getUrlDecoder().decode(response.getPrivateExponent());
        });
    }
}