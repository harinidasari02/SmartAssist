package com.smartassist.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Timeouts for calls to AI providers. Local models can be slow, so reads get 2 minutes. */
@Configuration
public class HttpClientConfig {
    @Bean
    RestClientCustomizer aiTimeouts() {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5_000);
            factory.setReadTimeout(120_000);
            builder.requestFactory(factory);
        };
    }
}
