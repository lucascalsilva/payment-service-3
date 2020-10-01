package com.camunda.training.paymentservice.workers;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class ChargeCreditWorker implements CommandLineRunner {

    private final ExternalTaskClient clientPayment;
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final Double credit = 1000.00;

    public ChargeCreditWorker(ExternalTaskClient clientPayment) {
        this.clientPayment = clientPayment;
    }

    @Override
    public void run(String... args) throws Exception {
        clientPayment.subscribe("credit-charging")
                .lockDuration(10000L).handler(this::handleTask).open();
    }

    private void handleTask(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            //Here is where I do my service logic...
            Double amount = (Double) externalTask.getVariable("amount");
            Double remainingAmount = Math.max(amount - credit, 0);

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("creditSufficient", remainingAmount == 0);
            variables.put("remainingAmount", remainingAmount);

            LOG.info("Running external task {} for topic {}", externalTask.getId(), externalTask.getTopicName());
            externalTaskService.complete(externalTask, variables);
            LOG.info("External task {} for topic {} completed", externalTask.getId(), externalTask.getTopicName());
        }
        catch(Exception ex){
            Integer retries = externalTask.getRetries();
            if(retries == null)
                retries = 3;
            else
                retries -= 1;

            LOG.error("Error when running external task {} for topic {}", externalTask.getId(), externalTask.getTopicName());
            externalTaskService.handleFailure(externalTask, "Unknown issue "+ex.getClass().getSimpleName(), Arrays.toString(ex.getStackTrace()), retries, 60000L);
        }
    }
}
