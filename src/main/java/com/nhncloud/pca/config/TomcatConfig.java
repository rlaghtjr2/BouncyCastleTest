package com.nhncloud.pca.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class TomcatConfig {
    @Value("${server.port}")
    private int httpPort;
    @Value("${server.ssl.port}")
    private int httpsPort;

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.setPort(httpsPort); // HTTPS 포트
        factory.addAdditionalTomcatConnectors(httpConnector()); // HTTP도 추가

        return factory;
    }

    private Connector httpConnector() {
        Connector connector =
            new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort); // HTTP 포트
        connector.setSecure(false);
        return connector;
    }
}
