package com.github.therenegadee.notification.sender.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationRequest;
import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class TelegramClient {

    private final RestClient telegramRestClient;

    private static final String SEND_MESSAGE_PATH = "/sendMessage";

    private static final String INTERNAL_SENDER_ERROR_CODE = "INTERNAL SENDER ERROR";
    private static final String TELEGRAM_API_ERROR_CODE = "TELEGRAM API ERROR";

    public TelegramClient(@Qualifier("telegramRestClient") RestClient telegramRestClient) {
        this.telegramRestClient = telegramRestClient;
    }

    public SendTelegramNotificationResult sendNotification(SendTelegramNotificationRequest request) {
        log.info("Начало отправки сообщения в Telegram чат (id: {}). Текст отправляемого сообщения: {}.",
                request.getChatId(), request.getMessage());
        TelegramSendMessageRequest telegramRequest = TelegramSendMessageRequest.builder()
                .chatId(request.getChatId())
                .message(request.getMessage())
                .build();
        try {
            ResponseEntity<TelegramSendMessageResponse> telegramResponseEntity = telegramRestClient.post()
                    .uri(uri -> uri
                            .path(SEND_MESSAGE_PATH)
                            .build()
                    )
                    .body(telegramRequest)
                    .retrieve()
                    .toEntity(TelegramSendMessageResponse.class);

            TelegramSendMessageResponse telegramResponse = telegramResponseEntity.getBody();
            if (Objects.isNull(telegramResponse)) {
                log.error("Полученный результат отправки сообщения в Telegram чат (id: {}) был null!", request.getChatId());
                return SendTelegramNotificationResult.builder()
                        .isNotificationSent(false)
                        .errorCode(TELEGRAM_API_ERROR_CODE)
                        .errorMessage("Полученный результат отправки сообщения был null!")
                        .build();
            }
            log.info("Сообщение в Telegram чат (id: {}) было успешно отправлено!", request.getChatId());
            return SendTelegramNotificationResult.builder()
                    .isNotificationSent(telegramResponse.isSentSuccessfully())
                    .build();
        } catch (RestClientResponseException rce) {
            TelegramSendMessageError errorResponse = Optional.ofNullable(rce.getResponseBodyAs(TelegramSendMessageError.class))
                    .orElseGet(() -> {
                        var errorResponseJsonBody = rce.getResponseBodyAsString(StandardCharsets.UTF_8);
                        log.error("Не удалось десериализовать ответ с ошибкой от API Telegram. Raw Body: {}.",
                                errorResponseJsonBody);
                        return TelegramSendMessageError.builder()
                                .errorCode(TELEGRAM_API_ERROR_CODE)
                                .description("HTTP Code: " + rce.getStatusCode().value() + " .Raw Body [\n" + errorResponseJsonBody + "\n]")
                                .build();
                    });
            log.error("В ходе отправки сообщения в Telegram чат (id: {}) произошла ошибка при обмене данными по HTTP.\s" +
                    "HTTP Статус полученной ошибки: {}. Сообщение об ошибке: {}.", request.getChatId(), rce.getStatusCode(), errorResponse.getDescription());
            return SendTelegramNotificationResult.builder()
                    .isNotificationSent(false)
                    .errorCode(errorResponse.getErrorCode())
                    .errorMessage(errorResponse.getDescription())
                    .build();
        } catch (Exception e) {
            log.error("В ходе отправки сообщения в Telegram чат (id: {}) произошла ошибка. Причина: {}.\nStackTrace: {}",
                    request.getChatId(), ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
            return SendTelegramNotificationResult.builder()
                    .isNotificationSent(false)
                    .errorCode(INTERNAL_SENDER_ERROR_CODE)
                    .errorMessage(ExceptionUtils.getMessage(e))
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TelegramSendMessageRequest {
        @JsonProperty("chat_id")
        private String chatId;

        @JsonProperty("text")
        private String message;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TelegramSendMessageResponse {

        @JsonProperty("ok")
        private boolean isSentSuccessfully;

        @JsonProperty("result")
        private Result result;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Result {

            @JsonProperty("message_id")
            private long messageId;

            @JsonProperty("date")
            private long date;

            @JsonProperty("text")
            private String messageText;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TelegramSendMessageError {

        @JsonProperty("ok")
        private boolean isSentSuccessfully;

        @JsonProperty("error_code")
        private String errorCode;

        @JsonProperty("description")
        private String description;
    }
}
