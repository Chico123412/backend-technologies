package com.autoservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "catalog.events";
    public static final String QUEUE = "reporting.catalog-events";

    @Bean
    DirectExchange catalogExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue catalogEventsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding catalogEventsBinding(Queue catalogEventsQueue, DirectExchange catalogExchange) {
        return BindingBuilder.bind(catalogEventsQueue).to(catalogExchange).with("service.changed");
    }

    @Bean
    Jackson2JsonMessageConverter eventConverter(ObjectMapper json) {
        return new Jackson2JsonMessageConverter(json);
    }
}
