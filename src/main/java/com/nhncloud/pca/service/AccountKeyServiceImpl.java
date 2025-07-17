package com.nhncloud.pca.service;

import static com.nimbusds.jose.jwk.Curve.P_256;
import static com.nimbusds.jose.jwk.Curve.P_384;
import static com.nimbusds.jose.jwk.Curve.P_521;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.constant.KeyAlgorithm;
import com.nhncloud.pca.constant.acme.AccountStatus;
import com.nhncloud.pca.entity.acme.AcmeAccountEntity;
import com.nhncloud.pca.model.response.AccountKeyResponse;
import com.nhncloud.pca.repository.acme.AcmeAccountRepository;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;

@Service
public class AccountKeyServiceImpl implements AccountKeyService {

    @Autowired
    private AcmeAccountRepository acmeAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public AccountKeyResponse generateAndSaveAccountKey(KeyAlgorithm algorithm, int keySize, Long caId) {
        String creationUser = "hoseok"; // 임시 사용자 설정

        // 키 생성
        AccountKeyResponse keyResponse = generateAccountKey(algorithm, keySize);

        // DB에 저장
        saveAccountToDatabase(keyResponse, caId, creationUser);

        return keyResponse;
    }

    private AccountKeyResponse generateAccountKey(KeyAlgorithm algorithm, int keySize) {
        // 키 크기 검증
        if (!algorithm.isSupportedKeySize(keySize)) {
            throw new IllegalArgumentException(
                String.format("Unsupported key size %d for algorithm %s. Supported sizes: %s",
                    keySize, algorithm.getAlgorithmName(),
                    Arrays.toString(algorithm.getSupportedKeySizes())));
        }

        switch (algorithm) {
            case RSA:
                return generateRsaKey(keySize);
            case EC:
                return generateEcKey(keySize);
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    private AccountKeyResponse generateRsaKey(int keySize) {
        try {
            // 1. RSA 키 쌍 생성
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // 2. RSA 키 쌍을 RSAKey 객체로 변환
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .build();

            // 3. JWK 형태로 변환
            return new AccountKeyResponse(
                rsaKey.getKeyType().getValue(),                    // kty
                rsaKey.getModulus().toString(),                    // n
                rsaKey.getPublicExponent().toString(),             // e
                rsaKey.getPrivateExponent().toString(),            // d
                rsaKey.getFirstPrimeFactor().toString(),           // p
                rsaKey.getSecondPrimeFactor().toString(),          // q
                rsaKey.getFirstFactorCRTExponent().toString(),     // dp
                rsaKey.getSecondFactorCRTExponent().toString(),    // dq
                rsaKey.getFirstCRTCoefficient().toString()         // qi
            );

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not available", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key", e);
        }
    }

    private AccountKeyResponse generateEcKey(int keySize) {
        try {
            // 1. EC 곡선 결정
            String curveName = getCurveName(keySize);

            // 2. EC 키 쌍 생성
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(curveName);
            keyPairGenerator.initialize(ecSpec);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // 3. EC 키 쌍을 ECKey 객체로 변환
            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

            ECKey ecKey = new ECKey.Builder(getCurveFromKeySize(keySize), publicKey)
                    .privateKey(privateKey)
                    .build();

            // 4. JWK 형태로 변환
            return new AccountKeyResponse(
                ecKey.getKeyType().getValue(),                     // kty
                ecKey.getCurve().getName(),                        // crv
                ecKey.getX().toString(),                           // x
                ecKey.getY().toString(),                           // y
                ecKey.getD().toString()                            // d
            );

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("EC algorithm not available", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate EC key", e);
        }
    }

    private String getCurveName(int keySize) {
        switch (keySize) {
            case 256:
                return "secp256r1"; // P-256
            case 384:
                return "secp384r1"; // P-384
            case 521:
                return "secp521r1"; // P-521
            default:
                throw new IllegalArgumentException("Unsupported EC key size: " + keySize);
        }
    }

    private Curve getCurveFromKeySize(int keySize) {
        switch (keySize) {
            case 256:
                return P_256;
            case 384:
                return P_384;
            case 521:
                return P_521;
            default:
                throw new IllegalArgumentException("Unsupported EC key size: " + keySize);
        }
    }

    private void saveAccountToDatabase(AccountKeyResponse keyResponse, Long caId, String creationUser) {
        try {
            // AccountKeyResponse를 JSON 문자열로 변환
            String privateKeyJson = objectMapper.writeValueAsString(keyResponse);

            // AcmeAccountEntity 생성 및 저장
            AcmeAccountEntity accountEntity = AcmeAccountEntity.builder()
                    .caId(caId)
                    .status(AccountStatus.VALID)
                    .privateKey(privateKeyJson)
                    .creationUser(creationUser)
                    .lastChangeUser(creationUser)
                    .termsOfServiceAgreed(false)
                    .build();

            acmeAccountRepository.save(accountEntity);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert key to JSON", e);
        }
    }
}