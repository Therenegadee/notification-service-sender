package com.github.therenegadee.notification.sender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendTelegramNotificationRequest {
    private String recipientContactValue;
    private String messageBody;
}
