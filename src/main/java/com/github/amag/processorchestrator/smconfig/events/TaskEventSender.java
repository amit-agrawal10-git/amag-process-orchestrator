package com.github.amag.processorchestrator.smconfig.events;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.services.ProcessManager;
import com.github.amag.processorchestrator.services.TaskManager;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.smconfig.interceptor.TaskInstanceChangeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
@Component
public class TaskEventSender {

    private final ArangoOperations arangoOperations;
    private final StateMachineFactory<TaskInstanceStatus, TaskInstanceEvent> taskInstanceStateMachineFactory;
    private final TaskInstanceChangeInterceptor taskInstanceChangeInterceptor;
    private final ProcessEventSender processEventSender;

    @Async(value = "taskInstExecutor")
    public void sendTaskInstanceEvent(UUID taskInstanceId, TaskInstanceEvent taskInstanceEvent){
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine = build(taskInstance);
            Message message
                    = MessageBuilder.withPayload(taskInstanceEvent)
                    .setHeader(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER,taskInstance.getArangoKey())
                    .build();
            stateMachine.sendEvent(message);

            if(stateMachine.hasStateMachineError()){
                sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()), TaskInstanceEvent.ERROR_OCCURRED);
                processEventSender.sendProcessInstanceEvent(UUID.fromString(taskInstance.getProcessInstance().getArangoKey()), ProcessInstanceEvent.ERROR_OCCURRED);
            }
        },() -> {
            log.error("Error while sending event");
        });
    }

    private StateMachine<TaskInstanceStatus, TaskInstanceEvent> build(TaskInstance taskInstance){
        StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine = taskInstanceStateMachineFactory.getStateMachine(UUID.fromString(taskInstance.getArangoKey()));
        stateMachine.stop();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(
                        sma -> {
                            sma.addStateMachineInterceptor(taskInstanceChangeInterceptor);
                            sma.resetStateMachine(new DefaultStateMachineContext<>(taskInstance.getStatus(), null,null,null));
                        }
                );
        stateMachine.start();
        return stateMachine;
    }

}
