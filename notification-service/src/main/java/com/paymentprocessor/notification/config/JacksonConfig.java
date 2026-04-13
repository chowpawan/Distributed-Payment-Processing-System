package com.paymentprocessor.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.config.EventsObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    private final ObjectMapper objectMapper;

    public JacksonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void configure() {
        EventsObjectMapper.configure(objectMapper);
    }
}
