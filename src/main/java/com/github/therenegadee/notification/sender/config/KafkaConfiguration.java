package com.github.therenegadee.notification.sender.config;

import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationError;
import com.github.therenegadee.notification.sender.dto.SendTelegramNotificationRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.consumer.OffsetResetStrategy.EARLIEST;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${integrations.kafka.cluster.addresses}")
    private String bootstrapAddresses;

    @Value("${integrations.kafka.telegram.send.group-id}")
    private String sendTelegramNotificationGroupId;

    @Bean("sendTelegramNotificationErrorProducer")
    public KafkaTemplate<String, SendTelegramNotificationError> sendTelegramNotificationErrorProducer() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaProducerConfig()));
    }

    private Map<String, Object> getKafkaProducerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddresses);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return config;
    }

    @Bean("sendTelegramNotificationListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SendTelegramNotificationRequest> sendTelegramNotificationListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SendTelegramNotificationRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        JsonDeserializer<SendTelegramNotificationRequest> deserializer = new JsonDeserializer<>(SendTelegramNotificationRequest.class);
        ConsumerFactory<String, SendTelegramNotificationRequest> consumerFactory = getKafkaConsumerFactory(deserializer, sendTelegramNotificationGroupId);
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    private <T> ConsumerFactory<String, T> getKafkaConsumerFactory(Deserializer<T> deserializer,
                                                                   String groupId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddresses);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer.getClass());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }
}
