package com.camunda.training.paymentservice.workers;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChargeCreditCardWorker implements CommandLineRunner {

    private final ExternalTaskClient clientPayment;
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    public ChargeCreditCardWorker(ExternalTaskClient clientPayment) {
        this.clientPayment = clientPayment;
    }

    @Override
    public void run(String... args) throws Exception {
        clientPayment.subscribe("credit-card-charging")
                .lockDuration(10000L).handler(this::handleTask).open();
    }

    private void handleTask(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        //Here is where I do my service logic...
        LOG.info("Running external task {} for topic {}", externalTask.getId(), externalTask.getTopicName());
        LOG.info("The remaining amount for task {} is {}", externalTask.getId(), externalTask.getVariable("remainingAmount"));
        Boolean doError = (Boolean) externalTask.getVariable("doError");

        if(doError != null && doError){
            externalTaskService.handleBpmnError(externalTask, "ChargeFailed", "Charge in the credit card failed for task "+externalTask.getId());
            LOG.info("External task {} for topic {} sent an error to Camunda", externalTask.getId(), externalTask.getTopicName());
        }
        else{
            externalTaskService.complete(externalTask);
            LOG.info("External task {} for topic {} completed", externalTask.getId(), externalTask.getTopicName());
        }
    }
}
