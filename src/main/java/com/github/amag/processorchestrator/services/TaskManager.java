package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.config.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.interceptor.TaskInstanceChangeInterceptor;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Service
@Slf4j
public class TaskManager {

    private final TaskInstanceRepository taskInstanceRepository;
    private final StateMachineFactory<TaskInstanceStatus, TaskInstanceEvent> stateMachineFactory;
    private final TaskInstanceChangeInterceptor taskInstanceChangeInterceptor;

    public void startTask(){
        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findTaskInstanceToStart(TaskInstanceStatus.PENDING, ProcessInstanceStatus.INPROGRESS, TaskInstanceStatus.COMPLETED);
        log.debug("Found task instance? {} ",optionalTaskInstance.isPresent());

        optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
            sendTaskInstanceEvent(foundTaskInstance, TaskInstanceEvent.PICKEDUP, TaskInstanceStatus.STARTED);
        }, () ->
                log.debug("Didn't find any pending task instance"));
    }

    private void sendTaskInstanceEvent(final TaskInstance taskInstance, TaskInstanceEvent taskInstanceEvent, TaskInstanceStatus targetStatusEnum ){
        StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine = build(taskInstance);
        Message message
                = MessageBuilder.withPayload(taskInstanceEvent)
                .setHeader(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER,taskInstance.getArangoKey())
                .build();
        stateMachine.sendEvent(message);
        awaitForStatus(UUID.fromString(taskInstance.getArangoKey()), targetStatusEnum);
    }

    private void awaitForStatus(UUID taskInstanceId, TaskInstanceStatus statusEnum) {

        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop Retries exceeded");
            }

            taskInstanceRepository.findById(taskInstanceId).ifPresentOrElse(taskInstance -> {
                if (statusEnum.equals(taskInstance.getStatus())) {
                    found.set(true);
                    log.debug("Instance Found");
                } else {
                    log.debug("Instance Status Not Equal. Expected: " + statusEnum.name() + " Found: " + taskInstance.getStatus().name());
                }
            }, () -> {
                log.debug("Instance Id Not Found");
            });

            if (!found.get()) {
                try {
                    log.debug("Sleeping for retry");
                    Thread.sleep(100);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

    }


    private StateMachine<TaskInstanceStatus, TaskInstanceEvent> build(TaskInstance taskInstance){
        StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine = stateMachineFactory.getStateMachine(UUID.fromString(taskInstance.getArangoKey()));
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
