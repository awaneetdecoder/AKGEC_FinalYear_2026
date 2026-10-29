package com.swachhdrishti.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** RestTemplate with hard timeouts so a slow Roboflow can never hang a request. */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(@Value("${roboflow.timeout-ms:3000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
