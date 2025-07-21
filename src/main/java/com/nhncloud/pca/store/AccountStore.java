package com.nhncloud.pca.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.entity.acme.AcmeAccountEntity;
import com.nhncloud.pca.model.response.AccountKeyResponse;
import com.nhncloud.pca.repository.acme.AcmeAccountRepository;
import com.nimbusds.jose.jwk.RSAKey;

@Component
public class AccountStore {

    @Autowired
    private AcmeAccountRepository acmeAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // kid로 공개키 조회 - DB 기반만 사용
    public RSAKey getKeyByKid(String kid) {
        try {
            // 1. kid URL에서 account id 추출 (예: https://localhost:8443/acme/acct/3 -> 3)
            Long accountId = extractAccountIdFromKid(kid);

            // 2. DB에서 AcmeAccountEntity 조회
            AcmeAccountEntity accountEntity = acmeAccountRepository.findById(accountId)
                    .orElse(null);

            if (accountEntity == null) {
                return null;
            }

            // 3. private_key(JSON 형태)를 AccountKeyResponse로 변환
            AccountKeyResponse keyResponse = objectMapper.readValue(
                    accountEntity.getPrivateKey(), AccountKeyResponse.class);

            // 4. AccountKeyResponse를 RSAKey로 변환
            RSAKey rsaKey = convertToRSAKey(keyResponse);

            return rsaKey;

        } catch (NumberFormatException e) {
            // kid가 숫자가 아닌 경우 처리 불가
            return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse private key JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get account key: " + e.getMessage(), e);
        }
    }

    /**
     * kid URL에서 account id 추출
     * 예: https://localhost:8443/acme/acct/3 -> 3
     */
    private Long extractAccountIdFromKid(String kid) {
        try {
            // kid가 숫자인 경우 (기존 방식 호환)
            return Long.parseLong(kid);
        } catch (NumberFormatException e) {
            // kid가 URL인 경우 마지막 부분에서 account id 추출
            if (kid.contains("/acct/")) {
                String[] parts = kid.split("/acct/");
                if (parts.length >= 2) {
                    String accountIdStr = parts[1];
                    // 추가 경로나 쿼리 파라미터가 있는 경우 제거
                    if (accountIdStr.contains("/")) {
                        accountIdStr = accountIdStr.substring(0, accountIdStr.indexOf("/"));
                    }
                    if (accountIdStr.contains("?")) {
                        accountIdStr = accountIdStr.substring(0, accountIdStr.indexOf("?"));
                    }
                    return Long.parseLong(accountIdStr);
                }
            }

            // URL에서 마지막 숫자 추출 (일반적인 경우)
            String[] urlParts = kid.split("/");
            if (urlParts.length > 0) {
                String lastPart = urlParts[urlParts.length - 1];
                // 쿼리 파라미터가 있는 경우 제거
                if (lastPart.contains("?")) {
                    lastPart = lastPart.substring(0, lastPart.indexOf("?"));
                }
                return Long.parseLong(lastPart);
            }

            throw new IllegalArgumentException("Cannot extract account id from kid: " + kid);
        }
    }

    /**
     * AccountKeyResponse를 RSAKey로 변환
     */
    private RSAKey convertToRSAKey(AccountKeyResponse keyResponse) {
        try {
            if ("RSA".equals(keyResponse.getKeyType())) {
                return new RSAKey.Builder(
                        com.nimbusds.jose.util.Base64URL.from(keyResponse.getModulus()),
                        com.nimbusds.jose.util.Base64URL.from(keyResponse.getExponent())
                )
                .privateExponent(com.nimbusds.jose.util.Base64URL.from(keyResponse.getPrivateExponent()))
                .firstPrimeFactor(com.nimbusds.jose.util.Base64URL.from(keyResponse.getFirstPrimeFactor()))
                .secondPrimeFactor(com.nimbusds.jose.util.Base64URL.from(keyResponse.getSecondPrimeFactor()))
                .firstFactorCRTExponent(com.nimbusds.jose.util.Base64URL.from(keyResponse.getFirstFactorCrtExponent()))
                .secondFactorCRTExponent(com.nimbusds.jose.util.Base64URL.from(keyResponse.getSecondFactorCrtExponent()))
                .firstCRTCoefficient(com.nimbusds.jose.util.Base64URL.from(keyResponse.getFirstCrtCoefficient()))
                .build();
            } else {
                throw new IllegalArgumentException("Only RSA keys are supported for ACME accounts");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert AccountKeyResponse to RSAKey: " + e.getMessage(), e);
        }
    }
}
