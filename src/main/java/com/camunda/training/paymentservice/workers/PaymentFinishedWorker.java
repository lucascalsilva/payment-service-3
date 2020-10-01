package com.camunda.training.paymentservice.workers;

import com.camunda.consulting.client.api.MessageApi;
import com.camunda.consulting.client.invoker.ApiClient;
import com.camunda.consulting.client.model.CorrelationMessageDto;
import com.camunda.consulting.client.model.VariableValueDto;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentFinishedWorker implements CommandLineRunner {

    private final ExternalTaskClient clientPayment;
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final MessageApi messageApi;

    public PaymentFinishedWorker(ExternalTaskClient clientPayment, ApiClient orderApiClient) {
        this.clientPayment = clientPayment;
        this.messageApi = new MessageApi(orderApiClient);
    }

    @Override
    public void run(String... args) throws Exception {
        clientPayment.subscribe("payment-finishing")
                .lockDuration(10000L).handler(this::handleTask).open();
    }

    private void handleTask(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        LOG.info("Running external task {} for topic {}", externalTask.getId(), externalTask.getTopicName());

        Map<String, VariableValueDto> variables = new HashMap<String, VariableValueDto>();
        variables.put("paymentOK", new VariableValueDto().value(externalTask.getVariable("paymentOK")).type("Boolean"));

        CorrelationMessageDto correlationMessageDto = new CorrelationMessageDto().messageName("ReceivePaymentMessage")
                .processVariables(variables)
                .businessKey(externalTask.getVariable("orderId"));

        messageApi.deliverMessage(correlationMessageDto);

        externalTaskService.complete(externalTask);
        LOG.info("External task {} for topic {} completed", externalTask.getId(), externalTask.getTopicName());
    }
}
