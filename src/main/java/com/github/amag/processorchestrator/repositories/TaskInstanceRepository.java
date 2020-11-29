package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TaskInstanceRepository extends ArangoRepository<TaskInstance, UUID> {

    Page<TaskInstance> findAllByProcessInstance(String processInstanceId, Pageable pageable);
    Page<TaskInstance> findAllByProcessTemplate(String processInstanceId, Pageable pageable);
    List<TaskInstance> findAllByProcessTemplateAndIsTemplateTrue(String processInstanceId);
    List<TaskInstance> findAllByProcessInstanceAndStatusIn(String processInstanceId, Set<TaskInstanceStatus> taskInstanceStatuses);

}
