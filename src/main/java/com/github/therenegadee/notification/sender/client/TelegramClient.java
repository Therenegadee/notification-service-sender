package com.github.therenegadee.notification.sender.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramClient {

    private final RestClient restClient;

    private static final String SEND_MESSAGE_PATH = "/sendMessage";
    private static final String CHAT_ID_QUERY_PARAM = "chat_id";
    private static final String MESSAGE_QUERY_PARAM = "text";

    @EventListener(ApplicationStartedEvent.class)
    public void test() {
        sendMessage("421409221", "kyky");
    }

    public void sendMessage(String chatId, String message) {
        var requestBody = SendTelegramMessageRequest.builder()
                .chatId(chatId)
                .message(message)
                .build();
        ResponseEntity<Object> response = restClient.post()
                .uri(uri -> uri
                        .path(SEND_MESSAGE_PATH)
                        .build()
                )
                .body(requestBody)
                .retrieve()
                .toEntity(Object.class);
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class SendTelegramMessageRequest {
        @JsonProperty(CHAT_ID_QUERY_PARAM)
        private String chatId;
        @JsonProperty(MESSAGE_QUERY_PARAM)
        private String message;
    }
}
