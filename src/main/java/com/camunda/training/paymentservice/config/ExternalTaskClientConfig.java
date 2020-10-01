package com.camunda.training.paymentservice.config;

import com.camunda.consulting.client.invoker.ApiClient;
import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalTaskClientConfig {

    @Bean
    public ExternalTaskClient clientOrder(){
        return ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest")
                .maxTasks(10)
                .disableBackoffStrategy()
                .asyncResponseTimeout(60000L)
                .build();
    }

    @Bean
    public ExternalTaskClient clientPayment(){
        return ExternalTaskClient.create()
                .baseUrl("http://localhost:8081/engine-rest")
                .maxTasks(10)
                .disableBackoffStrategy()
                .asyncResponseTimeout(60000L)
                .build();
    }

    @Bean
    public ApiClient paymentApiClient(){
        return new ApiClient().setBasePath("http://localhost:8081/engine-rest");
    }

    @Bean
    public ApiClient orderApiClient(){
        return new ApiClient();
    }
}
