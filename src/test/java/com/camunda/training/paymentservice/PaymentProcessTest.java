package com.camunda.training.paymentservice;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.List;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

@Deployment(resources = "payment-process.bpmn")
@SpringBootTest()
public class PaymentProcessTest {

    @Rule
    @ClassRule
    public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

    @Before
    public void setup(){
        init(rule.getProcessEngine());
    }

    @Test
    public void testHappyPath(){
        ProcessInstance processInstance = runtimeService().startProcessInstanceByMessage("PaymentRequestedMessage");

        assertThat(processInstance).isStarted().isWaitingAt("ChargeCreditTask").externalTask().hasTopicName("credit-charging");
        complete(externalTask(), withVariables("creditSufficient", true));
        assertThat(processInstance).isWaitingAt("PaymentDoneEndEvent").externalTask().hasTopicName("payment-finishing");
        complete(externalTask());
        assertThat(processInstance).isEnded().variables().containsEntry("paymentOK", true);
    }

    @Test
    public void testChargeCreditCard(){
        ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcess").startAfterActivity("ChargeCreditTask")
                .setVariable("creditSufficient", false).execute();

        assertThat(processInstance).isStarted().isWaitingAt("ChargeCreditCardTask").externalTask().hasTopicName("credit-card-charging");
        complete(externalTask());
        assertThat(processInstance).isWaitingAt("PaymentDoneEndEvent").externalTask().hasTopicName("payment-finishing");
        complete(externalTask());
        assertThat(processInstance).isEnded().variables().containsEntry("paymentOK", true);
    }

    @Test
    public void testPaymentFailed(){
        ProcessInstance processInstance = runtimeService().startProcessInstanceByMessage("PaymentRequestedMessage",
                withVariables("resolvable", false, "creditSufficient", false));

        assertThat(processInstance).isStarted().isWaitingAt("ChargeCreditTask").externalTask().hasTopicName("credit-charging");
        complete(externalTask());

        assertThat(processInstance).isStarted().isWaitingAt("ChargeCreditCardTask").externalTask().hasTopicName("credit-card-charging");
        List<LockedExternalTask> lockedTask = externalTaskService().fetchAndLock(1, "test")
                .topic("credit-card-charging", 10000L).execute();

        lockedTask.forEach(lockedExternalTask -> {
            externalTaskService().handleBpmnError(lockedExternalTask.getId(), "test", "ChargeFailed",
                    "Error message");
        });

        assertThat(processInstance).isWaitingAt("RefundCreditTask").externalTask().hasTopicName("credit-refunding");
        complete(externalTask());

        assertThat(processInstance).isWaitingAt("PaymentFailedEndEvent").externalTask().hasTopicName("payment-finishing");
        complete(externalTask());

        assertThat(processInstance).isEnded().variables().containsEntry("paymentOK", false);
    }

    @Test
    public void testHandleError() {
        ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcess")
                .startBeforeActivity("IsTheErrorResolvableGateway").setVariable("resolvable", true).execute();

        assertThat(processInstance).isWaitingAt("HandleErrorTask").task().hasCandidateGroup("accounting");
        complete(task());

        assertThat(processInstance).isWaitingAt("ChargeCreditTask");
    }

}