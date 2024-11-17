package com.github.therenegadee.notification.sender.interceptor;

import com.github.therenegadee.notification.sender.exception.BadRequestException;
import com.github.therenegadee.notification.sender.exception.IntegrationException;
import com.github.therenegadee.notification.sender.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

@Slf4j
public class RestClientErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
        return httpResponse.getStatusCode().is5xxServerError() ||
                httpResponse.getStatusCode().is4xxClientError();
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {
        HttpStatusCode statusCode = httpResponse.getStatusCode();
        if (statusCode.is5xxServerError()) {
            log.warn("Произошла ошибка на стороне вызываемоего сервиса. Код ошибки: {}.",
                    statusCode);
            switch ((HttpStatus) statusCode) {
                case INTERNAL_SERVER_ERROR -> throw new IntegrationException(
                        "Произошла ошибка на стороне вызываемого сервиса.");
                default -> throw new HttpClientErrorException(httpResponse.getStatusCode());
            }
        } else if (httpResponse.getStatusCode().is4xxClientError()) {
            log.warn("Произошла ошибка в ходе выполнения запроса вызываемым сервисом из-за некорректного параметра\s" +
                    "или отсутствия доступа к ресурсу. Код ошибки: {}.", statusCode);
            switch ((HttpStatus) statusCode) {
                case BAD_REQUEST -> throw new BadRequestException(httpResponse.getStatusText());
                case NOT_FOUND -> throw new NotFoundException(httpResponse.getStatusText());
                default ->  throw new HttpClientErrorException(httpResponse.getStatusCode());
            }
        }
    }
}
