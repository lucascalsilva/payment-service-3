package com.camunda.training.paymentservice.workers;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChargeCreditWorker implements CommandLineRunner {

    private final ExternalTaskClient externalTaskClient;
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    public ChargeCreditWorker(ExternalTaskClient externalTaskClient) {
        this.externalTaskClient = externalTaskClient;
    }

    @Override
    public void run(String... args) throws Exception {
        externalTaskClient.subscribe("credit-charging")
                .lockDuration(10000L).handler(this::handleTask).open();
    }

    private void handleTask(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        //Here is where I do my service logic...
        LOG.info("Running external task {} for topic {}", externalTask.getId(), externalTask.getTopicName());
        externalTaskService.complete(externalTask);
        LOG.info("External task {} for topic {} completed", externalTask.getId(), externalTask.getTopicName());
    }
}
