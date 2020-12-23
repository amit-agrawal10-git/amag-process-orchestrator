package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.ProcessArangoRepository;
import com.github.amag.processorchestrator.smconfig.events.TaskEventManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Service
@Slf4j
public class TaskManager {

    private final ProcessArangoRepository processArangoRepository;
    private final TaskEventManager taskEventManager;

    public static Predicate<ProcessInstance> isProcessInProgress = new Predicate<ProcessInstance>() {
        @Override
        public boolean test(ProcessInstance processInstance) {
            return processInstance.getStatus().equals(ProcessInstanceStatus.INPROGRESS);
        }
    };

    public static Predicate<TaskInstance> isTaskCompleted = new Predicate<TaskInstance>() {
        @Override
        public boolean test(TaskInstance taskInstance) {
            return taskInstance.getStatus().equals(TaskInstanceStatus.COMPLETED);
        }
    };

    public static Predicate<TaskInstance> isTaskReady = new Predicate<TaskInstance>() {
        @Override
        public boolean test(TaskInstance taskInstance) {
            return taskInstance.getStatus().equals(TaskInstanceStatus.READY);
        }
    };

    public static Predicate<TaskInstance> isTaskStarted = new Predicate<TaskInstance>() {
        @Override
        public boolean test(TaskInstance taskInstance) {
            return taskInstance.getStatus().equals(TaskInstanceStatus.STARTED);
        }
    };

    @EventListener(condition = "@taskManager.isTaskCompleted.test(#taskInstanceEvent)")
    public void findAndMarkReadyTaskByTaskInstance(TaskInstance taskInstanceEvent){
        Optional<TaskInstance> optionalTaskInstance =
                processArangoRepository.findTaskInstanceToStart(TaskInstanceStatus.PENDING, ProcessInstanceStatus.INPROGRESS, TaskInstanceStatus.COMPLETED, TaskInstanceEvent.DEPENDENCY_RESOLVED);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            taskEventManager.sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()),TaskInstanceEvent.DEPENDENCY_RESOLVED);
        },() ->  log.debug("Didn't find any available task"));
    }

    @EventListener(condition = "@taskManager.isProcessInProgress.test(#processInstance)")
    public void findAndMarkReadyTaskByProcessInstance(ProcessInstance processInstance){
        Optional<TaskInstance> optionalTaskInstance =
                processArangoRepository.findTaskInstanceToStart(TaskInstanceStatus.PENDING, ProcessInstanceStatus.INPROGRESS, TaskInstanceStatus.COMPLETED, TaskInstanceEvent.DEPENDENCY_RESOLVED);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            taskEventManager.sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()),TaskInstanceEvent.DEPENDENCY_RESOLVED);
        },() ->  log.debug("Didn't find any available task"));
    }

    @EventListener(condition = "@taskManager.isTaskReady.test(#taskInstanceEvent)")
    public void startTask(TaskInstance taskInstanceEvent){
            Optional<TaskInstance> optionalTaskInstance = processArangoRepository.findByStatus(TaskInstanceStatus.READY, TaskInstanceEvent.PICKEDUP);
            optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
                taskEventManager.sendTaskInstanceEvent(UUID.fromString(foundTaskInstance.getArangoKey()), TaskInstanceEvent.PICKEDUP);
            },()-> log.debug("Didn't find any ready task instance"));
    }

    @EventListener(condition = "@taskManager.isTaskStarted.test(#taskInstanceEvent)")
    public void executeTask(TaskInstance taskInstanceEvent){
            Optional<TaskInstance> optionalTaskInstance = processArangoRepository.findByStatus(TaskInstanceStatus.STARTED, TaskInstanceEvent.FINISHED);
            optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
                 taskEventManager.sendTaskInstanceEvent(UUID.fromString(foundTaskInstance.getArangoKey()), TaskInstanceEvent.FINISHED);
            }, ()-> log.debug("Didn't find any started task instance"));
    }

}
