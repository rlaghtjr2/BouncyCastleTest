package com.nhncloud.pca.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeyAlgorithm {

    RSA("RSA", "RSA", new int[]{1024, 2048, 3072, 4096}),
    EC("EC", "EC", new int[]{256, 384, 521}), // P-256, P-384, P-521
    ;

    private final String algorithmName;
    private final String keyType;
    private final int[] supportedKeySizes;

    /**
     * 지정된 키 크기가 해당 알고리즘에서 지원되는지 확인
     * @param keySize 키 크기
     * @return 지원 여부
     */
    public boolean isSupportedKeySize(int keySize) {
        for (int supportedSize : supportedKeySizes) {
            if (supportedSize == keySize) {
                return true;
            }
        }
        return false;
    }

    /**
     * 해당 알고리즘의 기본 키 크기 반환
     * @return 기본 키 크기
     */
    public int getDefaultKeySize() {
        return supportedKeySizes[1]; // 보통 두 번째 값이 기본값 (RSA: 2048, EC: 384)
    }

    /**
     * 문자열로부터 KeyAlgorithm 찾기
     * @param algorithmName 알고리즘 이름
     * @return KeyAlgorithm 또는 null
     */
    public static KeyAlgorithm fromString(String algorithmName) {
        for (KeyAlgorithm algorithm : values()) {
            if (algorithm.algorithmName.equalsIgnoreCase(algorithmName)) {
                return algorithm;
            }
        }
        return null;
    }
}