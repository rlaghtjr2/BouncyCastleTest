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
    public void test_gernerateIntermediateCA() throws NoSuchAlgorithmException, IOException, NoSuchProviderException, OperatorCreationException, CertificateException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.generateIntermediateCACsr();
    }

    @Test
    public void test_signIntermediateCaCsr() throws CertificateException, IOException, NoSuchAlgorithmException, OperatorCreationException {
        BouncyCastleService bouncyCastleService = new BouncyCastleService();
        bouncyCastleService.signIntermediateCACsr();
    }
}
