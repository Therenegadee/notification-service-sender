package com.github.therenegadee.notification.sender.listener;

import com.github.therenegadee.notification.sender.client.TelegramClient;
import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationError;
import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationRequest;
import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class TelegramSenderListener {

    private final TelegramClient telegramClient;
    private final KafkaTemplate<String, SendTelegramNotificationError> sendTelegramNotificationErrorProducer;

    @Value("${integrations.kafka.telegram.send.error-topic}")
    private String telegramSendNotificationErrorTopic;

    public TelegramSenderListener(TelegramClient telegramClient,
                                  @Qualifier("sendTelegramNotificationErrorProducer") KafkaTemplate<String, SendTelegramNotificationError> sendTelegramNotificationErrorProducer) {
        this.telegramClient = telegramClient;
        this.sendTelegramNotificationErrorProducer = sendTelegramNotificationErrorProducer;
    }

    @KafkaListener(topics = "${integrations.kafka.telegram.send.topic}",
            containerFactory = "sendTelegramNotificationListenerFactory",
            properties = {"spring.json.value.default.type=com.github.therenegadee.notification.sender.dto.SendTelegramNotificationRequest"})
    public void sendTelegramNotificationListener(@Payload SendTelegramNotificationRequest request,
                                                 Acknowledgment ack) {
        try {
            log.info("Received request to send a Telegram notification. ChatID = {}. Payload: {}.",
                    request.getRecipientContactValue(), request.getMessageBody());
            SendTelegramNotificationResult result = telegramClient.sendNotification(request);
            if (!result.isNotificationSent()) {
                log.error("Notification for the recipient with ChatID = \"{}\" was not sent. Reason: {}. Error code: {}.",
                        request.getRecipientContactValue(), result.getErrorMessage(), result.getErrorCode());
                log.info("Sending information to the topic with non-sent messages (topic: {}).", telegramSendNotificationErrorTopic);
                SendTelegramNotificationError errorInfo = SendTelegramNotificationError.builder()
                        .chatId(request.getRecipientContactValue())
                        .message(request.getMessageBody())
                        .errorMessage(result.getErrorMessage())
                        .errorCode(result.getErrorCode())
                        .build();
                sendTelegramNotificationErrorProducer.send(telegramSendNotificationErrorTopic, errorInfo)
                        .whenComplete((sendResult, exception) -> {
                            if (Objects.isNull(exception)) {
                                var metadata = sendResult.getRecordMetadata();
                                log.info("Information about the failed Telegram notification send (ChatID: {}) was successfully" +
                                                "\ssent to the topic: {}. Partition: {}. Offset: {}.", request.getRecipientContactValue(),
                                        telegramSendNotificationErrorTopic, metadata.partition(), metadata.offset());
                            } else {
                                log.error("An error occurred while trying to send information about the failed Telegram" +
                                                "\snotification send (ChatID: {}). Original Message: {}.\nStackTrace: {}.",
                                        request.getRecipientContactValue(), ExceptionUtils.getMessage(exception), ExceptionUtils.getStackTrace(exception));
                            }
                        });
            }
            log.info("Telegram notification (recipient with ChatID = {}) was successfully sent!", request.getRecipientContactValue());
        } finally {
            ack.acknowledge();
        }
    }

}
