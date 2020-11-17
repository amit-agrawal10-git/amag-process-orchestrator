package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.events.TaskEventManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class TaskManager {

    private final TaskInstanceRepository taskInstanceRepository;
    private final TaskEventManager taskEventManager;

    public void findAndMarkReadyTask(){
        Optional<TaskInstance> optionalTaskInstance =
                taskInstanceRepository.findTaskInstanceToStart(TaskInstanceStatus.PENDING, ProcessInstanceStatus.INPROGRESS, TaskInstanceStatus.COMPLETED, TaskInstanceEvent.DEPENDENCY_RESOLVED);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            taskEventManager.sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()),TaskInstanceEvent.DEPENDENCY_RESOLVED);
        },() ->  log.debug("Didn't find any available task"));
    }

    public void startTask(){
            Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findByStatus(TaskInstanceStatus.READY, TaskInstanceEvent.PICKEDUP);
            optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
                taskEventManager.sendTaskInstanceEvent(UUID.fromString(foundTaskInstance.getArangoKey()), TaskInstanceEvent.PICKEDUP);
            },()-> log.debug("Didn't find any ready task instance"));
    }

    public void executeTask(){
            Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findByStatus(TaskInstanceStatus.STARTED, TaskInstanceEvent.FINISHED);
            optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
                 taskEventManager.sendTaskInstanceEvent(UUID.fromString(foundTaskInstance.getArangoKey()), TaskInstanceEvent.FINISHED);
            }, ()-> log.debug("Didn't find any started task instance"));
    }

}
