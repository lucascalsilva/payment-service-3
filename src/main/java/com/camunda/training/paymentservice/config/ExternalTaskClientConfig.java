package com.camunda.training.paymentservice.config;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalTaskClientConfig {

    @Bean
    public ExternalTaskClient externalTaskClient(){
        return ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest")
                .maxTasks(10)
                .build();
    }

}
