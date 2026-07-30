package com.TrucVanban.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String DOCUMENT_EXCHANGE = "document.exchange";
    public static final String DOCUMENT_EXCHANGE_QUEUE = "document.exchange.queue";
    public static final String DOCUMENT_EXCHANGE_ROUTING_KEY = "document.exchange";

    @Bean
    public TopicExchange documentExchange() {
        return new TopicExchange(DOCUMENT_EXCHANGE);
    }

    @Bean
    public Queue documentExchangeQueue() {
        return QueueBuilder.durable(DOCUMENT_EXCHANGE_QUEUE).build();
    }

    @Bean
    public Binding documentExchangeBinding() {
        return BindingBuilder.bind(documentExchangeQueue())
                .to(documentExchange())
                .with(DOCUMENT_EXCHANGE_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        template.setReturnsCallback(returned ->
                log.error("RabbitMQ returned message: exchange={}, routingKey={}, replyCode={}, replyText={}",
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyCode(),
                        returned.getReplyText()
                )
        );
        return template;
    }
}
