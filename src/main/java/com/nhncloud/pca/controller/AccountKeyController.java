package com.nhncloud.pca.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhncloud.pca.constant.KeyAlgorithm;
import com.nhncloud.pca.model.response.AccountKeyResponse;
import com.nhncloud.pca.service.AccountKeyService;


@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Slf4j
public class AccountKeyController {

    private final AccountKeyService accountKeyService;

    /**
     * ACME Account용 키 쌍을 생성하고 DB에 저장한 후 JWK 형태로 반환
     *
     * @param algorithm 키 알고리즘 (RSA, EC 등)
     * @param keySize   키 크기
     * @param caId      CA ID
     * @return JWK 형태의 키 정보
     */
    @PostMapping("/generate-key")
    public ResponseEntity<AccountKeyResponse> generateAndSaveAccountKey(
        @RequestParam(value = "algorithm", defaultValue = "RSA") String algorithmName,
        @RequestParam(value = "keySize") Integer keySize,
        @RequestParam(value = "caId") Long caId) {

        try {
            log.info("Generating and saving account key with algorithm: {}, keySize: {}, caId: {}",
                algorithmName, keySize, caId);

            // 알고리즘 검증
            KeyAlgorithm algorithm = KeyAlgorithm.fromString(algorithmName);
            if (algorithm == null) {
                log.error("Unsupported algorithm: {}. Supported algorithms: {}", algorithmName,
                    Arrays.toString(KeyAlgorithm.values()));
                return ResponseEntity.badRequest().build();
            }

            // 키 크기 검증
            if (!algorithm.isSupportedKeySize(keySize)) {
                log.error("Unsupported key size: {} for algorithm: {}. Supported sizes: {}",
                    keySize, algorithm.getAlgorithmName(),
                    Arrays.toString(algorithm.getSupportedKeySizes()));
                return ResponseEntity.badRequest().build();
            }

            AccountKeyResponse response = accountKeyService.generateAndSaveAccountKey(algorithm, keySize, caId);

            log.info("Account key generated and saved successfully with algorithm: {}, keySize: {}, caId: {}",
                algorithm.getAlgorithmName(), keySize, caId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate and save account key", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 지원되는 알고리즘과 키 크기 정보를 반환
     *
     * @return 지원되는 알고리즘 정보
     */
    @GetMapping("/supported-algorithms")
    public ResponseEntity<Map<String, Object>> getSupportedAlgorithms() {
        Map<String, Object> algorithms = new HashMap<>();

        for (KeyAlgorithm algorithm : KeyAlgorithm.values()) {
            Map<String, Object> algorithmInfo = new HashMap<>();
            algorithmInfo.put("name", algorithm.getAlgorithmName());
            algorithmInfo.put("keyType", algorithm.getKeyType());
            algorithmInfo.put("supportedKeySizes", algorithm.getSupportedKeySizes());
            algorithmInfo.put("defaultKeySize", algorithm.getDefaultKeySize());

            algorithms.put(algorithm.getAlgorithmName(), algorithmInfo);
        }

        return ResponseEntity.ok(algorithms);
    }
}