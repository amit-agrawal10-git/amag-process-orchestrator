package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.interceptor.TaskInstanceChangeInterceptor;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.task.types.SimpleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Service
@Slf4j
public class TaskManager {

    private final TaskInstanceRepository taskInstanceRepository;
    private final StateMachineFactory<TaskInstanceStatus, TaskInstanceEvent> taskInstanceStateMachineFactory;
    private final TaskInstanceChangeInterceptor taskInstanceChangeInterceptor;

    public void findAndMarkReadyTask(){
        List<TaskInstance> taskInstances = taskInstanceRepository.findTaskInstanceToStart(TaskInstanceStatus.PENDING, ProcessInstanceStatus.INPROGRESS, TaskInstanceStatus.COMPLETED);
        if(taskInstances !=null) {
            log.debug("Found task instances? {} ",taskInstances.size());
            for (final TaskInstance taskInstance:taskInstances) {
                taskInstance.setStatus(TaskInstanceStatus.READY);
                taskInstanceRepository.save(taskInstance);
            }
        } else {
            log.debug("Didn't find any ready task");
        }
    }

    public void startTask(){
        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findByStatus(TaskInstanceStatus.READY);
        log.debug("Found task instance? {} ",optionalTaskInstance.isPresent());

        optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
            foundTaskInstance.setStatus(TaskInstanceStatus.STARTED);
            final TaskInstance savedTaskInstance = taskInstanceRepository.save(foundTaskInstance);
                Object object = savedTaskInstance.getTaskTemplate().getBaseAction();
                if (object instanceof SimpleAction) {
                    sendTaskInstanceEvent(UUID.fromString(savedTaskInstance.getArangoKey()), TaskInstanceEvent.FINISHED, TaskInstanceStatus.COMPLETED);
                }
        }, () ->
                log.debug("Didn't find any ready task instance"));
    }

    public void sendTaskInstanceEvent(UUID taskInstanceId, TaskInstanceEvent taskInstanceEvent, TaskInstanceStatus targetStatusEnum ){
        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findById(taskInstanceId);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine = build(taskInstance);
            Message message
                    = MessageBuilder.withPayload(taskInstanceEvent)
                    .setHeader(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER,taskInstance.getArangoKey())
                    .build();
            stateMachine.sendEvent(message);
            awaitForStatus(UUID.fromString(taskInstance.getArangoKey()), targetStatusEnum);
            if(stateMachine.hasStateMachineError()){
                sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()), TaskInstanceEvent.ERROR_OCCURRED, TaskInstanceStatus.FAILED);
            }
        },() -> {
            log.error("Error while sending event");
        });
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
