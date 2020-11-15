package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.smconfig.interceptor.TaskInstanceChangeInterceptor;
import com.github.amag.processorchestrator.task.types.SimpleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Service
@Slf4j
public class TaskManager {

    private final TaskInstanceRepository taskInstanceRepository;
    private final StateMachineFactory<TaskInstanceStatus, TaskInstanceEvent> taskInstanceStateMachineFactory;
    private final TaskInstanceChangeInterceptor taskInstanceChangeInterceptor;
    private final ProcessManager processManager;

    // todo add complete action to clean up old records from map
    private static Map<UUID, Set<TaskInstanceEvent>> SENT_TASK_EVENTS = new HashMap<>();

    @Async
    public void findAndMarkReadyTask(){
        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findTaskInstanceToStart(TaskInstanceStatus.PENDING, ProcessInstanceStatus.INPROGRESS, TaskInstanceStatus.COMPLETED);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()),TaskInstanceEvent.DEPENDENCY_RESOLVED,null);
        },() ->  log.debug("Didn't find any ready task"));
    }

    @Async
    public void startTask(final int maximumActiveTask){
        long activeTaskCount = taskInstanceRepository.countByStatus(TaskInstanceStatus.STARTED);
        if (activeTaskCount < maximumActiveTask) {
            Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findByStatus(TaskInstanceStatus.READY);
            optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
                sendTaskInstanceEvent(UUID.fromString(foundTaskInstance.getArangoKey()), TaskInstanceEvent.PICKEDUP, null);
            },()-> log.debug("Didn't find any ready task instance"));
        } else {
            log.debug("Maximum number of tasks are already running");
        }
    }

    @Async
    public void executeTask(){
            Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findByStatus(TaskInstanceStatus.STARTED);
            optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
                Object object = foundTaskInstance.getTaskTemplate().getBaseAction();
                if (object instanceof SimpleAction) {
                    sendTaskInstanceEvent(UUID.fromString(foundTaskInstance.getArangoKey()), TaskInstanceEvent.FINISHED, null);
                }
            }, ()-> log.debug("Didn't find any started task instance"));
    }

    public void sendTaskInstanceEvent(UUID taskInstanceId, TaskInstanceEvent taskInstanceEvent, TaskInstanceStatus targetStatusEnum ){
        Lock lock = new ReentrantLock();
        lock.lock();
        try {
            Set<TaskInstanceEvent> sentTaskEvents = SENT_TASK_EVENTS.get(taskInstanceId);
            if (sentTaskEvents != null && sentTaskEvents.contains(taskInstanceEvent)) {
                log.debug("Event {} already sent for Task Instance {} ", taskInstanceEvent, taskInstanceId);
                return;
            }
            if (sentTaskEvents == null)
                sentTaskEvents = new HashSet<TaskInstanceEvent>();

            sentTaskEvents.add(taskInstanceEvent);
            SENT_TASK_EVENTS.put(taskInstanceId, sentTaskEvents);
        } finally {
            lock.unlock();
            log.debug("Lock is released");
        }
        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findById(taskInstanceId);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine = build(taskInstance);
            Message message
                    = MessageBuilder.withPayload(taskInstanceEvent)
                    .setHeader(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER,taskInstance.getArangoKey())
                    .build();
            stateMachine.sendEvent(message);
            if(targetStatusEnum!=null)
                awaitForStatus(UUID.fromString(taskInstance.getArangoKey()), targetStatusEnum);
            if(stateMachine.hasStateMachineError()){
                sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()), TaskInstanceEvent.ERROR_OCCURRED, TaskInstanceStatus.FAILED);
                processManager.sendProcessInstanceEvent(UUID.fromString(taskInstance.getProcessInstance().getArangoKey()), ProcessInstanceEvent.ERROR_OCCURRED, ProcessInstanceStatus.FAILED);
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
