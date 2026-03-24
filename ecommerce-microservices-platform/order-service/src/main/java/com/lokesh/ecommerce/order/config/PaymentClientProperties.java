package com.lokesh.ecommerce.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentClientProperties {

    private String baseUrl = "http://localhost:8083";
}
