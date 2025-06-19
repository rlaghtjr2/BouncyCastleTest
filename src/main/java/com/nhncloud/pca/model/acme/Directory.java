package com.nhncloud.pca.model.acme;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
public class Directory {
    private final String newNonce;
    private final String newAccount;
    private final String newOrder;
    private final String newAuthz;
    private final String revokecert;
    private final String keyChange;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Meta meta;

    public Directory(String server, int port) {
        String baseUrl = String.format("https://%s:%d", server, port);
        this.newNonce = baseUrl + "/acme/new-nonce";
        this.newAccount = baseUrl + "/acme/new-account";
        this.newOrder = baseUrl + "/acme/new-order";
        this.newAuthz = baseUrl + "/acme/new-authz";
        this.revokecert = baseUrl + "/acme/revoke-cert";
        this.keyChange = baseUrl + "/acme/key-change";
    }
}
