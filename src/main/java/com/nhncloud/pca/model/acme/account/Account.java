package com.nhncloud.pca.model.acme.account;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.nhncloud.pca.constant.acme.AccountStatus;

@Data
@Builder
public class Account {
    private AccountStatus status;
    // 연락처 정보 (이메일 - "mailto:" 형식)
    private List<String> contact;
    // 약관 동의 여부
    private boolean termsOfServiceAgreed;
    // 기존 계정 조회 시 사용 (서버가 내려주는 값)
    private boolean onlyReturnExisting;
    // 외부 계정 바인딩 정보
    private Object externalAccountBinding;
    // 주문 목록
    private String orders;
}

