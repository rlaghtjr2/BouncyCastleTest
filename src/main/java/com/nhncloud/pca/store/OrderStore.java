package com.nhncloud.pca.store;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Component;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;
import com.nhncloud.pca.constant.acme.OrderStatus;
import com.nhncloud.pca.model.acme.Authorization;
import com.nhncloud.pca.model.acme.Identifier;
import com.nhncloud.pca.model.acme.Order;

@Component
public class OrderStore {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Order createOrder(List<Identifier> identifiers, List<Authorization> authorizations, String baseUrl) {
        String orderId = UUID.randomUUID().toString();
        String finalizeUrl = baseUrl + "/acme/order/" + orderId + "/finalize";
        Order order = Order.builder()
            .id(orderId)
            .identifiers(identifiers)
            .authorizations(authorizations)
            .finalize(finalizeUrl)
            .build();
        orders.put(orderId, order);
        return order;
    }

    public Order getOrder(String id) {
        return orders.get(id);
    }

    public Order findOrderByAuthzId(String authzId) {
        for (Order order : orders.values()) {
            if (order.getAuthorizations().contains(authzId)) {
                return order;
            }
        }
        return null;
    }

    public boolean isDomainAuthorized(String orderId, String domain) {
        Order order = orders.get(orderId);
        if (order == null || order.getAuthorizations().isEmpty()) {
            return false;
        }

        for (Authorization authz : order.getAuthorizations()) {
            if (authz != null &&
                domain.equalsIgnoreCase(authz.getIdentifier().getValue()) &&
                authz.getStatus().equals(AuthorizationStatus.VALID)) {
                return true;
            }
        }

        return false;
    }

    public void markReadyIfAllAuthzValid(Order order, AuthorizationStore authzStore) {
        boolean allValid = order.getAuthorizations().stream()
            .allMatch(authz -> authz != null && authz.getStatus().equals(AuthorizationStatus.VALID));

        if (allValid && order.getStatus().equals(OrderStatus.PENDING)) {
            order.setStatus(OrderStatus.READY);
        }
    }

    public void markProcessing(String orderId) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setStatus(OrderStatus.PROCESSING);
        }
    }

    public void finalizeOrder(String orderId, String certificateId, PKCS10CertificationRequest csr, String pemCertificate) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.csr = csr;
//            order.setCertificate(pemCertificate);
            order.setStatus(OrderStatus.VALID);
            order.setCertificate("/acme/certificate/" + certificateId);
        }
    }

    public void markInvalid(String orderId) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setStatus(OrderStatus.INVALID);
        }
    }

    public String getPemCertificate(String orderId) {
        Order order = orders.get(orderId);
        return order != null ? order.getCertificate() : null;
    }

}