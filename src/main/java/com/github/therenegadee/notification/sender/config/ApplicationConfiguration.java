package com.github.therenegadee.notification.sender.config;

import com.github.therenegadee.notification.sender.interceptor.RestClientErrorHandler;
import com.github.therenegadee.notification.sender.interceptor.RestClientLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfiguration {

    @Value("${integrations.telegram.url}")
    private String telegramBaseUrl;

    @Value("${integrations.telegram.token}")
    private String telegramToken;

    @Bean("telegramRestClient")
    public RestClient telegramRestClient(RestClientLoggingInterceptor restClientLoggingInterceptor) {
        return RestClient.builder()
                .baseUrl(telegramBaseUrl + telegramToken)
                .requestInterceptor(restClientLoggingInterceptor)
                .build();
    }

    @Bean
    public RestClientLoggingInterceptor restClientLoggingInterceptor() {
        return new RestClientLoggingInterceptor();
    }

    @Bean
    public RestClientErrorHandler restClientErrorHandler() {
        return new RestClientErrorHandler();
    }
}
