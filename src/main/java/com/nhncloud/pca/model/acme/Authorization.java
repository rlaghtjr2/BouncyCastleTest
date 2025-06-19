package com.nhncloud.pca.model.acme;

import java.time.LocalDateTime;
import java.util.List;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;

public class Authorization {
    private AuthorizationStatus status;
    private Identifier identifier;
    private LocalDateTime expires;
    private List<Challenge> challenges;
    private boolean wildcard;
}
