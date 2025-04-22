package com.nhncloud.pca.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@SpringBootTest
@Slf4j
class MTLSHttpClient {
    @Value("${mtls.server-url}")
    private String serverUrl;

    @Value("${mtls.keystore}")
    private Resource keyStore;

    @Value("${mtls.keystore-password}")
    private String keyStorePassword;

    @Value("${mtls.truststore}")
    private Resource trustStore;

    @Value("${mtls.truststore-password}")
    private String trustStorePassword;

    @Test
    void test_MTLS() throws Exception {
        // RestTemplate에 HttpClient 설정
        RestTemplate restTemplate = restTemplate();

        // REST 호출
        String response = restTemplate.getForObject(serverUrl, String.class);

        log.info("Response: " + response);
    }

    private RestTemplate restTemplate() throws Exception {
        // Set up SSL context with truststore and keystore
        SSLContext sslContext = new SSLContextBuilder()
            .loadKeyMaterial(
                keyStore.getURL(),
                keyStorePassword.toCharArray(),
                keyStorePassword.toCharArray()
            )
            .loadTrustMaterial(
                trustStore.getURL(),
                trustStorePassword.toCharArray()
            )
            .build();

        // Configure the SSLConnectionSocketFactory to use NoopHostnameVerifier
        SSLConnectionSocketFactory sslConFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

        // Use a connection manager with the SSL socket factory
        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslConFactory)
            .build();

        // Build the CloseableHttpClient and set the connection manager
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .build();

        // Set the HttpClient as the request factory for the RestTemplate
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }
}
