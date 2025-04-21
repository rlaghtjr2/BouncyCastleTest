package com.example.vault.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.X509Certificate;

@RestController
public class HelloController {
    @GetMapping("/secure-hello")
    public String secureHello(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null) {
            return "Client certificate: " + certs[0].getSubjectDN().getName();
        }
        return "No client certificate found";
    }
}
