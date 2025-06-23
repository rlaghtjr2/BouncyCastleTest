package com.nhncloud.pca.store;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class NonceStore {
    // 생성된 nonce를 저장 (동시성 안전)
    private final Set<String> nonceSet = ConcurrentHashMap.newKeySet();

    // nonce 생성 및 저장
    public String generateNonce() {
        String nonce = UUID.randomUUID().toString();
        nonceSet.add(nonce);
        return nonce;
    }

    // 클라이언트가 보낸 nonce가 유효한지 확인하고 제거
    public boolean consumeNonce(String nonce) {
        return nonceSet.remove(nonce); // 사용되면 삭제
    }

    // (선택) 현재 저장된 nonce 수 확인용 (디버깅용)
    public int size() {
        return nonceSet.size();
    }
}
