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

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentSendWorker implements CommandLineRunner {

    private final ExternalTaskClient clientOrder;
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final MessageApi messageApi;

    public PaymentSendWorker(ExternalTaskClient clientOrder, ApiClient paymentApiClient) {
        this.clientOrder = clientOrder;
        this.messageApi = new MessageApi(paymentApiClient);
    }

    @Override
    public void run(String... args) throws Exception {
        clientOrder.subscribe("payment-requesting")
                .lockDuration(10000L).handler(this::handleTask).open();
    }

    private void handleTask(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        //Here is where I do my service logic...
        LOG.info("Running external task {} for topic {}", externalTask.getId(), externalTask.getTopicName());
        Map<String, VariableValueDto> variables = new HashMap<String, VariableValueDto>();
        variables.put("amount", new VariableValueDto().value(externalTask.getVariable("amount")).type("Double"));
        variables.put("doError", new VariableValueDto().value(externalTask.getVariable("doError")).type("Boolean"));
        variables.put("resolvable", new VariableValueDto().value(externalTask.getVariable("resolvable")).type("Boolean"));
        variables.put("orderId", new VariableValueDto().value(externalTask.getBusinessKey()).type("String"));

        CorrelationMessageDto correlationMessageDto = new CorrelationMessageDto().messageName("PaymentRequestedMessage")
                .processVariables(variables);

        messageApi.deliverMessage(correlationMessageDto);
        externalTaskService.complete(externalTask);
        LOG.info("External task {} for topic {} completed", externalTask.getId(), externalTask.getTopicName());
    }
}
