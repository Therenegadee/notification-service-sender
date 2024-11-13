package com.github.therenegadee.notification.sender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendTelegramNotificationResult {
    private boolean isNotificationSent;
    private String errorCode;
    private String errorMessage;
}
