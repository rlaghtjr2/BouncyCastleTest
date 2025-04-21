import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.Test;

import com.example.vault.demo.BouncyCastleService;

public class BouncyCastleTest {
    @Test
    public void test_getBouncyCastleProviderAlgorithm() {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.getBouncyCastleProviderAlgorithm();
    }

    @Test
    public void test_getBouncyCastleRootCA() throws NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException, CertIOException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.generateRootCA();
    }

    @Test
    public void test_getBouncyCastleCSR() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.generateCsr();
    }

    @Test
    public void test_generateIntermediateCA() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException, CertificateException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.generateIntermediateCACsr();
    }

    @Test
    public void test_signIntermediateCaCsr() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.signIntermediateCACsr();
    }

    @Test
    public void test_generateLeafServerCsr() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.generateLeafCertificateCSR("server");
    }

    @Test
    public void test_signLeafServerCsr() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.signLeafCertificate("server");
    }

    @Test
    public void test_generateLeafClientCsr() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.generateLeafCertificateCSR("client");
    }

    @Test
    public void test_signLeafClientCsr() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.signLeafCertificate("client");
    }

    @Test
    public void test_generateChainCertificate() throws CertificateException, IOException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.generateChainCertificate();
    }
}
