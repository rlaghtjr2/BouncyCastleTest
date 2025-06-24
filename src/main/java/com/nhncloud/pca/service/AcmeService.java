package com.nhncloud.pca.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import com.nhncloud.pca.model.acme.CertificateResult;
import com.nhncloud.pca.model.acme.Directory;
import com.nhncloud.pca.model.acme.FinalizeResult;
import com.nhncloud.pca.model.acme.account.AccountCreationResult;
import com.nhncloud.pca.model.acme.authorization.AuthorizationResult;
import com.nhncloud.pca.model.acme.challenge.ChallengeResult;
import com.nhncloud.pca.model.acme.order.OrderCreationResult;
import com.nhncloud.pca.model.acme.order.OrderQueryResult;

public interface AcmeService {
    Directory getDirectory(HttpServletRequest request);

    String getNonce();

    AccountCreationResult createAccount(Map<String, String> request, HttpServletRequest httpRequest);

    OrderCreationResult createOrder(Map<String, String> request, String baseUrl);

    AuthorizationResult getAuthorization(String id, String baseUrl);

    ChallengeResult triggerChallenge(String id, Map<String, String> jwsRequest, String baseUrl);

    FinalizeResult finalizeOrder(String orderId, Map<String, String> jwsRequest);

    OrderQueryResult getOrder(String orderId, String baseUrl);

    CertificateResult getCertificate(String certificateId);
}
