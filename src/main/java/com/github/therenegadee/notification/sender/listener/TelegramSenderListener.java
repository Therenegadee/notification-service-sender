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
            containerFactory = "sendTelegramNotificationListenerFactory")
    public void sendTelegramNotificationListener(@Payload SendTelegramNotificationRequest request,
                                                 Acknowledgment ack) {
        try {
            log.info("Получен запрос на отправку уведомления в Telegram. ChatID = {}. Payload: {}.",
                    request.getChatId(), request.getMessage());
            SendTelegramNotificationResult result = telegramClient.sendNotification(request);
            if (!result.isNotificationSent()) {
                log.error("Уведомление для получателя с ChatID = \"{}\" не было отправлено. Причина: {}. Код ошибки: {}.",
                        request.getChatId(), result.getErrorMessage(), result.getErrorCode());
                log.info("Отправка информации в топик с неотправленными уведомлениями (\"{}\").", telegramSendNotificationErrorTopic);
                SendTelegramNotificationError errorInfo = SendTelegramNotificationError.builder()
                        .chatId(request.getChatId())
                        .message(request.getMessage())
                        .errorMessage(result.getErrorMessage())
                        .errorCode(result.getErrorCode())
                        .build();
                sendTelegramNotificationErrorProducer.send(telegramSendNotificationErrorTopic, errorInfo)
                        .whenComplete((sendResult, exception) -> {
                            if (Objects.isNull(exception)) {
                                var metadata = sendResult.getRecordMetadata();
                                log.info("Информация о неуспешной отправке уведомления в Telegram (ChatID: {}) было успешно отправлено в топик: {}." +
                                                "Partition: {}. Offset: {}", request.getChatId(), telegramSendNotificationErrorTopic, metadata.partition(),
                                        metadata.offset());
                            } else {
                                log.error("Произошла ошибка при попытке отправки информации о неуспешной отправке уведомления в Telegram (ChatID: {})." +
                                                "Причина: {}.\nStackTrace: {}", request.getChatId(), ExceptionUtils.getMessage(exception),
                                        ExceptionUtils.getStackTrace(exception));
                            }
                        });
            }
            log.info("Уведомление в Telegram (получатель с ChatID = {}) было успешно отправлено!", request.getChatId());
        } finally {
            ack.acknowledge();
        }
    }

}
