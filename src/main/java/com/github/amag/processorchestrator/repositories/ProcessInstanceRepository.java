package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProcessInstanceRepository extends ArangoRepository<ProcessInstance, UUID> {

    Page<ProcessInstance> findAllByStatusAndIsTemplateFalse(ProcessInstanceStatus status, Pageable pageable);

    Page<ProcessInstance> findAllByProcessTemplate(String processTemplate, Pageable pageable);

    Page<ProcessInstance> findAllByStatusAndProcessTemplate(String status, String processTemplate, Pageable pageable);
}
