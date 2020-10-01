package com.camunda.training.paymentservice;

import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

public class PaymentProcessTest {

    @Rule
    @ClassRule
    public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

    @Before
    public void setup(){
        init(rule.getProcessEngine());
    }

    @Deployment(resources = "payment-process.bpmn")
    @Test
    public void testHappyPath(){
        ProcessInstance processInstance = runtimeService().startProcessInstanceByMessage("PaymentRequestedMessage");
        assertThat(processInstance).isStarted().isWaitingAt("ChargeCreditTask").externalTask().hasTopicName("credit-charging");
        complete(externalTask(), withVariables("creditSufficient", true));
        assertThat(processInstance).isWaitingAt("PaymentDoneEndEvent").externalTask().hasTopicName("payment-finishing");
        complete(externalTask());
        assertThat(processInstance).isEnded().variables().containsEntry("paymentOK", true);
    }

    public void exampleOfLockingTasks(){
        //Handling a bpmn error to the flow in the charge credit card task
        List<LockedExternalTask> lockedTask = externalTaskService().fetchAndLock(1, "test")
                .topic("credit-card-charging", 10000L).execute();

        lockedTask.forEach(lockedExternalTask -> {
            externalTaskService().handleBpmnError(lockedExternalTask.getId(), "test", "ChargeFailed",
                    "Error message");
        });
    }

    public void exampleOfStartingFromAnotherPoint(){
        //Start process after the charge credit task
        runtimeService().createProcessInstanceModification("PaymentProcess").startAfterActivity("ChargeCreditTask")
                .setVariable("creditSufficient", false).execute();
    }



}
