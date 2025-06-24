package com.nhncloud.pca.model.acme.account;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AccountCreationResult {
    private final String accountUrl;
    private final List<String> contact;
    private final String replayNonce;
}
