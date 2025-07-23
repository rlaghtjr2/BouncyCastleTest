package com.nhncloud.pca.service;

import com.nhncloud.pca.model.acme.CertificateResult;
import com.nhncloud.pca.model.acme.Directory;
import com.nhncloud.pca.model.acme.FinalizeResult;
import com.nhncloud.pca.model.acme.JwsRequest;
import com.nhncloud.pca.model.acme.account.AccountCreationResult;
import com.nhncloud.pca.model.acme.authorization.AuthorizationResult;
import com.nhncloud.pca.model.acme.challenge.ChallengeResult;
import com.nhncloud.pca.model.acme.order.OrderCreationResult;
import com.nhncloud.pca.model.acme.order.OrderQueryResult;

import jakarta.servlet.http.HttpServletRequest;

public interface AcmeService {

    Directory getDirectory(HttpServletRequest request);

    AccountCreationResult createAccount(JwsRequest jwsRequest, HttpServletRequest request);

    OrderCreationResult createOrder(JwsRequest jwsRequest, String baseUrl, HttpServletRequest request);

    OrderQueryResult getOrder(Long orderId, String baseUrl);

    AuthorizationResult getAuthorization(Long authzId, String baseUrl);

    ChallengeResult triggerChallenge(Long id, JwsRequest jwsRequest, String baseUrl, HttpServletRequest httpRequest);

    FinalizeResult finalizeOrder(Long orderId, JwsRequest jwsRequest, HttpServletRequest httpRequest);

    CertificateResult getCertificate(Long certificateId, String baseUrl);
}
