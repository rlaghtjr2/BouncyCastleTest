package com.nhncloud.pca.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.RSAKey;

@Component
public class AccountStore {
    private final Map<String, RSAKey> accountKeys = new ConcurrentHashMap<>();

    // 계정 생성 시 공개키 저장 (accountUrl = kid 역할)
    public void saveAccount(String accountUrl, RSAKey jwk) {
        accountKeys.put(accountUrl, jwk);
    }

    // kid로 공개키 조회
    public RSAKey getKeyByKid(String kid) {
        return accountKeys.get(kid);
    }
    
}
