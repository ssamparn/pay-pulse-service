package com.paypulse.platform.infrastructure.soap.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class BatchPaymentSoapClientConfig {

    @Bean
    public Jaxb2Marshaller batchSoapMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan(
                "com.paypulse.platform.infrastructure.soap.model.req",
                "com.paypulse.platform.infrastructure.soap.model.rpy"
        );
        return marshaller;
    }
}

