package com.nhncloud.pca.model.acme;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.nhncloud.pca.constant.acme.AccountStatus;

@Data
@Builder
public class Account {
    private AccountStatus status;
    private List<String> contact;
    private boolean termsOfServiceAgreed;
    private boolean onlyReturnExisting;
    private Object externalAccountBinding;
    private String orders;
}

