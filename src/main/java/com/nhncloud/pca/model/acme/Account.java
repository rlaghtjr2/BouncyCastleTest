package com.nhncloud.pca.model.acme;

import java.util.List;

import com.nhncloud.pca.constant.acme.AccountStatus;

public class Account {
    private AccountStatus status;
    private List<String> contact;
    private boolean termsOfServiceAgreed;
    private boolean onlyReturnExisting;
    private Object externalAccountBinding;
    private String orders;
}
