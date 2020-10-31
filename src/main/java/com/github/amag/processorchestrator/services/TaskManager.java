package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class TaskManager {

    private final TaskInstanceRepository taskInstanceRepository;

    public void startTask(){


        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findTaskInstanceToStart(TaskInstanceStatus.PENDING, ProcessInstanceStatus.INPROGRESS, TaskInstanceStatus.COMPLETED);
        log.debug("Found task instance? {} ",optionalTaskInstance.isPresent());

        optionalTaskInstance.ifPresentOrElse(foundTaskInstance -> {
            foundTaskInstance.setStatus(TaskInstanceStatus.INPROGRESS);
            taskInstanceRepository.save(foundTaskInstance);
        }, () ->
                log.debug("Didn't find any pending task instance"));
    }

}
