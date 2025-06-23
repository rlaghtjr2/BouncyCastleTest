package com.nhncloud.pca.store;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class CertStore {
    private final Map<String, X509Certificate> certMap = new ConcurrentHashMap<>();

    public void save(String id, X509Certificate cert) {
        certMap.put(id, cert);
    }

    public X509Certificate get(String id) {
        return certMap.get(id);
    }
    
}
