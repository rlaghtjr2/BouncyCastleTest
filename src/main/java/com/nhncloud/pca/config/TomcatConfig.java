package com.nhncloud.pca.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class TomcatConfig {

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.setPort(8443); // HTTPS 포트
        factory.addAdditionalTomcatConnectors(httpConnector()); // HTTP도 추가

        return factory;
    }

    private Connector httpConnector() {
        Connector connector =
            new org.apache.catalina.connector.Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(80); // HTTP 포트
        connector.setSecure(false);
        return connector;
    }
}
