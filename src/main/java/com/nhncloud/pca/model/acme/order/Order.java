package com.nhncloud.pca.model.acme.order;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.authorization.Authorization;

@Data
@Builder
public class Order {

    private String id;
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    @Builder.Default
    private List<Identifier> identifiers = new ArrayList<>();
    @Builder.Default
    private List<Authorization> authorizations = new ArrayList<>();
    @Builder.Default
    private LocalDateTime expires = LocalDateTime.now().plusSeconds(300);
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;
    public PKCS10CertificationRequest csr;

    // 이 밑은 모두 url 형태
    // 주문 완료 시 사용
    private String finalize;
    // 주문 실패 시 사용
    private String error;
    // 인증서 정보
    private String certificate;
}