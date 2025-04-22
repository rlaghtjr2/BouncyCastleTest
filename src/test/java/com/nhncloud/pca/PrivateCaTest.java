package com.nhncloud.pca;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.Test;

import com.nhncloud.pca.service.PrivateCaService;

public class PrivateCaTest {
    @Test
    public void test_getBouncyCastleProviderAlgorithm() {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.getBouncyCastleProviderAlgorithm();
    }

    @Test
    public void test_getBouncyCastleRootCA() throws NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, CertIOException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.generateRootCA();
    }

    @Test
    public void test_getBouncyCastleCSR() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.generateCsr();
    }

    @Test
    public void test_generateIntermediateCA() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException, CertificateException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.generateIntermediateCACsr();
    }

    @Test
    public void test_signIntermediateCaCsr() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.signIntermediateCACsr();
    }

    @Test
    public void test_generateLeafServerCsr() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.generateLeafCertificateCSR("server");
    }

    @Test
    public void test_signLeafServerCsr() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.signLeafCertificate("server");
    }

    @Test
    public void test_generateLeafClientCsr() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.generateLeafCertificateCSR("client");
    }

    @Test
    public void test_signLeafClientCsr() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.signLeafCertificate("client");
    }

    @Test
    public void test_generateChainCertificate() throws CertificateException, IOException {
        PrivateCaService bouncyCastleService = new PrivateCaService();
        bouncyCastleService.generateChainCertificate();
    }
}
