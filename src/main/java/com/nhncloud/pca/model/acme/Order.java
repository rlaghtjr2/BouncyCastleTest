package com.nhncloud.pca.model.acme;

import java.time.LocalDateTime;
import java.util.List;

import com.nhncloud.pca.constant.acme.OrderStatus;

public class Order {
    private OrderStatus status;
    private List<Identifier> identifiers;
    private List<Authorization> authorizations;
    private LocalDateTime expires;
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;

    // url 형태
    private String finalize;
    private String error;
    private String certificate;
}
