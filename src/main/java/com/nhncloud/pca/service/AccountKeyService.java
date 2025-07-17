package com.nhncloud.pca.service;

import com.nhncloud.pca.constant.KeyAlgorithm;
import com.nhncloud.pca.model.response.AccountKeyResponse;

public interface AccountKeyService {

    /**
     * ACME Account용 키 쌍을 생성하고 DB에 저장한 후 JWK 형태로 반환
     * @param algorithm 키 알고리즘 (RSA, EC 등)
     * @param keySize 키 크기
     * @param caId CA ID
     * @return JWK 형태의 키 정보
     */
    AccountKeyResponse generateAndSaveAccountKey(KeyAlgorithm algorithm, int keySize, Long caId);
}