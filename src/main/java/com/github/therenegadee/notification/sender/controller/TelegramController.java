package com.github.therenegadee.notification.sender.controller;

import com.github.therenegadee.notification.sender.client.TelegramClient;
import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramClient telegramClient;

    @PostMapping
    public ResponseEntity<?> sendTelegramMessage(@RequestBody SendTelegramNotificationRequest request) {
        return ResponseEntity.ok(telegramClient.sendNotification(request));
    }
}
