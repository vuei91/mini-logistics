package com.cjlogistics.mini.dispatch.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitMqConfig {

    public static final String NOTIFICATION_EXCHANGE = "dispatch.events";
    public static final String NOTIFICATION_QUEUE = "dispatch.notification";
    public static final String DEAD_LETTER_EXCHANGE = "dispatch.dlx";
    public static final String DEAD_LETTER_QUEUE = "dispatch.notification.dlq";

    @Bean
    TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE));
    }

    @Bean
    Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }

    @Bean
    Binding dispatchConfirmedBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with("dispatch.confirmed");
    }

    @Bean
    Binding shipmentCompletedBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with("shipment.completed");
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }
}
