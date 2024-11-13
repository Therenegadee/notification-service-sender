package com.github.therenegadee.notification.sender.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        log.info("Отправлен {} запрос по адресу: {}. Headers: {}. {}", request.getMethod(), request.getURI(), request.getHeaders(),
                body.length != 0 ? new String(body, StandardCharsets.UTF_8) : "");
        ClientHttpResponse response = execution.execute(request, body);
        String responseBody = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        log.info("Получен ответ на {} запрос по адресу: {}. Статус ответа: {}. Headers: {}. Response Body: {}.", request.getMethod(),
                request.getURI(), response.getStatusCode(), response.getHeaders(), responseBody.isBlank() ? "пустое" : responseBody);
        return response;
    }
}
