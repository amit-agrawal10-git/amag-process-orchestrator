package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessInstanceRepository extends ArangoRepository<ProcessInstance, UUID> {

    Page<ProcessInstance> findAllByStatusAndIsTemplateFalse(ProcessInstanceStatus status, Pageable pageable);
    Page<ProcessInstance> findAllByIsTemplateTrue(Pageable pageable);
    Optional<ProcessInstance> findByProcessAndIsTemplateTrue(String processId);
    Page<ProcessInstance> findAllByProcessTemplate(String processTemplate, Pageable pageable);
    List<ProcessInstance> findAllByProcessTemplate(String processTemplateId);

    Page<ProcessInstance> findAllByStatusAndProcessTemplate(String status, String processTemplate, Pageable pageable);
    Page<ProcessInstance> findAllByIsTemplateFalse(Pageable pageable);
}
