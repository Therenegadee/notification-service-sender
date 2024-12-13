package com.github.therenegadee.notification.sender.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        log.info("Sending {} request on: {}. Headers: {}. {}", request.getMethod(), request.getURI(), request.getHeaders(),
                body.length != 0 ? new String(body, StandardCharsets.UTF_8) : "");
        ClientHttpResponse response = execution.execute(request, body);
        log.info("Received the response on {} request on: {}. HTTP Status: {}. Headers: {}.", request.getMethod(),
                request.getURI(), response.getStatusCode(), response.getHeaders());
        return response;
    }
}
